/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.hangar;

import com.mojang.serialization.MapCodec;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A fitting of the Rocket Hangar's control room that is not a whole block: a console, a chair, the status board on its
 * wall, the step and its railing, a block of the Rocket Design table. It is shaped as its model, turned the way it
 * faces; placed, it faces the player who places it, and its hangar turns it its own way as it forms.
 */
public class HangarFittingBlock extends HorizontalDirectionalBlock {

    private final int[][] boxes;
    private final MapCodec<HangarFittingBlock> codec;
    private final Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);

    private static final double PIXEL = 1.0 / 16;

    /**
     * A fitting shaped as {@code boxes}, each in sixteenths of a block as it faces north: from x, y and z to x, y and
     * z; a box may reach up to half a block over its own.
     */
    public HangarFittingBlock(final BlockBehaviour.Properties properties, final int[][] boxes) {
        super(properties.noOcclusion());
        this.boxes = boxes.clone();
        this.codec = simpleCodec(made -> new HangarFittingBlock(made, this.boxes));
        for (final Direction way : Direction.Plane.HORIZONTAL) {
            VoxelShape shape = Shapes.empty();
            for (final int[] box : boxes) {
                shape = Shapes.or(shape, turned(box, way));
            }
            this.shapes.put(way, shape.optimize());
        }
    }

    /** A box of a fitting, in sixteenths as it faces north, turned to face {@code way}. */
    public static VoxelShape turned(final int[] box, final Direction way) {
        final int x0 = box[0];
        final int z0 = box[2];
        final int x1 = box[3];
        final int z1 = box[5];
        final int[] at = switch (way) {
            case SOUTH -> new int[] {16 - x1, 16 - z1, 16 - x0, 16 - z0};
            case EAST -> new int[] {16 - z1, x0, 16 - z0, x1};
            case WEST -> new int[] {z0, 16 - x1, z1, 16 - x0};
            default -> new int[] {x0, z0, x1, z1};
        };
        return Shapes.box(at[0] * PIXEL, box[1] * PIXEL, at[1] * PIXEL, at[2] * PIXEL, box[4] * PIXEL,
                at[3] * PIXEL);
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos,
                                  final CollisionContext context) {
        return this.shapes.get(state.getValue(FACING));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return this.codec;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
