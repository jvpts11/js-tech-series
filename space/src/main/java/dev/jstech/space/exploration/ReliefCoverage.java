/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.exploration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * What a team has seen of a world's ground: the strips its crews photographed from orbit, each under one pass of the
 * orbit's ground track, which swings north and south of the equator as the world turns under it, and the places its
 * crews stood on. Beside what the team knows of the world's relief from its instruments, it is where the landing map
 * shows the ground; the rest lies under the grey of the unknown.
 *
 * <p>Pure: no game types.
 *
 * @param strips  where each photographed pass's ground track crosses the equator going north, in degrees east
 * @param visited the places stood on, as cells {@link #VISIT_CELL} blocks wide, each packed by {@link #cell}
 */
public record ReliefCoverage(List<Double> strips, Set<Long> visited) {

    /** Nothing seen. */
    public static final ReliefCoverage NONE = new ReliefCoverage(List.of(), Set.of());
    /** How far north and south a low orbit's ground track swings, and how far either side of it its camera sees. */
    public static final double TRACK = 25;
    public static final double SWATH = 6;
    /**
     * How long a pass of a low orbit takes, in ticks, as the world slides under it in the window, and how far the world
     * turns under it between two passes, in degrees: sixteen passes go round it.
     */
    public static final long PASS_TICKS = 1_500;
    public static final double PASS_SHIFT = 22.5;
    public static final int PASSES = 16;
    /** How wide a cell of the ground stood on is, in blocks: a crew sees about so far round where they stand. */
    public static final int VISIT_CELL = 512;
    private static final double ROUND = 360;
    private static final double HALF_ROUND = 180;
    /* How near two passes are taken for the same one, in degrees. */
    private static final double SAME_PASS = 0.5;
    /* The most passes kept, past which the oldest go, and the most cells kept, past which no more are. */
    private static final int MOST_STRIPS = 64;
    private static final int MOST_VISITED = 8_192;
    /* How fine the grid the share seen is reckoned on is, in degrees. */
    private static final double SHARE_STEP = 2;
    private static final int LOW_BITS = 32;
    private static final long LOW_MASK = 0xFFFF_FFFFL;

    public ReliefCoverage {
        strips = List.copyOf(strips);
        visited = Set.copyOf(visited);
    }

    /** The cell a block at {@code x}, {@code z} lies in, packed. */
    public static long cell(final double x, final double z) {
        final long column = (long) Math.floor(x / VISIT_CELL);
        final long row = (long) Math.floor(z / VISIT_CELL);
        return column << LOW_BITS | row & LOW_MASK;
    }

    /** The same with the pass crossing the equator at {@code node} photographed, unless one so near is already. */
    public ReliefCoverage photographed(final double node) {
        final double at = wrap(node);
        for (final double kept : this.strips) {
            if (Math.abs(wrap(kept - at)) < SAME_PASS) {
                return this;
            }
        }
        final List<Double> next = new ArrayList<>(this.strips);
        next.add(at);
        return new ReliefCoverage(next.subList(Math.max(0, next.size() - MOST_STRIPS), next.size()), this.visited);
    }

    /**
     * The same with the ground round a block at {@code x}, {@code z} stood on: its cell and the eight round it, while
     * fewer than the most kept are.
     */
    public ReliefCoverage visitedAt(final double x, final double z) {
        final List<Long> fresh = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                final long round = cell(x + dx * VISIT_CELL, z + dz * VISIT_CELL);
                if (!this.visited.contains(round)) {
                    fresh.add(round);
                }
            }
        }
        if (fresh.isEmpty() || this.visited.size() + fresh.size() > MOST_VISITED) {
            return this;
        }
        final Set<Long> next = new HashSet<>(this.visited);
        next.addAll(fresh);
        return new ReliefCoverage(this.strips, next);
    }

    /**
     * Whether the ground at {@code longitude}, {@code latitude}, the block {@code x}, {@code z}, is known, the team
     * knowing {@code relief} of the world's relief from its instruments: all of it known whole; a share of it as so
     * many of a mapping orbit's passes; and what the team photographed and stood on.
     */
    public boolean known(final double relief, final double longitude, final double latitude, final double x,
                         final double z) {
        return fromOrbit(relief, longitude, latitude) || this.visited.contains(cell(x, z));
    }

    /**
     * How much of the world's ground is known, from 0 to 1, as its area: what the instruments and the passes show; the
     * places stood on are too small to count.
     */
    public double share(final double relief) {
        if (relief >= 1) {
            return 1;
        }
        double seen = 0;
        double all = 0;
        for (double latitude = -90 + SHARE_STEP / 2; latitude < 90; latitude += SHARE_STEP) {
            final double area = Math.cos(Math.toRadians(latitude));
            for (double longitude = -HALF_ROUND + SHARE_STEP / 2; longitude < HALF_ROUND;
                 longitude += SHARE_STEP) {
                all += area;
                if (fromOrbit(relief, longitude, latitude)) {
                    seen += area;
                }
            }
        }
        return all <= 0 ? 0 : seen / all;
    }

    /**
     * The latitudes where the passes known cross {@code longitude}, the team knowing {@code relief} of the world's
     * relief: a mapping orbit's passes for that share, and those photographed; for a whole map of the ground at once.
     */
    public double[] tracks(final double relief, final double longitude) {
        final int mapped = (int) Math.floor(Math.clamp(relief, 0, 1) * PASSES);
        final double[] out = new double[mapped + this.strips.size()];
        for (int pass = 0; pass < mapped; pass++) {
            out[pass] = TRACK * Math.sin(Math.toRadians(longitude - pass * PASS_SHIFT));
        }
        for (int strip = 0; strip < this.strips.size(); strip++) {
            out[mapped + strip] = TRACK * Math.sin(Math.toRadians(longitude - this.strips.get(strip)));
        }
        return out;
    }

    /** Whether {@code latitude} lies in the swath of any of {@code tracks} ({@link #tracks}). */
    public static boolean under(final double[] tracks, final double latitude) {
        for (final double track : tracks) {
            if (Math.abs(latitude - track) < SWATH) {
                return true;
            }
        }
        return false;
    }

    /* Whether the ground at {@code longitude}, {@code latitude} is known from the instruments or the passes. */
    private boolean fromOrbit(final double relief, final double longitude, final double latitude) {
        return relief >= 1 || under(tracks(relief, longitude), latitude);
    }

    /* {@code degrees} brought between -180 and 180. */
    private static double wrap(final double degrees) {
        final double turned = (degrees + HALF_ROUND) % ROUND;
        return (turned < 0 ? turned + ROUND : turned) - HALF_ROUND;
    }
}
