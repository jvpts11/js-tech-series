/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.workshop;

import dev.jstech.core.id.IStableName;

/**
 * What the personal-use cards' work is paid with, as the server's setting chooses: the energy of the computer that has
 * the cards, the levels of whoever asked, both, or nothing.
 */
public enum UpdateCost implements IStableName {

    /** The computer's energy, and no levels: the default. */
    ENERGY("energy", true, false),
    /** Half the levels the game's own block would ask of whoever asked, at least one, and no lapis. */
    XP("xp", false, true),
    /** The energy and those levels. */
    BOTH("both", true, true),
    /** Nothing. */
    NONE("none", false, false);

    private final String serializedName;
    private final boolean energy;
    private final boolean levels;

    UpdateCost(final String serializedName, final boolean energy, final boolean levels) {
        this.serializedName = serializedName;
        this.energy = energy;
        this.levels = levels;
    }

    @Override
    public String serializedName() {
        return serializedName;
    }

    /** Whether the computer pays in energy. */
    public boolean energy() {
        return energy;
    }

    /** Whether whoever asked pays in levels. */
    public boolean levels() {
        return levels;
    }
}
