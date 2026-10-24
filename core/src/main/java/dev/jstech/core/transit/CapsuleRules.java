/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.transit;

/**
 * How a mod's pneumatic post works: how many stacks a capsule carries, how often a station sends one, and how fast it
 * travels through the tubes.
 *
 * @param stacks          the stacks a capsule carries
 * @param every           the ticks a station takes between two capsules
 * @param blocksPerSecond how far a capsule travels a second, in blocks
 */
public record CapsuleRules(int stacks, int every, double blocksPerSecond) {

    /** The ticks of a second. */
    public static final double TICKS_A_SECOND = 20.0;

    public CapsuleRules {
        if (stacks <= 0 || every <= 0 || !(blocksPerSecond > 0) || Double.isInfinite(blocksPerSecond)) {
            throw new IllegalArgumentException("a capsule carries something, a station sends now and then and a"
                    + " capsule travels; not " + stacks + ", " + every + " and " + blocksPerSecond);
        }
    }

    /** The ticks a capsule takes over {@code blocks} blocks, at least one. */
    public long ticksFor(final int blocks) {
        return Math.max(1L, (long) Math.ceil(Math.max(0, blocks) * TICKS_A_SECOND / this.blocksPerSecond));
    }
}
