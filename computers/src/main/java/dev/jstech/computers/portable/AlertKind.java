/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.portable;

import dev.jstech.core.id.IStableName;
import dev.jstech.core.id.StableNames;

/**
 * The five kinds of alert a network sends its players' portables (computers/docs/design/wireless.md): an item below
 * the level a player watches it at, an Operation that failed, a degraded power supply, a UPS on its battery, a
 * network conflict.
 *
 * <p>Pure: no part of the game.
 */
public enum AlertKind implements IStableName {

    ITEMS("items"),
    OPERATIONS("operations"),
    POWER("power"),
    UPS("ups"),
    CONFLICT("conflict");

    private final String serializedName;

    private static final StableNames<AlertKind> NAMES = StableNames.of(AlertKind.class);

    AlertKind(final String serializedName) {
        this.serializedName = serializedName;
    }

    /** The kind of that serialized name; an unknown one is an item's. */
    public static AlertKind byName(final String name) {
        return NAMES.byName(name, ITEMS);
    }

    @Override
    public String serializedName() {
        return serializedName;
    }
}
