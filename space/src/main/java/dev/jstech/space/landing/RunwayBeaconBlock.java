/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.landing;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The runway beacon, at the start of a flat stretch: a low base, a yellow stem that would break off under a wheel, a
 * head with a green lens to the ship coming in, as a runway's threshold lights are green, and a white lamp on top. It
 * faces the way its runway runs, the way its player looked as they put it down, and the world's landings keep it, so
 * a wheeled ship's autopilot finds it from far away.
 */
public final class RunwayBeaconBlock extends HorizontalDirectionalBlock {

    private static final MapCodec<RunwayBeaconBlock> CODEC = simpleCodec(RunwayBeaconBlock::new);
    /* Its base, its stem and its head, in sixteenths of a block; the same whichever way it faces. */
    private static final VoxelShape SHAPE = Shapes.or(Block.box(2, 0, 2, 14, 2, 14), Block.box(6, 2, 6, 10, 9, 10),
            Block.box(4, 9, 4, 12, 15, 12));

    public RunwayBeaconBlock(final Properties properties) {
        super(properties.noOcclusion());
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    /* Put down, or turned, it is noted with the way its runway runs. */
    @Override
    protected void onPlace(final BlockState state, final Level level, final BlockPos pos, final BlockState was,
                           final boolean movedByPiston) {
        super.onPlace(state, level, pos, was, movedByPiston);
        if (level instanceof ServerLevel server) {
            ShipLandings.noteBeacon(server, pos, state.getValue(FACING));
        }
    }

    /* Broken, or another block put in its place, it is let go. */
    @Override
    protected void onRemove(final BlockState state, final Level level, final BlockPos pos, final BlockState now,
                            final boolean movedByPiston) {
        if (level instanceof ServerLevel server && !now.is(this)) {
            ShipLandings.dropBeacon(server, pos);
        }
        super.onRemove(state, level, pos, now, movedByPiston);
    }

    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos,
                                  final CollisionContext context) {
        return SHAPE;
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
