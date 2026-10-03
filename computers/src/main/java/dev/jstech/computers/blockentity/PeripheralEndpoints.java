/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import dev.jstech.core.peripheral.PeripheralPorts;
import net.minecraft.nbt.CompoundTag;

/**
 * What one machine has on the other end of its peripheral cables, each by the packed position of the endpoint with
 * the kind of port it takes: the monitors at its desk on its video outputs, its speakers on its audio output, and
 * whatever else such a cable reaches on its device ports or on a hub's. Kept for a computer and for a rack alike.
 *
 * <p>The record is handed out as it stands rather than copied, because linking and unlinking a peripheral are made
 * on it directly by the owner support the machine implements.
 */
final class PeripheralEndpoints {

    private final PeripheralPorts ports = new PeripheralPorts();

    private static final String POSITIONS = "LinkedPeripherals";
    private static final String KINDS = "LinkedPeripheralKinds";
    private static final String HUBS = "LinkedPeripheralHubs";
    private static final String DISABLED = "DisabledPeripherals";

    /** The links as they stand: the record itself, which is where a link is made and unmade. */
    PeripheralPorts ports() {
        return this.ports;
    }

    void save(final CompoundTag tag) {
        if (this.ports.endpoints().isEmpty()) {
            return;
        }
        tag.putLongArray(POSITIONS, this.ports.positions());
        tag.putIntArray(KINDS, this.ports.kindIds());
        tag.putLongArray(HUBS, this.ports.hubPositions());
        final long[] disabled = this.ports.disabledPositions();
        if (disabled.length > 0) {
            tag.putLongArray(DISABLED, disabled);
        }
    }

    /*
     * A save from before the kinds of port has no kinds; its peripherals link again, each on a port of its kind. One
     * from before the hubs has no hubs; its peripherals' ways are checked again on their next tick. One with nothing
     * disabled has no list of them.
     */
    void load(final CompoundTag tag) {
        this.ports.restore(tag.getLongArray(POSITIONS), tag.getIntArray(KINDS), tag.getLongArray(HUBS),
                tag.getLongArray(DISABLED));
    }
}
