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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A piece of a Rocket Hangar's kit that takes its own shape once the hangar forms: a crane rail's girder along the top
 * of a wall, the crane bridge's box girder across the bay, the hoist's trolley and hook under it, a work platform's
 * deck with its rail; before, a block of the kit like any other. It looks the way it was placed facing, the way the
 * player faced; a forming hangar turns its rails, its bridge and its hoist to lie as they run.
 */
public final class HangarPieceBlock extends HorizontalDirectionalBlock {

    private final Kind kind;
    private final MapCodec<HangarPieceBlock> codec;
    private final Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);

    /** Whether its hangar is formed, so it takes its own shape. */
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");
    private static final double PIXEL = 1.0 / 16;

    public HangarPieceBlock(final BlockBehaviour.Properties properties, final Kind kind) {
        super(properties.noOcclusion());
        this.kind = kind;
        this.codec = simpleCodec(made -> new HangarPieceBlock(made, kind));
        for (final Direction way : Direction.Plane.HORIZONTAL) {
            VoxelShape shape = Shapes.empty();
            for (final int[] box : kind.boxes()) {
                shape = Shapes.or(shape, turned(box, way));
            }
            this.shapes.put(way, shape.optimize());
        }
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(FORMED, false));
    }

    /** What piece of the hangar it is. */
    public Kind kind() {
        return this.kind;
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos,
                                  final CollisionContext context) {
        return state.getValue(FORMED) ? this.shapes.get(state.getValue(FACING)) : Shapes.block();
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return this.codec;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FORMED);
    }

    /* A box of the piece, in sixteenths as it faces north, turned to face {@code way}. */
    private static VoxelShape turned(final int[] box, final Direction way) {
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

    /**
     * The pieces, each with its boxes in sixteenths as it faces north: the rail's girder runs east and west along its
     * wall, the bridge's and the hoist's north and south across the bay, the platform's rail stands on its north edge.
     */
    public enum Kind {
        RAIL(new int[][] {{0, 5, 4, 16, 16, 12}}),
        BRIDGE(new int[][] {{3, 4, 0, 13, 16, 16}}),
        HOIST(new int[][] {{3, 10, 2, 13, 16, 14}, {7, 3, 7, 9, 10, 9}, {5, 0, 5, 11, 3, 11}}),
        DOOR(new int[][] {{0, 0, 0, 16, 16, 16}}),
        PLATFORM(new int[][] {{0, 0, 0, 16, 2, 16}, {0, 2, 0, 16, 16, 1}});

        private final int[][] boxes;

        Kind(final int[][] boxes) {
            this.boxes = boxes;
        }

        /** Its boxes, in sixteenths, as it faces north. */
        public int[][] boxes() {
            return this.boxes.clone();
        }
    }
}
