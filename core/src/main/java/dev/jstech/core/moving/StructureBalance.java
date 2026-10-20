/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

/**
 * What the server's owner sets of the moving structures, from the series' balance file: how many blocks one structure
 * may be made of. Read from many threads, set from the server's.
 */
public final class StructureBalance {

    /** How many blocks a structure is made of at most, when nothing is set. */
    public static final int DEFAULT_MOST_BLOCKS = 65_536;
    private static volatile int mostBlocks = DEFAULT_MOST_BLOCKS;

    private StructureBalance() {
    }

    /** How many blocks a structure is made of at most. */
    public static int mostBlocks() {
        return mostBlocks;
    }

    /** Sets how many blocks a structure is made of at most; from the balance file. */
    public static void mostBlocks(final int most) {
        mostBlocks = Math.max(1, most);
    }
}
