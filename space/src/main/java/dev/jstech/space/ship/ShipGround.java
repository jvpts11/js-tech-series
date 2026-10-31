/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.ship;

import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.StructureFrame;
import dev.jstech.core.region.Box;
import dev.jstech.core.util.Loaded;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * What a ship flying as a moving structure meets of the world's land: it rests on what stands under its lowest blocks,
 * stepping up what is low enough, and it is stopped by land its blocks would go into, as a hill flown into. Each ship's
 * blocks are read once from its yard, its lowest of each column and every block's middle, and again when they change.
 * Land not loaded stops it, as it stops a player. Over the world's build limit, and under its floor, there is no land,
 * only the sky it climbs through to space: there only the world under it must be loaded.
 */
public final class ShipGround {

    /** How high a ship steps up onto what stands under it, in blocks: more is land it runs into. */
    public static final double STEP = 0.6;
    /** Stopped: land it would go into, or land not loaded. */
    public static final Contact BLOCKED = new Contact(0, false, true);
    /** Nothing under it. */
    public static final Contact FREE = new Contact(0, false, false);
    /* How far under the bottom of its lowest blocks a ship's footing is read, in blocks. */
    private static final double UNDER = 1.0e-3;
    /* Each ship's blocks as last read, by the ship. */
    private static final Map<MovingStructure, Points> READ = new WeakHashMap<>();

    private ShipGround() {
    }

    /**
     * What {@code ship}, standing as {@code frame}, meets of the land: whether it rests on it, and how far it must rise
     * to stand on it; or whether land goes into its blocks deeper than a step, or is not loaded.
     */
    public static Contact meet(final ServerLevel level, final MovingStructure ship, final StructureFrame frame) {
        return meet(level, ship, frame, STEP);
    }

    /**
     * What {@code ship}, standing as {@code frame}, meets of the land, rising as far as {@code most} blocks to stand
     * on it: as far as it came down this tick and a step, for a ship coming down onto land.
     */
    public static Contact meet(final ServerLevel level, final MovingStructure ship, final StructureFrame frame,
                               final double most) {
        final Points points = points(level, ship);
        double rise = 0;
        boolean touching = false;
        final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (final double[] feet : points.feet()) {
            final double x = frame.worldX(feet[0], feet[1], feet[2]);
            final double y = frame.worldY(feet[0], feet[1], feet[2]);
            final double z = frame.worldZ(feet[0], feet[1], feet[2]);
            at.set(x, y, z);
            if (!loaded(level, at)) {
                return BLOCKED;
            }
            final double top = top(level, at, x, y, z);
            if (top > y) {
                touching = true;
                rise = Math.max(rise, top - (y + UNDER));
            }
        }
        if (rise > most) {
            return BLOCKED;
        }
        for (final double[] middle : points.middles()) {
            final double x = frame.worldX(middle[0], middle[1], middle[2]);
            final double y = frame.worldY(middle[0], middle[1], middle[2]) + rise;
            final double z = frame.worldZ(middle[0], middle[1], middle[2]);
            at.set(x, y, z);
            if (!loaded(level, at)) {
                return BLOCKED;
            }
            if (top(level, at, x, y, z) > y) {
                return BLOCKED;
            }
        }
        return touching ? new Contact(rise, true, false) : FREE;
    }

    /* The top of the world's block at {@code at} over the point, where the point is in its shape; else -infinity. */
    private static double top(final ServerLevel level, final BlockPos at, final double x, final double y,
                              final double z) {
        final BlockState state = level.getBlockState(at);
        if (state.isAir()) {
            return Double.NEGATIVE_INFINITY;
        }
        final VoxelShape shape = state.getCollisionShape(level, at);
        double top = Double.NEGATIVE_INFINITY;
        for (final AABB part : shape.toAabbs()) {
            final AABB placed = part.move(at);
            if (x >= placed.minX && x <= placed.maxX && z >= placed.minZ && z <= placed.maxZ && y >= placed.minY
                    && y <= placed.maxY) {
                top = Math.max(top, placed.maxY);
            }
        }
        return top;
    }

    /* Whether the world at {@code at} is loaded: over the build limit or under the floor, the column under it. */
    private static boolean loaded(final ServerLevel level, final BlockPos at) {
        return level.isOutsideBuildHeight(at) ? Loaded.chunk(level, at.getX() >> 4, at.getZ() >> 4)
                : Loaded.at(level, at);
    }

    /* {@code ship}'s blocks, read again when its box or its count of blocks changed. */
    private static Points points(final ServerLevel level, final MovingStructure ship) {
        final Points known = READ.get(ship);
        if (known != null && known.bounds().equals(ship.bounds()) && known.blocks() == ship.blocks()) {
            return known;
        }
        final Points read = read(level, ship);
        READ.put(ship, read);
        return read;
    }

    /*
     * The bottom of the lowest block of each column of the ship's yard that collides, and the middle of every block
     * that does, in the yard's places.
     */
    private static Points read(final ServerLevel level, final MovingStructure ship) {
        final Box box = ship.bounds();
        final List<double[]> feet = new ArrayList<>();
        final List<double[]> middles = new ArrayList<>();
        final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                boolean footed = false;
                for (int y = box.minY(); y <= box.maxY(); y++) {
                    at.set(x, y, z);
                    final BlockState state = level.getBlockState(at);
                    if (state.isAir()) {
                        continue;
                    }
                    final VoxelShape shape = state.getCollisionShape(level, at);
                    if (shape.isEmpty()) {
                        continue;
                    }
                    if (!footed) {
                        feet.add(new double[] {x + 0.5, y + shape.min(Direction.Axis.Y) - UNDER, z + 0.5});
                        footed = true;
                    }
                    middles.add(new double[] {x + 0.5, y + 0.5, z + 0.5});
                }
            }
        }
        return new Points(box, ship.blocks(), List.copyOf(feet), List.copyOf(middles));
    }

    /**
     * What a ship meets of the land.
     *
     * @param rise     how far it must rise to stand on the land, in blocks
     * @param touching whether it rests on the land
     * @param blocked  whether land goes into its blocks deeper than a step, or is not loaded
     */
    public record Contact(double rise, boolean touching, boolean blocked) {
    }

    /* A ship's blocks as read: its box and count then, its feet and the middles of its blocks. */
    private record Points(Box bounds, int blocks, List<double[]> feet, List<double[]> middles) {
    }
}
