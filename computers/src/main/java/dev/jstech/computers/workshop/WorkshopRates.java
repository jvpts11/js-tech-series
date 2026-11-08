/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.workshop;

import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.EraTable;

/**
 * How fast the personal-use cards work, beside the blocks they stand in for: the Furnace Card smelts a number of times
 * a furnace's pace set by the era of the computer it is in, with no fuel, and the Enchanting Card draws its offers as a
 * table with every bookshelf around it. What the cards charge is {@link WorkshopPrice}'s. Every number here is a first
 * estimate.
 */
public final class WorkshopRates {

    /** The bookshelves the card's offers are drawn as if a table had around it: all it can use. */
    public static final int BOOKSHELVES = 15;
    private static final EraTable<Integer> FURNACE_SPEEDS = EraTable.ofFive(0, 2, 3, 4, 6);

    private WorkshopRates() {
    }

    /** How many times a furnace's pace the Furnace Card smelts at in a computer of {@code era}; none before Legacy. */
    public static int furnaceSpeed(final Era era) {
        return FURNACE_SPEEDS.get(era);
    }

    /** The ticks one item takes at {@code speed}: the recipe's own time divided by it, never under one tick. */
    public static int ticksPerItem(final int recipeTicks, final int speed) {
        if (speed <= 0) {
            return Integer.MAX_VALUE;
        }
        return Math.max(1, (Math.max(1, recipeTicks) + speed - 1) / speed);
    }
}
