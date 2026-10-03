/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.peripheral;

/**
 * A peripheral that gives its owner more ports: a hub. Linked like any other peripheral, it takes a port of its owner,
 * and every peripheral cabled to it takes one of the hub's ports instead of one of the owner's, as a real hub takes
 * one socket of a computer and offers several.
 *
 * <p>Only a hub linked to an owner lets peripherals through, and only those of a kind it passes: a screen cabled to a
 * hub of device ports finds no way through it. A hub repeats what it passes, so the cable after it reaches as far
 * again as the cable before it. A hub may hang from another hub.
 */
public interface IPeripheralHub extends IPeripheralEndpoint {

    /** How many peripherals can hang from the hub at once. */
    int hubPorts();

    /** Whether peripherals taking a port of {@code kind} pass through the hub; a device hub passes devices only. */
    default boolean passes(final PortKind kind) {
        return kind == PortKind.DEVICE;
    }
}
