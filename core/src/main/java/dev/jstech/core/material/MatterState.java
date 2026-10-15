/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.material;

import java.util.Optional;

/**
 * A state of matter, each with its own way of being kept and carried: solids in inventories, liquids in tanks and
 * pipes, gases in pressurised tanks, supercritical fluids in pressure vessels, slurries in lined lines, plasma and
 * exotic matter in containment. A material has the state it takes at room temperature, and may take others (a molten
 * metal is a liquid, steam a gas).
 *
 * <p>Pure: no game types.
 */
public enum MatterState {

    SOLID("solid"),
    LIQUID("liquid"),
    GAS("gas"),
    /** Neither liquid nor gas, past its critical point, as the carbon dioxide of aerogel making. */
    SUPERCRITICAL("supercritical"),
    /** Solids suspended in a liquid: drilling mud, a dissolved ore. */
    SLURRY("slurry"),
    PLASMA("plasma"),
    /** Each exotic form of matter, in the state it calls for, always contained. */
    EXOTIC("exotic");

    private final String serializedName;

    MatterState(final String serializedName) {
        this.serializedName = serializedName;
    }

    /** The name the state is written and sent as. */
    public String serializedName() {
        return this.serializedName;
    }

    /** The state written as {@code name}, or none. */
    public static Optional<MatterState> byName(final String name) {
        for (final MatterState state : values()) {
            if (state.serializedName.equals(name)) {
                return Optional.of(state);
            }
        }
        return Optional.empty();
    }
}
