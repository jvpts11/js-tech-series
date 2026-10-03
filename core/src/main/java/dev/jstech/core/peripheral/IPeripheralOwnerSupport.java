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

/**
 * An {@link IPeripheralOwner} that keeps its linked endpoints in a {@link PeripheralPorts}, so the block entities that
 * own peripherals share one implementation of the owner contract instead of each repeating it. Only how many ports of
 * each kind it has ({@link #ports}) stays per owner, since it depends on the installed hardware.
 */
public interface IPeripheralOwnerSupport extends IPeripheralOwner {

    /** The live record of linked endpoints backing this owner. */
    PeripheralPorts peripheralPorts();

    /** Marks the owner dirty after its endpoints change (typically {@code setChanged()}). */
    void markPeripheralChange();

    /** The linked endpoints' positions: a live view, in the order they were linked. */
    default Set<Long> peripheralEndpoints() {
        return peripheralPorts().endpoints();
    }

    @Override
    default PeripheralCableType cableType() {
        return PeripheralCableType.COMPUTING;
    }

    @Override
    default List<Long> linkedEndpoints() {
        return List.copyOf(peripheralEndpoints());
    }

    @Override
    default List<Long> enabledEndpoints() {
        return peripheralPorts().enabled();
    }

    @Override
    default PortKind kindOf(final long endpointPos) {
        return peripheralPorts().kindOf(endpointPos);
    }

    @Override
    default boolean isDisabled(final long endpointPos) {
        return peripheralPorts().disabled(endpointPos);
    }

    @Override
    default boolean setDisabled(final long endpointPos, final boolean disabled) {
        if (peripheralPorts().setDisabled(endpointPos, disabled)) {
            markPeripheralChange();
            return true;
        }
        return false;
    }

    @Override
    default int portsInUse(final PortKind kind) {
        return peripheralPorts().inUse(kind);
    }

    @Override
    default boolean holdsPort(final long endpointPos) {
        final PortKind kind = peripheralPorts().kindOf(endpointPos);
        return kind != null && peripheralPorts().holds(endpointPos, ports(kind));
    }

    @Override
    default void onEndpointLinked(final long endpointPos, final PortKind kind) {
        if (peripheralPorts().link(endpointPos, kind)) {
            markPeripheralChange();
        }
    }

    @Override
    default void onEndpointLinkedThrough(final long endpointPos, final PortKind kind, final long hubPos) {
        if (peripheralPorts().linkThrough(endpointPos, kind, hubPos)) {
            markPeripheralChange();
        }
    }

    @Override
    default int portsInUseThrough(final long hubPos) {
        return peripheralPorts().inUseThrough(hubPos);
    }

    @Override
    default OptionalLong hubOf(final long endpointPos) {
        return peripheralPorts().hubOf(endpointPos);
    }

    @Override
    default void onEndpointUnlinked(final long endpointPos) {
        if (peripheralPorts().unlink(endpointPos)) {
            markPeripheralChange();
        }
    }
}
