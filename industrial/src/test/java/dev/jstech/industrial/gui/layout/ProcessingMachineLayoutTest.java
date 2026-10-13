/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.gui.layout;

import dev.jstech.core.gui.layout.GuiLayout;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessingMachineLayoutTest {

    @Test
    void layout_isClean() {
        final GuiLayout layout = ProcessingMachineLayout.layout();
        assertTrue(layout.isClean(), () -> "overlaps " + layout.overlaps() + ", out of bounds "
                + layout.outOfBounds());
        assertTrue(layout.zeroSizedSolids().isEmpty(), () -> "zero-sized " + layout.zeroSizedSolids());
    }

    @Test
    void layout_keepsTheSlotsWhereTheMachinesAlwaysHadThem() {
        final GuiLayout layout = ProcessingMachineLayout.layout();
        assertEquals(new GuiLayout.SlotPosition(56, 35), layout.slotAt("input"));
        assertEquals(new GuiLayout.SlotPosition(116, 35), layout.slotAt("output"));
        assertEquals(new GuiLayout.SlotPosition(8, 84), layout.playerInventoryAt());
    }
}
