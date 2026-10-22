/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.explosion;

/**
 * The physics of a blast, pure: its energy as kilograms of TNT, the peak overpressure at a distance by the Kinney and
 * Graham fit to real blasts in air at sea level, how far a pressure reaches, when the shock wave arrives, what an
 * overpressure does to a body, how large the fireball is, and how fast and how far the fragments of a casing fly.
 * Distances are in metres, a block each; pressures in kilopascals.
 */
public final class BlastMath {

    /** The energy of a kilogram of TNT, in joules. */
    public static final double JOULES_PER_KILOGRAM_OF_TNT = 4.184e6;
    /** The air's pressure at sea level, in kilopascals. */
    public static final double SEA_LEVEL = 101.325;
    /** The speed of sound in air at sea level and in water, in metres a second. */
    public static final double SOUND_IN_AIR = 343;
    public static final double SOUND_IN_WATER = 1_480;
    /** Below this overpressure a body takes no harm, in kilopascals: the knock of a door slammed. */
    public static final double HARMLESS = 10;
    /** Below this overpressure a blast breaks nothing, in kilopascals: the weakest windows give at about one. */
    public static final double FAINTEST = 1;
    /** The least overpressure a block stands when nothing says more, in kilopascals: a flower's or a torch's. */
    public static final double LEAST_STANDING = 15;
    /** The Gurney velocity of TNT, the square root of twice the energy it gives a casing, in metres a second. */
    public static final double GURNEY_TNT = 2_440;
    /** The kinetic energy a fragment needs to cost a half heart, in joules: a pistol's bullet costs ten. */
    public static final double JOULES_PER_HALF_HEART = 50;
    /* How many half hearts each kilopascal past the harmless costs, and how far it throws a body for each. */
    private static final double HARM_PER_KILOPASCAL = 0.2;
    private static final double THROW_PER_KILOPASCAL = 0.02;
    private static final double FARTHEST_THROW = 3;
    /* The fit to the diameters of fireballs, D = 3.86 W^0.32 metres for W kilograms; half of it is the radius. */
    private static final double FIREBALL_RADIUS = 1.93;
    private static final double FIREBALL_POWER = 0.32;
    /* A tumbling fragment's drag coefficient, near a cube's. */
    private static final double FRAGMENT_DRAG = 1.0;
    /* The least TNT worth working with, so a fizzle divides by nothing. */
    private static final double LEAST_TNT = 1.0e-6;
    /* Air's ratio of its heats at constant pressure and volume, which a burst gas is taken to have. */
    private static final double AIR_HEAT_RATIO = 1.4;
    private static final double PASCALS_PER_KILOPASCAL = 1_000;
    private static final int TICKS_A_SECOND = 20;
    /* How closely a reach is found, as a share of the scaled distance. */
    private static final double REACH_PRECISION = 1.0e-4;
    /* The scaled distances a reach is looked for between, in metres for a kilogram's cube root. */
    private static final double NEAREST_SCALED = 0.05;
    private static final double FARTHEST_SCALED = 1_000;

    private BlastMath() {
    }

    /** How many kilograms of TNT {@code joules} make. */
    public static double tnt(final double joules) {
        return joules / JOULES_PER_KILOGRAM_OF_TNT;
    }

    /**
     * The energy a vessel of {@code cubicMetres} lets go as it bursts, holding a gas at {@code atmospheres} times the
     * open air's pressure, in joules: Brode's measure, the work the gas held above the air around it, (p - p0) V over
     * the ratio of its heats less one, the gas taken as air. None at or below the open air's pressure.
     */
    public static double burstEnergy(final double atmospheres, final double cubicMetres) {
        if (atmospheres <= 1 || cubicMetres <= 0) {
            return 0;
        }
        return (atmospheres - 1) * SEA_LEVEL * PASCALS_PER_KILOPASCAL * cubicMetres / (AIR_HEAT_RATIO - 1);
    }

    /** The scaled distance of {@code metres} from {@code tnt} kilograms: the metres over the cube root of the TNT. */
    public static double scaled(final double metres, final double tnt) {
        return metres / Math.cbrt(Math.max(LEAST_TNT, tnt));
    }

    /** The peak overpressure {@code metres} from {@code tnt} kilograms of TNT in open air, in kilopascals. */
    public static double overpressure(final double tnt, final double metres) {
        return overpressureAt(scaled(Math.max(metres, NEAREST_SCALED * Math.cbrt(Math.max(LEAST_TNT, tnt))), tnt));
    }

    /** The peak overpressure at the scaled distance {@code z}, by the Kinney and Graham fit, in kilopascals. */
    public static double overpressureAt(final double z) {
        final double a = 1 + (z / 4.5) * (z / 4.5);
        final double b = 1 + (z / 0.048) * (z / 0.048);
        final double c = 1 + (z / 0.32) * (z / 0.32);
        final double d = 1 + (z / 1.35) * (z / 1.35);
        return SEA_LEVEL * 808 * a / Math.sqrt(b * c * d);
    }

    /** How far from {@code tnt} kilograms of TNT the overpressure falls to {@code kilopascals}, in metres. */
    public static double reach(final double tnt, final double kilopascals) {
        double near = NEAREST_SCALED;
        double far = FARTHEST_SCALED;
        if (overpressureAt(near) <= kilopascals) {
            return 0;
        }
        while ((far - near) / far > REACH_PRECISION) {
            final double middle = (near + far) / 2;
            if (overpressureAt(middle) > kilopascals) {
                near = middle;
            } else {
                far = middle;
            }
        }
        return near * Math.cbrt(Math.max(LEAST_TNT, tnt));
    }

    /** How many ticks the shock wave takes to go {@code metres} at {@code speed} metres a second. */
    public static int arrival(final double metres, final double speed) {
        return (int) Math.floor(metres / speed * TICKS_A_SECOND);
    }

    /** What {@code kilopascals} of overpressure cost a body, in half hearts: nothing up to the harmless. */
    public static double harm(final double kilopascals) {
        return Math.max(0, kilopascals - HARMLESS) * HARM_PER_KILOPASCAL;
    }

    /** How hard {@code kilopascals} of overpressure throw a body, in blocks a tick. */
    public static double thrown(final double kilopascals) {
        return Math.min(FARTHEST_THROW, Math.max(0, kilopascals - HARMLESS) * THROW_PER_KILOPASCAL);
    }

    /**
     * The overpressure a block of the game's {@code resistance} to explosions stands, in kilopascals, when no figure of
     * its own is given: a flower or a torch the least, planks about 80, stone about 200, obsidian thousands.
     */
    public static double standsFromResistance(final double resistance) {
        return LEAST_STANDING + 20 * Math.pow(Math.max(0, resistance), 1.3);
    }

    /** The radius of the fireball of {@code tnt} kilograms of TNT, in metres. */
    public static double fireball(final double tnt) {
        return FIREBALL_RADIUS * Math.pow(Math.max(0, tnt), FIREBALL_POWER);
    }

    /**
     * How fast the fragments of a casing of {@code casing} kilograms round {@code tnt} kilograms of TNT fly off, by
     * Gurney's equation for a cylinder, in metres a second: a light casing round a large charge fastest.
     */
    public static double fragmentSpeed(final double casing, final double tnt) {
        if (tnt <= 0) {
            return 0;
        }
        return GURNEY_TNT / Math.sqrt(Math.max(0, casing) / tnt + 0.5);
    }

    /**
     * How far a fragment of {@code kilograms} of a material {@code density} kilograms a cubic metre dense goes before
     * the drag of a medium {@code mediumDensity} dense slows it to a share of 1/e of its speed, in metres: the
     * heavier and the denser the fragment, and the thinner the medium, the farther. In a vacuum it never slows.
     */
    public static double slowing(final double kilograms, final double density, final double mediumDensity) {
        if (mediumDensity <= 0) {
            return Double.POSITIVE_INFINITY;
        }
        final double side = Math.cbrt(kilograms / Math.max(1, density));
        return 2 * kilograms / (FRAGMENT_DRAG * mediumDensity * side * side);
    }

    /** The kinetic energy of {@code kilograms} at {@code speed} metres a second, in joules. */
    public static double kinetic(final double kilograms, final double speed) {
        return 0.5 * kilograms * speed * speed;
    }

    /** What a fragment striking with {@code joules} costs a body, in half hearts. */
    public static double fragmentHarm(final double joules) {
        return joules / JOULES_PER_HALF_HEART;
    }

    /**
     * How far a fragment flying off at {@code speed}, slowed as {@link #slowing} says, still costs a half heart, in
     * metres: none when it never does.
     */
    public static double fragmentReach(final double kilograms, final double speed, final double slowing) {
        final double start = kinetic(kilograms, speed);
        if (start <= JOULES_PER_HALF_HEART) {
            return 0;
        }
        // The energy falls as the square of the speed, e^(-2d/slowing).
        return slowing / 2 * Math.log(start / JOULES_PER_HALF_HEART);
    }
}
