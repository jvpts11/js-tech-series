/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock.plan;

/**
 * How a robot arm of two links stands to put its wrist on a point, elbow up: its turn about the upright, its upper
 * link's lean from the upright toward the point, its forearm's bend from the upper link, and its tool's bend from the
 * forearm to hang straight down. A point beyond its reach, or too near its shoulder, is reached as far as it goes, on
 * the line toward it. Each lean and bend turns about the arm's own sideways axis, forward being the way it is turned.
 * Pure, so it is tested without the game.
 */
public final class ArmReach {

    /* How far short of straight, or of folded back on itself, an arm stops, in blocks. */
    private static final double SLACK = 1.0e-3;
    private static final int AXES = 3;

    private ArmReach() {
    }

    /**
     * The pose of an arm whose shoulder is at {@code shoulder}, its links {@code upper} and {@code fore} long, putting
     * its wrist on {@code wrist}, or as near as it reaches.
     */
    public static Pose solve(final double[] shoulder, final double[] wrist, final double upper, final double fore) {
        final double dx = wrist[0] - shoulder[0];
        final double dy = wrist[1] - shoulder[1];
        final double dz = wrist[2] - shoulder[2];
        final double flat = Math.hypot(dx, dz);
        final double yaw = flat < SLACK ? 0 : Math.atan2(dx, dz);
        final double far = Math.sqrt(flat * flat + dy * dy);
        final double reach = Math.max(Math.abs(upper - fore) + SLACK, Math.min(far, upper + fore - SLACK));
        final double rise = Math.atan2(dy, flat);
        final double opening = Math.acos(clamp((upper * upper + reach * reach - fore * fore)
                / (2 * upper * reach)));
        // The upper link's rise from the level, toward the point, and the forearm's, down from the elbow to it.
        final double upperRise = rise + opening;
        final double[] elbow = along(shoulder, yaw, upperRise, upper);
        final double[] reached = far < SLACK ? along(shoulder, yaw, rise, reach) : new double[] {
            shoulder[0] + dx / far * reach, shoulder[1] + dy / far * reach, shoulder[2] + dz / far * reach};
        final double foreFlat = (reached[0] - elbow[0]) * Math.sin(yaw) + (reached[2] - elbow[2]) * Math.cos(yaw);
        final double foreRise = Math.atan2(reached[1] - elbow[1], foreFlat);
        final double upperLean = Math.PI / 2 - upperRise;
        final double foreLean = Math.PI / 2 - foreRise;
        return new Pose(yaw, upperLean, foreLean - upperLean, Math.PI - foreLean, elbow, reached);
    }

    /** Whether an arm whose shoulder is at {@code shoulder} reaches {@code wrist} with links so long. */
    public static boolean reaches(final double[] shoulder, final double[] wrist, final double upper,
                                  final double fore) {
        double far = 0;
        for (int axis = 0; axis < AXES; axis++) {
            far += (wrist[axis] - shoulder[axis]) * (wrist[axis] - shoulder[axis]);
        }
        return Math.sqrt(far) <= upper + fore - SLACK;
    }

    /* The point {@code length} from {@code from}, turned {@code yaw}, risen {@code rise} from the level. */
    private static double[] along(final double[] from, final double yaw, final double rise, final double length) {
        final double flat = Math.cos(rise) * length;
        return new double[] {from[0] + Math.sin(yaw) * flat, from[1] + Math.sin(rise) * length,
            from[2] + Math.cos(yaw) * flat};
    }

    private static double clamp(final double cosine) {
        return Math.max(-1, Math.min(1, cosine));
    }

    /**
     * An arm's pose: its turns, in radians, and where its elbow and wrist come to stand.
     *
     * @param yaw   its turn about the upright, from facing the way of z toward the way of x
     * @param upper its upper link's lean from the upright toward the way it faces
     * @param fore  its forearm's further lean from its upper link's
     * @param tool  its tool's further lean from its forearm's, which hangs it straight down
     * @param elbow where its elbow stands
     * @param wrist where its wrist stands
     */
    public record Pose(double yaw, double upper, double fore, double tool, double[] elbow, double[] wrist) {
    }
}
