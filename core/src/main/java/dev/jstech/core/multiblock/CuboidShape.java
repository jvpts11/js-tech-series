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
 * Any cuboid filled with units, as a crafting processor: every place of it holds a unit, the terminal one of them,
 * and its size is however far the units run from the terminal along each axis, within the limits. The kinds of
 * units are counted through the unit's place ("at least one storage unit"). Pure.
 *
 * @param unit   what fits each place, and what each counts and gives
 * @param sides  the least and the most each side may be, in blocks
 * @param limits the least and the most of each count, by name
 */
public record CuboidShape(Place unit, Limit sides, Map<String, Limit> limits) implements RuleShape {

    public CuboidShape {
        Objects.requireNonNull(unit, "unit");
        RuleShape.requireWithin(sides, MOST_SIDE, "side");
        limits = Map.copyOf(limits);
    }

    /** A cuboid of units, its sides within that limit, limiting no count. */
    public static CuboidShape of(final Place unit, final Limit sides) {
        return new CuboidShape(unit, sides, Map.of());
    }

    /** The same, keeping the count {@code count} between {@code least} and {@code most}. */
    public CuboidShape limit(final String count, final int least, final int most) {
        final Map<String, Limit> more = new LinkedHashMap<>(this.limits);
        more.put(count, new Limit(least, most));
        return new CuboidShape(this.unit, this.sides, more);
    }

    @Override
    public ShapeMatch check(final IBlockProvider provider, final long terminal) {
        final int[] c = PatternMatcher.decodePosition(terminal);
        final int[] lo = c.clone();
        final int[] hi = c.clone();
        final String[] names = {ShapeMatch.SIZE_X, ShapeMatch.SIZE_Y, ShapeMatch.SIZE_Z};
        for (int axis = 0; axis < 3; axis++) {
            extend(provider, c, axis, lo, hi);
        }
        // Along its edges too, so a unit missing on a line through the terminal is told as missing rather than
        // leaving a smaller cuboid that forms.
        for (int round = 0; round < 2; round++) {
            for (int axis = 0; axis < 3; axis++) {
                final int b = (axis + 1) % 3;
                final int d = (axis + 2) % 3;
                for (final int atB : new int[] {lo[b], hi[b]}) {
                    for (final int atD : new int[] {lo[d], hi[d]}) {
                        final int[] from = c.clone();
                        from[b] = atB;
                        from[d] = atD;
                        extend(provider, from, axis, lo, hi);
                    }
                }
            }
        }
        for (int axis = 0; axis < 3; axis++) {
            final int size = hi[axis] - lo[axis] + 1;
            if (!this.sides.contains(size)) {
                return Cells.wrongSize(terminal, names[axis], size);
            }
        }
        final ShapeTally tally = new ShapeTally();
        final List<Long> parts = new ArrayList<>();
        for (int y = lo[1]; y <= hi[1]; y++) {
            for (int z = lo[2]; z <= hi[2]; z++) {
                for (int x = lo[0]; x <= hi[0]; x++) {
                    final long pos = PatternMatcher.encodePosition(x, y, z);
                    if (pos == terminal) {
                        continue;
                    }
                    final String id = Cells.blockAt(provider, pos);
                    if (!this.unit.fits().matchesAt(provider, pos, id)) {
                        return Cells.wrongBlock(terminal, pos, ShapeMatch.UNIT, id);
                    }
                    tally.place(this.unit, provider, pos, id);
                    parts.add(pos);
                }
            }
        }
        final ShapeMatch.Failure counts = tally.outOfLimits(this.limits, terminal);
        if (counts != null) {
            return counts;
        }
        return new ShapeMatch.Formed(Orientation.NORTH, terminal, parts, Map.of(), List.of(), hi[0] - lo[0] + 1,
                hi[1] - lo[1] + 1, hi[2] - lo[2] + 1, 1, tally.counts(), tally.values());
    }

    /** As far as its walks read: its most side from the terminal, and one beyond. */
    @Override
    public int reach() {
        return this.sides.most() + 1;
    }

    @Override
    public @Nullable IBlockMatcher wanted(final String wanted) {
        return ShapeMatch.UNIT.equals(wanted) ? this.unit.fits() : null;
    }

    @Override
    public Optional<ShapePreview> preview(final int grow) {
        return Optional.of(ShapePreviews.cuboid(this, grow));
    }

    /* Widens the cuboid along an axis to as far as the units run both ways from a place. */
    private void extend(final IBlockProvider provider, final int[] from, final int axis, final int[] lo,
                        final int[] hi) {
        lo[axis] = Math.min(lo[axis], from[axis] - walk(provider, from, axis, -1));
        hi[axis] = Math.max(hi[axis], from[axis] + walk(provider, from, axis, 1));
    }

    /* How far the units run from a place one way along an axis, up to the most a side may be. */
    private int walk(final IBlockProvider provider, final int[] from, final int axis, final int way) {
        final int[] at = from.clone();
        for (int step = 1; step <= this.sides.most(); step++) {
            at[axis] = from[axis] + way * step;
            final long pos = PatternMatcher.encodePosition(at[0], at[1], at[2]);
            if (!this.unit.fits().matchesAt(provider, pos, Cells.blockAt(provider, pos))) {
                return step - 1;
            }
        }
        return this.sides.most();
    }
}
