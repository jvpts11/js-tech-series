/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.live;

/**
 * How often a picture shown in the world is drawn again, by how far the viewer is from it.
 *
 * <p>A screen in the world is drawn into a texture of its own, and drawing it costs the viewer's graphics card, never
 * the server. Close up it is drawn often enough to read as moving; a little further it still changes, slowly; further
 * than that nobody could read it, and only the screen's light shows. What a frame of it cost is spent on the frames
 * that are not drawn.
 *
 * <p>Pure maths, no rendering, so a unit test can pin the distances.
 */
public final class LiveRate {

    /** Up to this far, in blocks, the picture is drawn {@link #NEAR_FPS} times a second. */
    public static final int NEAR = 16;
    /** Up to this far it is drawn {@link #FAR_FPS} times a second; further, not at all. */
    public static final int FAR = 32;
    public static final int NEAR_FPS = 10;
    public static final int FAR_FPS = 2;
    /** What a picture never drawn yet counts as having been drawn at. */
    public static final long NEVER = Long.MIN_VALUE;

    private static final long NANOS_PER_SECOND = 1_000_000_000L;

    private LiveRate() {
    }

    /** How many times a second a picture this far away (squared, in blocks) is drawn; 0 when it is not drawn. */
    public static int framesPerSecond(final double distanceSquared) {
        if (distanceSquared <= (double) NEAR * NEAR) {
            return NEAR_FPS;
        }
        if (distanceSquared <= (double) FAR * FAR) {
            return FAR_FPS;
        }
        return 0;
    }

    /** Whether a picture this far away is shown at all, rather than only the screen's light. */
    public static boolean shows(final double distanceSquared) {
        return framesPerSecond(distanceSquared) > 0;
    }

    /**
     * Whether a picture last drawn at {@code lastNanos} is due to be drawn again at {@code nowNanos}, at {@code fps}
     * frames a second. One never drawn is due at once, as long as it is drawn at all.
     */
    public static boolean due(final long lastNanos, final long nowNanos, final int fps) {
        if (fps <= 0) {
            return false;
        }
        return lastNanos == NEVER || nowNanos - lastNanos >= NANOS_PER_SECOND / fps;
    }
}
