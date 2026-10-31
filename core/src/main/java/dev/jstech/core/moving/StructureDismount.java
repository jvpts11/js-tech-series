/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Where someone getting up from a seat of a moving structure stands, found among the structure's own blocks in its
 * yard: the nearest place they stand up in, going round the seat as they could, crawling if they must, through its
 * doorways and along its floor, its sides first as the game gets a rider off a minecart; with nowhere to stand, the
 * nearest they crouch in, then the nearest they crawl in, as the game has a rider leave a minecart in a low tunnel.
 * Never on what stands higher than the seat's own floor, a console or a cabinet beside it, nor up through a roof.
 */
final class StructureDismount {

    /* How many places along from the seat it goes looking, at most. */
    private static final int REACH = 6;
    /* How high over its block's foot a place's floor may be, in blocks, to be stood on: a slab, not a cabinet. */
    private static final double LOW_FLOOR = 0.5;
    private static final double HALF = 0.5;
    /* How far over the floor the box tried for someone standing starts, in blocks: a structure tipped a little. */
    private static final double OFF_THE_FLOOR = 0.1;
    /* The ways along the floor it goes on looking from a place: each side, and a step down or up. */
    private static final int[] STEPS = {0, -1, 1};
    private static final List<Pose> BY_ROOM = List.of(Pose.STANDING, Pose.CROUCHING, Pose.SWIMMING);

    private StructureDismount() {
    }

    /**
     * Where {@code rider}, getting up from the seat block {@code seat} of a yard in {@code level}, its seat looking
     * {@code way}, stands, in the yard, and how, the structure standing as {@code frame}; null with nowhere within
     * reach.
     */
    static @Nullable Place of(final Level level, final LivingEntity rider, final BlockPos seat, final Direction way,
                              final StructureFrame frame) {
        final List<Pose> poses = BY_ROOM.stream().filter(rider.getDismountPoses()::contains).toList();
        if (poses.isEmpty()) {
            return null;
        }
        final Place[] best = new Place[poses.size()];
        final Map<BlockPos, Integer> seen = new HashMap<>();
        final ArrayDeque<BlockPos> open = new ArrayDeque<>();
        seen.put(seat, 0);
        // Round the seat first, at its level and a step down: never up, onto the roof by a seat under it.
        for (final int dy : new int[] {0, -1}) {
            for (final int[] offset : DismountHelper.offsetsForDirection(way)) {
                final BlockPos next = seat.offset(offset[0], dy, offset[1]);
                if (seen.putIfAbsent(next, 1) == null) {
                    open.add(next);
                }
            }
        }
        while (!open.isEmpty()) {
            final BlockPos at = open.poll();
            for (int k = 0; k < poses.size(); k++) {
                if (best[k] == null) {
                    final Vec3 fits = fits(level, rider, at, poses.get(k), frame);
                    best[k] = fits == null ? null : new Place(fits, poses.get(k));
                }
            }
            if (best[0] != null) {
                return best[0];
            }
            final int along = seen.get(at);
            // Only through a place they could crawl along does it go on, so never through a wall or a closed door.
            if (along >= REACH || fits(level, rider, at, poses.getLast(), frame) == null) {
                continue;
            }
            for (final Direction side : Direction.Plane.HORIZONTAL) {
                for (final int dy : STEPS) {
                    final BlockPos next = at.relative(side).above(dy);
                    if (seen.putIfAbsent(next, along + 1) == null) {
                        open.add(next);
                    }
                }
            }
        }
        for (final Place place : best) {
            if (place != null) {
                return place;
            }
        }
        return null;
    }

    /*
     * Where {@code rider} stands at {@code at} as {@code pose} has them, on the floor right under their feet, low
     * enough and clear of everything; null when they do not. The floor is the top of what stands under them, not of
     * the whole block under them: a structure's blocks are shaped, and a block's highest edge beside them would leave
     * them in the air.
     */
    private static @Nullable Vec3 fits(final Level level, final LivingEntity rider, final BlockPos at,
                                       final Pose pose, final StructureFrame frame) {
        final EntityDimensions size = rider.getDimensions(pose);
        final double half = Math.min(size.width(), 1.0F) / 2.0;
        final double x = at.getX() + HALF;
        final double z = at.getZ() + HALF;
        final AABB under = new AABB(x - half, at.getY() - 1, z - half, x + half, at.getY() + LOW_FLOOR, z + half);
        double floor = Double.NEGATIVE_INFINITY;
        for (final VoxelShape shape : level.getBlockCollisions(rider, under)) {
            for (final AABB part : shape.toAabbs()) {
                if (part.intersects(under)) {
                    floor = Math.max(floor, part.maxY);
                }
            }
        }
        if (floor == Double.NEGATIVE_INFINITY || floor > at.getY() + LOW_FLOOR) {
            return null;
        }
        final Vec3 feet = new Vec3(x, floor, z);
        return DismountHelper.canDismountTo(level, rider, asInTheWorld(frame, feet, half, size.height())) ? feet
                : null;
    }

    /*
     * The box of the yard someone standing at {@code feet} fills, {@code half} wide each way and {@code height} high,
     * as the game keeps their box in the world, square to the world's ways and not the structure's, turned back into
     * the yard: wider than their own where the structure is turned, as its walls meet it, and lifted a hair off the
     * floor, which a structure tipped a little would otherwise have it dip into.
     */
    private static AABB asInTheWorld(final StructureFrame frame, final Vec3 feet, final double half,
                                     final double height) {
        final double wx = frame.worldX(feet.x, feet.y, feet.z);
        final double wy = frame.worldY(feet.x, feet.y, feet.z);
        final double wz = frame.worldZ(feet.x, feet.y, feet.z);
        double minX = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        for (final double dx : new double[] {-half, half}) {
            for (final double dy : new double[] {0, height}) {
                for (final double dz : new double[] {-half, half}) {
                    final double x = frame.yardX(wx + dx, wy + dy, wz + dz);
                    final double z = frame.yardZ(wx + dx, wy + dy, wz + dz);
                    minX = Math.min(minX, x);
                    minZ = Math.min(minZ, z);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, frame.yardY(wx + dx, wy + dy, wz + dz));
                    maxZ = Math.max(maxZ, z);
                }
            }
        }
        return new AABB(minX, feet.y + OFF_THE_FLOOR, minZ, maxX, Math.max(maxY, feet.y + height), maxZ);
    }

    /**
     * Where someone getting up stands, in the yard, and how.
     *
     * @param feet where their feet stand, in the yard
     * @param pose standing, crouching or crawling, as the room there has them
     */
    record Place(Vec3 feet, Pose pose) {
    }
}
