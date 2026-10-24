/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;

/**
 * Any box between a least and a most size, as a dynamic tank: a frame on its twelve edges, faces between them (casing,
 * glass, ports), and an inside read by its {@link InsideRule}. The terminal stands anywhere on its shell; the box is
 * found from it, walking along the shell to its edges, so it is whatever size the player built. Pure.
 *
 * @param frame  what fits the edges
 * @param face   what fits the faces between the edges
 * @param inside what may stand inside and what it gives
 * @param sides  the least and the most each horizontal side may be, in blocks, the shell counted in
 * @param height the least and the most it may be high
 * @param limits the least and the most of each count, by name
 */
public record BoxShape(Place frame, Place face, InsideRule inside, Limit sides, Limit height,
                       Map<String, Limit> limits) implements RuleShape {

    /* The six ways the inside can lie from the terminal: south, north, east, west, below, above. */
    private static final int[][] INWARD = {{0, 0, 1}, {0, 0, -1}, {1, 0, 0}, {-1, 0, 0}, {0, -1, 0}, {0, 1, 0}};

    public BoxShape {
        Objects.requireNonNull(frame, "frame");
        Objects.requireNonNull(face, "face");
        inside.requireKnownParts();
        RuleShape.requireWithin(sides, MOST_SIDE, "side");
        RuleShape.requireWithin(height, MOST_SIDE, "height");
        limits = Map.copyOf(limits);
    }

    /** A box with a frame, faces and an inside, its sides and height within those limits, limiting no count. */
    public static BoxShape of(final Place frame, final Place face, final InsideRule inside, final Limit sides,
                              final Limit height) {
        return new BoxShape(frame, face, inside, sides, height, Map.of());
    }

    /** The same, keeping the count {@code count} between {@code least} and {@code most}. */
    public BoxShape limit(final String count, final int least, final int most) {
        final Map<String, Limit> more = new LinkedHashMap<>(this.limits);
        more.put(count, new Limit(least, most));
        return new BoxShape(this.frame, this.face, this.inside, this.sides, this.height, more);
    }

    @Override
    public ShapeMatch check(final IBlockProvider provider, final long terminal) {
        final Cells.Best best = new Cells.Best();
        for (final int[] inward : INWARD) {
            final ShapeTally tally = new ShapeTally();
            final ShapeMatch result = attempt(provider, terminal, inward, tally);
            if (result instanceof ShapeMatch.Failure failure) {
                best.offer(failure, tally.matched());
            } else {
                return result;
            }
        }
        return best.failure();
    }

    /** As far as its walks read: its most side or height from the terminal, and one beyond. */
    @Override
    public int reach() {
        return Math.max(this.sides.most(), this.height.most()) + 1;
    }

    @Override
    public Optional<ShapePreview> preview(final int grow) {
        return Optional.of(ShapePreviews.box(this, grow));
    }

    @Override
    public @Nullable IBlockMatcher wanted(final String wanted) {
        return switch (wanted) {
            case ShapeMatch.FRAME -> this.frame.fits();
            case ShapeMatch.FACE -> this.face.fits();
            case ShapeMatch.INSIDE -> this.inside.allows();
            default -> null;
        };
    }

    /*
     * One attempt, the inside lying one way from the terminal: the terminal's face found by walking along the
     * shell from it, the depth by walking along an edge from the face's corner, then every place of the box read.
     */
    private ShapeMatch attempt(final IBlockProvider provider, final long terminal, final int[] inward,
                               final ShapeTally tally) {
        final int axis = inward[0] != 0 ? 0 : inward[1] != 0 ? 1 : 2;
        final int u = axis == 0 ? 1 : 0;
        final int v = axis == 2 ? 1 : 2;
        final int[] c = PatternMatcher.decodePosition(terminal);
        final int[] lo = c.clone();
        final int[] hi = c.clone();
        extend(provider, c, u, lo, hi, tally);
        extend(provider, c, v, lo, hi, tally);
        // Along the face's edges too, so one block missing on the terminal's row is told as missing.
        for (int round = 0; round < 2; round++) {
            for (final int along : new int[] {u, v}) {
                final int across = along == u ? v : u;
                for (final int at : new int[] {lo[across], hi[across]}) {
                    final int[] from = c.clone();
                    from[across] = at;
                    extend(provider, from, along, lo, hi, tally);
                }
            }
        }
        // The depth along each of the four edges that run inward, so one broken edge does not hide it.
        int depth = 0;
        for (final int atU : new int[] {lo[u], hi[u]}) {
            for (final int atV : new int[] {lo[v], hi[v]}) {
                final int[] corner = c.clone();
                corner[u] = atU;
                corner[v] = atV;
                depth = Math.max(depth, walk(provider, corner, axis, inward[axis], most(axis), tally));
            }
        }
        if (inward[axis] > 0) {
            hi[axis] = c[axis] + depth;
        } else {
            lo[axis] = c[axis] - depth;
        }
        final String[] names = {ShapeMatch.SIZE_X, ShapeMatch.SIZE_Y, ShapeMatch.SIZE_Z};
        for (int each = 0; each < 3; each++) {
            final int size = hi[each] - lo[each] + 1;
            if (!(each == 1 ? this.height : this.sides).contains(size)) {
                return Cells.wrongSize(terminal, names[each], size);
            }
        }
        final List<Long> parts = new ArrayList<>();
        final List<Long> within = new ArrayList<>();
        for (int y = lo[1]; y <= hi[1]; y++) {
            for (int z = lo[2]; z <= hi[2]; z++) {
                for (int x = lo[0]; x <= hi[0]; x++) {
                    final long pos = PatternMatcher.encodePosition(x, y, z);
                    if (pos == terminal) {
                        continue;
                    }
                    final int bounds = (x == lo[0] || x == hi[0] ? 1 : 0) + (y == lo[1] || y == hi[1] ? 1 : 0)
                            + (z == lo[2] || z == hi[2] ? 1 : 0);
                    if (bounds == 0) {
                        within.add(pos);
                        continue;
                    }
                    final Place place = bounds >= 2 ? this.frame : this.face;
                    final String id = Cells.blockAt(provider, pos);
                    if (!place.fits().matchesAt(provider, pos, id)) {
                        return Cells.wrongBlock(terminal, pos, bounds >= 2 ? ShapeMatch.FRAME : ShapeMatch.FACE, id);
                    }
                    tally.place(place, provider, pos, id);
                    parts.add(pos);
                }
            }
        }
        final ShapeMatch.Failure wrong = this.inside.read(provider, within, terminal, tally);
        if (wrong != null) {
            return wrong;
        }
        final ShapeMatch.Failure counts = tally.outOfLimits(this.limits, terminal);
        if (counts != null) {
            return counts;
        }
        return new ShapeMatch.Formed(Orientation.NORTH, terminal, parts, Map.of(), within, hi[0] - lo[0] + 1,
                hi[1] - lo[1] + 1, hi[2] - lo[2] + 1, 1, tally.counts(), tally.values());
    }

    /* Widens the box along an axis to as far as the shell runs both ways from a place. */
    private void extend(final IBlockProvider provider, final int[] from, final int axis, final int[] lo,
                        final int[] hi, final ShapeTally tally) {
        lo[axis] = Math.min(lo[axis], from[axis] - walk(provider, from, axis, -1, most(axis), tally));
        hi[axis] = Math.max(hi[axis], from[axis] + walk(provider, from, axis, 1, most(axis), tally));
    }

    /*
     * How far the shell runs from a place one way along an axis, as far as the most a side may be, so a box too large
     * is seen as too large rather than found smaller. Each place walked is progress, so of the ways tried, the one
     * that ran along the most shell is the one whose failure is told.
     */
    private int walk(final IBlockProvider provider, final int[] from, final int axis, final int way,
                     final int most, final ShapeTally tally) {
        final int[] at = from.clone();
        for (int step = 1; step <= most; step++) {
            at[axis] = from[axis] + way * step;
            final long pos = PatternMatcher.encodePosition(at[0], at[1], at[2]);
            final String id = Cells.blockAt(provider, pos);
            if (!this.frame.fits().matchesAt(provider, pos, id) && !this.face.fits().matchesAt(provider, pos, id)) {
                return step - 1;
            }
            tally.fits();
        }
        return most;
    }

    private int most(final int axis) {
        return axis == 1 ? this.height.most() : this.sides.most();
    }
}
