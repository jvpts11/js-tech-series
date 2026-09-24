/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio;

/**
 * How far the other channels are lowered while an alert plays: down quickly once one starts, back up slowly once it
 * ends, so an alarm is heard over the rest and the rest does not cut in and out around it.
 */
public final class DuckEnvelope {

    private final float depth;
    private final float downStep;
    private final float upStep;
    private float factor = 1.0F;

    /** How loud the other channels play under an alert, when the player has not said otherwise. */
    public static final float DEFAULT_DEPTH = 0.6F;
    /** How many ticks the other channels take to go down once an alert starts. */
    public static final int DEFAULT_DOWN_TICKS = 4;
    /** How many ticks they take to come back up once the last alert ends. */
    public static final int DEFAULT_UP_TICKS = 20;

    /**
     * @param depth     how loud the other channels play under an alert, from 0 to 1
     * @param downTicks how many ticks they take to go down to it
     * @param upTicks   how many ticks they take to come back to full
     */
    public DuckEnvelope(final float depth, final int downTicks, final int upTicks) {
        if (depth < 0.0F || depth > 1.0F) {
            throw new IllegalArgumentException("a depth between nothing and full: " + depth);
        }
        if (downTicks < 1 || upTicks < 1) {
            throw new IllegalArgumentException("a change takes at least a tick");
        }
        this.depth = depth;
        this.downStep = (1.0F - depth) / downTicks;
        this.upStep = (1.0F - depth) / upTicks;
    }

    /** The envelope with the default depth and speeds. */
    public static DuckEnvelope standard() {
        return new DuckEnvelope(DEFAULT_DEPTH, DEFAULT_DOWN_TICKS, DEFAULT_UP_TICKS);
    }

    /**
     * Moves one tick on.
     *
     * @param alerting whether an alert is playing now
     * @return how loud the other channels play from this tick, from the depth to 1
     */
    public float tick(final boolean alerting) {
        factor = alerting ? Math.max(depth, factor - downStep) : Math.min(1.0F, factor + upStep);
        return factor;
    }

    /** How loud the other channels play now, from the depth to 1. */
    public float factor() {
        return factor;
    }

    /** How many ticks the other channels take from here to come back to full, once no alert plays. */
    public int ticksToWhole() {
        return (int) Math.ceil((1.0F - factor) / upStep - 1e-4);
    }
}
