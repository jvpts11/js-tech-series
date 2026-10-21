/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.vehicle;

/**
 * A vehicle's switches as one number, saved with it and sent to every player seeing it: the ignition, the lights, the
 * indicators, the parking brake, the light inside, the doors' lock, a manual gearbox, whether it brakes, and which of
 * its doors stand open. Pure: no part of the game.
 *
 * @param bits the switches
 */
public record VehicleSwitches(int bits) {

    /** As a vehicle is made: its ignition on, its gearbox automatic, all else off and shut. */
    public static final VehicleSwitches MADE = new VehicleSwitches(1);

    private static final int IGNITION = 1;
    private static final int LIGHTS_SHIFT = 1;
    private static final int TWO_BITS = 3;
    private static final int INDICATORS_SHIFT = 3;
    private static final int PARKING_BRAKE = 1 << 5;
    private static final int INSIDE_LIGHT = 1 << 6;
    private static final int LOCKED = 1 << 7;
    private static final int MANUAL = 1 << 8;
    private static final int BRAKING = 1 << 9;
    private static final int DOORS_SHIFT = 10;
    /** How many doors' switches it keeps. */
    public static final int MOST_DOORS = 16;

    /** Whether its ignition is on. */
    public boolean ignition() {
        return (bits & IGNITION) != 0;
    }

    public VehicleSwitches ignition(final boolean on) {
        return with(IGNITION, on);
    }

    /** Its lights. */
    public Lights lights() {
        return Lights.of((bits >> LIGHTS_SHIFT) & TWO_BITS);
    }

    public VehicleSwitches lights(final Lights to) {
        return new VehicleSwitches((bits & ~(TWO_BITS << LIGHTS_SHIFT)) | (to.level() << LIGHTS_SHIFT));
    }

    /** Its indicators. */
    public Indicators indicators() {
        return Indicators.of((bits >> INDICATORS_SHIFT) & TWO_BITS);
    }

    public VehicleSwitches indicators(final Indicators to) {
        return new VehicleSwitches((bits & ~(TWO_BITS << INDICATORS_SHIFT)) | (to.level() << INDICATORS_SHIFT));
    }

    public boolean parkingBrake() {
        return (bits & PARKING_BRAKE) != 0;
    }

    public VehicleSwitches parkingBrake(final boolean on) {
        return with(PARKING_BRAKE, on);
    }

    public boolean insideLight() {
        return (bits & INSIDE_LIGHT) != 0;
    }

    public VehicleSwitches insideLight(final boolean on) {
        return with(INSIDE_LIGHT, on);
    }

    /** Whether its doors are locked, against all who may not drive it. */
    public boolean locked() {
        return (bits & LOCKED) != 0;
    }

    public VehicleSwitches locked(final boolean on) {
        return with(LOCKED, on);
    }

    /** Whether its gearbox changes only when told. */
    public boolean manual() {
        return (bits & MANUAL) != 0;
    }

    public VehicleSwitches manual(final boolean on) {
        return with(MANUAL, on);
    }

    /** Whether it brakes, which lights its brake lights. */
    public boolean braking() {
        return (bits & BRAKING) != 0;
    }

    public VehicleSwitches braking(final boolean on) {
        return with(BRAKING, on);
    }

    /** Whether its door {@code index} stands open. */
    public boolean open(final int index) {
        return index >= 0 && index < MOST_DOORS && (bits & (1 << (DOORS_SHIFT + index))) != 0;
    }

    public VehicleSwitches open(final int index, final boolean on) {
        if (index < 0 || index >= MOST_DOORS) {
            return this;
        }
        return with(1 << (DOORS_SHIFT + index), on);
    }

    private VehicleSwitches with(final int bit, final boolean on) {
        return new VehicleSwitches(on ? bits | bit : bits & ~bit);
    }

    /** How its lights are switched. */
    public enum Lights {
        OFF(0), SIDE(1), DIPPED(2), MAIN(3);

        private final int level;

        Lights(final int level) {
            this.level = level;
        }

        /** The next, round to off after the brightest. */
        public Lights next() {
            return of((level + 1) & TWO_BITS);
        }

        /** Whether the headlights light the way. */
        public boolean headlights() {
            return level >= DIPPED.level;
        }

        int level() {
            return level;
        }

        static Lights of(final int level) {
            return switch (level) {
                case 1 -> SIDE;
                case 2 -> DIPPED;
                case 3 -> MAIN;
                default -> OFF;
            };
        }
    }

    /** How its indicators are switched. */
    public enum Indicators {
        OFF(0), LEFT(1), RIGHT(2), HAZARD(3);

        private final int level;

        Indicators(final int level) {
            this.level = level;
        }

        /** Whether the left ones blink. */
        public boolean left() {
            return this == LEFT || this == HAZARD;
        }

        /** Whether the right ones blink. */
        public boolean right() {
            return this == RIGHT || this == HAZARD;
        }

        int level() {
            return level;
        }

        static Indicators of(final int level) {
            return switch (level) {
                case 1 -> LEFT;
                case 2 -> RIGHT;
                case 3 -> HAZARD;
                default -> OFF;
            };
        }
    }
}
