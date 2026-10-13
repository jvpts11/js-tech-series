/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/**
 * The subwoofer of a Transition speaker set: a box with no cable of its own, wired to a satellite as the real one is
 * wired to its amplifier. Set against a Transition speaker, it plays the bass the satellites leave out, so its
 * computer's Transition speakers play a recording whole. A computer needs only one; a second adds nothing.
 */
public class SubwooferBlock extends HorizontalDirectionalBlock {

    public static final MapCodec<SubwooferBlock> CODEC = simpleCodec(SubwooferBlock::new);

    public SubwooferBlock(final Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    /** Whether a subwoofer stands against the speaker at {@code speaker}, on any of its six faces, in a loaded chunk. */
    public static boolean against(final Level level, final BlockPos speaker) {
        for (final Direction face : Direction.values()) {
            final BlockPos beside = speaker.relative(face);
            if (level.isLoaded(beside) && level.getBlockState(beside).getBlock() instanceof SubwooferBlock) {
                return true;
            }
        }
        return false;
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
