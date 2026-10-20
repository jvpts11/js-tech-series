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
 * may be made of, and how much of a tick the physics of a dimension's structures may take. Read from many threads,
 * set from the server's.
 */
public final class StructureBalance {

    /** How many blocks a structure is made of at most, when nothing is set. */
    public static final int DEFAULT_MOST_BLOCKS = 65_536;
    /** How much of a tick a dimension's structures' physics takes at most, in milliseconds, when nothing is set. */
    public static final double DEFAULT_PHYSICS_MILLIS = 10.0;
    private static volatile int mostBlocks = DEFAULT_MOST_BLOCKS;
    private static volatile double physicsMillis = DEFAULT_PHYSICS_MILLIS;
    private static volatile boolean split = true;

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

    /** How much of a tick the physics of a dimension's structures takes at most, in milliseconds. */
    public static double physicsMillis() {
        return physicsMillis;
    }

    /** Sets how much of a tick the physics of a dimension's structures takes at most; from the balance file. */
    public static void physicsMillis(final double millis) {
        physicsMillis = Math.max(0.1, millis);
    }

    /** Whether a structure whose blocks are no longer all joined becomes as many structures as it now is. */
    public static boolean split() {
        return split;
    }

    /** Sets whether a structure no longer all joined parts; from the balance file. */
    public static void split(final boolean parts) {
        split = parts;
    }
}
