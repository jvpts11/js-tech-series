/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.engine.nextgre;

import dev.jstech.computers.api.planner.IExplainNode;
import dev.jstech.computers.api.planner.IPlannerOperator;
import dev.jstech.computers.api.planner.IPlannerRule;
import dev.jstech.computers.api.planner.IPlannerStatistic;
import dev.jstech.computers.api.planner.PlanCandidate;
import dev.jstech.computers.api.planner.PlanStep;
import dev.jstech.computers.crafting.CraftPlanner;
import dev.jstech.computers.crafting.CraftRouting;
import dev.jstech.computers.crafting.CraftingPattern;
import dev.jstech.computers.crafting.ProcessingPattern;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.uuid.NodeUuid;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * NextgreIQL's planner: it makes the plans a craft could run on, reckons what each would take, lets the statement's
 * hints, its own rules and other mods' rules have their say, and keeps the cheapest that was not set aside.
 *
 * <p>The plans it weighs: bench recipes first, the way the network plans; machine recipes first; each other bench
 * recipe of the result first; the raw materials taken from the fastest servers; and one stage at a time. What a plan
 * costs is the time it is reckoned to take, built up the plan's tree from the leaves: a step begins once what it is
 * made of is there, and the steps under it run side by side on as many lanes as the craft may use.
 *
 * <p>Everything it reads is captured on the server's thread first ({@link Inputs}), so a craft's weighing runs on a
 * virtual thread like the network's own planning. It never touches the world.
 */
public final class NextgrePlanner {

    /** The engine's own rules, by the id a Mainframe remembers each under. */
    public static final String WEIGH_MACHINES = "nextgre:weigh_machines";
    public static final String WEIGH_RECIPES = "nextgre:weigh_recipes";
    public static final String MEASURED_TIMES = "nextgre:measured_times";
    public static final String PARALLEL_STEPS = "nextgre:parallel_steps";
    public static final String FASTEST_SOURCES = "nextgre:fastest_sources";

    /** The engine's own rules, in the order the Rules tab lists them. */
    static final List<OwnRule> OWN_RULES = List.of(
            new OwnRule(WEIGH_MACHINES, NextgreTexts.RULE_MACHINES, NextgreTexts.RULE_MACHINES_TELLS),
            new OwnRule(WEIGH_RECIPES, NextgreTexts.RULE_RECIPES, NextgreTexts.RULE_RECIPES_TELLS),
            new OwnRule(MEASURED_TIMES, NextgreTexts.RULE_MEASURED, NextgreTexts.RULE_MEASURED_TELLS),
            new OwnRule(PARALLEL_STEPS, NextgreTexts.RULE_PARALLEL, NextgreTexts.RULE_PARALLEL_TELLS),
            new OwnRule(FASTEST_SOURCES, NextgreTexts.RULE_FASTEST, NextgreTexts.RULE_FASTEST_TELLS));

    /** How many of the result's other bench recipes are weighed, so a thing with twenty recipes stays cheap. */
    private static final int MOST_RECIPES = 4;

    private NextgrePlanner() {
    }

    /** Weighs the plans for what {@code in} asks and chooses one. */
    public static Weighed weigh(final Inputs in) {
        final long started = System.nanoTime();
        final Planned bench = planned(in, in.patterns(), false);
        if (bench.plan().steps().isEmpty()) {
            return new Weighed(List.of(), -1, System.nanoTime() - started,
                    NextgreTexts.NOTHING_MAKES.with(name(in.key())));
        }
        final boolean preferMachine = in.statement().has(NextgreStatement.PREFER_MACHINE);
        final boolean preferBench = in.statement().has(NextgreStatement.PREFER_BENCH);
        final List<Draft> drafts = new ArrayList<>();
        drafts.add(new Draft(NextgreTexts.AS_THE_NETWORK.text(), bench, in.routing(), false));
        Planned machine = null;
        if (in.on(WEIGH_MACHINES) || preferMachine) {
            machine = planned(in, in.patterns(), true);
            if (sameSteps(machine.plan(), bench.plan())) {
                machine = null;
            } else {
                drafts.add(new Draft(NextgreTexts.MACHINES_FIRST.text(), machine, in.routing(), true));
            }
        }
        if (in.on(WEIGH_RECIPES)) {
            final List<CraftingPattern> makers = new ArrayList<>();
            for (final CraftingPattern pattern : in.patterns()) {
                if (StorageKey.of(pattern.result()).equals(in.key())) {
                    makers.add(pattern);
                }
            }
            for (int i = 1; i < makers.size() && i < MOST_RECIPES; i++) {
                final List<CraftingPattern> ordered = new ArrayList<>(in.patterns());
                ordered.remove(makers.get(i));
                ordered.add(0, makers.get(i));
                final Planned other = planned(in, ordered, false);
                if (!sameSteps(other.plan(), bench.plan())) {
                    drafts.add(new Draft(NextgreTexts.WITH_RECIPE.with(i + 1, name(in.key())), other, in.routing(),
                            false));
                }
            }
        }
        final boolean baseIsMachine = preferMachine && machine != null;
        final Planned base = baseIsMachine ? machine : bench;
        if (in.on(FASTEST_SOURCES)) {
            final Set<NodeUuid> fastest = fastestServers(base.plan(), in);
            if (!fastest.isEmpty() && !fastest.equals(in.routing().prefer())) {
                drafts.add(new Draft(NextgreTexts.FASTEST_SOURCES.text(), base,
                        new CraftRouting(fastest, in.routing().avoid(), in.routing().maxParallel()), baseIsMachine));
            }
        }
        if (in.on(PARALLEL_STEPS) && in.lanes() > 1 && base.plan().steps().size() > 1
                && in.routing().maxParallel() != 1) {
            drafts.add(new Draft(NextgreTexts.ONE_AT_A_TIME.text(), base,
                    new CraftRouting(in.routing().prefer(), in.routing().avoid(), 1), baseIsMachine));
        }

        final List<Candidate> candidates = new ArrayList<>();
        int best = -1;
        for (int i = 0; i < drafts.size(); i++) {
            final Draft draft = drafts.get(i);
            final Shape shape = Shape.of(draft.planned().plan());
            final Reckoning reckoning = reckon(draft.planned().plan(), shape, draft.routing(), in);
            Text setAside = setAsideByHints(draft, machine != null, in);
            if (setAside == null && !draft.planned().plan().feasible()) {
                final Map.Entry<StorageKey, Long> lacking = draft.planned().plan().missing().entrySet().iterator()
                        .next();
                setAside = NextgreTexts.CANNOT_BE_MADE.with(lacking.getValue(), name(lacking.getKey()));
            }
            final PlanCandidate view = new PlanCandidate(in.key().registryId(), draft.planned().target(),
                    planSteps(draft.planned().plan(), reckoning), reckoning.total(), hintWords(in));
            for (final IPlannerRule rule : in.rules()) {
                if (in.on(rule.id().toString())) {
                    rule.weigh(view);
                }
            }
            for (final OperatorUse use : in.operators()) {
                use.operator().apply(use.value(), view);
            }
            final List<Text> notes = new ArrayList<>();
            for (final Component note : view.notes()) {
                notes.add(GameText.of(note));
            }
            if (setAside == null && view.isSetAside()) {
                setAside = GameText.of(view.whySetAside());
            }
            if (draft.planned().target() < in.quantity() && draft.planned().target() > 0) {
                notes.add(0, NextgreTexts.PARTIAL.with(draft.planned().target(), in.quantity()));
            }
            final Candidate candidate = new Candidate(i + 1, draft.description(), draft.planned().plan(),
                    draft.routing(), draft.machineFirst(), draft.planned().target(), view.cost(), setAside, notes,
                    reckoning);
            candidates.add(candidate);
            if (setAside == null && (best < 0 || candidate.cost() < candidates.get(best).cost())) {
                best = i;
            }
        }
        final Text said = best < 0 ? NextgreTexts.EVERY_PLAN_SET_ASIDE.with(name(in.key())) : Text.EMPTY;
        return new Weighed(candidates, best, System.nanoTime() - started, said);
    }

    /** The chosen plan's tree as it is shown, each node with the time reckoned for it. */
    static List<NextgrePlanView.Node> explain(final Candidate chosen, final Inputs in,
                                              final List<IExplainNode> contributions) {
        final CraftPlanner.Plan plan = chosen.plan();
        final Shape shape = Shape.of(plan);
        final Reckoning reckoning = chosen.reckoning();
        final List<NextgrePlanView.Node> nodes = new ArrayList<>();
        final int root = plan.steps().size() - 1;
        final List<String> rootHints = new ArrayList<>();
        if (in.statement().has(NextgreStatement.MAX_PARALLEL)) {
            rootHints.add(hint(in, NextgreStatement.MAX_PARALLEL));
        }
        for (final OperatorUse use : in.operators()) {
            rootHints.add(new NextgreStatement.Hint(use.operator().keyword(), use.value()).written());
        }
        final CraftPlanner.Step last = plan.steps().get(root);
        nodes.add(new NextgrePlanView.Node(-1, root, NextgrePlanView.ROOT, stepTitle(last),
                plan.steps().size() == 1 ? NextgreTexts.ONE_STEP.text()
                        : NextgreTexts.IN_PARALLEL.with(plan.steps().size(), reckoning.lanes()), reckoning.total(),
                NextgrePlanView.UNKNOWN, false, rootHints));
        final Set<Integer> shown = new HashSet<>();
        shown.add(root);
        addChildren(nodes, 0, root, plan, shape, reckoning, in, contributions, shown);
        for (final Map.Entry<StorageKey, Long> missing : plan.missing().entrySet()) {
            nodes.add(new NextgrePlanView.Node(0, -1, NextgrePlanView.MISSING,
                    NextgreTexts.MISSING.with(name(missing.getKey()), missing.getValue()),
                    NextgreTexts.HELD_NOWHERE.text(), NextgrePlanView.UNKNOWN, NextgrePlanView.UNKNOWN, false,
                    List.of()));
        }
        return nodes;
    }

    /** What a pull of {@code amount} of {@code key} takes and from where, taking the stores in routing order. */
    static PullPlan pullPlan(final StorageKey key, final long amount, final CraftRouting routing, final Inputs in) {
        final List<Source> ordered = new ArrayList<>(in.sources().getOrDefault(key, List.of()));
        ordered.sort(Comparator.comparingInt((Source s) -> routing.avoid().contains(s.server()) ? 2
                        : routing.prefer().contains(s.server()) ? 0 : 1)
                .thenComparingInt(Source::latency)
                .thenComparing(Comparator.comparingLong(Source::perTick).reversed()));
        long left = amount;
        long ticks = 0L;
        final List<String> names = new ArrayList<>();
        boolean preferredUsed = false;
        boolean avoidedHolds = false;
        for (final Source source : ordered) {
            if (routing.avoid().contains(source.server())) {
                avoidedHolds = true;
            }
            if (left <= 0L || source.quantity() <= 0L) {
                continue;
            }
            final long take = Math.min(left, source.quantity());
            ticks += NextgreCosts.pull(take, source.perTick(), source.latency());
            names.add(source.name());
            preferredUsed |= routing.prefer().contains(source.server());
            left -= take;
        }
        if (left > 0L) {
            ticks += NextgreCosts.pull(left, NextgreCosts.DEFAULT_ITEMS_PER_TICK, 0);
        }
        return new PullPlan(ticks, names, preferredUsed, avoidedHolds);
    }

    /** The plan for {@code in}'s request, scaled down to what can be made when a partial craft is allowed. */
    private static Planned planned(final Inputs in, final List<CraftingPattern> patterns, final boolean machineFirst) {
        CraftPlanner.Plan plan = CraftPlanner.plan(in.key(), in.quantity(), patterns, in.machines(), in.stock(),
                machineFirst);
        long target = in.quantity();
        if (!plan.feasible() && in.partial() && !plan.steps().isEmpty()) {
            final long most = CraftPlanner.maxFeasible(in.key(), in.quantity(), patterns, in.machines(), in.stock(),
                    machineFirst);
            if (most > 0) {
                target = most;
                plan = CraftPlanner.plan(in.key(), most, patterns, in.machines(), in.stock(), machineFirst);
            }
        }
        return new Planned(plan, target);
    }

    /* Why the statement's hints set this plan aside, or null when they leave it be. */
    @Nullable
    private static Text setAsideByHints(final Draft draft, final boolean machineAlternative, final Inputs in) {
        if (in.statement().has(NextgreStatement.PREFER_MACHINE) && machineAlternative && !draft.machineFirst()) {
            return NextgreTexts.SET_ASIDE_BY.with(NextgreStatement.PREFER_MACHINE);
        }
        if (in.statement().has(NextgreStatement.PREFER_BENCH) && draft.machineFirst()) {
            return NextgreTexts.SET_ASIDE_BY.with(NextgreStatement.PREFER_BENCH);
        }
        if (in.statement().has(NextgreStatement.PREFER_SOURCE) && !draft.routing().prefer()
                .equals(in.routing().prefer())) {
            return NextgreTexts.SET_ASIDE_BY.with(NextgreStatement.PREFER_SOURCE);
        }
        return null;
    }

    /** What every step of {@code plan} is reckoned to take, alone and with what is under it. */
    private static Reckoning reckon(final CraftPlanner.Plan plan, final Shape shape, final CraftRouting routing,
                                    final Inputs in) {
        final int n = plan.steps().size();
        final long[] own = new long[n];
        final long[] whole = new long[n];
        final int lanes = in.on(PARALLEL_STEPS) ? routing.capped(in.lanes()) : 1;
        final List<List<Long>> pullTimes = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            own[i] = ownTime(plan.steps().get(i), in);
            final List<Long> under = new ArrayList<>();
            for (final int child : shape.children().get(i)) {
                under.add(whole[child]);
            }
            final List<Long> pulls = new ArrayList<>();
            for (final Shape.Pull pull : shape.pulls().get(i)) {
                final long ticks = pullPlan(pull.key(), pull.amount(), routing, in).ticks();
                pulls.add(ticks);
                under.add(ticks);
            }
            pullTimes.add(pulls);
            whole[i] = NextgreCosts.node(own[i], under, lanes);
        }
        return new Reckoning(own, whole, pullTimes, lanes, routing, n == 0 ? 0L : whole[n - 1]);
    }

    /* What a step takes on its own: what another mod measured, what this network measured, or a fair guess. */
    private static long ownTime(final CraftPlanner.Step step, final Inputs in) {
        final StorageKey made = step.resultKey();
        final String id = made == null ? "" : made.id();
        final long guess = step.isMachine()
                ? NextgreCosts.machine(step.runs(), NextgreCosts.DEFAULT_MACHINE_TICKS, 1)
                : NextgreCosts.bench(step.runs(), step.unitsPerRun(), in.benchSpeed());
        long reckoned = guess;
        final Long measured = in.on(MEASURED_TIMES) ? in.perRun().get(NextgreState.timingKey(id, step.isMachine()))
                : null;
        if (measured != null && measured > 0L) {
            reckoned = measured * step.runs();
        }
        if (made != null) {
            final PlanStep view = new PlanStep(made.registryId(), step.runs(), step.produced(), step.isMachine(),
                    reckoned);
            for (final IPlannerStatistic statistic : in.statistics()) {
                final long estimate = statistic.estimate(view);
                if (estimate >= 0L) {
                    return estimate;
                }
            }
        }
        return reckoned;
    }

    private static void addChildren(final List<NextgrePlanView.Node> nodes, final int parentNode, final int step,
                                    final CraftPlanner.Plan plan, final Shape shape, final Reckoning reckoning,
                                    final Inputs in, final List<IExplainNode> contributions,
                                    final Set<Integer> shown) {
        final CraftPlanner.Step at = plan.steps().get(step);
        final StorageKey made = at.resultKey();
        if (made != null) {
            final PlanStep view = new PlanStep(made.registryId(), at.runs(), at.produced(), at.isMachine(),
                    reckoning.whole()[step]);
            for (final IExplainNode contribution : contributions) {
                for (final Component note : contribution.notes(view)) {
                    if (nodes.size() < NextgrePlanView.MAX_NODES) {
                        nodes.add(new NextgrePlanView.Node(parentNode, -1, NextgrePlanView.NOTE, GameText.of(note),
                                Text.EMPTY, NextgrePlanView.UNKNOWN, NextgrePlanView.UNKNOWN, false, List.of()));
                    }
                }
            }
        }
        final List<Shape.Pull> pulls = shape.pulls().get(step);
        for (int p = 0; p < pulls.size() && nodes.size() < NextgrePlanView.MAX_NODES; p++) {
            final Shape.Pull pull = pulls.get(p);
            final PullPlan from = pullPlan(pull.key(), pull.amount(), reckoning.routing(), in);
            final List<String> hints = new ArrayList<>();
            if (from.preferredUsed() && in.statement().has(NextgreStatement.PREFER_SOURCE)) {
                hints.add(hint(in, NextgreStatement.PREFER_SOURCE));
            }
            if (from.avoidedHolds() && in.statement().has(NextgreStatement.AVOID_SOURCE)) {
                hints.add(hint(in, NextgreStatement.AVOID_SOURCE));
            }
            nodes.add(new NextgrePlanView.Node(parentNode, -1, NextgrePlanView.PULL,
                    NextgreTexts.PULL.with(name(pull.key()), pull.amount()),
                    NextgreTexts.FROM.with(String.join(", ", from.servers())), reckoning.pulls().get(step).get(p),
                    NextgrePlanView.UNKNOWN, false, hints));
        }
        for (final int child : shape.children().get(step)) {
            if (!shown.add(child) || nodes.size() >= NextgrePlanView.MAX_NODES) {
                continue;
            }
            final CraftPlanner.Step under = plan.steps().get(child);
            final List<String> hints = new ArrayList<>();
            if (bothMakeIt(under.resultKey(), in)) {
                if (in.statement().has(NextgreStatement.PREFER_MACHINE)) {
                    hints.add(NextgreStatement.PREFER_MACHINE);
                } else if (in.statement().has(NextgreStatement.PREFER_BENCH)) {
                    hints.add(NextgreStatement.PREFER_BENCH);
                }
            }
            nodes.add(new NextgrePlanView.Node(parentNode, child,
                    under.isMachine() ? NextgrePlanView.MACHINE : NextgrePlanView.BENCH, stepTitle(under),
                    (under.isMachine() ? NextgreTexts.ON_A_MACHINE : NextgreTexts.AT_A_BENCH).with(under.runs()),
                    reckoning.whole()[child], NextgrePlanView.UNKNOWN, false, hints));
            addChildren(nodes, nodes.size() - 1, child, plan, shape, reckoning, in, contributions, shown);
        }
    }

    private static Text stepTitle(final CraftPlanner.Step step) {
        return (step.isMachine() ? NextgreTexts.PROCESS : NextgreTexts.CRAFT).with(step.resultText(),
                step.produced());
    }

    /* Whether both a bench recipe and a machine recipe on the network make {@code key}. */
    private static boolean bothMakeIt(@Nullable final StorageKey key, final Inputs in) {
        if (key == null) {
            return false;
        }
        boolean bench = false;
        for (final CraftingPattern pattern : in.patterns()) {
            bench |= StorageKey.of(pattern.result()).equals(key);
        }
        boolean machine = false;
        for (final ProcessingPattern pattern : in.machines()) {
            machine |= pattern.primaryOutput() != null && key.equals(pattern.primaryOutput().key());
        }
        return bench && machine;
    }

    /* For every raw material of the plan, the server that would hand it over fastest. */
    private static Set<NodeUuid> fastestServers(final CraftPlanner.Plan plan, final Inputs in) {
        final Set<NodeUuid> out = new LinkedHashSet<>();
        for (final Map.Entry<StorageKey, Long> raw : plan.rawConsumption().entrySet()) {
            Source best = null;
            long bestTicks = Long.MAX_VALUE;
            for (final Source source : in.sources().getOrDefault(raw.getKey(), List.of())) {
                if (source.quantity() < raw.getValue() || in.routing().avoid().contains(source.server())) {
                    continue;
                }
                final long ticks = NextgreCosts.pull(raw.getValue(), source.perTick(), source.latency());
                if (ticks < bestTicks) {
                    best = source;
                    bestTicks = ticks;
                }
            }
            if (best != null) {
                out.add(best.server());
            }
        }
        return out;
    }

    private static List<PlanStep> planSteps(final CraftPlanner.Plan plan, final Reckoning reckoning) {
        final List<PlanStep> out = new ArrayList<>();
        for (int i = 0; i < plan.steps().size(); i++) {
            final CraftPlanner.Step step = plan.steps().get(i);
            final StorageKey made = step.resultKey();
            if (made != null) {
                out.add(new PlanStep(made.registryId(), step.runs(), step.produced(), step.isMachine(),
                        reckoning.whole()[i]));
            }
        }
        return out;
    }

    private static Set<String> hintWords(final Inputs in) {
        final Set<String> out = new LinkedHashSet<>();
        for (final NextgreStatement.Hint hint : in.statement().hints()) {
            out.add(hint.keyword());
        }
        return out;
    }

    private static String hint(final Inputs in, final String keyword) {
        return new NextgreStatement.Hint(keyword, in.statement().value(keyword)).written();
    }

    private static boolean sameSteps(final CraftPlanner.Plan a, final CraftPlanner.Plan b) {
        return a.steps().equals(b.steps());
    }

    private static Text name(final StorageKey key) {
        return GameText.of(key.displayName());
    }

    /**
     * Everything a plan is weighed from, captured on the server's thread.
     *
     * @param key        what is asked for
     * @param quantity   how many
     * @param partial    whether less may be made when not all of it can
     * @param patterns   the bench patterns, every cell settled to an item
     * @param machines   the machine patterns
     * @param stock      what the network holds
     * @param sources    where each raw material the patterns use can come from
     * @param benchSpeed the work the running crafting computers do a tick, all together
     * @param lanes      how many stages the network may run at once
     * @param perRun     the measured ticks a run of each recipe takes, by {@link NextgreState#timingKey}
     * @param off        the rules switched off
     * @param statement  the statement, with its hints
     * @param routing    what the hints say about sources and stages at once
     * @param rules      other mods' rules
     * @param operators  other mods' hints the statement uses, with their values
     * @param statistics other mods' statistics
     */
    public record Inputs(StorageKey key, long quantity, boolean partial, List<CraftingPattern> patterns,
                         List<ProcessingPattern> machines, Map<StorageKey, Long> stock,
                         Map<StorageKey, List<Source>> sources, long benchSpeed, int lanes, Map<String, Long> perRun,
                         Set<String> off, NextgreStatement statement, CraftRouting routing, List<IPlannerRule> rules,
                         List<OperatorUse> operators, List<IPlannerStatistic> statistics) {

        /** Whether rule {@code id} is switched on. */
        public boolean on(final String id) {
            return !off.contains(id);
        }
    }

    /**
     * A store a raw material can come from.
     *
     * @param server   the server
     * @param name     its name, as a plan shows it
     * @param quantity how much of the material it holds
     * @param latency  the ticks it takes to answer
     * @param perTick  the items it, and the cable to it, carry a tick
     */
    public record Source(NodeUuid server, String name, long quantity, int latency, long perTick) {
    }

    /** Another mod's hint as a statement used it. */
    public record OperatorUse(IPlannerOperator operator, String value) {
    }

    /** One of the engine's own rules: its id, its name and what it does. */
    record OwnRule(String id, TextKey name, TextKey tells) {
    }

    /**
     * A plan weighed.
     *
     * @param number       its number among the plans weighed, from one
     * @param description  how it differs from the others
     * @param plan         the steps
     * @param routing      where its raw materials come from and how much runs at once
     * @param machineFirst whether machine recipes went before bench recipes
     * @param target       how many it makes
     * @param cost         what it was reckoned to take, in ticks, with what the rules and hints added
     * @param setAside     why it was set aside, or null when it was not
     * @param notes        why its cost changed
     * @param reckoning    the time reckoned for each of its steps
     */
    public record Candidate(int number, Text description, CraftPlanner.Plan plan, CraftRouting routing,
                            boolean machineFirst, long target, long cost, @Nullable Text setAside, List<Text> notes,
                            Reckoning reckoning) {

        /** The plan as the list of plans weighed shows it. */
        NextgrePlanView.Alternative alternative(final boolean chosen) {
            return new NextgrePlanView.Alternative(number, description, cost,
                    setAside == null ? Text.EMPTY : setAside, chosen, notes);
        }
    }

    /**
     * What a weighing came to.
     *
     * @param candidates every plan weighed
     * @param chosen     the chosen one's place in the list, or -1 when none could be
     * @param nanos      how long it took
     * @param said       why none was chosen, or empty
     */
    public record Weighed(List<Candidate> candidates, int chosen, long nanos, Text said) {

        /** The chosen plan, or null when none could be. */
        @Nullable
        public Candidate best() {
            return chosen < 0 ? null : candidates.get(chosen);
        }

        /** The plans as the list of plans weighed shows them. */
        List<NextgrePlanView.Alternative> alternatives() {
            final List<NextgrePlanView.Alternative> out = new ArrayList<>();
            for (int i = 0; i < candidates.size(); i++) {
                out.add(candidates.get(i).alternative(i == chosen));
            }
            return out;
        }
    }

    /**
     * The time reckoned for a plan.
     *
     * @param own     each step's own time, in ticks
     * @param whole   each step's time with what is under it
     * @param pulls   each step's pulls' times, in the order of its pulls
     * @param lanes   how many stages were reckoned to run at once
     * @param routing the routing it was reckoned with
     * @param total   the whole plan's
     */
    public record Reckoning(long[] own, long[] whole, List<List<Long>> pulls, int lanes, CraftRouting routing,
                            long total) {
    }

    /**
     * Where a pull's items come from, and what it takes.
     *
     * @param ticks         what it takes
     * @param servers       the servers' names, in the order they are drawn on
     * @param preferredUsed whether a server the hints prefer was drawn on
     * @param avoidedHolds  whether a server the hints avoid holds some of it
     */
    public record PullPlan(long ticks, List<String> servers, boolean preferredUsed, boolean avoidedHolds) {
    }

    /** A plan scaled to what it makes. */
    private record Planned(CraftPlanner.Plan plan, long target) {
    }

    /** A plan to weigh, before its cost is reckoned. */
    private record Draft(Text description, Planned planned, CraftRouting routing, boolean machineFirst) {
    }

    /**
     * A plan's steps as a tree: the steps each one is made from, and the raw materials it draws.
     *
     * @param children the earlier steps each step uses, by place in the plan
     * @param pulls    the raw materials each step draws from the servers
     */
    record Shape(List<List<Integer>> children, List<List<Pull>> pulls) {

        static Shape of(final CraftPlanner.Plan plan) {
            final List<CraftPlanner.Step> steps = plan.steps();
            final Map<StorageKey, Long> rawLeft = new HashMap<>(plan.rawConsumption());
            final List<List<Integer>> children = new ArrayList<>();
            final List<List<Pull>> pulls = new ArrayList<>();
            for (int i = 0; i < steps.size(); i++) {
                final CraftPlanner.Step step = steps.get(i);
                final Map<StorageKey, Long> ingredients = step.isMachine() ? step.machine().ingredientTotals()
                        : step.pattern().ingredientTotals();
                final List<Integer> under = new ArrayList<>();
                final List<Pull> drawn = new ArrayList<>();
                for (final Map.Entry<StorageKey, Long> ingredient : new LinkedHashMap<>(ingredients).entrySet()) {
                    final long need = ingredient.getValue() * step.runs();
                    // What the network holds goes first, as the planner takes it; the rest was made by a step before.
                    final long held = Math.min(need, rawLeft.getOrDefault(ingredient.getKey(), 0L));
                    if (held > 0L) {
                        drawn.add(new Pull(ingredient.getKey(), held));
                        rawLeft.merge(ingredient.getKey(), -held, Long::sum);
                    }
                    if (need > held) {
                        for (int j = i - 1; j >= 0; j--) {
                            if (ingredient.getKey().equals(steps.get(j).resultKey())) {
                                if (!under.contains(j)) {
                                    under.add(j);
                                }
                                break;
                            }
                        }
                    }
                }
                children.add(under);
                pulls.add(drawn);
            }
            return new Shape(children, pulls);
        }

        /** A raw material drawn by a step. */
        record Pull(StorageKey key, long amount) {
        }
    }
}
