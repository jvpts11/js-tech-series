/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.camera;

import dev.jstech.core.model.Easing;
import java.util.List;
import java.util.Objects;

/**
 * A camera moving along keyframes for a scene, a rocket's launch seen from outside: at each keyframe's time, a place
 * relative to where the scene is played, which way it looks, how it is rolled and its field of view. Between them the
 * place follows a smooth curve through every keyframe, and the rest goes over by each keyframe's easing. Written in
 * a resource pack under {@code assets/<namespace>/jstech/camera_paths/} ({@link CameraPathCodecs}), or declared by a
 * mod:
 *
 * <pre>{@code
 * {
 *   "keyframes": [
 *     {"time": 0, "position": [0, 2, -12], "yaw": 0, "pitch": -5},
 *     {"time": 60, "position": [8, 6, -6], "yaw": 50, "pitch": -15, "easing": "easeInOutSine"},
 *     {"time": 120, "position": [0, 30, -20], "pitch": -40, "fov": 50, "easing": "easeOutQuad"}
 *   ]
 * }
 * }</pre>
 *
 * <p>The place is in blocks from the origin the scene is played at, turned with it: z ahead, x to the left, y up, as
 * the game's own coordinates face south. The yaw is in degrees from the origin's facing, a field of view of 0 the
 * player's own.
 *
 * @param keyframes its keyframes, two or more, in order of time
 * @param hideHud   whether the HUD is hidden while it plays
 */
public record CameraPath(List<Keyframe> keyframes, boolean hideHud) {

    public CameraPath {
        keyframes = List.copyOf(keyframes);
        if (!ordered(keyframes)) {
            throw new IllegalArgumentException("a camera path has two keyframes or more, each later than the one "
                    + "before it");
        }
    }

    /** Whether {@code keyframes} make a path: two or more, each later than the one before. */
    public static boolean ordered(final List<Keyframe> keyframes) {
        if (keyframes.size() < 2) {
            return false;
        }
        for (int i = 1; i < keyframes.size(); i++) {
            if (keyframes.get(i).time() <= keyframes.get(i - 1).time()) {
                return false;
            }
        }
        return true;
    }

    /** How long it plays, in ticks: the time of its last keyframe. */
    public int length() {
        return this.keyframes.get(this.keyframes.size() - 1).time();
    }

    /** Where the camera is at {@code ticks} after the scene began, held at its ends before and after it. */
    public Sample at(final double ticks) {
        final int last = this.keyframes.size() - 1;
        if (ticks <= this.keyframes.get(0).time()) {
            return this.keyframes.get(0).sample();
        }
        if (ticks >= this.keyframes.get(last).time()) {
            return this.keyframes.get(last).sample();
        }
        int i = 0;
        while (this.keyframes.get(i + 1).time() <= ticks) {
            i++;
        }
        final Keyframe from = this.keyframes.get(i);
        final Keyframe to = this.keyframes.get(i + 1);
        final Keyframe before = this.keyframes.get(Math.max(0, i - 1));
        final Keyframe after = this.keyframes.get(Math.min(last, i + 2));
        final double u = to.easing().apply((ticks - from.time()) / (double) (to.time() - from.time()), Double.NaN);
        return new Sample(curve(before.x(), from.x(), to.x(), after.x(), u),
                curve(before.y(), from.y(), to.y(), after.y(), u), curve(before.z(), from.z(), to.z(), after.z(), u),
                angle(from.yaw(), to.yaw(), u), (float) lerp(from.pitch(), to.pitch(), u),
                (float) lerp(from.roll(), to.roll(), u), fov(from.fov(), to.fov(), u));
    }

    /* A uniform Catmull-Rom curve through {@code p1} and {@code p2}, which passes through every keyframe. */
    private static double curve(final double p0, final double p1, final double p2, final double p3, final double u) {
        final double u2 = u * u;
        final double u3 = u2 * u;
        return 0.5 * ((2 * p1) + (-p0 + p2) * u + (2 * p0 - 5 * p1 + 4 * p2 - p3) * u2
                + (-p0 + 3 * p1 - 3 * p2 + p3) * u3);
    }

    private static double lerp(final double a, final double b, final double u) {
        return a + (b - a) * u;
    }

    /* The way between two headings that turns least. */
    private static float angle(final float a, final float b, final double u) {
        double turn = (b - a) % 360.0;
        if (turn > 180.0) {
            turn -= 360.0;
        } else if (turn < -180.0) {
            turn += 360.0;
        }
        return (float) (a + turn * u);
    }

    /* A field of view of 0, the player's own, is kept until both ends name one. */
    private static double fov(final double a, final double b, final double u) {
        return a > 0 && b > 0 ? lerp(a, b, u) : (u < 1.0 ? a : b);
    }

    /**
     * One keyframe of a camera path.
     *
     * @param time   when it is reached, in ticks from the scene's start
     * @param x      blocks to the left of the origin, as the origin faces
     * @param y      blocks above it
     * @param z      blocks ahead of it
     * @param yaw    which way the camera looks, in degrees from the origin's facing
     * @param pitch  how far it looks down, in degrees; up is below nothing
     * @param roll   how far it is rolled, in degrees
     * @param fov    its field of view in degrees, or 0 for the player's own
     * @param easing how the way here from the keyframe before goes over
     */
    public record Keyframe(int time, double x, double y, double z, float yaw, float pitch, float roll, double fov,
                           Easing easing) {

        /** The widest field of view a keyframe names, the game's own widest. */
        public static final double MOST_FOV = 110.0;

        public Keyframe {
            Objects.requireNonNull(easing, "easing");
            if (time < 0 || fov < 0 || fov > MOST_FOV) {
                throw new IllegalArgumentException("a keyframe is at a time from 0, with a field of view of 0 to "
                        + MOST_FOV);
            }
        }

        /** Its place, as three numbers. */
        public List<Double> position() {
            return List.of(this.x, this.y, this.z);
        }

        /** The camera at it. */
        public Sample sample() {
            return new Sample(this.x, this.y, this.z, this.yaw, this.pitch, this.roll, this.fov);
        }
    }

    /**
     * The camera at one moment of a path, relative to its origin.
     *
     * @param x     blocks to the left of the origin
     * @param y     blocks above it
     * @param z     blocks ahead of it
     * @param yaw   degrees from the origin's facing
     * @param pitch degrees down
     * @param roll  degrees rolled
     * @param fov   degrees of field of view, or 0 for the player's own
     */
    public record Sample(double x, double y, double z, float yaw, float pitch, float roll, double fov) {
    }
}
