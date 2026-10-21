/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.vehicle;

import dev.jstech.core.id.IStableName;

/** What a vehicle's light is for, which says when it shines. */
public enum VehicleLightRole implements IStableName {
    /** Lights the way ahead, dipped or full, while the lights are on. */
    HEAD("head"),
    /** A red light behind, while the lights are on. */
    TAIL("tail"),
    /** A red light behind, bright while it brakes. */
    BRAKE("brake"),
    /** Blinks on the left while the driver indicates left, or for a hazard. */
    INDICATOR_LEFT("indicator_left"),
    /** Blinks on the right while the driver indicates right, or for a hazard. */
    INDICATOR_RIGHT("indicator_right"),
    /** Lights the inside while it is switched on. */
    INSIDE("inside");

    private final String serializedName;

    VehicleLightRole(final String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String serializedName() {
        return serializedName;
    }
}
