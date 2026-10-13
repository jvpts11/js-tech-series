/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multipart;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Where a part sits on each face of a block, and which part a player is pointing at. A part takes a plate twelve
 * pixels across and five deep against its face, wide enough to hit and outside whatever runs through the block's
 * middle, so the highlight and the click hug the part rather than the block.
 */
public final class PartBoxes {

    private static final Map<Direction, AABB> BOXES = new EnumMap<>(Direction.class);
    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);
    /** How far outside a part's box a click still counts as on the part. */
    private static final double SLACK = 0.02;
    /** How much nearer the part has to be than the block's middle for a ray to choose it. */
    private static final double TIE = 1.0e-4;

    static {
        final double a = 2.0 / 16.0;
        final double b = 14.0 / 16.0;
        final double d = 5.0 / 16.0;
        BOXES.put(Direction.DOWN, new AABB(a, 0.0, a, b, d, b));
        BOXES.put(Direction.UP, new AABB(a, 1.0 - d, a, b, 1.0, b));
        BOXES.put(Direction.NORTH, new AABB(a, a, 0.0, b, b, d));
        BOXES.put(Direction.SOUTH, new AABB(a, a, 1.0 - d, b, b, 1.0));
        BOXES.put(Direction.WEST, new AABB(0.0, a, a, d, b, b));
        BOXES.put(Direction.EAST, new AABB(1.0 - d, a, a, 1.0, b, b));
        BOXES.forEach((face, box) -> SHAPES.put(face,
                Shapes.box(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ)));
    }

    private PartBoxes() {
    }

    /** The box of a part on {@code face}, within the block. */
    public static AABB box(final Direction face) {
        return BOXES.get(face);
    }

    /** The shape of a part on {@code face}, within the block. */
    public static VoxelShape shape(final Direction face) {
        return SHAPES.get(face);
    }

    /**
     * The face whose part {@code location} lies on, among the faces {@code occupied} says hold one; null when it lies
     * on none.
     */
    public static @Nullable Direction faceAt(final BlockPos pos, final Vec3 location,
                                             final Predicate<Direction> occupied) {
        final Vec3 local = location.subtract(Vec3.atLowerCornerOf(pos));
        for (final Direction face : Direction.values()) {
            if (occupied.test(face) && BOXES.get(face).inflate(SLACK).contains(local)) {
                return face;
            }
        }
        return null;
    }

    /**
     * The part a ray from {@code start} to {@code end} meets first, among the faces {@code occupied} says hold one, or
     * null when it meets none or meets {@code middle}, what runs through the block, before any of them.
     */
    public static @Nullable Direction aimed(final BlockPos pos, final Vec3 start, final Vec3 end,
                                            final VoxelShape middle, final Predicate<Direction> occupied) {
        final BlockHitResult middleHit = middle.isEmpty() ? null : middle.clip(start, end, pos);
        final double middleDistance = middleHit == null ? Double.MAX_VALUE
                : start.distanceToSqr(middleHit.getLocation());
        Direction best = null;
        double bestDistance = Double.MAX_VALUE;
        for (final Direction face : Direction.values()) {
            if (!occupied.test(face)) {
                continue;
            }
            final BlockHitResult hit = SHAPES.get(face).clip(start, end, pos);
            if (hit != null) {
                final double distance = start.distanceToSqr(hit.getLocation());
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = face;
                }
            }
        }
        return best != null && bestDistance <= middleDistance + TIE ? best : null;
    }
}
