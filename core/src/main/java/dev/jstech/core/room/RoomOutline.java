/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.room;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * The outline of a room as it is drawn: the edges where its surface turns, round its outer corners and into its inner
 * ones, each run of them along a line joined into one segment. A box of a room is its twelve edges, however large.
 */
public final class RoomOutline {

    /** The most segments an outline holds; a room more ragged than this is drawn by its first ones. */
    public static final int MOST = 4_096;

    private RoomOutline() {
    }

    /** The outline of a room of {@code cells}, bounded by {@code faces}. */
    public static List<Segment> of(final LongSet cells, final List<RoomFace> faces) {
        final Map<Line, IntArrayList> lines = new LinkedHashMap<>();
        for (final RoomFace face : faces) {
            final BlockPos cell = face.cell();
            final Direction out = face.side();
            for (final Direction along : Direction.values()) {
                if (along.getAxis() == out.getAxis()) {
                    continue;
                }
                // The face runs on flat into the next block's face there: no edge.
                final BlockPos next = cell.relative(along);
                if (cells.contains(next.asLong()) && !cells.contains(next.relative(out).asLong())) {
                    continue;
                }
                edge(lines, cell, out, along);
            }
        }
        final List<Segment> segments = new ArrayList<>();
        for (final Map.Entry<Line, IntArrayList> line : lines.entrySet()) {
            join(line.getKey(), line.getValue(), segments);
            if (segments.size() >= MOST) {
                return List.copyOf(segments.subList(0, MOST));
            }
        }
        return List.copyOf(segments);
    }

    /* Notes the edge of {@code cell}'s face towards {@code out} on its side towards {@code along}. */
    private static void edge(final Map<Line, IntArrayList> lines, final BlockPos cell, final Direction out,
                             final Direction along) {
        BlockPos corner = cell;
        if (out.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
            corner = corner.relative(out);
        }
        if (along.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
            corner = corner.relative(along);
        }
        final Direction.Axis axis = third(out.getAxis(), along.getAxis());
        final Line line = switch (axis) {
            case X -> new Line(axis, corner.getY(), corner.getZ());
            case Y -> new Line(axis, corner.getX(), corner.getZ());
            case Z -> new Line(axis, corner.getX(), corner.getY());
        };
        final IntArrayList starts = lines.computeIfAbsent(line, ignored -> new IntArrayList());
        final int start = axis.choose(corner.getX(), corner.getY(), corner.getZ());
        if (!starts.contains(start)) {
            starts.add(start);
        }
    }

    /* Joins the unit edges of one line into segments, each run of them one. */
    private static void join(final Line line, final IntArrayList starts, final List<Segment> into) {
        final int[] sorted = starts.toIntArray();
        IntArrays.quickSort(sorted);
        int runStart = sorted[0];
        int runEnd = sorted[0] + 1;
        for (int i = 1; i <= sorted.length; i++) {
            if (i < sorted.length && sorted[i] == runEnd) {
                runEnd++;
                continue;
            }
            into.add(line.segment(runStart, runEnd));
            if (i < sorted.length) {
                runStart = sorted[i];
                runEnd = sorted[i] + 1;
            }
        }
    }

    /* The axis neither {@code a} nor {@code b}. */
    private static Direction.Axis third(final Direction.Axis a, final Direction.Axis b) {
        if (a != Direction.Axis.X && b != Direction.Axis.X) {
            return Direction.Axis.X;
        }
        return a != Direction.Axis.Y && b != Direction.Axis.Y ? Direction.Axis.Y : Direction.Axis.Z;
    }

    /**
     * A straight run of a room's outline, from one corner to another, in block coordinates.
     *
     * @param x1 where it starts, across
     * @param y1 where it starts, up
     * @param z1 where it starts, along
     * @param x2 where it ends, across
     * @param y2 where it ends, up
     * @param z2 where it ends, along
     */
    public record Segment(int x1, int y1, int z1, int x2, int y2, int z2) {
    }

    /*
     * The line a run of edges lies on: its axis, and its place along the other two, in the order x, y, z.
     */
    private record Line(Direction.Axis axis, int first, int second) {

        /* The run from {@code from} to {@code to} along this line. */
        Segment segment(final int from, final int to) {
            return switch (this.axis) {
                case X -> new Segment(from, this.first, this.second, to, this.first, this.second);
                case Y -> new Segment(this.first, from, this.second, this.first, to, this.second);
                case Z -> new Segment(this.first, this.second, from, this.first, this.second, to);
            };
        }
    }
}
