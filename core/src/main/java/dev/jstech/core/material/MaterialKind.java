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
 * What kind of material it is, set by its state at room temperature: the kind says which forms make sense for it (an
 * ingot of a metal, a boule of a crystal, a gem of a gem) and the state it is in unless it says otherwise.
 *
 * <p>Pure: no game types.
 */
public enum MaterialKind {

    METAL("metal", MatterState.SOLID),
    /** A crystal grown or cut, as silicon or quartz. */
    CRYSTAL("crystal", MatterState.SOLID),
    GEM("gem", MatterState.SOLID),
    /** A solid that is neither metal nor crystal: carbon, sulfur, a plastic. */
    NONMETAL_SOLID("nonmetal_solid", MatterState.SOLID),
    /** What comes out of the ground, as chalcopyrite, not copper. */
    MINERAL("mineral", MatterState.SOLID),
    LIQUID("liquid", MatterState.LIQUID),
    GAS("gas", MatterState.GAS),
    /** An exotic form of matter, from real physics, observed or predicted. */
    EXOTIC("exotic", MatterState.EXOTIC);

    private final String serializedName;
    private final MatterState roomState;

    MaterialKind(final String serializedName, final MatterState roomState) {
        this.serializedName = serializedName;
        this.roomState = roomState;
    }

    /** The name the kind is written and sent as. */
    public String serializedName() {
        return this.serializedName;
    }

    /** The state a material of this kind is in at room temperature. */
    public MatterState roomState() {
        return this.roomState;
    }

    /** The kind written as {@code name}, or none. */
    public static Optional<MaterialKind> byName(final String name) {
        for (final MaterialKind kind : values()) {
            if (kind.serializedName.equals(name)) {
                return Optional.of(kind);
            }
        }
        return Optional.empty();
    }
}
