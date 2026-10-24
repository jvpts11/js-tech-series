/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.jetbrains.annotations.Nullable;

/**
 * What a structure's places add up to while it is checked: how many places fit, the counts by name and the numbers by
 * key, each gathered as its places say. Pure.
 */
final class ShapeTally {

    private final Map<String, Integer> counts = new HashMap<>();
    private final Map<String, Place.Gather> gathers = new HashMap<>();
    /* Per key: the least, the most, the sum and how many numbers were added. */
    private final Map<String, double[]> numbers = new HashMap<>();
    private int matched;

    /** Adds a place that fits: its count, the entry standing there and its number, when it has them. */
    void place(final Place place, final IBlockProvider provider, final long pos, final String blockId) {
        this.matched++;
        final Place.Counted counted = place.counted();
        if (counted != null && counted.which().matchesAt(provider, pos, blockId)) {
            count(counted.name(), 1);
        }
        final Place.Entry entry = place.entryAt(provider, pos, blockId);
        if (entry != null) {
            count(entry.name(), 1);
        }
        final Place.Valued valued = place.valued();
        if (valued != null) {
            final Double number = valued.of(provider, pos, blockId);
            if (number != null) {
                value(valued.key(), valued.how(), number);
            }
        }
    }

    /** Adds a place that fits, counting and giving nothing. */
    void fits() {
        this.matched++;
    }

    /** Adds {@code by} to the count of {@code name}. */
    void count(final String name, final int by) {
        this.counts.merge(name, by, Integer::sum);
    }

    /** Adds a number to {@code key}, gathered as the first number of that key said. */
    void value(final String key, final Place.Gather how, final double number) {
        this.gathers.putIfAbsent(key, how);
        final double[] held = this.numbers.computeIfAbsent(key,
                k -> new double[] {Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 0, 0});
        held[0] = Math.min(held[0], number);
        held[1] = Math.max(held[1], number);
        held[2] += number;
        held[3]++;
    }

    /** How many places fit so far. */
    int matched() {
        return this.matched;
    }

    Map<String, Integer> counts() {
        return this.counts;
    }

    /** Each key's numbers gathered into one. */
    Map<String, Double> values() {
        final Map<String, Double> out = new HashMap<>();
        this.numbers.forEach((key, held) -> out.put(key, switch (this.gathers.get(key)) {
            case LEAST -> held[0];
            case MOST -> held[1];
            case SUM -> held[2];
            case MEAN -> held[2] / held[3];
        }));
        return out;
    }

    /** The first count, by name, outside its limit; none when every count is within. */
    @Nullable
    ShapeMatch.Failure outOfLimits(final Map<String, Limit> limits, final long terminal) {
        for (final Map.Entry<String, Limit> limit : new TreeMap<>(limits).entrySet()) {
            final int count = this.counts.getOrDefault(limit.getKey(), 0);
            if (!limit.getValue().contains(count)) {
                return new ShapeMatch.Failure(ShapeMatch.Wrong.COUNT, terminal, 0, 0, 0, limit.getKey(),
                        Integer.toString(count));
            }
        }
        return null;
    }

    /**
     * The first pair of counts that must go together and do not, as a coolant's inlet with its outlet: the count of
     * the first is wrong, against the count of the second; none when every pair agrees.
     */
    @Nullable
    ShapeMatch.Failure apart(final List<MultiblockPattern.Together> pairs, final long terminal) {
        for (final MultiblockPattern.Together pair : pairs) {
            final int first = this.counts.getOrDefault(pair.first(), 0);
            final int second = this.counts.getOrDefault(pair.second(), 0);
            if (first != second) {
                return new ShapeMatch.Failure(ShapeMatch.Wrong.APART, terminal, 0, 0, 0, pair.first(),
                        pair.second());
            }
        }
        return null;
    }
}
