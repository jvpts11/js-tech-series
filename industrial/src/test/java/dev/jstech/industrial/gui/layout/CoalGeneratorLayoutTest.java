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

class CoalGeneratorLayoutTest {

    @Test
    void layout_isClean() {
        final GuiLayout layout = CoalGeneratorLayout.layout();
        assertTrue(layout.isClean(), () -> "overlaps " + layout.overlaps() + ", out of bounds "
                + layout.outOfBounds());
        assertTrue(layout.zeroSizedSolids().isEmpty(), () -> "zero-sized " + layout.zeroSizedSolids());
    }

    @Test
    void layout_keepsTheFuelSlotWhereTheGeneratorAlwaysHadIt() {
        final GuiLayout layout = CoalGeneratorLayout.layout();
        assertEquals(new GuiLayout.SlotPosition(80, 53), layout.slotAt("fuel"));
        assertEquals(new GuiLayout.SlotPosition(8, 84), layout.playerInventoryAt());
    }
}
