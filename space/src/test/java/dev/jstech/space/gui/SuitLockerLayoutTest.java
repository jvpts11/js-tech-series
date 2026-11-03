/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.core.gui.layout.GuiLayout;
import java.util.List;
import org.junit.jupiter.api.Test;

class SuitLockerLayoutTest {

    @Test
    void layout_keepsEveryPlaceAndTheInventoryApartAndInsideTheScreen() {
        final GuiLayout layout = SuitLockerLayout.layout();
        assertEquals(List.of(), layout.overlaps());
        assertEquals(List.of(), layout.outOfBounds());
        assertTrue(layout.isClean());
    }

    @Test
    void places_areNineOneForEachThingTheLockerHolds() {
        assertEquals(9, SuitLockerLayout.PLACES.size());
        assertEquals(SuitLockerLayout.PLACES.size(), SuitLockerLayout.PLACES.stream()
                .map(at -> at[0] + "," + at[1]).distinct().count());
    }

    @Test
    void layout_putsTheInventoryUnderTheLockerAndItsLabelOverIt() {
        final int lowest = SuitLockerLayout.PLACES.stream().mapToInt(at -> at[1]).max().orElseThrow();
        assertTrue(lowest + 18 <= SuitLockerLayout.INVENTORY_LABEL_Y);
        assertTrue(SuitLockerLayout.INVENTORY_LABEL_Y + GuiLayout.LINE_HEIGHT <= SuitLockerLayout.INVENTORY_Y);
    }
}
