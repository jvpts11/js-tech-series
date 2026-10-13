/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.core.client.motion.MotionClock;
import dev.jstech.core.motion.Motion;
import dev.jstech.core.motion.MotionKinds;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * The moment a machine's boot picture gives way to its desktop: the boot screen leaves its colour behind as it goes,
 * and the desktop that comes up on the same monitor straight after draws it over itself, thinning away at the pace
 * of its system's own fade, so the picture melts into the desktop rather than cutting to it.
 */
public final class SceneHandoff {

    /** How long the colour waits for a desktop, on the motion clock; a desktop opened later comes up at once. */
    private static final double WAIT_MS = 2000.0;

    /** The colour the last boot screen left, the monitor it was on, and when; null once a desktop took it. */
    @Nullable
    private static Left left;

    private SceneHandoff() {
    }

    /** A boot screen going away from that monitor, leaving the colour its picture was drawn on. */
    public static void leave(final BlockPos monitor, final int colour) {
        left = new Left(monitor.immutable(), colour, MotionClock.now());
    }

    /**
     * The veil a desktop coming up on that monitor draws over itself: the colour left there a moment ago, giving way
     * at the pace of the desktop's own fade, or none when no boot screen just left it.
     */
    static Veil take(final BlockPos monitor, final DesktopMotion motion) {
        final Left was = left;
        if (was == null || !was.monitor().equals(monitor) || MotionClock.now() - was.atMs() > WAIT_MS) {
            return Veil.NONE;
        }
        left = null;
        return new Veil(was.colour(), motion.start(MotionKinds.SCENE_FADE));
    }

    /** What a boot screen left behind. */
    private record Left(BlockPos monitor, int colour, double atMs) {
    }

    /**
     * A flat cover of one colour over the desktop, thinning away along its motion.
     *
     * @param colour the boot picture's colour, opaque
     * @param motion how it thins away
     */
    record Veil(int colour, Motion motion) {

        /** No cover at all. */
        static final Veil NONE = new Veil(0, Motion.FINISHED);

        /** How opaque the cover is at {@code nowMs}, from 0 to 255: all of it at the start, none once it is over. */
        int alpha(final double nowMs) {
            if (motion.done(nowMs)) {
                return 0;
            }
            final double along = motion.progress(nowMs);
            return (int) Math.round(255.0 * (motion.outward() ? along : 1.0 - along));
        }
    }
}
