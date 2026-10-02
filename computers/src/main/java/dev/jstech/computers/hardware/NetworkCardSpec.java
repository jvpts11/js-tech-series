/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import dev.jstech.core.tier.HardwareEra;

import java.util.Objects;

/**
 * A network adapter: the Optical Network Card, what a machine on the backbone needs to take the fibre. Without one the
 * machine takes the copper of its backbone and leaves the fibre out, as a machine with no fibre port would.
 *
 * @param era      the age of machine it was made for
 * @param bus      the slot it takes
 * @param tdpWatts what it draws
 */
public record NetworkCardSpec(HardwareEra era, PcieGeneration bus, int tdpWatts) implements IExpansionCardSpec {

    public NetworkCardSpec {
        Objects.requireNonNull(era, "era must not be null");
        Objects.requireNonNull(bus, "bus must not be null");
        if (tdpWatts < 0) {
            throw new IllegalArgumentException("tdpWatts must be >= 0; got " + tdpWatts);
        }
    }

    @Override
    public ExpansionCardKind kind() {
        return ExpansionCardKind.NETWORK;
    }
}
