/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.wan;

import dev.jstech.core.jstechseries.network.DataLink;
import dev.jstech.core.uuid.NetworkUuid;
import java.util.Objects;

/**
 * One network's side of a federation: a port of one of its WAN Gateway Computers whose line is up to a port of a
 * gateway on another network that has a Mainframe of its own. Each side publishes its own, so a federation stands while
 * both do.
 *
 * @param from       the network this side is
 * @param to         the network at the far end of the line
 * @param gateway    where this side's gateway stands, as a packed block position
 * @param port       its port, 1 to 4
 * @param farGateway where the far side's gateway stands
 * @param farPort    its port
 * @param line       the line between them
 * @param capacity   the most items a tick this side's gateway can pass: its processors' capacity
 * @param settings   this side's port switches: what {@code to} may do on {@code from}, and whether this port passes
 *                   {@code to}'s requests on to the other networks {@code from} is federated with
 */
public record FederationEdge(NetworkUuid from, NetworkUuid to, long gateway, int port, long farGateway, int farPort,
                             DataLink line, long capacity, PortSettings settings) {

    public FederationEdge {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(line, "line");
        Objects.requireNonNull(settings, "settings");
    }

    /** The most items a tick cross this side of the line: the line's speed, or the gateway's when it is slower. */
    public long speed() {
        return Math.min(line.throughput(), capacity);
    }
}
