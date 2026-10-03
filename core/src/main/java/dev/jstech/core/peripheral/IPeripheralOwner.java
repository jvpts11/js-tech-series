/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.peripheral;

import java.util.List;
import java.util.OptionalLong;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

/**
 * Contract for a BlockEntity that OWNS a set of peripheral endpoints via cables of one peripheral system. It has so
 * many ports of each {@link PortKind}, as its hardware gives them, and each endpoint it links takes one of its kind.
 */
public interface IPeripheralOwner {

    PeripheralCableType cableType();

    List<Long> linkedEndpoints();

    /**
     * The linked endpoints the owner reads and writes: every linked one but those disabled, by themselves or through a
     * hub on their way. A disabled endpoint stays linked and keeps its port.
     */
    default List<Long> enabledEndpoints() {
        return linkedEndpoints();
    }

    /** The kind of port the linked endpoint at {@code endpointPos} takes, or null when it is not linked. */
    @Nullable
    default PortKind kindOf(final long endpointPos) {
        return null;
    }

    /** Whether the linked endpoint at {@code endpointPos} is disabled, by itself or through a hub on its way. */
    default boolean isDisabled(final long endpointPos) {
        return false;
    }

    /** Disables or enables the linked endpoint at {@code endpointPos}; whether that changed anything. */
    default boolean setDisabled(final long endpointPos, final boolean disabled) {
        return false;
    }

    /** How many ports of {@code kind} the owner has: how many endpoints of that kind it can have linked at once. */
    int ports(PortKind kind);

    /** How many of its ports of {@code kind} linked endpoints take. */
    int portsInUse(PortKind kind);

    /**
     * Whether the endpoint at {@code endpointPos} is linked and still has a port: false once the owner lost ports
     * (a card taken out), for the endpoints linked last of that kind, which then unlink and wait for a free one.
     */
    boolean holdsPort(long endpointPos);

    /** The endpoint at {@code endpointPos} was linked, taking one of the owner's own ports of {@code kind}. */
    void onEndpointLinked(long endpointPos, PortKind kind);

    /**
     * The endpoint at {@code endpointPos} was linked through the {@link IPeripheralHub} at {@code hubPos}, taking one
     * of the hub's ports of {@code kind} rather than one of the owner's.
     */
    void onEndpointLinkedThrough(long endpointPos, PortKind kind, long hubPos);

    /** How many linked endpoints take a port of the hub at {@code hubPos}. */
    int portsInUseThrough(long hubPos);

    /** The hub the linked endpoint at {@code endpointPos} hangs from; empty when it is on the owner's own port. */
    OptionalLong hubOf(long endpointPos);

    void onEndpointUnlinked(long endpointPos);

    default Set<Long> occupiedPositions(long ownerPos) {
        return Set.of(ownerPos);
    }
}
