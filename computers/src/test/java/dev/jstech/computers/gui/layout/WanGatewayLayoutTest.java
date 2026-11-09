/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

import dev.jstech.computers.gui.layout.AssemblyGeometry.Seat;
import dev.jstech.computers.gui.layout.AssemblyGeometry.SeatKind;
import dev.jstech.core.gui.layout.GuiLayout;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WanGatewayLayoutTest {

    @Test
    void layout_isClean() {
        final GuiLayout l = WanGatewayLayout.layout();
        assertTrue(l.isClean(), () -> "overlaps " + l.overlaps() + ", out of bounds " + l.outOfBounds());
        assertTrue(l.zeroSizedSolids().isEmpty(), () -> "zero-sized " + l.zeroSizedSolids());
        final GuiLayout cooling = WanGatewayLayout.coolingLayout();
        assertTrue(cooling.isClean(), () -> "overlaps " + cooling.overlaps() + ", out " + cooling.outOfBounds());
    }

    @Test
    void layout_seatsEachSlotWhereItsPartIsDrawn() {
        final AssemblyGeometry geo = WanGatewayLayout.geometry();
        final GuiLayout l = WanGatewayLayout.layout();
        assertEquals(at(geo.seat(SeatKind.BOARD)), l.slotAt("mobo"));
        assertEquals(at(geo.seat(SeatKind.PSU)), l.slotAt("psu"));
        assertEquals(at(geo.seats(SeatKind.CPU).get(3)), l.slotAt("cpu_3"));
        assertEquals(at(geo.seats(SeatKind.RAM).get(7)), l.slotAt("ram_7"));
        assertEquals(at(geo.seats(SeatKind.CARD).get(5)), l.slotAt("gpu_5"));
        assertEquals(at(geo.seats(SeatKind.DISK).get(3)), l.slotAt("disk_3"));
        assertEquals(new GuiLayout.SlotPosition(AssemblyGeometry.INVENTORY_X, geo.inventoryY()), l.playerInventoryAt());
    }

    @Test
    void geometry_hasNoOperationsStripAndRoomForTheLinesRow() {
        final AssemblyGeometry geo = WanGatewayLayout.geometry();
        assertNull(geo.operations(), "a gateway runs no Operations of its own");
        final AssemblyGeometry.Box lines = geo.serviceSwitch(1);
        assertTrue(lines.y() + lines.h() <= geo.lamps().y(), "the lines' lamps stand over the lamps' panel");
    }

    private static GuiLayout.SlotPosition at(final Seat seat) {
        return new GuiLayout.SlotPosition(seat.x(), seat.y());
    }
}
