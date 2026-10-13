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

class MainframeLayoutTest {

    @Test
    void layout_isClean() {
        final GuiLayout l = MainframeLayout.layout();
        assertTrue(l.isClean(), () -> "overlaps " + l.overlaps() + ", out of bounds " + l.outOfBounds());
        assertTrue(l.zeroSizedSolids().isEmpty(), () -> "zero-sized " + l.zeroSizedSolids());
    }

    @Test
    void layout_seatsEachSlotWhereItsPartIsDrawn() {
        final AssemblyGeometry geo = MainframeLayout.geometry();
        final GuiLayout l = MainframeLayout.layout();
        assertEquals(at(geo.seat(SeatKind.BOARD)), l.slotAt("mobo"));
        assertEquals(at(geo.seat(SeatKind.PSU)), l.slotAt("psu"));
        assertEquals(at(geo.seats(SeatKind.CPU).get(3)), l.slotAt("cpu_3"));
        assertEquals(at(geo.seats(SeatKind.RAM).get(7)), l.slotAt("ram_7"));
        assertEquals(at(geo.seats(SeatKind.CARD).get(5)), l.slotAt("gpu_5"));
        assertEquals(at(geo.seats(SeatKind.DISK).get(3)), l.slotAt("disk_3"));
        assertEquals(new GuiLayout.SlotPosition(AssemblyGeometry.INVENTORY_X, geo.inventoryY()), l.playerInventoryAt());
    }

    @Test
    void geometry_carriesTheOperationsStripUnderTheCase() {
        final AssemblyGeometry geo = MainframeLayout.geometry();
        final AssemblyGeometry.Box ops = geo.operations();
        assertTrue(ops != null && ops.y() >= geo.caseBox().y() + geo.caseBox().h(), "the strip sits under the case");
        assertTrue(ops.y() + ops.h() <= geo.inventoryY(), "the strip ends above the inventory");
    }

    private static GuiLayout.SlotPosition at(final Seat seat) {
        return new GuiLayout.SlotPosition(seat.x(), seat.y());
    }
}
