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
 * The three families of mobile systems (computers/docs/design/wireless.md): Midsoft's Frames Pocket on the PDAs, after
 * the Pocket PC; Quince's qOS on the qPhones and the qPads; and Droidix, open, on the other makers' phones and tablets,
 * after Android. An app is made for families, as a program is for platforms, and each family has its store, served by
 * the network's Mirror.
 *
 * <p>Pure: no part of the game.
 */
public enum MobileFamily implements IStableName {

    FRAMES_POCKET("frames_pocket"),
    QOS("qos"),
    DROIDIX("droidix");

    private final String serializedName;

    MobileFamily(final String serializedName) {
        this.serializedName = serializedName;
    }

    /** The family's name. */
    public TextKey word() {
        return switch (this) {
            case FRAMES_POCKET -> PortableTexts.FRAMES_POCKET;
            case QOS -> PortableTexts.QOS;
            case DROIDIX -> PortableTexts.DROIDIX;
        };
    }

    /** The name of the family's store. */
    public TextKey store() {
        return switch (this) {
            case FRAMES_POCKET -> PortableTexts.FRAMES_MARKETPLACE;
            case QOS -> PortableTexts.QUINCE_STORE;
            case DROIDIX -> PortableTexts.DROIDIX_STORE;
        };
    }

    @Override
    public String serializedName() {
        return serializedName;
    }
}
