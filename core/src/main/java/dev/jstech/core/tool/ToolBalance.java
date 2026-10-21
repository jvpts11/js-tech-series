/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.tool;

/**
 * What the server's settings say of tools, set each time the world's settings are read: how many blocks a tool breaks
 * or places in one go, how much harder than the block broken a block in its area may be, and how many blocks a copy
 * holds. A tool's own numbers never go past these.
 */
public final class ToolBalance {

    /** The most blocks a tool breaks in one blow, the one broken among them, unless the settings say otherwise. */
    public static final int DEFAULT_MOST_BROKEN = 64;
    /** How much harder than the block broken one in the area may be, unless the settings say otherwise. */
    public static final double DEFAULT_HARDNESS_MARGIN = 2.0;
    /** The most blocks a building tool places in one use, unless the settings say otherwise. */
    public static final int DEFAULT_MOST_PLACED = 64;
    /** The most blocks a copy of a region holds, unless the settings say otherwise. */
    public static final int DEFAULT_MOST_COPIED = 4096;

    private static volatile int mostBroken = DEFAULT_MOST_BROKEN;
    private static volatile double hardnessMargin = DEFAULT_HARDNESS_MARGIN;
    private static volatile int mostPlaced = DEFAULT_MOST_PLACED;
    private static volatile int mostCopied = DEFAULT_MOST_COPIED;

    private ToolBalance() {
    }

    /** The most blocks a tool breaks in one blow, the one broken among them. */
    public static int mostBroken() {
        return mostBroken;
    }

    public static void setMostBroken(final int value) {
        mostBroken = value;
    }

    /**
     * How much harder than the block broken, as the game measures hardness, a block in a tool's area may be and still
     * be taken: stone is 1.5, an ore 3, obsidian 50.
     */
    public static double hardnessMargin() {
        return hardnessMargin;
    }

    public static void setHardnessMargin(final double value) {
        hardnessMargin = value;
    }

    /** The most blocks a building tool places in one use. */
    public static int mostPlaced() {
        return mostPlaced;
    }

    public static void setMostPlaced(final int value) {
        mostPlaced = value;
    }

    /** The most blocks a copy of a region holds. */
    public static int mostCopied() {
        return mostCopied;
    }

    public static void setMostCopied(final int value) {
        mostCopied = value;
    }
}
