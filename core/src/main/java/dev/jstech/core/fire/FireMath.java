/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.fire;

/**
 * The arithmetic of fire, pure: how the air's oxygen and the wind change how fast a fire spreads to a place beside it,
 * how far the wind carries it, how hot a fire makes the air some way off, and how much smoke reaches a place above it.
 * Distances are in metres, a block each; pressures in kilopascals; speeds in metres a second.
 */
public final class FireMath {

    /** The oxygen of the air at sea level, in kilopascals: the fire of the game as it is. */
    public static final double OXYGEN_AT_SEA_LEVEL = 21.2;
    /** Below this partial pressure of oxygen nothing burns, in kilopascals. */
    public static final double OXYGEN_FIRE_NEEDS = 12.0;
    /** From this share of oxygen in the air on a fire is an inferno. */
    public static final double INFERNO_SHARE = 0.5;
    /** How much hotter a fire makes the air right at its flames, in kelvin. */
    public static final double FLAME_RISE = 400;
    /** The most fires together make the air hotter, in kelvin. */
    public static final double MOST_RISE = 900;
    /* How many times faster an inferno spreads than its oxygen alone says, and the most oxygen speeds a fire up. */
    private static final double INFERNO = 3;
    private static final double MOST_FROM_OXYGEN = 5;
    /* The wind speed at which a fire spreads twice as fast downwind, and the least share it keeps upwind. */
    private static final double WIND_DOUBLES = 5;
    private static final double LEAST_UPWIND = 0.25;
    /* The wind a fire needs to leap a block downwind, and how far it can leap at most. */
    private static final double LEAPS_FROM = 6;
    private static final int FARTHEST_LEAP = 3;
    /* How fast the heat of a fire falls with the distance, and how fast its smoke spreads out as it rises. */
    private static final double HEAT_SPREAD = 2;
    private static final double SMOKE_SPREAD = 4;
    private static final double SMOKE_THINS = 8;

    private FireMath() {
    }

    /**
     * How many times as fast as in the air at sea level a fire spreads in air with {@code partial} kilopascals of
     * oxygen, {@code share} of it: never with too little, faster the more, an inferno in air half or more oxygen.
     */
    public static double oxygen(final double partial, final double share) {
        if (partial < OXYGEN_FIRE_NEEDS) {
            return 0;
        }
        final double faster = Math.min(MOST_FROM_OXYGEN, partial / OXYGEN_AT_SEA_LEVEL);
        return share >= INFERNO_SHARE ? faster * INFERNO : faster;
    }

    /**
     * How many times as fast a fire spreads toward the place ({@code dx}, {@code dz}) away from it in a wind of
     * ({@code windX}, {@code windZ}): faster downwind, slower upwind, as fast straight up or down.
     */
    public static double wind(final double windX, final double windZ, final int dx, final int dz) {
        final double across = Math.sqrt((double) dx * dx + (double) dz * dz);
        if (across == 0) {
            return 1;
        }
        final double along = (windX * dx + windZ * dz) / across;
        return Math.max(LEAST_UPWIND, 1 + along / WIND_DOUBLES);
    }

    /** How many blocks downwind a fire leaps in a wind of {@code speed}: none in a breeze. */
    public static int leap(final double speed) {
        return (int) Math.min(FARTHEST_LEAP, Math.floor(speed / LEAPS_FROM));
    }

    /** How much hotter one fire makes the air {@code metres} from its flames, in kelvin. */
    public static double heatFrom(final double metres) {
        return FLAME_RISE / (1 + metres * metres / HEAT_SPREAD);
    }

    /** The temperature of air of {@code ambient} kelvin with fires adding {@code rise} kelvin, held to the most. */
    public static double heated(final double ambient, final double rise) {
        return ambient + Math.min(MOST_RISE, Math.max(0, rise));
    }

    /**
     * How much of one fire's smoke reaches a place {@code up} metres above it and {@code across} metres to the side:
     * none from a fire above, as smoke rises, and less the farther it spread and thinned.
     */
    public static double smokeFrom(final double up, final double across) {
        if (up < 0) {
            return 0;
        }
        return 1 / ((1 + across * across / SMOKE_SPREAD) * (1 + up / SMOKE_THINS));
    }

    /** How much of the light the smoke of {@code fires} takes away, from 0 to 1, as {@link #smokeFrom} weighs them. */
    public static double obscuration(final double fires) {
        return 1 - Math.exp(-Math.max(0, fires));
    }
}
