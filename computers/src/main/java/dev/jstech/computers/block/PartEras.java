/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.block;

import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * The era a part of a Mainframe or of a rack cabinet keeps in its state, stamped by the origin as it raises the part:
 * the parts are one shared block, and the age of the whole is what sets its skin and the cables the part takes. It is
 * the era's place among the series' seven, from the Vintage (0) to the Advanced (4), the eras these machines exist
 * in: they are the series' own, so no era an add-on brings moves the number a world has saved.
 */
public final class PartEras {

    /** The value of the latest era a part can keep, the Advanced's. */
    public static final int LAST = HardwareEra.seriesIndex(HardwareEra.ADVANCED);

    private PartEras() {
    }

    /** The era property of a part block. */
    public static IntegerProperty property() {
        return IntegerProperty.create("era", 0, LAST);
    }

    /** The value a part of a machine of {@code era} keeps; an era after the Advanced keeps the Advanced's. */
    public static int valueFor(final Era era) {
        return HardwareEra.seriesIndex(HardwareEra.withParts(era));
    }

    /** The era a part's {@code value} names. */
    public static Era eraOf(final int value) {
        return HardwareEra.ofSeriesIndex(value);
    }
}
