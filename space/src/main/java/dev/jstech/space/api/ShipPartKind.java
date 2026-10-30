/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.api;

import dev.jstech.core.id.IStableName;

/** What a part of a starship is: every part is a multiblock of its own. */
public enum ShipPartKind implements IStableName {

    /** Where the ship is flown from. */
    COCKPIT("cockpit"),
    /** The structure, with its airlock, its docking port or its corridor. */
    HULL("hull"),
    /** Cargo. */
    HOLD("hold"),
    /** Power. */
    REACTOR("reactor"),
    /** A functional module: a workshop, a laboratory, quarters, computing, small cargo. */
    MODULE("module"),
    /** Thrust in space. */
    MAIN_ENGINE("main_engine"),
    /** Flying in the air, its nozzles turning down for a vertical take-off. */
    AIR_ENGINE("air_engine"),
    /** Lift in the air. */
    WINGS("wings"),
    /** Upright fins. */
    FINS("fins"),
    /** Manoeuvring thrusters: turning, docking, landing precisely. */
    THRUSTERS("thrusters"),
    /** Wheels or feet. */
    GEAR("gear");

    private final String serializedName;

    ShipPartKind(final String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String serializedName() {
        return this.serializedName;
    }
}
