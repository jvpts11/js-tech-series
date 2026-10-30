/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.pad;

import com.mojang.serialization.MapCodec;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Flame Deflector, at the foot of the flame trench under the launcher's opening: a ridge that splits the exhaust
 * both ways along the trench. Its ridge runs across the way it faces, its slopes falling toward and away from it;
 * a row of them spans the trench from wall to wall.
 */
public final class FlameDeflectorBlock extends HorizontalDirectionalBlock {

    private final Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);

    private static final MapCodec<FlameDeflectorBlock> CODEC = simpleCodec(FlameDeflectorBlock::new);
    /* The ridge's shape in steps a quarter of a block high, each narrower than the one under it. */
    private static final int STEPS = 4;
    private static final double PIXELS = 16.0;

    public FlameDeflectorBlock(final Properties properties) {
        super(properties.noOcclusion());
        for (final Direction way : Direction.Plane.HORIZONTAL) {
            VoxelShape shape = Shapes.empty();
            final int step = (int) PIXELS / STEPS;
            for (int k = 0; k < STEPS; k++) {
                final double in = k * step / 2.0;
                shape = Shapes.or(shape, way.getAxis() == Direction.Axis.Z
                        ? Block.box(0, k * step, in, PIXELS, (k + 1) * step, PIXELS - in)
                        : Block.box(in, k * step, 0, PIXELS - in, (k + 1) * step, PIXELS));
            }
            this.shapes.put(way, shape.optimize());
        }
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos,
                                  final CollisionContext context) {
        return this.shapes.get(state.getValue(FACING));
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
