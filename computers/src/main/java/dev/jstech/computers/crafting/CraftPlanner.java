/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.crafting;

import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.util.Sizes;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The recursive CRAFT planner: given the network's pattern set and its current stock, expands a request ("512 pistons") bottom-up into ordered pattern executions.
 */
public final class CraftPlanner {

    /*
     * How many steps deep a plan goes before it calls the rest missing. The longest route the series has is a chip's:
     * an Advanced memory chip is 33 machine steps from its silicon wafer, and its materials have steps of their own
     * before that; a loop never gets this deep, as a step never asks again for what it is making.
     */
    private static final int MAX_DEPTH = 64;

    private CraftPlanner() {
    }

    /**
     * One recipe executed {@code runs} times, inputs guaranteed by the steps before it: either a bench pattern
     * a Crafting Computer runs, or a machine pattern the network feeds through a Crafting Interface.
     */
    public record Step(@Nullable CraftingPattern pattern, @Nullable ProcessingPattern machine, long runs) {

        public static Step bench(final CraftingPattern pattern, final long runs) {
            return new Step(pattern, null, runs);
        }

        public static Step machine(final ProcessingPattern machine, final long runs) {
            return new Step(null, machine, runs);
        }

        public boolean isMachine() {
            return machine != null;
        }

        /** Units of the result one run yields. */
        public long perRun() {
            if (machine != null) {
                final ProcessingPattern.ProcessingOutput primary = machine.primaryOutput();
                return primary == null ? 1 : Math.max(1, primary.amount());
            }
            return Math.max(1, pattern.result().getCount());
        }

        public StorageKey resultKey() {
            if (machine != null) {
                final ProcessingPattern.ProcessingOutput primary = machine.primaryOutput();
                return primary == null ? null : primary.key();
            }
            return StorageKey.of(pattern.result());
        }

        public String resultName() {
            return resultText().english();
        }

        /** What the step is called: its recipe's name, which tells apart steps that make the same thing. */
        public String name() {
            return machine != null ? machine.displayName() : pattern.displayName();
        }

        /** What the step makes, named in the language of whoever reads it. */
        public Text resultText() {
            final StorageKey key = resultKey();
            return key == null ? Text.literal("?") : GameText.of(key.displayName());
        }

        /** Work units one run costs a computer: the ingredients it handles. */
        public long unitsPerRun() {
            return machine != null ? Math.max(1, machine.inputs().size()) : Math.max(1, pattern.filledCells());
        }

        public long produced() {
            return Sizes.times(runs, perRun());
        }
    }

    /**
     * The bill of materials for a request. {@code missing} holds everything a plan falls short of, made things too;
     * {@code unmakeable} only what the stock lacks and nothing on the network makes, the raw shortage behind it.
     */
    public record Plan(List<Step> steps, Map<StorageKey, Long> rawConsumption,
                       Map<StorageKey, Long> missing, long produced, Map<StorageKey, Long> unmakeable) {

        public boolean feasible() {
            return missing.isEmpty();
        }
    }

    public static Plan plan(final StorageKey resultKey, final long quantity,
                            final List<CraftingPattern> patterns, final Map<StorageKey, Long> stock) {
        return plan(resultKey, quantity, patterns, List.of(), stock);
    }

    /**
     * Plans with bench patterns AND machine patterns: an ingredient no bench pattern makes may come out of a
     * machine, whose own inputs are planned the same way, so a request expands through both kinds of recipe.
     */
    public static Plan plan(final StorageKey resultKey, final long quantity,
                            final List<CraftingPattern> patterns, final List<ProcessingPattern> machines,
                            final Map<StorageKey, Long> stock) {
        return plan(resultKey, quantity, patterns, machines, stock, false);
    }

    /**
     * The same, with the choice between a bench and a machine turned around when {@code machineFirst}: a thing that
     * both a machine and a bench make is made on the machine. An engine weighing both plans asks for each.
     */
    public static Plan plan(final StorageKey resultKey, final long quantity,
                            final List<CraftingPattern> patterns, final List<ProcessingPattern> machines,
                            final Map<StorageKey, Long> stock, final boolean machineFirst) {
        return plan(resultKey, quantity, Recipes.of(patterns, machines), stock, machineFirst);
    }

    private static Plan plan(final StorageKey resultKey, final long quantity, final Recipes recipes,
                             final Map<StorageKey, Long> stock, final boolean machineFirst) {
        final State state = new State(recipes, stock, machineFirst);
        final long covered = state.produce(resultKey, quantity, 0, new HashSet<>(), true);
        return new Plan(mergeMachineSteps(state.steps), Map.copyOf(state.rawConsumption),
                Map.copyOf(state.missing), covered, Map.copyOf(state.unmakeable));
    }

    /**
     * Folds later machine steps of the same pattern into the first one, so a machine is fed once for all the
     * runs a request needs instead of once per ingredient that needs it. A later step only moves up when every
     * input of that machine is raw stock or is made by a step that already precedes the first occurrence, so
     * the dependency order still holds; making more of an intermediate earlier never harms a later consumer.
     */
    private static List<Step> mergeMachineSteps(final List<Step> steps) {
        final List<Step> merged = new ArrayList<>();
        for (final Step step : steps) {
            boolean folded = false;
            if (step.isMachine()) {
                for (int i = 0; i < merged.size(); i++) {
                    final Step earlier = merged.get(i);
                    if (earlier.isMachine() && earlier.machine().sameRecipe(step.machine())
                            && inputsAvailableBefore(step.machine(), merged, i)) {
                        merged.set(i, Step.machine(earlier.machine(), earlier.runs() + step.runs()));
                        folded = true;
                        break;
                    }
                }
            }
            if (!folded) {
                merged.add(step);
            }
        }
        return List.copyOf(merged);
    }

    /** Whether every input of {@code machine} is raw stock or produced by a step before index {@code at}. */
    private static boolean inputsAvailableBefore(final ProcessingPattern machine, final List<Step> steps, final int at) {
        final Set<StorageKey> madeAnywhere = new HashSet<>();
        for (final Step step : steps) {
            final StorageKey made = step.resultKey();
            if (made != null) {
                madeAnywhere.add(made);
            }
        }
        final Set<StorageKey> madeEarlier = new HashSet<>();
        for (int i = 0; i < at; i++) {
            final StorageKey made = steps.get(i).resultKey();
            if (made != null) {
                madeEarlier.add(made);
            }
        }
        for (final ProcessingPattern.ProcessingInput in : machine.inputs()) {
            // An input no step makes is raw stock (locked from the start); an intermediate must already exist.
            if (madeAnywhere.contains(in.key()) && !madeEarlier.contains(in.key())) {
                return false;
            }
        }
        return true;
    }

    public static long maxFeasible(final StorageKey resultKey, final long quantity,
                                   final List<CraftingPattern> patterns, final Map<StorageKey, Long> stock) {
        return maxFeasible(resultKey, quantity, patterns, List.of(), stock);
    }

    public static long maxFeasible(final StorageKey resultKey, final long quantity,
                                   final List<CraftingPattern> patterns, final List<ProcessingPattern> machines,
                                   final Map<StorageKey, Long> stock) {
        return maxFeasible(resultKey, quantity, patterns, machines, stock, false);
    }

    /** The most of {@code resultKey} the plan made with the bench and machine order of {@code machineFirst} reaches. */
    public static long maxFeasible(final StorageKey resultKey, final long quantity,
                                   final List<CraftingPattern> patterns, final List<ProcessingPattern> machines,
                                   final Map<StorageKey, Long> stock, final boolean machineFirst) {
        long low = 0;
        /*
         * Cap the search ceiling so the midpoint arithmetic below cannot overflow when quantity is near
         * Long.MAX_VALUE (e.g. an IQL CRAFT with no count cap); a craft beyond this bound is unrealistic.
         */
        long high = Math.min(quantity, 2_000_000_000L);
        // Indexed once: every probe below plans the same recipes again.
        final Recipes recipes = Recipes.of(patterns, machines);
        while (low < high) {
            final long mid = low + (high - low + 1) / 2;
            if (plan(resultKey, mid, recipes, stock, machineFirst).feasible()) {
                low = mid;
            } else {
                high = mid - 1;
            }
        }
        return low;
    }

    /**
     * The recipes of a planning call by what they make, each list in the order it was given, so a lookup costs one hash
     * instead of a pass over every pattern.
     */
    private record Recipes(Map<StorageKey, List<CraftingPattern>> benches,
                           Map<StorageKey, List<ProcessingPattern>> machines) {

        static Recipes of(final List<CraftingPattern> patterns, final List<ProcessingPattern> machines) {
            final Map<StorageKey, List<CraftingPattern>> benches = new HashMap<>();
            for (final CraftingPattern pattern : patterns) {
                final List<CraftingPattern> ways = benches.computeIfAbsent(StorageKey.of(pattern.result()),
                        key -> new ArrayList<>());
                if (!ways.contains(pattern)) {
                    ways.add(pattern);
                }
            }
            final Map<StorageKey, List<ProcessingPattern>> byOutput = new HashMap<>();
            for (final ProcessingPattern machine : machines) {
                final ProcessingPattern.ProcessingOutput primary = machine.primaryOutput();
                if (primary != null && !machine.inputs().isEmpty()) {
                    final List<ProcessingPattern> ways = byOutput.computeIfAbsent(primary.key(),
                            key -> new ArrayList<>());
                    if (!ways.contains(machine)) {
                        ways.add(machine);
                    }
                }
            }
            return new Recipes(benches, byOutput);
        }

        /* The ways to make {@code key}: its bench patterns, then its machine ones, or the machines first. */
        List<Way> ways(final StorageKey key, final boolean machineFirst) {
            final List<Way> benchWays = new ArrayList<>();
            for (final CraftingPattern pattern : benches.getOrDefault(key, List.of())) {
                benchWays.add(new Way(pattern, null));
            }
            final List<Way> machineWays = new ArrayList<>();
            for (final ProcessingPattern machine : machines.getOrDefault(key, List.of())) {
                machineWays.add(new Way(null, machine));
            }
            final List<Way> out = new ArrayList<>(machineFirst ? machineWays : benchWays);
            out.addAll(machineFirst ? benchWays : machineWays);
            return out;
        }
    }

    /* One way to make a thing: a bench pattern or a machine pattern. */
    private record Way(@Nullable CraftingPattern pattern, @Nullable ProcessingPattern machine) {
    }

    /**
     * Mutable planning pass: virtual stock + intermediates, consumed as the tree expands.
     */
    private static final class State {
        private final Recipes recipes;
        private final Map<StorageKey, Long> remainingStock;
        private final Map<StorageKey, Long> intermediates = new HashMap<>();
        private final List<Step> steps = new ArrayList<>();
        private final Map<StorageKey, Long> rawConsumption = new LinkedHashMap<>();
        private final Map<StorageKey, Long> missing = new LinkedHashMap<>();
        private final Map<StorageKey, Long> unmakeable = new LinkedHashMap<>();
        /* The runs what was taken of each kept input still lasts, which later steps use before taking more. */
        private final Map<ProcessingPattern.ProcessingInput, Long> lasting = new HashMap<>();
        /* How to take back each change made so far, the last first: a way tried and found short is taken back. */
        private final Deque<Runnable> undo = new ArrayDeque<>();
        private final boolean machineFirst;

        private State(final Recipes recipes, final Map<StorageKey, Long> stock, final boolean machineFirst) {
            this.recipes = recipes;
            this.remainingStock = new HashMap<>(stock);
            this.machineFirst = machineFirst;
        }

        private long produce(final StorageKey key, final long quantity, final int depth,
                             final Set<StorageKey> chain, final boolean isRoot) {
            long deficit = quantity;

            if (!isRoot) {
                deficit -= takeFrom(intermediates, key, deficit);
                final long fromStock = takeFrom(remainingStock, key, deficit);
                if (fromStock > 0) {
                    add(rawConsumption, key, fromStock);
                    deficit -= fromStock;
                }
            }
            if (deficit <= 0) {
                return quantity;
            }
            final boolean looped = depth >= MAX_DEPTH || chain.contains(key);
            final List<Way> ways = looped ? List.of() : recipes.ways(key, machineFirst);
            if (ways.isEmpty()) {
                add(missing, key, deficit);
                if (!looped) {
                    add(unmakeable, key, deficit);
                }
                return quantity - deficit;
            }
            return quantity - deficit + bestOf(ways, key, deficit, depth, chain);
        }

        /*
         * Makes {@code deficit} of {@code key} the first of {@code ways} that covers all of it, or, where none does,
         * the one that covers most. Each way is tried on the plan as it stands and taken back before the next is tried,
         * so a way that loops back on itself (zinc ingots from zinc dust made of zinc ingots) gives way to one from
         * stock.
         */
        private long bestOf(final List<Way> ways, final StorageKey key, final long deficit, final int depth,
                            final Set<StorageKey> chain) {
            if (ways.size() == 1) {
                return make(ways.getFirst(), key, deficit, depth, chain);
            }
            final int before = undo.size();
            Way best = ways.getFirst();
            long bestMade = -1;
            for (final Way way : ways) {
                final long made = make(way, key, deficit, depth, chain);
                if (made >= deficit) {
                    return made;
                }
                if (made > bestMade) {
                    bestMade = made;
                    best = way;
                }
                while (undo.size() > before) {
                    undo.pop().run();
                }
            }
            return make(best, key, deficit, depth, chain);
        }

        /* Makes as much of {@code deficit} of {@code key} as {@code way} can, and says how much that is. */
        private long make(final Way way, final StorageKey key, final long deficit, final int depth,
                          final Set<StorageKey> chain) {
            final CraftingPattern pattern = way.pattern();
            final ProcessingPattern machine = way.machine();
            final long perRun = pattern != null ? Math.max(1, pattern.result().getCount())
                    : Math.max(1, machine.primaryOutput().amount());
            final long runs = Sizes.ceilDiv(deficit, perRun);

            // Secure every ingredient before this step executes (dependency order).
            chain.add(key);
            long feasibleRuns = runs;
            if (pattern != null) {
                for (final Map.Entry<StorageKey, Long> ingredient : pattern.ingredientTotals().entrySet()) {
                    final long need = Sizes.times(ingredient.getValue(), runs);
                    final long got = produce(ingredient.getKey(), need, depth + 1, chain, false);
                    if (got < need) {
                        // Short on this ingredient: only the runs it fully covers can execute.
                        feasibleRuns = Math.min(feasibleRuns, got / ingredient.getValue());
                    }
                }
            } else {
                for (final StorageKey ingredient : machine.inputKeys()) {
                    final ProcessingPattern.ProcessingInput kept = machine.keptInput(ingredient);
                    if (kept != null) {
                        feasibleRuns = Math.min(feasibleRuns, keep(kept, runs, depth, chain));
                        continue;
                    }
                    final long need = machine.need(ingredient, runs);
                    final long got = produce(ingredient, need, depth + 1, chain, false);
                    if (got < need) {
                        feasibleRuns = Math.min(feasibleRuns, machine.runsCovered(ingredient, got, runs));
                    }
                }
            }
            chain.remove(key);

            if (feasibleRuns < runs) {
                /*
                 * The uncovered remainder of the request is missing; surplus ingredients secured
                 * above stay in the virtual pools (the real operation only locks what it uses).
                 */
                add(missing, key, deficit - feasibleRuns * perRun);
            }
            if (feasibleRuns <= 0) {
                return 0L;
            }
            steps.add(pattern != null ? Step.bench(pattern, feasibleRuns) : Step.machine(machine, feasibleRuns));
            undo.push(steps::removeLast);
            final long produced = feasibleRuns * perRun;
            final long surplus = produced - Math.min(deficit, produced);
            if (surplus > 0) {
                add(intermediates, key, surplus); // rounding overflow, never wasted
            }
            return Math.min(deficit, produced);
        }

        /*
         * How many of {@code runs} an input kept in the machine covers. What earlier steps took of it still lasts the
         * runs they did not use, as the machine keeps it between jobs: eight layers of one wafer, each a pattern of
         * its own, take one dopant dust between them, not one each.
         */
        private long keep(final ProcessingPattern.ProcessingInput kept, final long runs, final int depth,
                          final Set<StorageKey> chain) {
            final long credit = lasting.getOrDefault(kept, 0L);
            final long fromCredit = Math.min(credit, runs);
            final long left = runs - fromCredit;
            final long need = kept.need(left);
            final long got = need > 0 ? produce(kept.key(), need, depth + 1, chain, false) : 0L;
            final long gotRuns = Sizes.times(got / kept.amount(), kept.lasts());
            final long covered = Math.min(left, gotRuns);
            set(lasting, kept, credit - fromCredit + gotRuns - covered);
            return fromCredit + covered;
        }

        private long takeFrom(final Map<StorageKey, Long> pool, final StorageKey key, final long want) {
            if (want <= 0) {
                return 0;
            }
            final long have = pool.getOrDefault(key, 0L);
            final long taken = Math.min(have, want);
            if (taken > 0) {
                set(pool, key, have - taken);
            }
            return taken;
        }

        private <K> void add(final Map<K, Long> map, final K key, final long amount) {
            set(map, key, map.getOrDefault(key, 0L) + amount);
        }

        /* Sets {@code key} in {@code map}, keeping how to put it back as it was. */
        private <K> void set(final Map<K, Long> map, final K key, final long value) {
            final Long was = map.put(key, value);
            undo.push(() -> {
                if (was == null) {
                    map.remove(key);
                } else {
                    map.put(key, was);
                }
            });
        }
    }
}
