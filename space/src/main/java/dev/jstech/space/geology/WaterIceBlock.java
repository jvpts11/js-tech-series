/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.geology;

import dev.jstech.core.heat.Thawing;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The water ice of Mars' caps, which is not the Earth's ice: hard as rock in their cold, rough with the dust between
 * its layers so nothing slides on it, and, warmed, turning to water only where water can stay liquid, a sealed room
 * with air; anywhere else, in a vacuum or in Mars' thin air, it turns straight to vapour and is gone
 * ({@link Thawing}).
 */
public final class WaterIceBlock extends Block {

    public WaterIceBlock(final Properties properties) {
        super(properties);
    }

    @Override
    protected void randomTick(final BlockState state, final ServerLevel level, final BlockPos pos,
                              final RandomSource random) {
        thaw(level, pos);
    }

    /** Melts or sublimates the ice at {@code pos} if it is warmed; whether it did. */
    public static boolean thaw(final ServerLevel level, final BlockPos pos) {
        if (!Thawing.warmed(level, pos)) {
            return false;
        }
        if (Thawing.meltsToWater(level, pos)) {
            level.setBlockAndUpdate(pos, Blocks.WATER.defaultBlockState());
            level.neighborChanged(pos, Blocks.WATER, pos);
        } else {
            level.removeBlock(pos, false);
        }
        return true;
    }
}
