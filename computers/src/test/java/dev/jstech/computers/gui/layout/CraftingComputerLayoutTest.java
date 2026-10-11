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
import dev.jstech.computers.hardware.CaseBays;
import dev.jstech.core.gui.layout.GuiLayout;
import dev.jstech.core.tier.HardwareEra;
import org.junit.jupiter.api.Test;

class CraftingComputerLayoutTest {

    @Test
    void layout_isCleanInEveryEra() {
        for (final HardwareEra era : HardwareEra.values()) {
            final GuiLayout l = CraftingComputerLayout.layout(era);
            assertTrue(l.isClean(), () -> era + ": overlaps " + l.overlaps() + ", out of bounds " + l.outOfBounds());
            assertTrue(l.zeroSizedSolids().isEmpty(), () -> era + ": zero-sized " + l.zeroSizedSolids());
        }
    }

    @Test
    void layout_seatsEachSlotWhereItsPartIsDrawn() {
        for (final HardwareEra era : HardwareEra.values()) {
            final AssemblyGeometry geo = CraftingComputerLayout.geometry(era);
            final GuiLayout l = CraftingComputerLayout.layout(era);
            assertEquals(at(geo.seat(SeatKind.BOARD)), l.slotAt("mobo"), era + " board");
            assertEquals(at(geo.seat(SeatKind.PSU)), l.slotAt("psu"), era + " supply");
            assertEquals(at(geo.seat(SeatKind.CPU)), l.slotAt("cpu"), era + " processor");
            assertEquals(at(geo.seats(SeatKind.CARD).get(0)), l.slotAt("pcie_0"), era + " first card");
            assertEquals(at(geo.seats(SeatKind.CARD).get(3)), l.slotAt("pcie_3"), era + " last card");
            assertEquals(at(geo.seats(SeatKind.DISK).get(0)), l.slotAt("disk_0"), era + " first disk");
            assertEquals(new GuiLayout.SlotPosition(AssemblyGeometry.INVENTORY_X, geo.inventoryY()),
                    l.playerInventoryAt(), era + " inventory");
        }
    }

    @Test
    void layout_hasTheBaysOfItsEraCase() {
        for (final HardwareEra era : HardwareEra.values()) {
            final long bays = CraftingComputerLayout.layout(era).elements().stream()
                    .filter(e -> e.name().startsWith("bay_")).count();
            assertEquals(CaseBays.of(era), bays, era + " bays");
        }
    }

    private static GuiLayout.SlotPosition at(final Seat seat) {
        return new GuiLayout.SlotPosition(seat.x(), seat.y());
    }
}
