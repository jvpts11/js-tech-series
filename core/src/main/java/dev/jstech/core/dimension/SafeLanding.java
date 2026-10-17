/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.dimension;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Where a traveller arriving in a dimension can stand: on firm ground, with room above for their height and nothing in
 * it to drown, burn or smother them. Looked for near where they asked to arrive, first at that height and then up and
 * down from it, then round it in widening rings; where there is no ground at all, a small platform is laid under them.
 */
public final class SafeLanding {

    /* How far round, and how far up and down, a safe place is looked for, in blocks. */
    private static final int MOST_AROUND = 8;
    private static final int MOST_UP_OR_DOWN = 48;
    /* Half the side of the platform laid where there is no ground, in blocks. */
    private static final int PLATFORM = 1;

    private SafeLanding() {
    }

    /**
     * A safe place to stand in {@code level} near {@code near}, for a traveller {@code height} blocks tall: the block
     * their feet are in.
     */
    public static BlockPos find(final ServerLevel level, final BlockPos near, final double height) {
        final int tall = Math.max(1, (int) Math.ceil(height));
        final int y = Math.clamp(near.getY(), level.getMinBuildHeight() + 1, level.getMaxBuildHeight() - tall - 1);
        for (int ring = 0; ring <= MOST_AROUND; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) {
                        continue;
                    }
                    final BlockPos found = column(level, near.getX() + dx, y, near.getZ() + dz, tall);
                    if (found != null) {
                        return found;
                    }
                }
            }
        }
        final BlockPos feet = new BlockPos(near.getX(), y, near.getZ());
        platform(level, feet.below());
        return feet;
    }

    /** Whether a traveller {@code tall} blocks tall can stand with their feet in {@code feet}. */
    public static boolean safe(final ServerLevel level, final BlockPos feet, final int tall) {
        final BlockPos below = feet.below();
        final BlockState ground = level.getBlockState(below);
        if (!ground.isFaceSturdy(level, below, Direction.UP) || !ground.getFluidState().isEmpty()) {
            return false;
        }
        for (int up = 0; up < tall; up++) {
            final BlockPos at = feet.above(up);
            final BlockState state = level.getBlockState(at);
            if (!state.getCollisionShape(level, at).isEmpty() || !state.getFluidState().isEmpty()
                    || state.getBlock() instanceof BaseFireBlock || state.getFluidState().is(FluidTags.LAVA)) {
                return false;
            }
        }
        return true;
    }

    /* The nearest safe place in one column, from {@code y} up and down alternately; none when there is none. */
    private static BlockPos column(final ServerLevel level, final int x, final int y, final int z, final int tall) {
        for (int step = 0; step <= MOST_UP_OR_DOWN; step++) {
            for (final int dy : step == 0 ? new int[] {0} : new int[] {step, -step}) {
                final int at = y + dy;
                if (at <= level.getMinBuildHeight() || at + tall >= level.getMaxBuildHeight()) {
                    continue;
                }
                final BlockPos feet = new BlockPos(x, at, z);
                if (safe(level, feet, tall)) {
                    return feet;
                }
            }
        }
        return null;
    }

    /* A small platform of stone, where there is no ground to stand on. */
    private static void platform(final ServerLevel level, final BlockPos centre) {
        for (int dx = -PLATFORM; dx <= PLATFORM; dx++) {
            for (int dz = -PLATFORM; dz <= PLATFORM; dz++) {
                final BlockPos at = centre.offset(dx, 0, dz);
                if (level.getBlockState(at).canBeReplaced()) {
                    level.setBlockAndUpdate(at, Blocks.STONE.defaultBlockState());
                }
            }
        }
    }
}
