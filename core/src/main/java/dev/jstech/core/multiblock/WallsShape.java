/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

/**
 * A floor of any outline with walls round it, as a smeltery: the controller stands in a wall, the inside behind it is
 * filled out over the floor to whatever outline the walls close, and the walls rise as high as they run unbroken,
 * within the limits. The corners where two walls meet are not needed. The inside is read by its {@link InsideRule}.
 * Pure.
 *
 * @param floor  what fits the floor, under every place of the inside
 * @param wall   what fits the walls
 * @param inside what may stand inside and what it gives
 * @param area   the least and the most places the floor may have
 * @param height the least and the most the walls may be high, the floor not counted
 * @param limits the least and the most of each count, by name
 */
public record WallsShape(Place floor, Place wall, InsideRule inside, Limit area, Limit height,
                         Map<String, Limit> limits) implements RuleShape {

    /* The four ways the inside can lie from the controller: north, east, south, west. */
    private static final int[][] INWARD = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};

    public WallsShape {
        Objects.requireNonNull(floor, "floor");
        Objects.requireNonNull(wall, "wall");
        inside.requireKnownParts();
        RuleShape.requireWithin(area, MOST_AREA, "area");
        RuleShape.requireWithin(height, MOST_SIDE, "height");
        limits = Map.copyOf(limits);
    }

    /** A floor and walls with an inside, its area and height within those limits, limiting no count. */
    public static WallsShape of(final Place floor, final Place wall, final InsideRule inside, final Limit area,
                                final Limit height) {
        return new WallsShape(floor, wall, inside, area, height, Map.of());
    }

    /** The same, keeping the count {@code count} between {@code least} and {@code most}. */
    public WallsShape limit(final String count, final int least, final int most) {
        final Map<String, Limit> more = new LinkedHashMap<>(this.limits);
        more.put(count, new Limit(least, most));
        return new WallsShape(this.floor, this.wall, this.inside, this.area, this.height, more);
    }

    @Override
    public ShapeMatch check(final IBlockProvider provider, final long controller) {
        final Cells.Best best = new Cells.Best();
        for (final int[] inward : INWARD) {
            final ShapeTally tally = new ShapeTally();
            final ShapeMatch result = attempt(provider, controller, inward, tally);
            if (result instanceof ShapeMatch.Failure failure) {
                best.offer(failure, tally.matched());
            } else {
                return result;
            }
        }
        return best.failure();
    }

    /**
     * As far as it reads: a floor no farther from the controller than its area allows in a line, nor than the most a
     * side of a shape may be, with the walls beyond it; and its walls' most height.
     */
    @Override
    public int reach() {
        return Math.max(span(), this.height.most()) + 1;
    }

    @Override
    public @Nullable IBlockMatcher wanted(final String wanted) {
        return switch (wanted) {
            case ShapeMatch.FLOOR -> this.floor.fits();
            case ShapeMatch.WALL -> this.wall.fits();
            case ShapeMatch.INSIDE -> this.inside.allows();
            default -> null;
        };
    }

    /*
     * One attempt, the inside lying one way from the controller: down to the floor from the place behind it, the
     * floor's outline filled out at that level, then the walls read layer by layer up from it.
     */
    private ShapeMatch attempt(final IBlockProvider provider, final long controller, final int[] inward,
                               final ShapeTally tally) {
        final long start = Cells.offset(controller, inward[0], 0, inward[1]);
        if (!fitsInside(provider, start)) {
            return Cells.wrongBlock(controller, start, ShapeMatch.INSIDE, Cells.blockAt(provider, start));
        }
        long low = start;
        int down = 0;
        while (fitsInside(provider, Cells.offset(low, 0, -1, 0))) {
            low = Cells.offset(low, 0, -1, 0);
            if (++down >= this.height.most()) {
                return Cells.wrongSize(controller, ShapeMatch.SIZE_Y, down + 1);
            }
        }
        final Set<Long> region = new LinkedHashSet<>();
        final Set<Long> walls = new LinkedHashSet<>();
        final Deque<Long> queue = new ArrayDeque<>();
        final int[] c = PatternMatcher.decodePosition(controller);
        final int span = span();
        region.add(low);
        queue.add(low);
        while (!queue.isEmpty()) {
            final long at = queue.poll();
            for (final int[] side : INWARD) {
                final long next = Cells.offset(at, side[0], 0, side[1]);
                if (region.contains(next) || walls.contains(next)) {
                    continue;
                }
                if (next != controller && fitsInside(provider, next)) {
                    region.add(next);
                    tally.fits();
                    final int[] xyz = PatternMatcher.decodePosition(next);
                    if (region.size() > this.area.most() || Math.abs(xyz[0] - c[0]) > span
                            || Math.abs(xyz[2] - c[2]) > span) {
                        return Cells.wrongSize(controller, ShapeMatch.AREA, region.size());
                    }
                    queue.add(next);
                } else {
                    walls.add(next);
                }
            }
        }
        if (region.size() < this.area.least()) {
            return Cells.wrongSize(controller, ShapeMatch.AREA, region.size());
        }
        final List<Long> parts = new ArrayList<>();
        for (final long cell : region) {
            final long under = Cells.offset(cell, 0, -1, 0);
            final String id = Cells.blockAt(provider, under);
            if (!this.floor.fits().matchesAt(provider, under, id)) {
                return Cells.wrongBlock(controller, under, ShapeMatch.FLOOR, id);
            }
            tally.place(this.floor, provider, under, id);
            parts.add(under);
        }
        final int controllerLayer = PatternMatcher.decodePosition(controller)[1]
                - PatternMatcher.decodePosition(low)[1];
        final List<Long> within = new ArrayList<>();
        int layers = 0;
        while (layers < this.height.most()) {
            final Long broken = brokenWall(provider, walls, layers, controller);
            if (broken != null) {
                if (layers <= controllerLayer || layers < this.height.least()) {
                    return Cells.wrongBlock(controller, broken, ShapeMatch.WALL, Cells.blockAt(provider, broken));
                }
                break;
            }
            for (final long column : walls) {
                final long pos = Cells.offset(column, 0, layers, 0);
                if (pos != controller) {
                    tally.place(this.wall, provider, pos, Cells.blockAt(provider, pos));
                    parts.add(pos);
                }
            }
            for (final long cell : region) {
                within.add(Cells.offset(cell, 0, layers, 0));
            }
            layers++;
        }
        final ShapeMatch.Failure wrong = this.inside.read(provider, within, controller, tally);
        if (wrong != null) {
            return wrong;
        }
        final ShapeMatch.Failure counts = tally.outOfLimits(this.limits, controller);
        if (counts != null) {
            return counts;
        }
        final int[] lo = {Integer.MAX_VALUE, 0, Integer.MAX_VALUE};
        final int[] hi = {Integer.MIN_VALUE, 0, Integer.MIN_VALUE};
        for (final long column : walls) {
            final int[] xyz = PatternMatcher.decodePosition(column);
            lo[0] = Math.min(lo[0], xyz[0]);
            hi[0] = Math.max(hi[0], xyz[0]);
            lo[2] = Math.min(lo[2], xyz[2]);
            hi[2] = Math.max(hi[2], xyz[2]);
        }
        return new ShapeMatch.Formed(Orientation.NORTH, controller, parts, Map.of(), within, hi[0] - lo[0] + 1,
                layers + 1, hi[2] - lo[2] + 1, 1, tally.counts(), tally.values());
    }

    /* The first place of the walls' layer that is not a wall, or null when the layer is whole. */
    @Nullable
    private Long brokenWall(final IBlockProvider provider, final Set<Long> walls, final int layer,
                            final long controller) {
        for (final long column : walls) {
            final long pos = Cells.offset(column, 0, layer, 0);
            if (pos != controller && !this.wall.fits().matchesAt(provider, pos, Cells.blockAt(provider, pos))) {
                return pos;
            }
        }
        return null;
    }

    private boolean fitsInside(final IBlockProvider provider, final long pos) {
        return this.inside.fits(provider, pos, Cells.blockAt(provider, pos));
    }

    /* The farthest along x or z a place of the floor may be from the controller. */
    private int span() {
        return Math.min(this.area.most(), MOST_SIDE);
    }
}
