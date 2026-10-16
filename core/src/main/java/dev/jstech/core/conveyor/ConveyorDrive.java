/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.conveyor;

/**
 * What makes a kind of conveyor move: nothing at all, mechanical power through a shaft at any of its belts' sides,
 * or energy from any of its belts' stores. A run of belts moves as one: a shaft turns every belt of it, and energy taken
 * in at one belt runs them all.
 */
public sealed interface ConveyorDrive permits ConveyorDrive.Free, ConveyorDrive.Mechanical, ConveyorDrive.Energy {

    /** It moves by itself, at its full speed: for a belt a mod powers some other way, or none. */
    static ConveyorDrive free() {
        return new Free();
    }

    /**
     * It moves as a shaft turns it, at its full speed at {@code ratedRpm} and slower below, asking {@code torque}
     * newton-metres of its network for each of its belts.
     */
    static ConveyorDrive mechanical(final double ratedRpm, final double torque) {
        return new Mechanical(ratedRpm, torque);
    }

    /** It moves on energy, {@code watts} for each belt while it carries anything, slower when it gets less. */
    static ConveyorDrive energy(final double watts) {
        return new Energy(watts);
    }

    /** Moving by itself. */
    record Free() implements ConveyorDrive {
    }

    /**
     * Moving as a shaft turns it.
     *
     * @param ratedRpm the speed of its shaft at which it moves at its full speed
     * @param torque   what each belt asks of its network, in newton-metres
     */
    record Mechanical(double ratedRpm, double torque) implements ConveyorDrive {

        public Mechanical {
            if (!(ratedRpm > 0.0) || !(torque >= 0.0)) {
                throw new IllegalArgumentException("a belt moves at its full speed at some speed of its shaft, and "
                        + "asks no less than nothing: " + ratedRpm + " rpm, " + torque + " N·m");
            }
        }
    }

    /**
     * Moving on energy.
     *
     * @param watts what each belt draws while it carries anything
     */
    record Energy(double watts) implements ConveyorDrive {

        public Energy {
            if (!(watts > 0.0)) {
                throw new IllegalArgumentException("a belt moving on energy draws some: " + watts + " W");
            }
        }
    }
}
