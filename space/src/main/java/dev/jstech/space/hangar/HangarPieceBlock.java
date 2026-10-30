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
 * A piece of a Rocket Hangar's kit that takes its own shape once the hangar forms: a crane rail's girder along a wall
 * under the roof, the crane bridge's box girder across the bay on the rails, the hoist's trolley under it, a work
 * platform's deck, its railing along the edge it faces; before, a block of the kit like any other. Formed, the
 * hangar's terminal draws them whole, as the approved hangar has them, and the pieces show nothing of their own. A
 * piece looks the way it was placed facing, the way the player faced; a forming hangar turns its rails, its bridge and
 * its hoist to lie as they run, and tells each platform whether it is at the edge it faces.
 */
public final class HangarPieceBlock extends HorizontalDirectionalBlock {

    private final Kind kind;
    private final MapCodec<HangarPieceBlock> codec;
    private final Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
    private final Map<Direction, VoxelShape> edged = new EnumMap<>(Direction.class);

    /** Whether its hangar is formed, so it takes its own shape. */
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");
    /** Whether a work platform is at the edge of its deck it faces, so its railing stands there. */
    public static final BooleanProperty EDGE = BooleanProperty.create("edge");
    private static final double PIXEL = 1.0 / 16;

    public HangarPieceBlock(final BlockBehaviour.Properties properties, final Kind kind) {
        super(properties.noOcclusion());
        this.kind = kind;
        this.codec = simpleCodec(made -> new HangarPieceBlock(made, kind));
        for (final Direction way : Direction.Plane.HORIZONTAL) {
            VoxelShape shape = Shapes.empty();
            for (final int[] box : kind.boxes) {
                shape = Shapes.or(shape, turned(box, way));
            }
            VoxelShape edge = shape;
            for (final int[] box : kind.edge) {
                edge = Shapes.or(edge, turned(box, way));
            }
            this.shapes.put(way, shape.optimize());
            this.edged.put(way, edge.optimize());
        }
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(FORMED, false)
                .setValue(EDGE, false));
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
        if (!state.getValue(FORMED)) {
            return Shapes.block();
        }
        return (state.getValue(EDGE) ? this.edged : this.shapes).get(state.getValue(FACING));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return this.codec;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FORMED, EDGE);
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
     * The pieces, each with its boxes in sixteenths as it faces north, and those it adds at its edge: the rail's
     * girder a block square along its wall; the bridge's girder over the rails and the hoist's trolley under it, from
     * a little above their blocks' feet; the platform's deck at its block's top, its railing on its north edge.
     */
    public enum Kind {
        RAIL(new int[][] {{0, 0, 0, 16, 16, 16}}, new int[][] {}),
        BRIDGE(new int[][] {{0, 2, 0, 16, 16, 16}}, new int[][] {}),
        HOIST(new int[][] {{0, 3, 0, 16, 16, 16}}, new int[][] {}),
        DOOR(new int[][] {{0, 0, 0, 16, 16, 16}}, new int[][] {}),
        PLATFORM(new int[][] {{0, 11, 0, 16, 16, 16}}, new int[][] {{0, 16, 0, 16, 24, 1}});

        private final int[][] boxes;
        private final int[][] edge;

        Kind(final int[][] boxes, final int[][] edge) {
            this.boxes = boxes;
            this.edge = edge;
        }
    }
}
