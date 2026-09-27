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

import dev.jstech.core.gui.layout.GuiLayout;
import org.junit.jupiter.api.Test;

class PersonalComputerLayoutTest {

    @Test
    void layout_isClean() {
        final GuiLayout l = PersonalComputerLayout.layout();
        assertTrue(l.isClean(), () -> "overlaps " + l.overlaps() + ", out of bounds " + l.outOfBounds());
        assertTrue(l.zeroSizedSolids().isEmpty(), () -> "zero-sized " + l.zeroSizedSolids());
    }

    @Test
    void layout_keepsTheHardwareSlotsWhereThePcAlwaysHadThem() {
        final GuiLayout l = PersonalComputerLayout.layout();
        assertEquals(new GuiLayout.SlotPosition(8, 40), l.slotAt("mobo"));
        assertEquals(new GuiLayout.SlotPosition(8, 73), l.slotAt("psu"));
        assertEquals(new GuiLayout.SlotPosition(44, 40), l.slotAt("cpu"));
        assertEquals(new GuiLayout.SlotPosition(44, 73), l.slotAt("ram_0"));
        assertEquals(new GuiLayout.SlotPosition(44, 106), l.slotAt("gpu_0"));
        assertEquals(new GuiLayout.SlotPosition(8, 106), l.slotAt("disk_0"));
        assertEquals(new GuiLayout.SlotPosition(8, 138), l.playerInventoryAt());
    }
}
