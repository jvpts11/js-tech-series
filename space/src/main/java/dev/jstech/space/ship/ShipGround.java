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
import dev.jstech.core.moving.SweptBoxes;
import dev.jstech.core.region.Box;
import dev.jstech.core.util.Loaded;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * What a ship flying as a moving structure meets of the world's land: it rests on what stands under its lowest blocks,
 * stepping up what is low enough, and it is stopped by land its blocks would go into, as a hill flown into: any of its
 * blocks' boxes, however it is turned, so a beam thinner than a block cannot pass between two of its blocks. Each
 * ship's blocks are read once from its yard, its lowest of each column and every block's boxes, and again when they
 * change. Land not loaded stops it, as it stops a player. Over the world's build limit, and under its floor, there is
 * no land, only the sky it climbs through to space: there only the world under it must be loaded.
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
        if (rise > most || inLand(level, frame, points.solids(), rise)) {
            return BLOCKED;
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

    /*
     * Whether land goes into any of the ship's blocks {@code solids}, the ship standing as {@code frame} risen
     * {@code rise}, or is not loaded where they reach: each block's cube, turned as the ship stands, reaches over some
     * of the world's blocks, and each of their boxes, seen from the ship, is tried against the block's own boxes.
     */
    private static boolean inLand(final ServerLevel level, final StructureFrame frame, final List<Solid> solids,
                                  final double rise) {
        final double[] rows = new double[9];
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                rows[row * 3 + column] = frame.rotation(row, column);
            }
        }
        final SweptBoxes.Turned seen = new SweptBoxes.Turned();
        final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (final Solid solid : solids) {
            final double[] reach = frame.worldBox(solid.x(), solid.y(), solid.z(), solid.x() + 1, solid.y() + 1,
                    solid.z() + 1);
            for (int x = Mth.floor(reach[0]); x <= Mth.floor(reach[3] - SweptBoxes.SKIN); x++) {
                for (int z = Mth.floor(reach[2]); z <= Mth.floor(reach[5] - SweptBoxes.SKIN); z++) {
                    for (int y = Mth.floor(reach[1] + rise); y <= Mth.floor(reach[4] + rise - SweptBoxes.SKIN);
                         y++) {
                        at.set(x, y, z);
                        if (!loaded(level, at)) {
                            return true;
                        }
                        final BlockState state = level.getBlockState(at);
                        if (state.isAir()) {
                            continue;
                        }
                        for (final AABB part : state.getCollisionShape(level, at).toAabbs()) {
                            // The ship risen is the land lowered, as the ship sees it.
                            seen(frame, part.move(x, y - rise, z), rows, seen);
                            if (overlaps(seen, solid.boxes())) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    /* Sets {@code seen} to the world's box {@code box} as the ship standing as {@code frame} sees it, in its yard. */
    private static void seen(final StructureFrame frame, final AABB box, final double[] rows,
                             final SweptBoxes.Turned seen) {
        final double cx = (box.minX + box.maxX) / 2;
        final double cy = (box.minY + box.maxY) / 2;
        final double cz = (box.minZ + box.maxZ) / 2;
        seen.set(frame.yardX(cx, cy, cz), frame.yardY(cx, cy, cz), frame.yardZ(cx, cy, cz),
                (box.maxX - box.minX) / 2, (box.maxY - box.minY) / 2, (box.maxZ - box.minZ) / 2, rows);
    }

    /* Whether {@code seen} goes into any of {@code boxes}, six numbers each in the yard, more than touching it. */
    private static boolean overlaps(final SweptBoxes.Turned seen, final double[] boxes) {
        for (int i = 0; i < boxes.length; i += 6) {
            final SweptBoxes.Contact touch = SweptBoxes.sweep(seen, 0, 0, 0, boxes[i], boxes[i + 1], boxes[i + 2],
                    boxes[i + 3], boxes[i + 4], boxes[i + 5]);
            if (touch != null && touch.overlapping()) {
                return true;
            }
        }
        return false;
    }

    /* Whether the world at {@code at} is loaded: over the build limit or under the floor, the column under it. */
    private static boolean loaded(final ServerLevel level, final BlockPos at) {
        return level.isOutsideBuildHeight(at) ? Loaded.chunk(level, at.getX() >> 4, at.getZ() >> 4)
                : Loaded.at(level, at);
    }

    /* {@code ship}'s blocks, read again when its box changed or any of its blocks did. */
    private static Points points(final ServerLevel level, final MovingStructure ship) {
        final Points known = READ.get(ship);
        if (known != null && known.bounds().equals(ship.bounds()) && known.changes() == ship.changes()) {
            return known;
        }
        final Points read = read(level, ship);
        READ.put(ship, read);
        return read;
    }

    /*
     * The bottom of the lowest block of each column of the ship's yard that collides, and the boxes of every block
     * that does, in the yard's places.
     */
    private static Points read(final ServerLevel level, final MovingStructure ship) {
        final Box box = ship.bounds();
        final List<double[]> feet = new ArrayList<>();
        final List<Solid> solids = new ArrayList<>();
        final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                boolean footed = false;
                for (int y = box.minY(); y <= box.maxY(); y++) {
                    at.set(x, y, z);
                    final BlockState state = level.getBlockState(at);
                    // Its boarding stair let down rests on the land as the ship does, never what it stands on.
                    if (state.isAir() || state.is(ShipStairs.BLOCK.get())) {
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
                    final List<AABB> parts = shape.toAabbs();
                    final double[] boxes = new double[parts.size() * 6];
                    for (int i = 0; i < parts.size(); i++) {
                        final AABB part = parts.get(i);
                        boxes[i * 6] = x + part.minX;
                        boxes[i * 6 + 1] = y + part.minY;
                        boxes[i * 6 + 2] = z + part.minZ;
                        boxes[i * 6 + 3] = x + part.maxX;
                        boxes[i * 6 + 4] = y + part.maxY;
                        boxes[i * 6 + 5] = z + part.maxZ;
                    }
                    solids.add(new Solid(x, y, z, boxes));
                }
            }
        }
        return new Points(box, ship.changes(), List.copyOf(feet), List.copyOf(solids));
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

    /* A ship's blocks as read: its box and how many times its blocks had changed then, its feet and its solids. */
    private record Points(Box bounds, int changes, List<double[]> feet, List<Solid> solids) {
    }

    /* A block of a ship that collides, at {@code (x, y, z)} in its yard, its boxes six numbers each in the yard. */
    private record Solid(int x, int y, int z, double[] boxes) {
    }
}
