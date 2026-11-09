/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.portable;

import dev.jstech.core.id.IStableName;
import dev.jstech.core.text.TextKey;

/**
 * What a portable device is (computers/docs/design/wireless.md): a PDA in the Legacy and the Transition, a smartphone
 * from the Transition on, a tablet from the Standard on.
 *
 * <p>Pure: no part of the game.
 */
public enum PortableKind implements IStableName {

    PDA("pda", 1_800),
    SMARTPHONE("smartphone", 2_400),
    TABLET("tablet", 2_400);

    private final String serializedName;
    private final int batterySeconds;

    PortableKind(final String serializedName, final int batterySeconds) {
        this.serializedName = serializedName;
        this.batterySeconds = batterySeconds;
    }

    /**
     * Whether it comes from the factory with a camera and GPS, as the real smartphones and tablets of each era did and
     * the PDAs did not.
     */
    public boolean hasCameraAndGps() {
        return this != PDA;
    }

    /**
     * How long a full charge keeps it on, in seconds of the server running: two game days on a smartphone or a tablet,
     * a day and a half on a PDA (estimates).
     */
    public int batterySeconds() {
        return batterySeconds;
    }

    /** The word a tooltip calls it by. */
    public TextKey word() {
        return switch (this) {
            case PDA -> PortableTexts.KIND_PDA;
            case SMARTPHONE -> PortableTexts.KIND_SMARTPHONE;
            case TABLET -> PortableTexts.KIND_TABLET;
        };
    }

    @Override
    public String serializedName() {
        return serializedName;
    }
}
