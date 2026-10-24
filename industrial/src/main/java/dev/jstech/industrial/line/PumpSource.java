/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.line;

import dev.jstech.core.util.Loaded;
import dev.jstech.core.worldgen.reservoir.Reservoirs;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

/**
 * Where a pump of J's Industrial takes its fluid from the world (industrial/docs/design/fluids-and-gases.md,
 * industrial/docs/design/world.md): a water pump draws water without end from a source next to it, as a pump in a
 * river does; a drainage pump sucks up the fluid blocks around it, a flooded mine, lava, a spill, each source block a
 * bucket; and a well pump drains the reservoir of oil, gas or brine below it, block by block, each block of soaked
 * rock turning back into plain rock once it holds no more.
 */
public sealed interface PumpSource permits PumpSource.EndlessWater, PumpSource.Drains, PumpSource.Well {

    /** Water without end, {@code perTick} millibuckets a tick, from a water source block next to the pump. */
    static PumpSource endlessWater(final int perTick) {
        return new EndlessWater(perTick);
    }

    /** The fluid source blocks within {@code radius} blocks, the nearest first, one every {@code every} ticks. */
    static PumpSource drains(final int radius, final int every) {
        return new Drains(radius, every);
    }

    /** The reservoir straight below, {@code perTake} millibuckets every {@code every} ticks. */
    static PumpSource well(final int perTake, final int every) {
        return new Well(perTake, every);
    }

    /**
     * How many ticks a take is: the pump counts them itself, since a machine that waits sleeps, and wakes on no beat
     * of the clock.
     */
    int every();

    /**
     * What the pump at {@code pos} takes now, given {@code room} millibuckets of room in its tank and what it
     * remembers of where it took from last: taken from the world already; empty for nothing.
     */
    FluidStack take(ServerLevel level, BlockPos pos, int room, Memory memory);

    /** What a pump remembers of where it took from, so it need not look again each time. */
    final class Memory {

        private @Nullable BlockPos at;

        /** Where it took from last, if anywhere. */
        public Optional<BlockPos> at() {
            return Optional.ofNullable(this.at);
        }

        /** It took from {@code where}, or will look again for none. */
        public void remember(final @Nullable BlockPos where) {
            this.at = where;
        }
    }

    /**
     * Endless water.
     *
     * @param perTick the millibuckets it draws a tick
     */
    record EndlessWater(int perTick) implements PumpSource {

        public EndlessWater {
            if (perTick <= 0) {
                throw new IllegalArgumentException("a pump draws something a tick, not " + perTick);
            }
        }

        /** It draws every tick. */
        @Override
        public int every() {
            return 1;
        }

        @Override
        public FluidStack take(final ServerLevel level, final BlockPos pos, final int room, final Memory memory) {
            if (room <= 0) {
                return FluidStack.EMPTY;
            }
            for (final Direction side : Direction.values()) {
                final BlockPos beside = pos.relative(side);
                if (Loaded.at(level, beside) && level.getFluidState(beside).isSourceOfType(Fluids.WATER)) {
                    return new FluidStack(Fluids.WATER, Math.min(room, this.perTick));
                }
            }
            return FluidStack.EMPTY;
        }
    }

    /**
     * Drainage.
     *
     * @param radius how far it reaches, in blocks
     * @param every  how many ticks it takes to drain a block
     */
    record Drains(int radius, int every) implements PumpSource {

        /** A source block is a bucket of its fluid. */
        public static final int BUCKET = 1_000;

        public Drains {
            if (radius <= 0 || every <= 0) {
                throw new IllegalArgumentException("a drainage pump reaches a block and takes some time, not " + radius
                        + " and " + every);
            }
        }

        @Override
        public FluidStack take(final ServerLevel level, final BlockPos pos, final int room, final Memory memory) {
            if (room < BUCKET) {
                return FluidStack.EMPTY;
            }
            final Optional<BlockPos> nearest = nearest(level, pos);
            if (nearest.isEmpty()) {
                return FluidStack.EMPTY;
            }
            final FluidState found = level.getFluidState(nearest.get());
            level.setBlockAndUpdate(nearest.get(), Blocks.AIR.defaultBlockState());
            return new FluidStack(found.getType(), BUCKET);
        }

        /* The nearest fluid source block within reach, or none. */
        private Optional<BlockPos> nearest(final ServerLevel level, final BlockPos pos) {
            BlockPos best = null;
            double bestDistance = Double.MAX_VALUE;
            for (final BlockPos at : BlockPos.betweenClosed(pos.offset(-this.radius, -this.radius, -this.radius),
                    pos.offset(this.radius, this.radius, this.radius))) {
                if (!Loaded.at(level, at)) {
                    continue;
                }
                // A block of the fluid itself, never a waterlogged block, which it would take away with the water.
                final FluidState fluid = level.getFluidState(at);
                if (fluid.isSource() && level.getBlockState(at).liquid()) {
                    final double distance = at.distSqr(pos);
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = at.immutable();
                    }
                }
            }
            return Optional.ofNullable(best);
        }
    }

    /**
     * A well, its shaft going straight down to the reservoir below the pump, as deep as it lies: it drains the
     * reservoir's soaked rock block by block, the nearest first, keeping to one block until it holds no more.
     *
     * @param perTake how much it draws a take, in millibuckets
     * @param every   how many ticks a take is
     */
    record Well(int perTake, int every) implements PumpSource {

        public Well {
            if (perTake <= 0 || every <= 0) {
                throw new IllegalArgumentException("a well draws something and takes some time, not " + perTake
                        + " and " + every);
            }
        }

        @Override
        public FluidStack take(final ServerLevel level, final BlockPos pos, final int room, final Memory memory) {
            if (room <= 0) {
                return FluidStack.EMPTY;
            }
            Optional<BlockPos> block = memory.at().filter(at -> Reservoirs.at(level, at).isPresent());
            if (block.isEmpty()) {
                block = below(level, pos).flatMap(reservoir -> Reservoirs.nearestHolding(level, reservoir, pos));
                memory.remember(block.orElse(null));
            }
            if (block.isEmpty()) {
                return FluidStack.EMPTY;
            }
            final Optional<Reservoirs.Held> held = Reservoirs.at(level, block.get());
            final Optional<Fluid> fluid = held.flatMap(found -> BuiltInRegistries.FLUID.getOptional(
                    found.reservoir().kind().type().fluid()));
            if (fluid.isEmpty()) {
                return FluidStack.EMPTY;
            }
            final int drawn = Reservoirs.drain(level, block.get(), Math.min(room, this.perTake), false);
            return drawn <= 0 ? FluidStack.EMPTY : new FluidStack(fluid.get(), drawn);
        }

        /* The reservoir straight below the pump, the nearest if several lie under it. */
        private static Optional<Reservoirs.Reservoir> below(final ServerLevel level, final BlockPos pos) {
            for (final Reservoirs.Reservoir reservoir : Reservoirs.near(level, pos, 0)) {
                if (reservoir.body().y() < pos.getY()) {
                    return Optional.of(reservoir);
                }
            }
            return Optional.empty();
        }
    }
}
