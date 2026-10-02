/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.peripheral;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class PeripheralPortsTest {

    @Test
    void inUse_countsEachKindApart() {
        final PeripheralPorts ports = new PeripheralPorts();
        ports.link(1L, PortKind.DEVICE);
        ports.link(2L, PortKind.VIDEO);
        ports.link(3L, PortKind.DEVICE);

        assertEquals(2, ports.inUse(PortKind.DEVICE));
        assertEquals(1, ports.inUse(PortKind.VIDEO));
        assertEquals(0, ports.inUse(PortKind.AUDIO));
    }

    @Test
    void link_saysWhetherAnythingChanged() {
        final PeripheralPorts ports = new PeripheralPorts();

        assertTrue(ports.link(1L, PortKind.DEVICE));
        assertFalse(ports.link(1L, PortKind.DEVICE));
        assertTrue(ports.unlink(1L));
        assertFalse(ports.unlink(1L));
    }

    @Test
    void holds_keepsThePortsOfTheFirstLinked() {
        final PeripheralPorts ports = new PeripheralPorts();
        ports.link(1L, PortKind.VIDEO);
        ports.link(2L, PortKind.DEVICE);
        ports.link(3L, PortKind.VIDEO);

        // One video output left: the screen linked first keeps it, and a device between them counts for nothing.
        assertTrue(ports.holds(1L, 1));
        assertFalse(ports.holds(3L, 1));
        assertTrue(ports.holds(3L, 2));
        assertFalse(ports.holds(9L, 4));
    }

    @Test
    void endpoints_isALiveViewThatUnlinks() {
        final PeripheralPorts ports = new PeripheralPorts();
        ports.link(1L, PortKind.DEVICE);
        ports.link(2L, PortKind.AUDIO);

        ports.endpoints().remove(1L);

        assertEquals(List.of(2L), List.copyOf(ports.endpoints()));
        assertNull(ports.kindOf(1L));
    }

    @Test
    void restore_readsBackWhatWasSavedInOrder() {
        final PeripheralPorts saved = new PeripheralPorts();
        saved.link(7L, PortKind.AUDIO);
        saved.link(5L, PortKind.DEVICE);
        saved.link(6L, PortKind.VIDEO);

        final PeripheralPorts loaded = new PeripheralPorts();
        loaded.restore(saved.positions(), saved.kindIds());

        assertArrayEquals(new long[] {7L, 5L, 6L}, loaded.positions());
        assertEquals(PortKind.VIDEO, loaded.kindOf(6L));
    }

    @Test
    void restore_leavesOutAPositionWithoutAKind() {
        // A save from before the kinds, or with a kind no longer known: those peripherals link again.
        final PeripheralPorts loaded = new PeripheralPorts();
        loaded.restore(new long[] {1L, 2L, 3L}, new int[] {PortKind.VIDEO.id(), 99});

        assertEquals(List.of(1L), List.copyOf(loaded.endpoints()));
    }
}
