/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.block;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A block's shape for each way it faces, from its boxes as it stands facing north, each turned as its model is turned
 * to the block's facing: what stood at the north side stands at the east when it faces east.
 */
public final class FacingShapes {

    private static final double SIXTEEN = 16.0;
    /* The least a box is across, so a flat one still has a shape to point at. */
    private static final double THINNEST = 0.01;

    private FacingShapes() {
    }

    /** The shapes of the boxes {@code north} (x0, y0, z0, x1, y1, z1 in sixteenths), for every horizontal facing. */
    public static Map<Direction, VoxelShape> of(final double[]... north) {
        final Map<Direction, VoxelShape> out = new EnumMap<>(Direction.class);
        for (final Direction facing : Direction.Plane.HORIZONTAL) {
            VoxelShape shape = Shapes.empty();
            for (final double[] box : north) {
                shape = Shapes.or(shape, box(box, facing));
            }
            out.put(facing, shape.optimize());
        }
        return out;
    }

    /** One box, given as it stands facing north, as it stands facing {@code facing}. */
    public static VoxelShape box(final double[] box, final Direction facing) {
        final int turns = switch (facing) {
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        };
        double x0 = box[0];
        double z0 = box[2];
        double x1 = box[3];
        double z1 = box[5];
        for (int t = 0; t < turns; t++) {
            final double nx0 = SIXTEEN - z1;
            final double nx1 = SIXTEEN - z0;
            z0 = x0;
            z1 = x1;
            x0 = nx0;
            x1 = nx1;
        }
        return Block.box(x0, box[1], z0, Math.max(x1, x0 + THINNEST), Math.max(box[4], box[1] + THINNEST),
                Math.max(z1, z0 + THINNEST));
    }
}
