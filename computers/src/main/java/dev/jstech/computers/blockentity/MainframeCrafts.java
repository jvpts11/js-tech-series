/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import dev.jstech.computers.crafting.AnyTagResolver;
import dev.jstech.computers.crafting.CraftPlanner;
import dev.jstech.computers.crafting.CraftPlanning;
import dev.jstech.computers.crafting.CraftingPattern;
import dev.jstech.computers.crafting.ICraftIo;
import dev.jstech.computers.crafting.MultiStagePattern;
import dev.jstech.computers.crafting.NetworkCraftOperation;
import dev.jstech.computers.crafting.NetworkMultiStageOperation;
import dev.jstech.computers.crafting.NetworkProcessingOperation;
import dev.jstech.computers.crafting.NetworkRecipe;
import dev.jstech.computers.crafting.PendingCraftOperation;
import dev.jstech.computers.crafting.ProcessingPattern;
import dev.jstech.computers.operation.INetworkOperation;
import dev.jstech.computers.operation.NetworkStorage;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.network.NetworkSystem;
import dev.jstech.core.util.Sizes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

/**
 * What the network can make, and the making of it.
 *
 * <p>Making something is the one thing the network does that is not a straight move. It has to find out what
 * on the network knows how to make the thing, decide which of several ways to do it, work out what that needs
 * and whether any of it has to be made first, and then run the result as one or many Operations that may
 * outlive whoever asked. That is a subject of its own, and it used to be a thousand lines down the middle of
 * the machine that owns the network, among the wiring and the readouts.
 *
 * <p>There is one way in for everybody: a terminal, a window and a program all ask the same thing and get the
 * same answer, which is why the choice between a machine recipe, a pipeline and a recursive plan is made here
 * and nowhere else.
 */
final class MainframeCrafts {

    private final MainframeBlockEntity mainframe;

    MainframeCrafts(final MainframeBlockEntity mainframe) {
        this.mainframe = mainframe;
    }

    /** Where the network's Crafting Computers are, which is where any making of anything happens. */
    List<BlockPos> craftingComputers() {
        return positionsOf(true);
    }

    /** Where the network's supercomputers are, which is what lets several crafts run at once. */
    List<BlockPos> supercomputers() {
        return positionsOf(false);
    }

    /**
     * The network's parallel craft-slot capacity from its online supercomputers, as {@code [used, total]}.
     * The Tasks view uses it to show how many crafts can run at once and how many are currently running.
     */
    int[] craftSlots() {
        int used = 0;
        int total = 0;
        for (final BlockPos pos : supercomputers()) {
            if (mainframe.getLevel() != null
                    && mainframe.getLevel().getBlockEntity(pos) instanceof HbwInterfaceBlockEntity sc
                    && sc.clusterOnline()) {
                total += (int) sc.parallelCrafts();
                used += sc.craftSlotsInUse();
            }
        }
        return new int[] {used, total};
    }

    /** Every bench pattern the network's running Crafting Computers hold. */
    List<CraftingPattern> patterns() {
        final List<CraftingPattern> patterns = new ArrayList<>();
        for (final CraftingComputerBlockEntity cc : runningComputers()) {
            patterns.addAll(cc.romPatterns());
        }
        return patterns;
    }

    /** The plain machine (processing) patterns on the network, for the recursive craft planner. */
    List<ProcessingPattern> processingPatterns() {
        final List<ProcessingPattern> machines = new ArrayList<>();
        for (final NetworkRecipe recipe : machineRecipes()) {
            recipe.proc().ifPresent(machines::add);
        }
        return machines;
    }

    /** Every machine recipe (processing / multi-stage) the network's running Crafting Computers hold. */
    List<NetworkRecipe> machineRecipes() {
        final List<NetworkRecipe> recipes = new ArrayList<>();
        for (final CraftingComputerBlockEntity cc : runningComputers()) {
            recipes.addAll(cc.machineRecipes());
        }
        return recipes;
    }

    /** The network's bench patterns with {@code preferred} first (when it is one of them, or given at all). */
    List<CraftingPattern> patternsPreferring(@Nullable final CraftingPattern preferred) {
        final List<CraftingPattern> all = patterns();
        if (preferred == null) {
            return all;
        }
        final List<CraftingPattern> ordered = new ArrayList<>(all.size() + 1);
        ordered.add(preferred);
        for (final CraftingPattern pattern : all) {
            if (!pattern.equals(preferred)) {
                ordered.add(pattern);
            }
        }
        return ordered;
    }

    /**
     * Every recipe on the network that makes {@code key}, in a stable order: the machine recipes first
     * (processing and multi-stage, in the order the Recipe ROMs hold them), then each bench pattern with that
     * result. A craft dialog lists these so the player can pick one, and the index into this list is what a
     * craft request names; the list only changes when a ROM does.
     */
    List<NetworkRecipe> recipesFor(final StorageKey key) {
        final List<NetworkRecipe> out = new ArrayList<>();
        for (final NetworkRecipe recipe : machineRecipes()) {
            if (key.equals(recipe.resultKey()) && recipe.usesMachine()) {
                out.add(recipe);
            }
        }
        for (final CraftingPattern pattern : patterns()) {
            if (key.equals(StorageKey.of(pattern.result()))) {
                out.add(NetworkRecipe.ofBench(pattern));
            }
        }
        return out;
    }

    /**
     * Whether anything on the network produces {@code key}: a bench pattern in a Recipe ROM, or a machine
     * recipe. The cheap answer a prompt needs at once, before the plan itself is made.
     */
    boolean anythingMakes(final StorageKey key) {
        for (final CraftingPattern pattern : patterns()) {
            if (key.equals(StorageKey.of(pattern.result()))) {
                return true;
            }
        }
        for (final NetworkRecipe recipe : machineRecipes()) {
            if (key.equals(recipe.resultKey())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether the network has a multi-stage recipe whose end result is {@code key}, so a caller can offer the
     * player the choice between the pipeline and the flat, recursively-planned path.
     */
    boolean hasMultiStageRecipe(final StorageKey key) {
        for (final NetworkRecipe recipe : machineRecipes()) {
            if (key.equals(recipe.resultKey()) && recipe.multi().isPresent()) {
                return true;
            }
        }
        return false;
    }

    /**
     * The optional ceiling on how many jobs of one machine type may run at once, 0 for no ceiling.
     *
     * <p>Declared on a Crafting Computer's Machines tab; the first computer on the network that says anything
     * about that type decides, and the rest of the network follows it.
     */
    int maxJobsFor(final String machineKey) {
        for (final BlockPos pos : craftingComputers()) {
            if (mainframe.getLevel() != null
                    && mainframe.getLevel().getBlockEntity(pos) instanceof CraftingComputerBlockEntity cc) {
                final CraftingComputerBlockEntity.MachineConfig cfg = cc.machineConfig(machineKey);
                if (cfg != CraftingComputerBlockEntity.MachineConfig.DEFAULT) {
                    return cfg.maxJobs();
                }
            }
        }
        return CraftingComputerBlockEntity.MachineConfig.DEFAULT.maxJobs();
    }

    /**
     * Plans and runs a recursive craft of {@code key}, with one extra pattern beside the network's own.
     *
     * <p>The extra one is for a pipeline's bench stage, which carries its own pattern and has to be makeable
     * even where that pattern was never loaded into any Recipe ROM on the network.
     */
    @Nullable
    NetworkCraftOperation craft(final StorageKey key, final long demand, final boolean partial,
                                final String requesterLabel, @Nullable final CraftingPattern extraPattern) {
        if (!canCraft(demand)) {
            return null;
        }
        final Map<StorageKey, Long> stock = mainframe.networkIndex().snapshot();
        /*
         * A cell that accepts a tag is settled here, against what the network holds right now, so the planner
         * and the craft itself only ever see exact items.
         */
        final List<CraftingPattern> patterns = AnyTagResolver.resolveAll(patterns(), stock);
        final CraftingPattern extra = extraPattern == null ? null : AnyTagResolver.resolve(extraPattern, stock);
        if (extra != null && !patterns.contains(extra)) {
            patterns.add(extra);
        }
        // Machine patterns take part in the plan: an ingredient no bench makes may come out of a machine.
        final CraftPlanning.Planned planned =
                CraftPlanning.plan(key, demand, partial, patterns, processingPatterns(), stock);
        if (planned == null) {
            return null; // nothing on the network makes it, or not enough of it for a full request
        }
        return plannedCraft(key, demand, planned.plan(), requesterLabel, extra);
    }

    /**
     * Runs an already-made plan as a craft. The record keeps the ORIGINAL request: a scaled-down partial run
     * settles as COMPLETED_PARTIAL showing produced vs requested, exactly what the player asked to see.
     */
    @Nullable
    NetworkCraftOperation plannedCraft(final StorageKey key, final long demand, final CraftPlanner.Plan plan,
                                       final String requesterLabel,
                                       @Nullable final CraftingPattern extraPattern) {
        if (!canCraft(demand) || plan.steps().isEmpty()
                || !(mainframe.getLevel() instanceof ServerLevel serverLevel)) {
            return null;
        }
        final NetworkCraftOperation operation = new NetworkCraftOperation(
                serverLevel, mainframe.networkUuid(), key, demand, plan, mainframe.networkIndex(),
                UUID.randomUUID(), craftingComputers(), supercomputers(),
                requesterLabel, extraPattern, mainframe);
        mainframe.takeOn(operation);
        return operation;
    }

    /**
     * Runs a machine recipe: feeds the pattern's inputs into the matching machine (declared on a Crafting
     * Switch) and collects its outputs back into the network, until {@code demand} of the primary output is
     * produced or the pattern times out.
     *
     * <p>With an {@code io} the step draws from and returns to that instead of the network. A recursive craft
     * passes its own pool there so its machine steps pipeline through the pool, concurrent and race-free,
     * rather than through the shared network; such a step is ephemeral and does not persist a reload.
     */
    @Nullable
    NetworkProcessingOperation machineRun(final ProcessingPattern pattern, final long demand,
                                          final String requesterLabel, @Nullable final ICraftIo io) {
        if (!canCraft(demand) || !(mainframe.getLevel() instanceof ServerLevel serverLevel)) {
            return null;
        }
        final NetworkProcessingOperation operation = new NetworkProcessingOperation(
                serverLevel, mainframe.networkUuid(), pattern, demand, craftingComputers(),
                UUID.randomUUID(), requesterLabel, io);
        mainframe.takeOn(operation);
        return operation;
    }

    /** Runs a multi-stage recipe: an ordered pipeline of bench/processing stages, one at a time. */
    @Nullable
    NetworkMultiStageOperation pipeline(final MultiStagePattern pattern, final long demand,
                                        final String requesterLabel) {
        if (!canCraft(demand)) {
            return null;
        }
        // Bench stages that accept a tag are settled against stock now, the way a plain craft's are.
        final MultiStagePattern resolved = AnyTagResolver.resolve(pattern, mainframe.networkIndex().snapshot());
        final NetworkMultiStageOperation operation =
                new NetworkMultiStageOperation(mainframe, resolved, demand, requesterLabel);
        mainframe.takeOn(operation);
        return operation;
    }

    /**
     * The one craft entry point every surface routes a request through, so all of them behave the same.
     *
     * <p>If a machine recipe on the network produces {@code key} directly, that recipe runs, as a processing
     * run or as a pipeline; otherwise a recursive bench-and-machine craft is planned. Where a machine recipe's
     * own inputs are not all in stock and other patterns can make them, the whole tree runs as one craft with
     * the machine as a step; where nothing can make a missing input, the bare machine run delivers what the
     * network does hold. {@code partial} applies only to the recursive fallback, since a machine run always
     * delivers what it can.
     *
     * <p>{@code preferMultiStage} decides which wins where a thing can be made BOTH by a pipeline and by
     * composing the individual step patterns: true runs the pipeline; false lets the recursive planner build
     * the tree from the flat patterns.
     */
    @Nullable
    INetworkOperation request(final StorageKey key, final long demand, final boolean partial,
                              final String label, @Nullable final Runnable onSettle,
                              final boolean preferMultiStage) {
        for (final NetworkRecipe recipe : machineRecipes()) {
            if (!key.equals(recipe.resultKey())) {
                continue;
            }
            if (recipe.proc().isPresent()) {
                final INetworkOperation op = runProcessing(recipe.proc().get(), key, demand, label);
                settle(op, onSettle);
                return op;
            }
            if (recipe.multi().isPresent() && preferMultiStage) {
                final NetworkMultiStageOperation op = pipeline(recipe.multi().get(), demand, label);
                if (op != null && onSettle != null) {
                    op.onSettle(onSettle);
                }
                return op;
            }
        }
        /*
         * No machine makes it directly (or a pipeline was declined): plan a recursive bench-and-machine craft.
         * The planning runs off the tick; the request is listed as pending until the plan lands.
         */
        final PendingCraftOperation op = planAsync(key, demand, partial, label, null);
        if (op != null && onSettle != null) {
            op.onSettle(onSettle);
        }
        return op;
    }

    /**
     * The same, with the recipe the player picked out of {@link #recipesFor}.
     *
     * <p>An index nobody offered falls back to choosing for them, which is what a stale window sends.
     */
    @Nullable
    INetworkOperation request(final StorageKey key, final long demand, final boolean partial,
                              final String label, @Nullable final Runnable onSettle, final int recipe) {
        final List<NetworkRecipe> recipes = recipesFor(key);
        if (recipe < 0 || recipe >= recipes.size()) {
            return request(key, demand, partial, label, onSettle, true);
        }
        final NetworkRecipe chosen = recipes.get(recipe);
        final INetworkOperation op;
        if (chosen.proc().isPresent()) {
            op = runProcessing(chosen.proc().get(), key, demand, label);
        } else if (chosen.multi().isPresent()) {
            op = pipeline(chosen.multi().get(), demand, label);
        } else {
            op = planAsync(key, demand, partial, label, chosen.bench().orElse(null));
        }
        settle(op, onSettle);
        return op;
    }

    /**
     * Plans a recursive craft on a virtual thread, with {@code preferred} ahead of every other bench pattern.
     *
     * <p>The request shows at once as a pending craft; when the plan lands, the real craft takes over with the
     * level and the settle callback given meanwhile. Preferring one pattern is how a result that several
     * patterns make is built by the one the player picked, since the planner takes the first that makes a
     * thing. Nothing comes back only where nothing on the network makes it at all, or no craft can run here.
     */
    @Nullable
    PendingCraftOperation planAsync(final StorageKey key, final long demand, final boolean partial,
                                    final String label, @Nullable final CraftingPattern preferred) {
        if (!canCraft(demand) || !anythingMakes(key)) {
            return null;
        }
        final Map<StorageKey, Long> stock = mainframe.networkIndex().snapshot();
        final List<CraftingPattern> patterns = AnyTagResolver.resolveAll(patternsPreferring(preferred), stock);
        final PendingCraftOperation pending = new PendingCraftOperation(mainframe, key, demand, partial, label);
        mainframe.takeOn(pending);
        pending.start(mainframe.dispatch(), patterns, processingPatterns(), stock);
        return pending;
    }

    /**
     * Runs a processing recipe for {@code demand} of {@code key}: the whole tree as one craft when an input is
     * short and other patterns make it (all-or-nothing), else the bare machine run with what the network holds.
     */
    @Nullable
    private INetworkOperation runProcessing(final ProcessingPattern machine, final StorageKey key,
                                            final long demand, final String label) {
        if (!inputsInStock(machine, demand)) {
            final NetworkCraftOperation planned = craft(key, demand, false, label, null);
            if (planned != null) {
                return planned;
            }
        }
        return machineRun(machine, demand, label, null);
    }

    /** Whether the network currently stocks every input a processing run producing {@code quantity} would use. */
    private boolean inputsInStock(final ProcessingPattern machine, final long quantity) {
        if (!(mainframe.getLevel() instanceof ServerLevel serverLevel) || mainframe.networkUuid() == null) {
            return true;
        }
        final NetworkStorage storage = NetworkStorage.of(serverLevel, mainframe.networkUuid());
        final ProcessingPattern.ProcessingOutput primary = machine.primaryOutput();
        final long runs = Sizes.ceilDiv(quantity, primary == null ? 1 : Math.max(1, primary.amount()));
        final Map<StorageKey, Long> need = new HashMap<>();
        for (final ProcessingPattern.ProcessingInput in : machine.inputs()) {
            need.merge(in.key(), in.amount() * runs, Long::sum);
        }
        for (final Map.Entry<StorageKey, Long> entry : need.entrySet()) {
            if (storage.count(entry.getKey()) < entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    /** Hooks {@code onSettle} onto whichever kind of craft operation came out, when there is one to hook. */
    private static void settle(@Nullable final INetworkOperation op, @Nullable final Runnable onSettle) {
        if (op == null || onSettle == null) {
            return;
        }
        if (op instanceof NetworkCraftOperation craft) {
            craft.onSettle(onSettle);
        } else if (op instanceof NetworkProcessingOperation processing) {
            processing.onSettle(onSettle);
        } else if (op instanceof NetworkMultiStageOperation multi) {
            multi.onSettle(onSettle);
        } else if (op instanceof PendingCraftOperation pending) {
            pending.onSettle(onSettle);
        }
    }

    /**
     * Whether this Mainframe can take a craft on at all.
     *
     * <p>Without a booted system the dispatcher never ticks, so an Operation submitted here would sit forever
     * holding what the caller already took out of the world. Refusing lets a caller give it back.
     */
    private boolean canCraft(final long demand) {
        return mainframe.isRunning() && mainframe.hasOs() && mainframe.getLevel() instanceof ServerLevel
                && mainframe.networkUuid() != null && demand > 0;
    }

    /** The Crafting Computers on the network that are switched on, which are the only ones that can make. */
    private List<CraftingComputerBlockEntity> runningComputers() {
        final List<CraftingComputerBlockEntity> out = new ArrayList<>();
        for (final BlockPos pos : craftingComputers()) {
            if (mainframe.getLevel() != null
                    && mainframe.getLevel().getBlockEntity(pos) instanceof CraftingComputerBlockEntity cc
                    && cc.isRunning()) {
                out.add(cc);
            }
        }
        return out;
    }

    private List<BlockPos> positionsOf(final boolean craftingComputers) {
        if (mainframe.networkUuid() == null || !(mainframe.getLevel() instanceof ServerLevel serverLevel)) {
            return List.of();
        }
        final NetworkSystem network = NetworkSystem.get(serverLevel);
        final List<BlockPos> positions = new ArrayList<>();
        if (craftingComputers) {
            for (final var node : network.craftingComputersOf(mainframe.networkUuid())) {
                positions.add(BlockPos.of(node.pos()));
            }
        } else {
            for (final var node : network.supercomputersOf(mainframe.networkUuid())) {
                positions.add(BlockPos.of(node.pos()));
            }
        }
        return positions;
    }
}
