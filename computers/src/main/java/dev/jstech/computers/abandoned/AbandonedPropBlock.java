/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.abandoned;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A fitting of an abandoned building (computers/docs/design/survival.md, "Abandoned buildings"): a board on a wall, a
 * light under a ceiling, a sign, a van's wheel. Only the buildings set it down, one block's share of its fitting each
 * ({@link AbandonedProps}); it turns with the building, gives its part's light, and leaves nothing behind when broken.
 */
public final class AbandonedPropBlock extends HorizontalDirectionalBlock {

    /** Which part of the fittings the block is. */
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, AbandonedProps.count() - 1);
    private static final MapCodec<AbandonedPropBlock> CODEC = simpleCodec(AbandonedPropBlock::new);

    public AbandonedPropBlock(final Properties properties) {
        super(properties.lightLevel(state -> AbandonedProps.light(state.getValue(PART))));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, 0));
    }

    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos,
                                  final CollisionContext context) {
        return AbandonedProps.shape(state.getValue(PART), state.getValue(FACING));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }
}
