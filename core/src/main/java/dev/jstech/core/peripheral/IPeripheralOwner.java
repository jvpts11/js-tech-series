/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.peripheral;

import java.util.List;
import java.util.Set;

/**
 * Contract for a BlockEntity that OWNS a set of peripheral endpoints via cables of one peripheral system. It has so
 * many ports of each {@link PortKind}, as its hardware gives them, and each endpoint it links takes one of its kind.
 */
public interface IPeripheralOwner {

    PeripheralCableType cableType();

    List<Long> linkedEndpoints();

    /** How many ports of {@code kind} the owner has: how many endpoints of that kind it can have linked at once. */
    int ports(PortKind kind);

    /** How many of its ports of {@code kind} linked endpoints take. */
    int portsInUse(PortKind kind);

    /**
     * Whether the endpoint at {@code endpointPos} is linked and still has a port: false once the owner lost ports
     * (a card taken out), for the endpoints linked last of that kind, which then unlink and wait for a free one.
     */
    boolean holdsPort(long endpointPos);

    /** The endpoint at {@code endpointPos} was linked, taking a port of {@code kind}. */
    void onEndpointLinked(long endpointPos, PortKind kind);

    void onEndpointUnlinked(long endpointPos);

    default Set<Long> occupiedPositions(long ownerPos) {
        return Set.of(ownerPos);
    }
}
