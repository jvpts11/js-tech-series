/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.network;

import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.uuid.NetworkUuid;

import java.util.Objects;

/**
 * Snapshot of a Server Router operating on a network: the network, where the router stands, and the era it was built
 * in, which sets how many Server Racks it manages across all its datacenter sections.
 */
public record ServerRouterElement(
        NetworkUuid networkUuid,
        long pos,
        Era era
) implements INetworkTopologyElement {

    public ServerRouterElement {
        Objects.requireNonNull(networkUuid, "networkUuid must not be null");
        Objects.requireNonNull(era, "era must not be null");
    }

    public int maxRacks() {
        return maxRacksFor(era);
    }

    /**
     * How many racks a router of {@code era} manages: none in the Vintage, which has no datacenter sections, then 4,
     * 8, 16 and 32 from the Legacy to the Advanced. An era past the Advanced manages what the Advanced does until its
     * own router is designed, and an era an add-on brings what the series' era before it does.
     */
    public static int maxRacksFor(final Era era) {
        return switch (HardwareEra.seriesLevel(era)) {
            case 0 -> 0;
            case 1 -> 4;
            case 2 -> 8;
            case 3 -> 16;
            default -> 32;
        };
    }
}
