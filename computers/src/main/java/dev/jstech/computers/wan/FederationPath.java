/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.wan;

import dev.jstech.core.uuid.NetworkUuid;
import java.util.List;

/**
 * The way from one network to another it is federated with, directly or through networks that relay for it.
 *
 * @param networks every network on the way, the asking one first and the one reached last
 * @param out      the side of each link the way leaves by, one a link: the first leaves the asking network
 * @param grant    the reached network's port the way comes in by: what the asking network may do there
 * @param speed    the most items a tick the way carries: its slowest line or gateway
 */
public record FederationPath(List<NetworkUuid> networks, List<FederationEdge> out, PortSettings grant, long speed) {

    public FederationPath {
        networks = List.copyOf(networks);
        out = List.copyOf(out);
    }

    /** The network the way reaches. */
    public NetworkUuid target() {
        return networks.getLast();
    }

    /** How many lines the way crosses. */
    public int hops() {
        return out.size();
    }

    /** The networks between the two ends, each relaying for the asking one. */
    public List<NetworkUuid> relays() {
        return networks.subList(1, networks.size() - 1);
    }

    /** Whether the asking network may do {@code right} on the one reached. */
    public boolean allows(final WanRight right) {
        return grant.allows(right);
    }
}
