/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.engine.nextgre;

import dev.jstech.computers.advancement.Acting;
import dev.jstech.computers.api.planner.IPlannerOperator;
import dev.jstech.computers.api.planner.IPlannerRule;
import dev.jstech.computers.api.planner.IPlannerStatistic;
import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.crafting.CraftPlanner;
import dev.jstech.computers.crafting.CraftPlanning;
import dev.jstech.computers.crafting.CraftRouting;
import dev.jstech.computers.crafting.CraftingPattern;
import dev.jstech.computers.crafting.NetworkCraftOperation;
import dev.jstech.computers.crafting.ProcessingPattern;
import dev.jstech.computers.engine.CraftRequest;
import dev.jstech.computers.engine.EngineDef;
import dev.jstech.computers.engine.ICraftPlanning;
import dev.jstech.computers.engine.INetworkEngine;
import dev.jstech.computers.engine.NetworkEngines;
import dev.jstech.computers.operation.INetworkOperation;
import dev.jstech.computers.operation.NetworkIndex;
import dev.jstech.computers.operation.index.ItemLocation;
import dev.jstech.computers.operation.payload.OperationRecord;
import dev.jstech.computers.operation.payload.network.NetworkLookup;
import dev.jstech.computers.program.IqlEngine;
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.computers.program.iql.IIqlView;
import dev.jstech.computers.program.iql.IqlOperation;
import dev.jstech.computers.program.iql.IqlParseResult;
import dev.jstech.computers.program.iql.IqlParser;
import dev.jstech.computers.program.iql.IqlTable;
import dev.jstech.computers.program.iql.IqlVerb;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.network.NetworkSystem;
import dev.jstech.core.network.ServerNode;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.util.Loaded;
import dev.jstech.core.uuid.NodeUuid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * NextgreIQL, the explicit engine: it shows exactly how it built a plan and lets the player take part in it.
 *
 * <p>Its dialect is the network's language with three things more. {@code EXPLAIN} before a CRAFT or a SELECT shows
 * the plan without running it: the plans weighed with their costs, and the chosen one as a tree, each step with the
 * time reckoned for it. {@code EXPLAIN ANALYZE} runs it as well, and the plan fills in with what each step took as
 * the craft goes. Hints after a CRAFT change the plan: {@code PREFER SOURCE}, {@code AVOID SOURCE},
 * {@code MAX PARALLEL}, {@code PREFER MACHINE}, {@code PREFER BENCH}, and whatever other mods add. Everything else
 * is the language's core, which every engine answers.
 *
 * <p>Every craft it is asked for, from any window or bus, it plans by weighing (see {@link NextgrePlanner}), and it
 * measures how each one ran, so its next plans reckon with this network's own times. Those times, its rules and the
 * plans it made lately are kept on the Mainframe.
 */
public final class NextgreEngine implements INetworkEngine {

    /** Each Mainframe's state while it is loaded, read from its save the first time it is asked for. */
    private final Map<MainframeBlockEntity, NextgreState> states = new WeakHashMap<>();

    /** What a store is taken to carry a tick when nothing on the way to it limits it. */
    private static final long UNLIMITED_PER_TICK = 1024L;
    private static final String NAME_COLUMN = "step";
    private static final List<String> COLUMNS = List.of(NAME_COLUMN, "where", "estimate", "actual", "hints");
    private static final String INDENT = "  ";
    private static final String NOBODY = "NextgreIQL";

    @Override
    public EngineDef def() {
        return NetworkEngines.NEXTGRE_IQL;
    }

    @Override
    public IqlEngine.Outcome query(final MainframeBlockEntity core, final IIqlView caller, final String statement,
                                   final int rowLimit) {
        final NextgreStatement parsed;
        try {
            parsed = NextgreStatement.parse(statement, NextgreExtensions.dialectOperators());
        } catch (final IllegalArgumentException e) {
            return fail(NextgreTexts.EXPLAIN_NEEDS.text());
        }
        if (!parsed.explain() && parsed.hints().isEmpty()) {
            final IqlEngine.Outcome outcome = new IqlEngine(core, caller, rowLimit, false).run(statement);
            if (outcome.ok() && isAnalyze(statement)) {
                return new IqlEngine.Outcome(true, analyze(core), outcome.rows(), outcome.table());
            }
            return outcome;
        }
        final IqlParseResult inner = IqlParser.tryParse(parsed.core());
        if (!inner.ok()) {
            return new IqlEngine(core, caller, rowLimit, false).run(parsed.core());
        }
        final IqlOperation operation = inner.isDefinition() || inner.isBus() || inner.isCrafting()
                || inner.isRedstone() ? null : inner.operation();
        if (operation != null && operation.verb() == IqlVerb.CRAFT) {
            return craftStatement(core, statement, parsed, operation);
        }
        if (parsed.explain() && operation != null && operation.verb() == IqlVerb.SELECT && parsed.hints().isEmpty()) {
            return explainSelect(core, statement, operation);
        }
        return fail((parsed.explain() ? NextgreTexts.EXPLAINS_ONLY : NextgreTexts.HINTS_ON_CRAFT).text());
    }

    @Override
    @Nullable
    public INetworkOperation craft(final MainframeBlockEntity core, final CraftRequest request) {
        // A recipe the player picked, or a pipeline the asker wants, is the player's plan, not the engine's.
        if (request.recipe() != CraftRequest.ANY_RECIPE
                || request.preferMultiStage() && core.hasMultiStageRecipe(request.key())) {
            return INetworkEngine.super.craft(core, request);
        }
        final String statement = "CRAFT " + request.demand() + " " + request.key().id();
        final Gathered gathered = gather(core, request.key(), request.demand(), request.partial(),
                new NextgreStatement(false, false, statement, List.of()));
        if (gathered.inputs() == null) {
            return INetworkEngine.super.craft(core, request);
        }
        final NextgrePlanner.Inputs in = gathered.inputs();
        final AtomicReference<NextgrePlanner.Weighed> weighed = new AtomicReference<>();
        final long at = core.getLevel() == null ? 0L : core.getLevel().getGameTime();
        final var pending = core.submitEnginePlannedCraft(request.key(), request.demand(), request.partial(),
                request.label(), () -> {
                    final NextgrePlanner.Weighed w = NextgrePlanner.weigh(in);
                    weighed.set(w);
                    final NextgrePlanner.Candidate best = w.best();
                    return best == null ? new CraftPlanning.Routed(null, CraftRouting.NONE)
                            : new CraftPlanning.Routed(new CraftPlanning.Planned(best.plan(), best.target()),
                                    best.routing());
                }, craft -> {
                    final NextgrePlanner.Weighed w = weighed.get();
                    if (w != null) {
                        remember(core, statement, false, at, w, in, craft, Text.EMPTY);
                    }
                });
        if (pending != null && request.onSettle() != null) {
            pending.onSettle(request.onSettle());
        }
        return pending;
    }

    @Override
    public ICraftPlanning planner(final MainframeBlockEntity core) {
        return new ICraftPlanning() {
            /*
             * What a weighing captures from the world does not depend on the item asked for, and a catalog asks
             * for one plan per pattern: it is gathered once, on the first ask, and shared by the asks that follow.
             */
            private Gathered gathered;

            @Override
            public CraftPlanner.Plan plan(final StorageKey key, final long quantity,
                                          final List<CraftingPattern> patterns,
                                          final List<ProcessingPattern> machines, final Map<StorageKey, Long> stock) {
                final NextgrePlanner.Candidate best = previewOf(gathered(key, quantity, false), key, quantity, false,
                        patterns, machines, stock);
                return best != null ? best.plan()
                        : ICraftPlanning.REFERENCE.plan(key, quantity, patterns, machines, stock);
            }

            @Override
            public long maxFeasible(final StorageKey key, final long quantity, final List<CraftingPattern> patterns,
                                    final List<ProcessingPattern> machines, final Map<StorageKey, Long> stock) {
                final NextgrePlanner.Candidate best = previewOf(gathered(key, quantity, true), key, quantity, true,
                        patterns, machines, stock);
                return best != null ? best.target()
                        : ICraftPlanning.REFERENCE.maxFeasible(key, quantity, patterns, machines, stock);
            }

            private Gathered gathered(final StorageKey key, final long quantity, final boolean partial) {
                if (gathered == null) {
                    gathered = gather(core, key, quantity, partial,
                            new NextgreStatement(false, false, "", List.of()));
                }
                return gathered;
            }
        };
    }

    /** The plans this engine made lately on {@code core}, the newest first, each where it stands now. */
    public List<NextgrePlanView> history(final MainframeBlockEntity core) {
        final NextgreState state = state(core);
        final List<NextgrePlanView> out = new ArrayList<>();
        for (final NextgrePlanView plan : state.history()) {
            out.add(live(core, state, plan));
        }
        return out;
    }

    /** The plan numbered {@code id}, where it stands now, or null when it is not kept. */
    @Nullable
    public NextgrePlanView plan(final MainframeBlockEntity core, final int id) {
        final NextgreState state = state(core);
        final NextgrePlanView plan = state.plan(id);
        return plan == null ? null : live(core, state, plan);
    }

    /** Gathers the statistics afresh: what the network holds and where. Says what it gathered. */
    public Text analyze(final MainframeBlockEntity core) {
        final NextgreState state = state(core);
        final int servers = core.getLevel() instanceof ServerLevel level && core.networkUuid() != null
                ? NetworkSystem.get(level).serversOf(core.networkUuid()).size() : 0;
        state.analyzed(core.getLevel() == null ? 0L : core.getLevel().getGameTime(),
                core.networkIndex().snapshot().size(), servers);
        save(core, state);
        return NextgreTexts.ANALYZED.with(state.itemTypes(), state.servers(), state.timings().size());
    }

    /**
     * Every rule of the planner, the engine's own first, each with whether it is on for {@code core}; then the hints
     * its dialect takes, its own and other mods'.
     */
    public List<RuleRow> rules(final MainframeBlockEntity core) {
        final NextgreState state = state(core);
        final List<RuleRow> out = new ArrayList<>();
        for (final NextgrePlanner.OwnRule rule : NextgrePlanner.OWN_RULES) {
            out.add(new RuleRow(rule.id(), rule.name().text(), rule.tells().text(), state.on(rule.id()),
                    RuleRow.OWN_RULE));
        }
        for (final IPlannerRule rule : NextgreExtensions.rules()) {
            out.add(new RuleRow(rule.id().toString(), GameText.of(rule.name()), GameText.of(rule.description()),
                    state.on(rule.id().toString()), RuleRow.ADDED_RULE));
        }
        for (final NextgreStatement.Operator hint : NextgreStatement.BUILT_IN) {
            out.add(new RuleRow(hint.keyword(), Text.literal(hint.keyword()), NextgreTexts.hintTells(hint.keyword()),
                    true, RuleRow.OWN_HINT));
        }
        for (final IPlannerOperator operator : NextgreExtensions.operators()) {
            out.add(new RuleRow(operator.id().toString(), Text.literal(operator.keyword()),
                    GameText.of(operator.description()), true, RuleRow.ADDED_HINT));
        }
        return out;
    }

    /** Switches rule {@code id} the other way on {@code core}; false when there is no such rule. */
    public boolean toggleRule(final MainframeBlockEntity core, final String id) {
        boolean known = false;
        for (final NextgrePlanner.OwnRule rule : NextgrePlanner.OWN_RULES) {
            known |= rule.id().equals(id);
        }
        for (final IPlannerRule rule : NextgreExtensions.rules()) {
            known |= rule.id().toString().equals(id);
        }
        if (!known) {
            return false;
        }
        final NextgreState state = state(core);
        state.toggle(id);
        save(core, state);
        return true;
    }

    /** What the planner reckons with, as the Statistics tab lists it. */
    public List<StatRow> statistics(final MainframeBlockEntity core) {
        final NextgreState state = state(core);
        final List<StatRow> out = new ArrayList<>();
        final long now = core.getLevel() == null ? 0L : core.getLevel().getGameTime();
        out.add(new StatRow(NextgreTexts.STAT_ITEM_TYPES.text(), Text.literal(Integer.toString(state.itemTypes()))));
        out.add(new StatRow(NextgreTexts.STAT_SERVERS.text(), Text.literal(Integer.toString(state.servers()))));
        out.add(new StatRow(NextgreTexts.STAT_TIMED.text(), Text.literal(Integer.toString(state.timings().size()))));
        int measured = 0;
        for (final NextgreState.Timing timing : state.timings().values()) {
            measured += timing.samples();
        }
        out.add(new StatRow(NextgreTexts.STAT_MEASURED.text(), Text.literal(Integer.toString(measured))));
        out.add(new StatRow(NextgreTexts.STAT_ANALYZED.text(), state.analyzedAt() < 0 ? NextgreTexts.STAT_NEVER.text()
                : NextgreTexts.STAT_MINUTES_AGO.with(Math.max(0L, now - state.analyzedAt()) / 1200L)));
        state.timings().forEach((key, timing) -> {
            final boolean machine = key.startsWith("machine:");
            final StorageKey item = StorageKey.byId(key.substring(key.indexOf(':') + 1));
            final Text name = item == null ? Text.literal(key) : GameText.of(item.displayName());
            out.add(new StatRow((machine ? NextgreTexts.STAT_ON_A_MACHINE : NextgreTexts.STAT_AT_A_BENCH).with(name),
                    NextgreTexts.STAT_PER_RUN.with(timing.perRun(), timing.samples())));
        });
        if (core.getLevel() instanceof ServerLevel level) {
            for (final IPlannerStatistic statistic : NextgreExtensions.statistics()) {
                out.add(new StatRow(GameText.of(statistic.name()),
                        GameText.of(statistic.read(level, core.getBlockPos()))));
            }
        }
        return out;
    }

    /* A CRAFT with its hints, explained, run, or both. */
    private IqlEngine.Outcome craftStatement(final MainframeBlockEntity core, final String statement,
                                             final NextgreStatement parsed, final IqlOperation operation) {
        final StorageKey key = StorageKey.byName(operation.item());
        if (key == null) {
            return fail(NextgreTexts.UNKNOWN_ITEM.with(operation.item()));
        }
        final long quantity = Math.max(1L, operation.quantity());
        final Gathered gathered = gather(core, key, quantity, true, parsed);
        if (gathered.inputs() == null) {
            return fail(gathered.error());
        }
        final NextgrePlanner.Inputs in = gathered.inputs();
        final NextgrePlanner.Weighed weighed = NextgrePlanner.weigh(in);
        final NextgrePlanner.Candidate best = weighed.best();
        final long at = core.getLevel() == null ? 0L : core.getLevel().getGameTime();
        if (best == null) {
            final NextgrePlanView view = remember(core, statement, parsed.analyze(), at, weighed, in, null,
                    weighed.said());
            return new IqlEngine.Outcome(false, weighed.said(), rows(view), table(view));
        }
        final Text explained = NextgreTexts.EXPLAINED.with(best.number(), weighed.candidates().size(), best.cost(),
                Math.max(0L, weighed.nanos() / 1_000_000L));
        if (parsed.explain() && !parsed.analyze()) {
            final NextgrePlanView view = remember(core, statement, false, at, weighed, in, null, explained);
            return new IqlEngine.Outcome(true, explained, rows(view), table(view));
        }
        final NetworkCraftOperation craft = core.submitPlannedCraft(key, best.target(), best.plan(), label(core),
                null, best.routing());
        if (craft == null) {
            return fail(NextgreTexts.NOT_STARTED.with(GameText.of(key.displayName())));
        }
        core.notePlanned();
        final String shortId = craft.operationId().toString().substring(0, 8);
        final Text said = parsed.explain()
                ? NextgreTexts.ANALYZING.with(best.number(), weighed.candidates().size(), shortId)
                : NextgreTexts.CRAFT_STARTED.with(best.target(), GameText.of(key.displayName()), shortId,
                        best.number(), weighed.candidates().size());
        final NextgrePlanView view = remember(core, statement, parsed.analyze(), at, weighed, in, craft, said);
        return parsed.explain() ? new IqlEngine.Outcome(true, said, rows(view), table(view))
                : new IqlEngine.Outcome(true, said, List.of());
    }

    /* EXPLAIN SELECT: which servers would hand the items over, and what that takes. */
    private IqlEngine.Outcome explainSelect(final MainframeBlockEntity core, final String statement,
                                            final IqlOperation operation) {
        final StorageKey key = StorageKey.byName(operation.item());
        if (key == null) {
            return fail(NextgreTexts.UNKNOWN_ITEM.with(operation.item()));
        }
        final long quantity = operation.quantity() > 0 ? operation.quantity() : 1L;
        final Gathered gathered = gather(core, key, quantity, true,
                new NextgreStatement(true, false, statement, List.of()));
        if (gathered.inputs() == null) {
            return fail(gathered.error());
        }
        final NextgrePlanner.Inputs in = withSourcesOf(core, gathered.inputs(), key);
        final NextgrePlanner.PullPlan seek = NextgrePlanner.pullPlan(key, quantity, CraftRouting.NONE, in);
        final List<NextgrePlanView.Node> nodes = new ArrayList<>();
        final Text name = GameText.of(key.displayName());
        nodes.add(new NextgrePlanView.Node(-1, -1, NextgrePlanView.SEEK, NextgreTexts.SEEK.with(name, quantity),
                NextgreTexts.FROM.with(String.join(", ", seek.servers())), seek.ticks(), NextgrePlanView.UNKNOWN,
                false, List.of()));
        // The rows are the draws the SEEK total reckons, in its order, so the two always add up.
        for (final NextgrePlanner.Draw draw : seek.draws()) {
            nodes.add(new NextgrePlanView.Node(0, -1, NextgrePlanView.PULL, NextgreTexts.PULL.with(name, draw.amount()),
                    NextgreTexts.FROM.with(draw.server()), draw.ticks(), NextgrePlanView.UNKNOWN, false, List.of()));
        }
        if (seek.uncovered() > 0L) {
            nodes.add(new NextgrePlanView.Node(0, -1, NextgrePlanView.PULL,
                    NextgreTexts.PULL.with(name, seek.uncovered()), NextgreTexts.HELD_NOWHERE.text(),
                    seek.uncoveredTicks(), NextgrePlanView.UNKNOWN, false, List.of()));
        }
        final Text said = NextgreTexts.SEEK_PLANNED.with(quantity, name, Math.max(0, nodes.size() - 1),
                seek.ticks());
        final NextgreState state = state(core);
        final NextgrePlanView view = new NextgrePlanView(state.takeId(), statement, false,
                core.getLevel() == null ? 0L : core.getLevel().getGameTime(),
                List.of(new NextgrePlanView.Alternative(1, NextgreTexts.AS_THE_NETWORK.text(), seek.ticks(),
                        Text.EMPTY, true, List.of())), nodes, 0L, NextgrePlanView.UNKNOWN, NextgrePlanView.PLANNED,
                said);
        state.remember(view, null);
        save(core, state);
        return new IqlEngine.Outcome(true, said, rows(view), table(view));
    }

    /* Keeps a plan, and watches the craft it became, if any, until it settles. */
    private NextgrePlanView remember(final MainframeBlockEntity core, final String statement, final boolean analyze,
                                     final long at, final NextgrePlanner.Weighed weighed,
                                     final NextgrePlanner.Inputs in, @Nullable final NetworkCraftOperation craft,
                                     final Text outcome) {
        final NextgreState state = state(core);
        final NextgrePlanner.Candidate best = weighed.best();
        final List<NextgrePlanView.Node> nodes = best == null ? List.of()
                : NextgrePlanner.explain(best, in, NextgreExtensions.explainNodes());
        final NextgrePlanView view = new NextgrePlanView(state.takeId(), statement, analyze, at,
                weighed.alternatives(), nodes, weighed.nanos() / 1000L, NextgrePlanView.UNKNOWN,
                craft == null ? NextgrePlanView.PLANNED : NextgrePlanView.RUNNING, outcome);
        state.remember(view, craft);
        save(core, state);
        if (craft != null) {
            craft.observe(done -> settled(core, view.id(), done));
        }
        return view;
    }

    /* A watched craft settled: what it took goes into its plan and into the times the planner reckons with. */
    private void settled(final MainframeBlockEntity core, final int id, final NetworkCraftOperation craft) {
        final NextgreState state = state(core);
        final NextgrePlanView plan = state.plan(id);
        if (plan != null) {
            state.replace(measuredOn(plan, craft, craft.finishedAt()));
        }
        state.settled(id);
        for (int i = 0; i < craft.plan().steps().size(); i++) {
            final CraftPlanner.Step step = craft.plan().steps().get(i);
            final long from = craft.stepStartedAt(i);
            final long to = craft.stepFinishedAt(i);
            if (from >= 0 && to >= from && step.resultKey() != null) {
                state.measured(NextgreState.timingKey(step.resultKey().id(), step.isMachine()), to - from,
                        craft.stepRunsDone(i));
            }
        }
        save(core, state);
    }

    /* The plan as it stands now: with what its craft has taken so far, while it runs. */
    private NextgrePlanView live(final MainframeBlockEntity core, final NextgreState state,
                                 final NextgrePlanView plan) {
        final NetworkCraftOperation craft = state.craftOf(plan.id());
        if (craft == null || core.getLevel() == null) {
            return plan;
        }
        return measuredOn(plan, craft, core.getLevel().getGameTime());
    }

    /* The plan's nodes with what its craft took, as of game time {@code now}. */
    private static NextgrePlanView measuredOn(final NextgrePlanView plan, final NetworkCraftOperation craft,
                                              final long now) {
        final long submitted = craft.submittedAt();
        final long started = craft.startedAt();
        final long finished = craft.finishedAt();
        final List<NextgrePlanView.Node> nodes = new ArrayList<>();
        for (final NextgrePlanView.Node node : plan.nodes()) {
            if (node.kind() == NextgrePlanView.ROOT) {
                nodes.add(node.measured((finished >= 0 ? finished : now) - submitted, finished >= 0));
            } else if (node.kind() == NextgrePlanView.PULL) {
                nodes.add(node.measured((started >= 0 ? started : now) - submitted, started >= 0));
            } else if (node.step() >= 0 && started >= 0) {
                final long to = craft.stepFinishedAt(node.step());
                final long from = craft.stepStartedAt(node.step());
                nodes.add(to >= 0 ? node.measured(to - started, true)
                        : from >= 0 ? node.measured(now - started, false) : node);
            } else {
                nodes.add(node);
            }
        }
        final byte state = finished < 0 ? NextgrePlanView.RUNNING
                : craft.craftStatus() == OperationRecord.STATUS_COMPLETED ? NextgrePlanView.DONE
                : NextgrePlanView.FAILED;
        return plan.with(nodes, finished >= 0 ? finished - submitted : NextgrePlanView.UNKNOWN, state);
    }

    /* The weighing a window's preview asks for, with no hints and the patterns it hands over. */
    @Nullable
    private NextgrePlanner.Candidate previewOf(final Gathered gathered, final StorageKey key,
                                               final long quantity, final boolean partial,
                                               final List<CraftingPattern> patterns,
                                               final List<ProcessingPattern> machines,
                                               final Map<StorageKey, Long> stock) {
        if (gathered.inputs() == null) {
            return null;
        }
        final NextgrePlanner.Inputs base = gathered.inputs();
        return NextgrePlanner.weigh(new NextgrePlanner.Inputs(key, quantity, partial, patterns, machines, stock,
                base.sources(), base.benchSpeed(), base.lanes(), base.perRun(), base.off(), base.statement(),
                base.routing(), base.rules(), base.operators(), base.statistics())).best();
    }

    /*
     * Captures on the server's thread everything a weighing reads: the patterns, the stock, where each raw material
     * is and how fast it comes, how fast the computers craft, what was measured, and what the hints say.
     */
    private Gathered gather(final MainframeBlockEntity core, final StorageKey key, final long quantity,
                            final boolean partial, final NextgreStatement statement) {
        if (!(core.getLevel() instanceof ServerLevel level) || core.networkUuid() == null) {
            return new Gathered(null, NextgreTexts.NOTHING_MAKES.with(GameText.of(key.displayName())));
        }
        final Map<StorageKey, Long> stock = core.networkIndex().snapshot();
        final List<CraftingPattern> patterns = core.resolvedNetworkPatterns(stock);
        final List<ProcessingPattern> machines = core.networkProcessingPatterns();
        final Set<StorageKey> used = new HashSet<>();
        for (final CraftingPattern pattern : patterns) {
            used.addAll(pattern.ingredientTotals().keySet());
        }
        for (final ProcessingPattern machine : machines) {
            used.addAll(machine.ingredientTotals().keySet());
        }
        final Map<NodeUuid, String> names = new HashMap<>();
        final Map<NodeUuid, Long> speeds = new HashMap<>();
        final Map<StorageKey, List<NextgrePlanner.Source>> sources = new HashMap<>();
        for (final StorageKey ingredient : used) {
            sources.put(ingredient, sourcesOf(level, core, ingredient, names, speeds));
        }
        long benchSpeed = 0L;
        int lanes = 0;
        for (final BlockPos pos : core.craftingComputerPositions()) {
            if (Loaded.blockEntity(level, pos) instanceof CraftingComputerBlockEntity cc && cc.canCraft()) {
                benchSpeed += cc.craftingThroughput();
                lanes += cc.craftingThreads();
            }
        }
        final Set<NodeUuid> prefer = new HashSet<>();
        final Set<NodeUuid> avoid = new HashSet<>();
        int maxParallel = 0;
        for (final NextgreStatement.Hint hint : statement.hints()) {
            switch (hint.keyword()) {
                case NextgreStatement.PREFER_SOURCE, NextgreStatement.AVOID_SOURCE -> {
                    final NodeUuid server = serverNamed(level, core, hint.value());
                    if (server == null) {
                        return new Gathered(null, NextgreTexts.NO_SERVER.with(hint.value()));
                    }
                    (hint.keyword().equals(NextgreStatement.PREFER_SOURCE) ? prefer : avoid).add(server);
                }
                case NextgreStatement.MAX_PARALLEL -> {
                    maxParallel = hint.number();
                    if (maxParallel <= 0) {
                        return new Gathered(null, NextgreTexts.BAD_PARALLEL.text());
                    }
                }
                default -> { }
            }
        }
        final List<NextgrePlanner.OperatorUse> operators = new ArrayList<>();
        for (final NextgreStatement.Hint hint : statement.hints()) {
            for (final IPlannerOperator operator : NextgreExtensions.operators()) {
                if (new NextgreStatement.Operator(operator.keyword(), false).keyword().equals(hint.keyword())) {
                    operators.add(new NextgrePlanner.OperatorUse(operator, hint.value()));
                }
            }
        }
        final NextgreState state = state(core);
        return new Gathered(new NextgrePlanner.Inputs(key, quantity, partial, patterns, machines, stock, sources,
                Math.max(1L, benchSpeed), Math.max(1, lanes), state.perRun(), state.off(), statement,
                new CraftRouting(prefer, avoid, maxParallel), NextgreExtensions.rules(), operators,
                NextgreExtensions.statistics()), Text.EMPTY);
    }

    /* The inputs with the sources of {@code key} added, for a read of an item no pattern uses. */
    private NextgrePlanner.Inputs withSourcesOf(final MainframeBlockEntity core, final NextgrePlanner.Inputs in,
                                                final StorageKey key) {
        if (in.sources().containsKey(key) || !(core.getLevel() instanceof ServerLevel level)) {
            return in;
        }
        final Map<StorageKey, List<NextgrePlanner.Source>> sources = new HashMap<>(in.sources());
        sources.put(key, sourcesOf(level, core, key, new HashMap<>(), new HashMap<>()));
        return new NextgrePlanner.Inputs(in.key(), in.quantity(), in.partial(), in.patterns(), in.machines(),
                in.stock(), sources, in.benchSpeed(), in.lanes(), in.perRun(), in.off(), in.statement(),
                in.routing(), in.rules(), in.operators(), in.statistics());
    }

    private static List<NextgrePlanner.Source> sourcesOf(final ServerLevel level, final MainframeBlockEntity core,
                                                         final StorageKey key, final Map<NodeUuid, String> names,
                                                         final Map<NodeUuid, Long> speeds) {
        final List<NextgrePlanner.Source> out = new ArrayList<>();
        for (final ItemLocation location : core.networkIndex().locations(key)) {
            final NodeUuid server = location.server();
            final String name = names.computeIfAbsent(server, node -> NetworkLookup.serverLabel(level, node));
            final long perTick = speeds.computeIfAbsent(server, node -> {
                final long cap = Math.min(NetworkIndex.serverThroughputCap(level, node),
                        NetworkIndex.serverLinkCap(level, core.networkUuid(), node));
                return cap == Long.MAX_VALUE ? UNLIMITED_PER_TICK : Math.max(1L, cap);
            });
            out.add(new NextgrePlanner.Source(server, name, location.quantity(), location.latencyTicks(), perTick));
        }
        return out;
    }

    @Nullable
    private static NodeUuid serverNamed(final ServerLevel level, final MainframeBlockEntity core, final String name) {
        for (final ServerNode node : NetworkSystem.get(level).serversOf(core.networkUuid())) {
            if (NetworkLookup.serverLabel(level, node.nodeUuid()).equalsIgnoreCase(name.strip())) {
                return node.nodeUuid();
            }
        }
        return null;
    }

    private static String label(final MainframeBlockEntity core) {
        if (core.getLevel() instanceof ServerLevel level) {
            final ServerPlayer player = Acting.current().map(id -> level.getServer().getPlayerList().getPlayer(id))
                    .orElse(null);
            if (player != null) {
                return player.getGameProfile().getName();
            }
        }
        return NOBODY;
    }

    private static boolean isAnalyze(final String statement) {
        return statement.strip().replace(";", "").strip().toUpperCase(Locale.ROOT).equals(IqlVerb.ANALYZE.name());
    }

    /* The plan's lines, as the prompt prints a read: each step moved in under its parent. */
    private static List<ICliComputer.StoredItem> rows(final NextgrePlanView view) {
        final List<ICliComputer.StoredItem> out = new ArrayList<>();
        final int[] depth = depths(view);
        for (int i = 0; i < view.nodes().size(); i++) {
            final NextgrePlanView.Node node = view.nodes().get(i);
            out.add(new ICliComputer.StoredItem(NextgreTexts.INDENTED.with(INDENT.repeat(depth[i]), node.title()),
                    Math.max(0L, node.estimate()), node.detail()));
        }
        return out;
    }

    private static IqlTable table(final NextgrePlanView view) {
        final List<List<Text>> rows = new ArrayList<>();
        final int[] depth = depths(view);
        for (int i = 0; i < view.nodes().size(); i++) {
            final NextgrePlanView.Node node = view.nodes().get(i);
            rows.add(List.of(NextgreTexts.INDENTED.with(INDENT.repeat(depth[i]), node.title()), node.detail(),
                    ticks(node.estimate()), ticks(node.actual()), Text.literal(String.join(", ", node.hints()))));
        }
        return new IqlTable(COLUMNS, rows);
    }

    private static Text ticks(final long ticks) {
        return ticks < 0 ? Text.EMPTY : NextgreTexts.TICKS.with(ticks);
    }

    private static int[] depths(final NextgrePlanView view) {
        final int[] depth = new int[view.nodes().size()];
        for (int i = 0; i < depth.length; i++) {
            final int parent = view.nodes().get(i).parent();
            depth[i] = parent >= 0 && parent < i ? depth[parent] + 1 : 0;
        }
        return depth;
    }

    private static IqlEngine.Outcome fail(final Text said) {
        return new IqlEngine.Outcome(false, said, List.of());
    }

    private NextgreState state(final MainframeBlockEntity core) {
        return states.computeIfAbsent(core, mainframe -> NextgreState.load(mainframe.engineData(def().program())));
    }

    private void save(final MainframeBlockEntity core, final NextgreState state) {
        final var tag = core.engineData(def().program());
        for (final String old : List.copyOf(tag.getAllKeys())) {
            tag.remove(old);
        }
        state.save(tag);
        core.markEngineDataChanged();
    }

    /**
     * A rule or a hint as the Rules tab lists it.
     *
     * @param id    its id; a hint's is its words
     * @param name  its name
     * @param tells what it does
     * @param on    whether it is on for this Mainframe; a hint always is
     * @param kind  {@link #OWN_RULE}, {@link #ADDED_RULE}, {@link #OWN_HINT} or {@link #ADDED_HINT}
     */
    public record RuleRow(String id, Text name, Text tells, boolean on, byte kind) {

        /** One of the engine's own rules, which can be switched off. */
        public static final byte OWN_RULE = 0;
        /** A rule another mod added, which can be switched off. */
        public static final byte ADDED_RULE = 1;
        /** One of the hints the engine's dialect takes. */
        public static final byte OWN_HINT = 2;
        /** A hint another mod added to the dialect. */
        public static final byte ADDED_HINT = 3;

        /** Whether it can be switched on and off. */
        public boolean switchable() {
            return kind == OWN_RULE || kind == ADDED_RULE;
        }
    }

    /**
     * A statistic as the Statistics tab lists it.
     *
     * @param name  what it is
     * @param value what it reads
     */
    public record StatRow(Text name, Text value) {
    }

    /* What a weighing reads, or why it cannot be had. */
    private record Gathered(@Nullable NextgrePlanner.Inputs inputs, Text error) {
    }
}
