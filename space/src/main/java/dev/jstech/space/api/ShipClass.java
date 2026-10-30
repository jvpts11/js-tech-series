/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.api;

import dev.jstech.core.id.IStableName;

/**
 * A starship's class, which sets the joint its fuselage sections share and how much room its hull has for modules. The
 * small ship, chosen for short trips, specific jobs and easy parking, cheaper, easier and faster to build than a large
 * one, has room for one medium module, or two or three small ones.
 */
public enum ShipClass implements IStableName {

    SMALL("small", 3);

    private final String serializedName;
    private final int room;

    ShipClass(final String serializedName, final int room) {
        this.serializedName = serializedName;
        this.room = room;
    }

    @Override
    public String serializedName() {
        return this.serializedName;
    }

    /** How much room its hull has for modules: a small module takes one, a medium one three. */
    public int room() {
        return this.room;
    }
}
