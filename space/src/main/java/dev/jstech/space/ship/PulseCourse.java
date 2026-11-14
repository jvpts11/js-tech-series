/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.ship;

import dev.jstech.space.universe.SolarBody;
import dev.jstech.space.universe.SystemLayout;
import org.jetbrains.annotations.Nullable;

/**
 * Where a ship's pulse jump goes in its system's space, in the spirit of No Man's Sky: level, along its nose's
 * heading, far faster than it flies, the space being a layer the worlds hang under; never begun over a world's low
 * orbit, nor over the Sun; and dropped out of, by itself, the moment its way comes over the low orbit of a world, so
 * it comes out of the jump ready to go down, or to the edge of the Sun. Distances in blocks, a block a metre. Every
 * figure is an estimate, set by playing. Pure: no game types.
 */
public final class PulseCourse {

    /** How fast a ship jumps, in metres a second: the Moon is a few seconds away, Mars a few tens. */
    public static final double SPEED = 3_000;
    /** How long its pulse drive spools up, its key held, before it jumps, in seconds. */
    public static final double SPOOL_SECONDS = 1.5;
    /** How fast it turns while it jumps, in degrees a second: barely at all. */
    public static final double TURNS = 4.5;
    /** How many times as fast its reactor burns its deuterium while it spools and jumps. */
    public static final double BURNS = 6;
    /** How near another ship it drops out, in blocks. */
    public static final double NEAR_SHIP = 500;

    private PulseCourse() {
    }

    /** Whether a jump may begin at ({@code x}, {@code z}): over no world's low orbit, and not over the Sun. */
    public static boolean mayBegin(final double x, final double z) {
        return SystemLayout.over(x, z) == null && !overTheSun(x, z);
    }

    /**
     * Where a jump from ({@code fromX}, {@code fromZ}) going on to ({@code toX}, {@code toZ}) drops out by itself: the
     * first place on the way where it comes over a world's low orbit, or over the Sun; none when it goes on.
     */
    public static @Nullable Stop stopOn(final double fromX, final double fromZ, final double toX, final double toZ) {
        Stop first = null;
        double nearest = Double.POSITIVE_INFINITY;
        for (final SolarBody body : SolarBody.values()) {
            final double[] middle = SystemLayout.centre(body);
            final double t = entering(fromX, fromZ, toX, toZ, middle[0], middle[2],
                    SystemLayout.radius(body) * (1 + SystemLayout.LOW_ORBIT));
            if (t < nearest) {
                nearest = t;
                first = new Stop(body, fromX + (toX - fromX) * t, fromZ + (toZ - fromZ) * t);
            }
        }
        final double[] sun = SystemLayout.sun();
        final double t = entering(fromX, fromZ, toX, toZ, sun[0], sun[2], SystemLayout.SUN_RADIUS);
        if (t < nearest) {
            first = new Stop(null, fromX + (toX - fromX) * t, fromZ + (toZ - fromZ) * t);
        }
        return first;
    }

    /**
     * The world whose low orbit is nearest ({@code x}, {@code z}), and how far off that orbit is, in blocks, for the
     * HUD to bracket it.
     */
    public static Nearest nearest(final double x, final double z) {
        SolarBody best = SolarBody.OVERWORLD;
        double range = Double.POSITIVE_INFINITY;
        for (final SolarBody body : SolarBody.values()) {
            final double[] middle = SystemLayout.centre(body);
            final double off = Math.max(0, Math.hypot(x - middle[0], z - middle[2])
                    - SystemLayout.radius(body) * (1 + SystemLayout.LOW_ORBIT));
            if (off < range) {
                best = body;
                range = off;
            }
        }
        return new Nearest(best, range);
    }

    /* Whether ({@code x}, {@code z}) stands over the Sun. */
    private static boolean overTheSun(final double x, final double z) {
        final double[] sun = SystemLayout.sun();
        return Math.hypot(x - sun[0], z - sun[2]) < SystemLayout.SUN_RADIUS;
    }

    /*
     * How far along the way from ({@code ax}, {@code az}) to ({@code bx}, {@code bz}), from nought to one, it first
     * comes within {@code reach} of ({@code cx}, {@code cz}) from outside; infinite when it never does, or starts
     * within it.
     */
    private static double entering(final double ax, final double az, final double bx, final double bz,
                                   final double cx, final double cz, final double reach) {
        final double dx = bx - ax;
        final double dz = bz - az;
        final double fx = ax - cx;
        final double fz = az - cz;
        final double c = fx * fx + fz * fz - reach * reach;
        if (c <= 0) {
            return Double.POSITIVE_INFINITY;
        }
        final double a = dx * dx + dz * dz;
        final double b = 2 * (fx * dx + fz * dz);
        final double seen = b * b - 4 * a * c;
        if (a <= 0 || seen < 0) {
            return Double.POSITIVE_INFINITY;
        }
        final double t = (-b - Math.sqrt(seen)) / (2 * a);
        return t >= 0 && t <= 1 ? t : Double.POSITIVE_INFINITY;
    }

    /**
     * Where a jump dropped out by itself.
     *
     * @param body the world whose low orbit it came over, or null for the Sun
     * @param x    where, in the space
     * @param z    where, in the space
     */
    public record Stop(@Nullable SolarBody body, double x, double z) {
    }

    /**
     * The world whose low orbit is nearest.
     *
     * @param body  the world
     * @param range how far off its low orbit is, in blocks
     */
    public record Nearest(SolarBody body, double range) {
    }
}
