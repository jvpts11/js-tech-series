/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.orbit;

/**
 * The sums a rocket flies by, as real rockets do. Its delta-v is Tsiolkovsky's: its engines' efficiency times the
 * logarithm of how much heavier it is full than empty. It lifts off only when its thrust beats its weight, which is its
 * mass times the pull of the world it stands on, so the same rocket that struggles off the Overworld leaps off the
 * Moon. And the legs of a trip cost what they cost for real, from the Earth's surface.
 */
public final class Rocketry {

    /** From the Overworld's surface to a low orbit, in metres per second. */
    public static final double SURFACE_TO_LOW_ORBIT = 9_400;
    /** From a low orbit to the Moon's orbit. */
    public static final double LOW_ORBIT_TO_MOON_ORBIT = 4_100;
    /** From the Moon's orbit down to its surface, and back up. */
    public static final double MOON_ORBIT_TO_SURFACE = 1_700;
    /** From a low orbit to a transfer to Mars. */
    public static final double LOW_ORBIT_TO_MARS_TRANSFER = 3_600;
    /** From Mars' surface to its orbit. */
    public static final double MARS_SURFACE_TO_ORBIT = 4_100;
    /** The capture burn into Mars' orbit, arriving from a transfer, for a rocket that does not aerobrake. */
    public static final double MARS_CAPTURE = 2_000;
    /** The capture burn into the Overworld's low orbit, coming home, for a rocket that does not aerobrake. */
    public static final double OVERWORLD_CAPTURE = 3_100;

    private Rocketry() {
    }

    /**
     * What a stage can change its speed by, in metres per second: its engines' specific impulse, in seconds, times the
     * standard pull times the logarithm of its mass full over its mass empty.
     */
    public static double deltaV(final double specificImpulse, final double wetMass, final double dryMass) {
        if (!(dryMass > 0) || wetMass < dryMass) {
            throw new IllegalArgumentException("a stage weighs something empty, and no less full; got " + wetMass
                    + " full and " + dryMass + " empty");
        }
        return specificImpulse * OrbitScale.STANDARD_GRAVITY * Math.log(wetMass / dryMass);
    }

    /**
     * How many times its weight a rocket's thrust is on a world: its thrust in newtons over its mass in kilograms times
     * the world's pull, as a share of the Overworld's.
     */
    public static double thrustToWeight(final double thrust, final double mass, final double pull) {
        if (!(mass > 0) || !(pull > 0)) {
            throw new IllegalArgumentException("a rocket weighs something on a world that pulls; got " + mass + " kg at "
                    + pull + " g");
        }
        return thrust / (mass * OrbitScale.STANDARD_GRAVITY * pull);
    }

    /** Whether a rocket's thrust lifts it off a world: more than its weight there. */
    public static boolean lifts(final double thrust, final double mass, final double pull) {
        return thrustToWeight(thrust, mass, pull) > 1;
    }
}
