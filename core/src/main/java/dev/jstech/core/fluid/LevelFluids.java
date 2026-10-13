/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.fluid;

import dev.jstech.core.JsCore;
import dev.jstech.core.cable.CableParts;
import dev.jstech.core.diagnostic.Diagnostics;
import dev.jstech.core.energy.internal.ProportionalSplitter;
import dev.jstech.core.grid.CoreGrids;
import dev.jstech.core.grid.Grid;
import dev.jstech.core.grid.GridKind;
import dev.jstech.core.transfer.Batch;
import dev.jstech.core.transfer.HandlerSteps;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

/**
 * The fluid grid of one dimension as it moves fluids: each connected part of the grid with the blocks its pipes plug
 * into. Pipes are passive: a part moves, each tick, at most what its slowest pipe carries (the pressure it holds), and
 * only the fluids every pipe of it is made for, which the rest leave where they are.
 *
 * <p>A block that gives a fluid and would not take it back (a machine's output) feeds the blocks that take it and give
 * nothing of it (a machine's input); what it has left fills the blocks that both take and give it (a tank); and a tank
 * feeds what the inputs still want. Each share is in proportion to what is wanted, and each move takes out and puts in
 * as one batch, so nothing is lost on the way. Two tanks alone never pour into each other; a tank whose face lets
 * fluid only out gives through that face like a machine's output. The parts are worked out again only when the grid's
 * shape changes or a pipe comes to meet another block.
 */
public final class LevelFluids {

    private final List<Part> parts = new ArrayList<>();
    private final Map<Long, Integer> partOfWire = new HashMap<>();
    private long[] lastTick = new long[0];
    private long builtVersion = -1L;
    private long builtTaps = -1L;
    private long taps;

    /** What the debug screen calls the time the fluid grid takes to move a tick's fluids. */
    public static final ResourceLocation TIMING = ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "fluid_grid");

    /** How many millibuckets the part of the grid the wire numbered {@code wire} is in moved in the last tick. */
    public OptionalLong movedLastTickOf(final long wire) {
        final Integer part = this.partOfWire.get(wire);
        return part == null || part >= this.lastTick.length ? OptionalLong.empty()
                : OptionalLong.of(this.lastTick[part]);
    }

    /** A pipe came to meet a block or stopped meeting one: the parts are worked out again before the next tick. */
    public void tapsChanged() {
        this.taps++;
    }

    /** Moves the tick's fluids through every part of the grid of {@code level}. */
    public void tick(final ServerLevel level) {
        final Grid grid = CoreGrids.of(level, GridKind.FLUID);
        if (grid.size() == 0) {
            this.parts.clear();
            this.partOfWire.clear();
            this.lastTick = new long[0];
            return;
        }
        if (grid.version() != this.builtVersion || this.taps != this.builtTaps) {
            build(level, grid);
            this.builtVersion = grid.version();
            this.builtTaps = this.taps;
        }
        final long start = System.nanoTime();
        final long[] moved = new long[this.parts.size()];
        for (int i = 0; i < moved.length; i++) {
            moved[i] = this.parts.get(i).tick();
        }
        this.lastTick = moved;
        Diagnostics.record(TIMING, start);
    }

    private void build(final ServerLevel level, final Grid grid) {
        this.parts.clear();
        this.partOfWire.clear();
        for (final CableParts.Part found : CableParts.of(level, grid)) {
            long rate = Long.MAX_VALUE;
            PipeLimits limits = null;
            for (final CableParts.WireAt wire : found.wires()) {
                rate = Math.min(rate, wire.runTooLong() ? 0L : wire.type().throughput());
                limits = limits == null ? wire.type().pipe() : limits.and(wire.type().pipe());
                this.partOfWire.put(wire.number(), this.parts.size());
            }
            if (limits == null) {
                continue;
            }
            final Set<Long> seen = new HashSet<>();
            final List<BlockCapabilityCache<IFluidHandler, @Nullable Direction>> ends = new ArrayList<>();
            for (final CableParts.Plug plug : found.plugs()) {
                if (seen.add(plug.at().asLong())) {
                    ends.add(BlockCapabilityCache.create(Capabilities.FluidHandler.BLOCK, level, plug.at(),
                            plug.face()));
                }
            }
            this.parts.add(new Part(rate, limits, ends));
        }
    }

    /* One connected part of the grid: how much it moves in a tick, what it carries, and the blocks it meets. */
    private static final class Part {

        private final long rate;
        private final PipeLimits limits;
        private final List<BlockCapabilityCache<IFluidHandler, @Nullable Direction>> ends;

        private Part(final long rate, final PipeLimits limits,
                     final List<BlockCapabilityCache<IFluidHandler, @Nullable Direction>> ends) {
            this.rate = rate;
            this.limits = limits;
            this.ends = ends;
        }

        /* Moves this tick's fluids, and says how many millibuckets moved. */
        private long tick() {
            if (this.rate <= 0 || this.ends.size() < 2) {
                return 0L;
            }
            final List<IFluidHandler> handlers = new ArrayList<>(this.ends.size());
            for (final BlockCapabilityCache<IFluidHandler, @Nullable Direction> end : this.ends) {
                final IFluidHandler handler = end.getCapability();
                if (handler != null) {
                    handlers.add(handler);
                }
            }
            final long[] budget = {this.rate};
            for (final IFluidHandler from : handlers) {
                give(from, handlers, false, false, budget);
            }
            for (final IFluidHandler from : handlers) {
                give(from, handlers, false, true, budget);
            }
            for (final IFluidHandler from : handlers) {
                give(from, handlers, true, false, budget);
            }
            return this.rate - budget[0];
        }

        /*
         * Gives what {@code from} offers to the blocks of the round that take it: from a block that would not take it
         * back (or, in the last round, from one that would) to blocks that would not give it (or, in the second round,
         * to ones that would).
         */
        private void give(final IFluidHandler from, final List<IFluidHandler> handlers, final boolean fromStore,
                          final boolean toStores, final long[] budget) {
            if (budget[0] <= 0) {
                return;
            }
            final FluidStack offer = from.drain((int) Math.min(Integer.MAX_VALUE, budget[0]),
                    IFluidHandler.FluidAction.SIMULATE);
            if (offer.isEmpty() || !this.limits.carries(offer.getFluid())
                    || takes(from, offer) != fromStore) {
                return;
            }
            final Map<Long, Long> wants = new LinkedHashMap<>();
            for (int i = 0; i < handlers.size(); i++) {
                final IFluidHandler to = handlers.get(i);
                if (to == from || !takes(to, offer) || gives(to, offer) != toStores) {
                    continue;
                }
                final int want = to.fill(offer.copy(), IFluidHandler.FluidAction.SIMULATE);
                if (want > 0) {
                    wants.put((long) i, (long) want);
                }
            }
            if (wants.isEmpty()) {
                return;
            }
            final Map<Long, Long> shares = ProportionalSplitter.split(offer.getAmount(), wants, Map.of());
            for (final Long index : wants.keySet()) {
                final int amount = (int) Math.min(budget[0], shares.getOrDefault(index, 0L));
                if (amount <= 0) {
                    continue;
                }
                final FluidStack moving = offer.copyWithAmount(amount);
                final Batch.Outcome outcome = new Batch()
                        .add(HandlerSteps.drain(from, moving))
                        .add(HandlerSteps.fill(handlers.get(index.intValue()), moving))
                        .commit();
                if (outcome.done()) {
                    budget[0] -= amount;
                }
            }
        }

        private static boolean takes(final IFluidHandler handler, final FluidStack fluid) {
            return handler.fill(fluid.copyWithAmount(1), IFluidHandler.FluidAction.SIMULATE) > 0;
        }

        private static boolean gives(final IFluidHandler handler, final FluidStack fluid) {
            return !handler.drain(fluid.copyWithAmount(1), IFluidHandler.FluidAction.SIMULATE).isEmpty();
        }
    }
}
