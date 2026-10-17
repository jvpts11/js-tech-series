/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio.acoustics;

/**
 * What a sound travels through where the listener is, and what that does to it: in a vacuum nothing carries it, so
 * only what the listener touches is heard, through the body; under water everything is muffled and comes faster; in
 * thin air, as on Mars, sounds are weaker, as weak as the air is thin.
 *
 * @param kind        air, water or nothing
 * @param pressure    the air's pressure in kilopascals, for air
 * @param temperature the air's temperature in degrees Celsius, which sets how fast sound goes in it
 */
public record SoundMedium(Kind kind, double pressure, double temperature) {

    /** The air at sea level, at 15 °C. */
    public static final SoundMedium SEA_LEVEL = new SoundMedium(Kind.AIR, 101.325, 15);
    /** Under this pressure there is no air to carry a sound, in kilopascals. */
    public static final double VACUUM_BELOW = 0.05;
    /**
     * How near a sound must be for the listener to be touching what makes it, in blocks: their own steps, from their
     * feet, below their ears, included.
     */
    public static final double TOUCHING = 2.0;
    /* What is left of a sound felt through the body rather than heard through the air: soft, and dull. */
    private static final double TOUCHED_GAIN = 0.4;
    private static final double TOUCHED_HIGHS = 0.25;
    /* What water leaves of the high notes. */
    private static final double WATER_HIGHS = 0.12;
    /* Sound in still air at 0 °C, and in water, in metres a second. */
    private static final double SPEED_AT_ZERO = 331.3;
    private static final double SPEED_IN_WATER = 1481.0;
    private static final double ZERO_KELVIN = -273.15;

    public SoundMedium {
        if (pressure < 0 || temperature < ZERO_KELVIN) {
            throw new IllegalArgumentException("not a medium: " + pressure + " kPa at " + temperature + " °C");
        }
    }

    /** The medium of a place: water when the listener's head is in it, else air, or nothing at so little pressure. */
    public static SoundMedium of(final double kilopascals, final double celsius, final boolean underWater) {
        if (underWater) {
            return new SoundMedium(Kind.WATER, kilopascals, celsius);
        }
        return new SoundMedium(kilopascals < VACUUM_BELOW ? Kind.VACUUM : Kind.AIR, kilopascals, celsius);
    }

    /** Whether it carries a sound from afar at all. */
    public boolean carries() {
        return this.kind != Kind.VACUUM;
    }

    /**
     * How loud a sound {@code blocks} away is heard through it, from 1: in thin air as loud as the pressure's square
     * root over the sea level's, which the energy a sound wave carries follows; in a vacuum only what is touched.
     */
    public double gain(final double blocks) {
        return switch (this.kind) {
            case VACUUM -> blocks <= TOUCHING ? TOUCHED_GAIN : 0;
            case WATER -> 1;
            case AIR -> Math.min(1, Math.sqrt(this.pressure / SEA_LEVEL.pressure));
        };
    }

    /** How much of its high notes a sound {@code blocks} away keeps through it, from 1. */
    public double highs(final double blocks) {
        return switch (this.kind) {
            case VACUUM -> blocks <= TOUCHING ? TOUCHED_HIGHS : 0;
            case WATER -> WATER_HIGHS;
            case AIR -> 1;
        };
    }

    /** How fast sound goes through it, in metres a second; nothing in a vacuum. */
    public double speed() {
        return switch (this.kind) {
            case VACUUM -> 0;
            case WATER -> SPEED_IN_WATER;
            case AIR -> SPEED_AT_ZERO * Math.sqrt(1 + this.temperature / -ZERO_KELVIN);
        };
    }

    /** What a sound travels through. */
    public enum Kind {
        AIR,
        WATER,
        VACUUM
    }
}
