/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.physics;

/**
 * The arithmetic of the physics kit, without the game: the change to a thing's speed in a tick that pulls it as a law
 * says (its usual fall undone first), how many times a thing ticks at a rate of time, and where a crossing comes out
 * of a space that wraps round or leads elsewhere. Speeds are in blocks a tick, as the game counts them.
 */
public final class PhysicsMath {

    /** The hardest a pull towards a point grows near it, as a share of its strength at its radius. */
    public static final double CLOSEST = 4.0;
    private static final double TINY = 1e-6;

    private PhysicsMath() {
    }

    /**
     * The change to a thing's upward speed to make its fall of {@code falling} a tick (as it falls now) a fall of
     * {@code g} times {@code fall} (its fall at the overworld's pull): {up}.
     */
    public static double scaled(final double g, final double fall, final double falling) {
        return falling - g * fall;
    }

    /**
     * The change to a thing's speed to pull it along {@code dx, dy, dz} at {@code g} times {@code fall}, its present
     * fall of {@code falling} undone: {x, y, z}.
     */
    public static double[] towards(final double dx, final double dy, final double dz, final double g,
                                   final double fall, final double falling) {
        final double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < TINY) {
            return new double[] {0, falling, 0};
        }
        final double strength = g * fall / length;
        return new double[] {dx * strength, falling + dy * strength, dz * strength};
    }

    /**
     * The change to the speed of a thing at {@code x, y, z} to pull it towards {@code cx, cy, cz} at {@code g} times
     * {@code fall} at {@code radius}, falling with the square of the distance beyond and rising within, to
     * {@link #CLOSEST} times at most; its present fall of {@code falling} undone: {x, y, z}.
     */
    public static double[] centre(final double x, final double y, final double z, final double cx, final double cy,
                                  final double cz, final double g, final double radius, final double fall,
                                  final double falling) {
        final double dx = cx - x;
        final double dy = cy - y;
        final double dz = cz - z;
        final double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance < TINY) {
            return new double[] {0, falling, 0};
        }
        final double nearness = Math.min(CLOSEST, radius * radius / (distance * distance));
        final double strength = g * fall * nearness / distance;
        return new double[] {dx * strength, falling + dy * strength, dz * strength};
    }

    /**
     * How many times a thing ticks this tick at {@code rate} times the usual, with {@code carried} of a tick left from
     * before: {times, left over}.
     */
    public static double[] ticks(final double rate, final double carried) {
        final double total = Math.max(0, carried) + Math.max(0, rate);
        final double times = Math.floor(total);
        return new double[] {times, total - times};
    }

    /**
     * Where {@code at} comes back in along one axis of a space wrapping round from {@code min} to {@code max}
     * (excluded): through the opposite side, as far in as it went out.
     */
    public static double wrap(final double at, final double min, final double max) {
        final double size = max - min;
        if (size <= 0 || at >= min && at < max) {
            return at;
        }
        return min + ((at - min) % size + size) % size;
    }

    /** Where {@code at}, a place along one axis of a span, falls on another span, scaled to its size. */
    public static double map(final double at, final double fromMin, final double fromSize, final double toMin,
                             final double toSize) {
        return fromSize <= 0 ? toMin : toMin + (at - fromMin) / fromSize * toSize;
    }
}
