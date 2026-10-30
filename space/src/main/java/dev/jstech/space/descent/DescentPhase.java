/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.descent;

import dev.jstech.core.id.IStableName;

/**
 * Where a rocket is in its coming down to a world, as its flight computer tells it: none; leaving its orbit for the
 * spot over the landing; falling through the world's sky, glowing where there is air; under its parachutes; braking
 * on its engines down to its legs; and landed.
 */
public enum DescentPhase implements IStableName {

    NONE("none"),
    DEORBIT("deorbit"),
    ENTRY("entry"),
    PARACHUTES("parachutes"),
    BRAKING("braking"),
    LANDED("landed");

    private final String serializedName;

    DescentPhase(final String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String serializedName() {
        return this.serializedName;
    }

    /** Whether the rocket is on its way down. */
    public boolean descending() {
        return this == DEORBIT || this == ENTRY || this == PARACHUTES || this == BRAKING;
    }
}
