/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.portable;

import dev.jstech.core.id.IStableName;

/**
 * A mobile system as it was in its years (computers/docs/design/wireless.md), each a look of its family: Frames Pocket
 * of 2002 and of 2007, the qOS of 2007, 2013 and 2017 on, Droidix of 2009, 2014 and 2019. A portable comes with its
 * own from the factory, with no installer, and keeps it.
 *
 * <p>Each takes room on its device's storage: none on the first Frames Pocket, which ran from its read-only memory,
 * and more with each generation after it (estimates).
 *
 * <p>Pure: no part of the game.
 */
public enum MobileSystem implements IStableName {

    FRAMES_POCKET_2002("frames_pocket_2002", MobileFamily.FRAMES_POCKET, 2002, 0),
    FRAMES_POCKET_2007("frames_pocket_2007", MobileFamily.FRAMES_POCKET, 2007, 256),
    QOS_2007("qos_2007", MobileFamily.QOS, 2007, 512),
    DROIDIX_2009("droidix_2009", MobileFamily.DROIDIX, 2009, 256),
    QOS_2013("qos_2013", MobileFamily.QOS, 2013, 4_096),
    DROIDIX_2014("droidix_2014", MobileFamily.DROIDIX, 2014, 2_048),
    QOS_2017("qos_2017", MobileFamily.QOS, 2017, 6_144),
    DROIDIX_2019("droidix_2019", MobileFamily.DROIDIX, 2019, 8_192);

    private final String serializedName;
    private final MobileFamily family;
    private final int year;
    private final int footprintMb;

    MobileSystem(final String serializedName, final MobileFamily family, final int year, final int footprintMb) {
        this.serializedName = serializedName;
        this.family = family;
        this.year = year;
        this.footprintMb = footprintMb;
    }

    /** The family it is a look of. */
    public MobileFamily family() {
        return family;
    }

    /** The year whose look it wears. */
    public int year() {
        return year;
    }

    /** The megabytes of its device's storage it takes. */
    public int footprintMb() {
        return footprintMb;
    }

    @Override
    public String serializedName() {
        return serializedName;
    }
}
