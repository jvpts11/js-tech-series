/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock;

/**
 * The world's rules for building a multiblock for a player, as its settings file gives them: how many blocks a second
 * a tool that builds one places, and how far from the terminal the player may walk before it stops.
 */
public final class BuildBalance {

    /** How many blocks a second a tool builds, unless the settings say otherwise. */
    public static final int DEFAULT_BLOCKS_PER_SECOND = 5;
    /** How far from the terminal, in blocks, the player may be while it builds, unless the settings say otherwise. */
    public static final int DEFAULT_REACH = 32;

    private static volatile int blocksPerSecond = DEFAULT_BLOCKS_PER_SECOND;
    private static volatile int reach = DEFAULT_REACH;

    private BuildBalance() {
    }

    /** How many blocks a second a tool that builds a multiblock places. */
    public static int blocksPerSecond() {
        return blocksPerSecond;
    }

    /** How far from the terminal, in blocks, the player may be while it builds. */
    public static int reach() {
        return reach;
    }

    public static void setBlocksPerSecond(final int value) {
        blocksPerSecond = value;
    }

    public static void setReach(final int value) {
        reach = value;
    }
}
