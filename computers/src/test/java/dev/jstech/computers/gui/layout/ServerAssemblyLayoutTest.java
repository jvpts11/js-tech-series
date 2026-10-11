/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.gui.layout.AssemblyGeometry.Seat;
import dev.jstech.computers.gui.layout.AssemblyGeometry.SeatKind;
import dev.jstech.core.gui.layout.GuiLayout;
import org.junit.jupiter.api.Test;

class ServerAssemblyLayoutTest {

    @Test
    void layout_isClean() {
        final GuiLayout l = ServerAssemblyLayout.layout();
        assertTrue(l.isClean(), () -> "overlaps " + l.overlaps() + ", out of bounds " + l.outOfBounds());
        assertTrue(l.zeroSizedSolids().isEmpty(), () -> "zero-sized " + l.zeroSizedSolids());
    }

    @Test
    void layout_seatsEachSlotWhereItsPartIsDrawn() {
        final AssemblyGeometry geo = ServerAssemblyLayout.geometry();
        final GuiLayout l = ServerAssemblyLayout.layout();
        assertEquals(at(geo.seat(SeatKind.BOARD)), l.slotAt("mobo"));
        assertEquals(at(geo.seat(SeatKind.PSU)), l.slotAt("psu"));
        assertEquals(at(geo.seats(SeatKind.CPU).get(0)), l.slotAt("cpu_0"));
        assertEquals(at(geo.seats(SeatKind.RAM).get(0)), l.slotAt("ram_0"));
        assertEquals(at(geo.seats(SeatKind.CARD).get(0)), l.slotAt("gpu_0"));
        assertEquals(new GuiLayout.SlotPosition(AssemblyGeometry.INVENTORY_X, geo.inventoryY()), l.playerInventoryAt());
    }

    @Test
    void geometry_hasNoDrivesOfItsOwnAndNoSwitches() {
        // A rack server's drives are the rack's, and it is switched on in its rack.
        final AssemblyGeometry geo = ServerAssemblyLayout.geometry();
        assertTrue(geo.seats(SeatKind.DISK).isEmpty(), "no disk seats");
        assertTrue(geo.seats(SeatKind.BAY).isEmpty(), "no bays");
        assertEquals(0, geo.switches());
    }

    private static GuiLayout.SlotPosition at(final Seat seat) {
        return new GuiLayout.SlotPosition(seat.x(), seat.y());
    }
}
