/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Checks a drawn pattern against the world: in each way it may stand, the four turns first, and for a pattern that
 * grows, with its repeated slice there as many times as the world holds it, the most first. Pure.
 */
final class ShapeMatcher {

    private ShapeMatcher() {
    }

    /** The pattern formed with its terminal at {@code terminal}, or the failure of the likeliest attempt. */
    static ShapeMatch match(final MultiblockPattern pattern, final IBlockProvider provider, final long terminal) {
        final Cells.Best best = new Cells.Best();
        final int least = pattern.repeat().map(r -> r.times().least()).orElse(1);
        for (final Orientation orientation : pattern.orientations()) {
            final int most = pattern.repeat().isPresent()
                    ? Math.max(least, copies(pattern, orientation, provider, terminal)) : 1;
            for (int times = most; times >= least; times--) {
                final ShapeTally tally = new ShapeTally();
                final ShapeMatch result = attempt(pattern, pattern.grown(times), orientation, times, provider,
                        terminal, tally);
                if (result instanceof ShapeMatch.Failure failure) {
                    best.offer(failure, tally.matched());
                } else {
                    return result;
                }
            }
        }
        return best.failure();
    }

    /* One attempt: the grown pattern stood one way, every place read in the drawing's order. */
    private static ShapeMatch attempt(final MultiblockPattern pattern, final MultiblockPattern.Grid grid,
                                      final Orientation orientation, final int times, final IBlockProvider provider,
                                      final long terminal, final ShapeTally tally) {
        final int[] c = PatternMatcher.decodePosition(terminal);
        final char insideLetter = pattern.inside().map(MultiblockPattern.Inside::letter)
                .orElse(MultiblockPattern.IGNORE_CHAR);
        final List<Long> parts = new ArrayList<>();
        final List<Long> inside = new ArrayList<>();
        final Map<Long, PortKind> ports = new HashMap<>();
        final int[] min = {0, 0, 0};
        final int[] max = {0, 0, 0};
        final char[][][] cells = grid.cells();
        for (int y = 0; y < cells.length; y++) {
            for (int z = 0; z < cells[y].length; z++) {
                for (int x = 0; x < cells[y][z].length; x++) {
                    final char letter = cells[y][z][x];
                    if (letter == MultiblockPattern.IGNORE_CHAR) {
                        continue;
                    }
                    final int[] w = orientation.apply(x - grid.terminalX(), y - grid.terminalY(),
                            z - grid.terminalZ());
                    for (int axis = 0; axis < 3; axis++) {
                        min[axis] = Math.min(min[axis], w[axis]);
                        max[axis] = Math.max(max[axis], w[axis]);
                    }
                    if (letter == MultiblockPattern.TERMINAL_CHAR) {
                        continue;
                    }
                    final long pos = PatternMatcher.encodePosition(c[0] + w[0], c[1] + w[1], c[2] + w[2]);
                    if (letter == insideLetter) {
                        inside.add(pos);
                        continue;
                    }
                    final Place place = pattern.places().get(letter);
                    final String id = Cells.blockAt(provider, pos);
                    if (!place.fits().matchesAt(provider, pos, id)) {
                        return Cells.wrongBlock(terminal, pos, String.valueOf(letter), id);
                    }
                    tally.place(place, provider, pos, id);
                    parts.add(pos);
                    // An entry standing at a place makes it the port of its own kind; else the letter's port, if any.
                    final Place.Entry entry = place.entryAt(provider, pos, id);
                    final PortKind port = entry != null ? entry.port() : pattern.ports().get(letter);
                    if (port != null) {
                        ports.put(pos, port);
                    }
                }
            }
        }
        if (pattern.inside().isPresent()) {
            final ShapeMatch.Failure wrong = pattern.inside().get().rule().read(provider, inside, terminal, tally);
            if (wrong != null) {
                return wrong;
            }
        }
        final ShapeMatch.Failure counts = tally.outOfLimits(pattern.limits(), terminal);
        if (counts != null) {
            return counts;
        }
        final ShapeMatch.Failure apart = tally.apart(pattern.together(), terminal);
        if (apart != null) {
            return apart;
        }
        return new ShapeMatch.Formed(orientation, terminal, parts, ports, inside, max[0] - min[0] + 1,
                max[1] - min[1] + 1, max[2] - min[2] + 1, times, tally.counts(), tally.values());
    }

    /*
     * How many times, up to its most, the repeated slice stands in the world one after another, from the copy nearest
     * the terminal outwards: the times to try first, so a structure that grows is found in one attempt.
     */
    private static int copies(final MultiblockPattern pattern, final Orientation orientation,
                              final IBlockProvider provider, final long terminal) {
        final MultiblockPattern.Repeat repeat = pattern.repeat().orElseThrow();
        final int axis = repeat.axis();
        final int[] c = PatternMatcher.decodePosition(terminal);
        final int[] from = {pattern.terminalX(), pattern.terminalY(), pattern.terminalZ()};
        final int step = from[axis] < repeat.index() ? 1 : -1;
        final int[] size = {pattern.sizeX(), pattern.sizeY(), pattern.sizeZ()};
        final int[] others = otherAxes(axis);
        final char insideLetter = pattern.inside().map(MultiblockPattern.Inside::letter)
                .orElse(MultiblockPattern.IGNORE_CHAR);
        int count = 0;
        for (int copy = 0; copy < repeat.times().most(); copy++) {
            for (int a = 0; a < size[others[0]]; a++) {
                for (int b = 0; b < size[others[1]]; b++) {
                    final int[] p = new int[3];
                    p[axis] = repeat.index();
                    p[others[0]] = a;
                    p[others[1]] = b;
                    final char letter = pattern.charAt(p[0], p[1], p[2]);
                    if (letter == MultiblockPattern.IGNORE_CHAR) {
                        continue;
                    }
                    final int[] rel = {p[0] - from[0], p[1] - from[1], p[2] - from[2]};
                    rel[axis] += step * copy;
                    final int[] w = orientation.apply(rel[0], rel[1], rel[2]);
                    final long pos = PatternMatcher.encodePosition(c[0] + w[0], c[1] + w[1], c[2] + w[2]);
                    final String id = Cells.blockAt(provider, pos);
                    final boolean fits = letter == insideLetter
                            ? pattern.inside().orElseThrow().rule().fits(provider, pos, id)
                            : pattern.places().get(letter).fits().matchesAt(provider, pos, id);
                    if (!fits) {
                        return count;
                    }
                }
            }
            count++;
        }
        return count;
    }

    private static int[] otherAxes(final int axis) {
        return switch (axis) {
            case MultiblockPattern.AXIS_X -> new int[] {MultiblockPattern.AXIS_Y, MultiblockPattern.AXIS_Z};
            case MultiblockPattern.AXIS_Y -> new int[] {MultiblockPattern.AXIS_X, MultiblockPattern.AXIS_Z};
            default -> new int[] {MultiblockPattern.AXIS_X, MultiblockPattern.AXIS_Y};
        };
    }
}
