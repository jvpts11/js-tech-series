/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import dev.jstech.computers.wireless.WirelessStandard;
import dev.jstech.core.jstechseries.tier.Era;
import java.util.Objects;

/**
 * A Wi-Fi card (computers/docs/design/wireless.md): joins the small computer it sits in to the network through an
 * access point instead of a cable, at the speed of the older of the two. It draws nothing of its own.
 *
 * @param era the age of machine it was made for, whose Wi-Fi it speaks
 * @param bus the slot it takes: PCI on the Legacy, PCI Express later
 */
public record WifiCardSpec(Era era, PcieGeneration bus) implements IExpansionCardSpec {

    public WifiCardSpec {
        Objects.requireNonNull(era, "era must not be null");
        Objects.requireNonNull(bus, "bus must not be null");
        if (WirelessStandard.of(era) == null) {
            throw new IllegalArgumentException("a Wi-Fi card is of an era with Wi-Fi; got " + era);
        }
    }

    /** The Wi-Fi it speaks: its era's. */
    public WirelessStandard standard() {
        return Objects.requireNonNull(WirelessStandard.of(era), "checked when it was made");
    }

    @Override
    public int tdpWatts() {
        return 0;
    }

    @Override
    public ExpansionCardKind kind() {
        return ExpansionCardKind.WIFI;
    }
}
