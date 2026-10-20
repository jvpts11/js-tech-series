/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving.physics;

/**
 * What happens where two things meet, in kilograms, metres, seconds and joules: how hard a blow is, whether it breaks
 * a block, how much a contact bounces, how much it holds against sliding, and how a wheel's spring holds it up.
 * <ul>
 *   <li>A blow's energy is half the two masses' reduced mass times the square of the speed they close at: a thing
 *       striking the ground strikes with its own mass, the ground giving nothing.</li>
 *   <li>A block breaks when the blow's energy at it is more than it holds: its strength, in kilopascals, is the
 *       energy it takes, in kilojoules, a cubic metre of it.</li>
 *   <li>A contact holds against sliding by the grip of both sides, from how slippery each is: ice barely, stone
 *       well.</li>
 * </ul>
 */
public final class ContactMath {

    /* The game's own slipperiness of a block, and the grip it is taken to give: dry stone on stone. */
    private static final double PLAIN_SLIPPERINESS = 0.6;
    private static final double PLAIN_GRIP = 0.6;
    /* How much of a blow comes back as a bounce, for blocks: little. */
    private static final double BOUNCE = 0.2;
    /* A blow closing slower than this, in metres a second, bounces not at all, so a body at rest settles. */
    private static final double SETTLES = 0.5;
    private static final double JOULES_PER_KILOJOULE = 1000;

    private ContactMath() {
    }

    /** The reduced mass of two bodies, in kilograms; the first's own when the second does not give, as the ground. */
    public static double reducedMass(final double first, final double second) {
        if (second == Double.POSITIVE_INFINITY) {
            return first;
        }
        if (first == Double.POSITIVE_INFINITY) {
            return second;
        }
        return first * second / (first + second);
    }

    /** The energy of a blow, in joules, of bodies of {@code reducedMass} closing at {@code speed}. */
    public static double energy(final double reducedMass, final double speed) {
        return 0.5 * reducedMass * speed * speed;
    }

    /**
     * Whether a blow of {@code joules} at a block breaks it: more than the block, of {@code strength} kilopascals and
     * {@code volume} cubic metres, takes.
     */
    public static boolean breaks(final double joules, final double strength, final double volume) {
        return joules > strength * volume * JOULES_PER_KILOJOULE;
    }

    /** The grip of a block, against sliding, from the game's slipperiness of it: 0.6 for most, ice nearly none. */
    public static double grip(final double slipperiness) {
        return PLAIN_GRIP * Math.max(0, 1 - slipperiness) / (1 - PLAIN_SLIPPERINESS);
    }

    /** The grip between two sides, from both: as the weaker of two surfaces mostly sets it. */
    public static double grip(final double first, final double second) {
        return Math.sqrt(Math.max(0, first) * Math.max(0, second));
    }

    /** How much of a blow closing at {@code speed} comes back: a little, and none when slow, so a body settles. */
    public static double bounce(final double speed) {
        return Math.abs(speed) < SETTLES ? 0 : BOUNCE;
    }

    /**
     * How hard a spring holds up, in newtons: {@code stiffness} newtons a metre pressed by {@code pressed} metres,
     * damped by {@code damping} newton seconds a metre against the speed it is pressed at; it never pulls.
     */
    public static double spring(final double pressed, final double pressing, final double stiffness,
                                final double damping) {
        return Math.max(0, stiffness * pressed + damping * pressing);
    }

    /**
     * How hard friction holds against a slide of {@code speed} metres a second, in newtons: as much as it takes to
     * stop the slide over {@code seconds} for a body of {@code kilograms}, and no more than {@code grip} times the
     * weight on it, {@code load}; against the slide.
     */
    public static double friction(final double speed, final double kilograms, final double seconds,
                                  final double grip, final double load) {
        final double stopping = kilograms * Math.abs(speed) / seconds;
        return -Math.signum(speed) * Math.min(stopping, grip * Math.max(0, load));
    }
}
