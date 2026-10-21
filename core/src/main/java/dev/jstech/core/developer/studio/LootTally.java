/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer.studio;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * How a loot table's drops spread over many rolls: for each item, in how many rolls it came, how many each roll gave
 * on average, and the fewest and the most one roll gave.
 */
public final class LootTally {

    private final Map<String, Counted> counted = new LinkedHashMap<>();
    private int rolls;

    /** Adds one roll's drops, each item with how many came. */
    public void roll(final Map<String, Integer> drops) {
        this.rolls++;
        drops.forEach((item, count) -> {
            if (count > 0) {
                this.counted.computeIfAbsent(item, any -> new Counted()).add(count);
            }
        });
    }

    /** How many rolls were added. */
    public int rolls() {
        return this.rolls;
    }

    /** Each item, the one that came in the most rolls first. */
    public List<Row> rows() {
        final List<Row> out = new ArrayList<>();
        this.counted.forEach((item, counts) -> out.add(new Row(item, counts.rolls,
                this.rolls == 0 ? 0.0 : (double) counts.total / this.rolls,
                counts.rolls < this.rolls ? 0 : counts.fewest, counts.most)));
        out.sort(Comparator.comparingInt(Row::inRolls).reversed());
        return out;
    }

    /**
     * One item's spread.
     *
     * @param item    the item
     * @param inRolls in how many rolls it came
     * @param perRoll how many each roll gave on average, every roll counted
     * @param fewest  the fewest a roll gave, none when some roll gave none
     * @param most    the most a roll gave
     */
    public record Row(String item, int inRolls, double perRoll, int fewest, int most) {
    }

    /* What one item came to so far. */
    private static final class Counted {

        private int rolls;
        private long total;
        private int fewest = Integer.MAX_VALUE;
        private int most;

        void add(final int count) {
            this.rolls++;
            this.total += count;
            this.fewest = Math.min(this.fewest, count);
            this.most = Math.max(this.most, count);
        }
    }
}
