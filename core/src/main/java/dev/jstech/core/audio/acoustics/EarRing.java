/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio.acoustics;

/**
 * Ears ringing after a blast close by: for a few seconds everything is muffled and dull, the more and the longer the
 * nearer the blast and the stronger, coming back as the ringing fades. Ear protection keeps it from happening.
 *
 * @param strength how hard the ears ring, from 0 (not at all) to 1 (deafened)
 * @param ticks    how long it lasts
 */
public record EarRing(double strength, int ticks) {

    /** Ears that do not ring. */
    public static final EarRing NONE = new EarRing(0, 0);
    /** How many blocks away a blast of power 1 still rings the ears: a creeper's (3) up to nine, TNT's (4) twelve. */
    public static final double REACH_PER_POWER = 3.0;
    /** How long the faintest ringing lasts, and the hardest, in ticks. */
    public static final int SHORTEST = 40;
    public static final int LONGEST = 200;
    /* What the hardest ringing leaves of a sound's loudness, and of its high notes. */
    private static final double LEAST_HEARD = 0.15;
    private static final double LEAST_HIGHS = 0.05;

    public EarRing {
        if (strength < 0 || strength > 1 || ticks < 0) {
            throw new IllegalArgumentException("not a ringing: " + strength + " for " + ticks + " ticks");
        }
    }

    /** The ringing a blast of {@code power} leaves at {@code blocks} away; none for ears protected. */
    public static EarRing from(final double power, final double blocks, final boolean protectedEars) {
        final double reach = power * REACH_PER_POWER;
        if (protectedEars || power <= 0 || blocks >= reach) {
            return NONE;
        }
        final double near = 1 - Math.max(0, blocks) / reach;
        final double strength = near * near;
        return new EarRing(strength, (int) Math.round(SHORTEST + strength * (LONGEST - SHORTEST)));
    }

    /** Whether the ears ring at all. */
    public boolean rings() {
        return this.strength > 0 && this.ticks > 0;
    }

    /** The harder of two ringings, as a second blast while the ears still ring from the first leaves them. */
    public EarRing louder(final EarRing other, final int ticksLeftOfThis) {
        final double now = strengthAt(ticksLeftOfThis);
        return other.strength >= now ? other : new EarRing(now, ticksLeftOfThis);
    }

    /** How hard the ears ring with {@code ticksLeft} to go: hardest at first, fading smoothly to nothing. */
    public double strengthAt(final int ticksLeft) {
        if (!rings() || ticksLeft <= 0) {
            return 0;
        }
        final double left = Math.min(1, (double) ticksLeft / this.ticks);
        return this.strength * left * left * (3 - 2 * left);
    }

    /** What is left of a sound's loudness with {@code ticksLeft} to go, from 1. */
    public double heardAt(final int ticksLeft) {
        return 1 - (1 - LEAST_HEARD) * strengthAt(ticksLeft);
    }

    /** What is left of a sound's high notes with {@code ticksLeft} to go, from 1. */
    public double highsAt(final int ticksLeft) {
        return 1 - (1 - LEAST_HIGHS) * strengthAt(ticksLeft);
    }
}
