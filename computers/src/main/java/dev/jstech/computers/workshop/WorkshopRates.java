/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.workshop;

import dev.jstech.core.tier.HardwareEra;

/**
 * What the personal-use cards charge and how fast they work, beside the blocks they stand in for. The Furnace Card
 * smelts a number of times a furnace's pace set by the era of the computer it is in, with no fuel; the Enchanting
 * Card asks fewer levels than a table and no lapis; the Anvil Card asks two thirds of an anvil's levels. Every number
 * here is a first estimate.
 */
public final class WorkshopRates {

    /** The levels a table asks for its three offers is 1, 2 and 3; the card asks these. */
    private static final int[] ENCHANT_LEVELS = {1, 1, 2};
    /** The bookshelves the card's offers are drawn as if a table had around it: all it can use. */
    public static final int BOOKSHELVES = 15;

    private WorkshopRates() {
    }

    /** How many times a furnace's pace the Furnace Card smelts at in a computer of {@code era}; none before Legacy. */
    public static int furnaceSpeed(final HardwareEra era) {
        return switch (era) {
            case VINTAGE -> 0;
            case LEGACY -> 2;
            case TRANSITION -> 3;
            case STANDARD -> 4;
            case ADVANCED, EXA, SINGULARITY -> 6;
        };
    }

    /** The ticks one item takes at {@code speed}: the recipe's own time divided by it, never under one tick. */
    public static int ticksPerItem(final int recipeTicks, final int speed) {
        if (speed <= 0) {
            return Integer.MAX_VALUE;
        }
        return Math.max(1, (Math.max(1, recipeTicks) + speed - 1) / speed);
    }

    /** The levels offer {@code offer} (0, 1 or 2) costs on the card. */
    public static int enchantLevels(final int offer) {
        return ENCHANT_LEVELS[Math.max(0, Math.min(ENCHANT_LEVELS.length - 1, offer))];
    }

    /** The levels the card asks where an anvil asks {@code anvilLevels}: two thirds of it, rounded up. */
    public static int anvilLevels(final int anvilLevels) {
        return anvilLevels <= 0 ? 0 : (anvilLevels * 2 + 2) / 3;
    }
}
