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

/**
 * Any cuboid filled with units, as a crafting processor: every place of it holds a unit, the controller one of them,
 * and its size is however far the units run from the controller along each axis, within the limits. The kinds of
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
    public ShapeMatch check(final IBlockProvider provider, final long controller) {
        final int[] c = PatternMatcher.decodePosition(controller);
        final int[] lo = c.clone();
        final int[] hi = c.clone();
        final String[] names = {ShapeMatch.SIZE_X, ShapeMatch.SIZE_Y, ShapeMatch.SIZE_Z};
        for (int axis = 0; axis < 3; axis++) {
            lo[axis] = c[axis] - walk(provider, c, axis, -1);
            hi[axis] = c[axis] + walk(provider, c, axis, 1);
            final int size = hi[axis] - lo[axis] + 1;
            if (!this.sides.contains(size)) {
                return Cells.wrongSize(controller, names[axis], size);
            }
        }
        final ShapeTally tally = new ShapeTally();
        final List<Long> parts = new ArrayList<>();
        for (int y = lo[1]; y <= hi[1]; y++) {
            for (int z = lo[2]; z <= hi[2]; z++) {
                for (int x = lo[0]; x <= hi[0]; x++) {
                    final long pos = PatternMatcher.encodePosition(x, y, z);
                    if (pos == controller) {
                        continue;
                    }
                    final String id = Cells.blockAt(provider, pos);
                    if (!this.unit.fits().matchesAt(provider, pos, id)) {
                        return Cells.wrongBlock(controller, pos, ShapeMatch.UNIT, id);
                    }
                    tally.place(this.unit, provider, pos, id);
                    parts.add(pos);
                }
            }
        }
        final ShapeMatch.Failure counts = tally.outOfLimits(this.limits, controller);
        if (counts != null) {
            return counts;
        }
        return new ShapeMatch.Formed(Orientation.NORTH, controller, parts, Map.of(), List.of(), hi[0] - lo[0] + 1,
                hi[1] - lo[1] + 1, hi[2] - lo[2] + 1, 1, tally.counts(), tally.values());
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
