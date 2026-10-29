/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.geology;

import dev.jstech.core.air.AirMix;
import dev.jstech.core.heat.Thawing;
import dev.jstech.core.room.SealedRooms;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * The frost of carbon dioxide over Mars' caps. Carbon dioxide is never a liquid below five atmospheres, so dry ice
 * warmed turns to gas whatever the air round it, even in a sealed room: little by little, each share of the block's
 * gas going into the room it lies in, until the block is gone. A whole block holds the gas of a cubic metre of dry
 * ice, some 35,000 moles, as much as 850 blocks of the Earth's air hold of theirs: a habitat that keeps it warm fills
 * with carbon dioxide.
 */
public final class DryIceBlock extends Block {

    /** The shares a block gives off, one each time it is warmed. */
    public static final int SHARES = 8;
    /** How many of its shares the block has given off; it keeps its look until the last goes. */
    public static final IntegerProperty SUBLIMATED = IntegerProperty.create("sublimated", 0, SHARES - 1);
    /** The moles of carbon dioxide a block holds: a cubic metre of dry ice, 1,562 kg, at 44.01 g a mole. */
    public static final double MOLES = 1_562_000 / 44.01;

    public DryIceBlock(final Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(SUBLIMATED, 0));
    }

    /** Gives off a share of the dry ice at {@code pos} if it is warmed; whether it did. */
    public static boolean sublimate(final ServerLevel level, final BlockPos pos, final BlockState state) {
        if (!Thawing.warmed(level, pos)) {
            return false;
        }
        SealedRooms.give(level, pos, AirMix.CARBON_DIOXIDE, MOLES / SHARES);
        final int given = state.getValue(SUBLIMATED) + 1;
        if (given >= SHARES) {
            level.removeBlock(pos, false);
        } else {
            level.setBlockAndUpdate(pos, state.setValue(SUBLIMATED, given));
        }
        return true;
    }

    @Override
    protected void randomTick(final BlockState state, final ServerLevel level, final BlockPos pos,
                              final RandomSource random) {
        sublimate(level, pos, state);
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SUBLIMATED);
    }
}
