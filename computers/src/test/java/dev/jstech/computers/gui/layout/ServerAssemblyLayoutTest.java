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

class ServerAssemblyLayoutTest {

    @Test
    void layout_isClean() {
        final GuiLayout l = ServerAssemblyLayout.layout();
        assertTrue(l.isClean(), () -> "overlaps " + l.overlaps() + ", out of bounds " + l.outOfBounds());
        assertTrue(l.zeroSizedSolids().isEmpty(), () -> "zero-sized " + l.zeroSizedSolids());
    }

    @Test
    void layout_keepsTheHardwareSlotsWhereTheServerAlwaysHadThem() {
        final GuiLayout l = ServerAssemblyLayout.layout();
        assertEquals(new GuiLayout.SlotPosition(8, 96), l.slotAt("mobo"));
        assertEquals(new GuiLayout.SlotPosition(26, 96), l.slotAt("psu"));
        assertEquals(new GuiLayout.SlotPosition(52, 96), l.slotAt("cpu_0"));
        assertEquals(new GuiLayout.SlotPosition(52, 126), l.slotAt("ram_0"));
        assertEquals(new GuiLayout.SlotPosition(52, 174), l.slotAt("gpu_0"));
        assertEquals(new GuiLayout.SlotPosition(8, 214), l.playerInventoryAt());
    }
}
