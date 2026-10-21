/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.vehicle;

import dev.jstech.core.id.IStableName;
import dev.jstech.core.text.TextKey;

/** The kinds of parts a vehicle's places take, each place one kind. */
public enum VehiclePartKind implements IStableName {
    /** What turns the driven wheels, the rotors or the propellers: burning fuel or drawing on the battery. */
    ENGINE("engine", VehicleTexts.ENGINE),
    /** A wheel on its suspension: driven, steering, or neither. */
    WHEEL("wheel", VehicleTexts.WHEEL),
    /** The tank the engine burns from. */
    TANK("tank", VehicleTexts.TANK),
    /** The battery an electric engine draws on. */
    BATTERY("battery", VehicleTexts.BATTERY),
    /** The gears between the engine and the wheels. */
    GEARBOX("gearbox", VehicleTexts.GEARBOX),
    /** A light: a headlight, an indicator, a brake light, a light inside. */
    LIGHT("light", VehicleTexts.LIGHT),
    /** The horn. */
    HORN("horn", VehicleTexts.HORN),
    /** A rotor that holds a flying vehicle up. */
    ROTOR("rotor", VehicleTexts.ROTOR),
    /** A propeller that drives a vehicle afloat. */
    PROPELLER("propeller", VehicleTexts.PROPELLER);

    private final String serializedName;
    private final TextKey name;

    VehiclePartKind(final String serializedName, final TextKey name) {
        this.serializedName = serializedName;
        this.name = name;
    }

    @Override
    public String serializedName() {
        return serializedName;
    }

    /** What a player calls a part of this kind. */
    public TextKey title() {
        return name;
    }
}
