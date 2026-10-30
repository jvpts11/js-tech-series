/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.ship;

import dev.jstech.core.multiblock.plan.IPlanMember;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A block of a starship's kit that works aboard, a suit locker, an oxygen panel, a bunk, so it never merges into its
 * part's formed model: once its part is formed the model draws it with the furniture round it, so the block draws
 * nothing and lets the light through, while it still stands where it is and a click still works it.
 */
public abstract class ShipFixtureBlock extends Block implements IPlanMember {

    /** Whether its part is formed, its model drawing it. */
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    protected ShipFixtureBlock(final Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FORMED, false));
    }

    @Override
    public void partFormed(final ServerLevel level, final BlockPos pos, final boolean formed) {
        final BlockState state = level.getBlockState(pos);
        if (state.hasProperty(FORMED) && state.getValue(FORMED) != formed) {
            level.setBlock(pos, state.setValue(FORMED, formed), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected VoxelShape getOcclusionShape(final BlockState state, final BlockGetter level, final BlockPos pos) {
        return state.getValue(FORMED) ? Shapes.empty() : super.getOcclusionShape(state, level, pos);
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FORMED);
    }
}
