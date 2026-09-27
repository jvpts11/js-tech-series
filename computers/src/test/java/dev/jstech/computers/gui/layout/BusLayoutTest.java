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

class BusLayoutTest {

    @Test
    void layout_hasNoSolidOverlaps() {
        final GuiLayout l = BusLayout.layout();
        assertTrue(l.overlaps().isEmpty(), "overlapping elements: " + l.overlaps());
    }

    @Test
    void layout_keepsEveryElementInBounds() {
        final GuiLayout l = BusLayout.layout();
        assertTrue(l.outOfBounds().isEmpty(), "out-of-bounds elements: " + l.outOfBounds());
    }

    @Test
    void layout_isClean() {
        assertTrue(BusLayout.layout().isClean());
    }

    @Test
    void layout_endsTheLongestTitleBeforeTheStatusLamp() {
        final GuiLayout l = BusLayout.layout();
        final GuiLayout.Box title = l.boxAt("title");
        final GuiLayout.Box lamp = l.boxAt("statusLamp");
        assertTrue(title.x() + title.width() < lamp.x(),
                "the title ends at " + (title.x() + title.width()) + ", the lamp starts at " + lamp.x());
    }

    @Test
    void layout_keepsTheFilterSlotAndInventoryWhereTheyAlwaysWere() {
        final GuiLayout l = BusLayout.layout();
        assertEquals(new GuiLayout.SlotPosition(12, 44), l.slotAt("filterSlot"));
        assertEquals(new GuiLayout.SlotPosition(8, 107), l.playerInventoryAt());
    }
}
