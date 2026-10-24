/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.line;

import dev.jstech.core.util.Loaded;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Where a pump of J's Industrial takes its fluid from the world (industrial/docs/design/fluids-and-gases.md): a water
 * pump draws water without end from a source next to it, as a pump in a river does; a drainage pump sucks up the
 * fluid blocks around it, a flooded mine, lava, a spill, each source block a bucket.
 */
public sealed interface PumpSource permits PumpSource.EndlessWater, PumpSource.Drains {

    /** Water without end, {@code perTick} millibuckets a tick, from a water source block next to the pump. */
    static PumpSource endlessWater(final int perTick) {
        return new EndlessWater(perTick);
    }

    /** The fluid source blocks within {@code radius} blocks, the nearest first, one every {@code every} ticks. */
    static PumpSource drains(final int radius, final int every) {
        return new Drains(radius, every);
    }

    /**
     * How many ticks a take is: the pump counts them itself, since a machine that waits sleeps, and wakes on no beat
     * of the clock.
     */
    int every();

    /**
     * What the pump at {@code pos} takes now, given {@code room} millibuckets of room in its tank: taken from the world
     * already; empty for nothing.
     */
    FluidStack take(ServerLevel level, BlockPos pos, int room);

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
        public FluidStack take(final ServerLevel level, final BlockPos pos, final int room) {
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
        public FluidStack take(final ServerLevel level, final BlockPos pos, final int room) {
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
}
