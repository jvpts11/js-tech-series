/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.crc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.core.gui.layout.GuiLayout;
import dev.jstech.core.machine.MachineScreenLayout;
import org.junit.jupiter.api.Test;

class CrcLayoutTest {

    @Test
    void processors_keepsItsSectionsDataReadoutsAndInventoryApartAndInside() {
        assertClean(CrcLayout.processors());
    }

    @Test
    void research_keepsItsChartCardNowAndQueueApartAndInside() {
        assertClean(CrcLayout.research());
    }

    @Test
    void group_keepsItsDrawingTableAndRuleApartAndInside() {
        assertClean(CrcLayout.group());
    }

    @Test
    void place_numbersSectionThenRowThenColumn() {
        assertEquals(CrcLayout.place(0).x() + CrcLayout.SLOT, CrcLayout.place(1).x());
        assertEquals(CrcLayout.place(0).y() + CrcLayout.SLOT, CrcLayout.place(3).y());
        assertEquals(CrcLayout.place(0).x() + CrcLayout.SECTION_PITCH, CrcLayout.place(9).x());
        assertEquals(CrcLayout.place(0).y(), CrcLayout.place(9).y());
    }

    @Test
    void playerInventory_isCentredUnderTheWindowAsTheMenuPutsIt() {
        assertEquals(CrcLayout.WIDTH - CrcLayout.INVENTORY_X - CrcLayout.INVENTORY_WIDTH, CrcLayout.INVENTORY_X,
                CrcLayout.WIDTH % 2 == 0 ? 0 : 1);
        assertTrue(CrcLayout.INVENTORY_Y >= CrcLayout.WINDOW_TOP + CrcLayout.WINDOW_HEIGHT);
        assertEquals(CrcLayout.INVENTORY_X + 8, MachineScreenLayout.playerInventory(CrcLayout.WIDTH).x());
    }

    @Test
    void yearX_runsAcrossTheAxisAndHoldsTheYearsPastItsEnds() {
        assertEquals(CrcLayout.YEAR_LEFT, CrcLayout.yearX(CrcLayout.FIRST_YEAR));
        assertEquals(CrcLayout.YEAR_RIGHT, CrcLayout.yearX(CrcLayout.LAST_YEAR));
        assertEquals(CrcLayout.YEAR_LEFT, CrcLayout.yearX(1800));
        assertEquals(CrcLayout.YEAR_RIGHT, CrcLayout.yearX(2200));
        assertTrue(CrcLayout.yearX(1960) > CrcLayout.yearX(1950));
    }

    @Test
    void cabinetX_spreadsTheCabinetsInsideTheDrawing() {
        for (int count = 1; count <= CrcLayout.CABINETS_SHOWN; count++) {
            for (int i = 0; i < count; i++) {
                final int x = CrcLayout.cabinetX(i, count);
                assertTrue(x > CrcLayout.SCHEMATIC_LEFT && x + CrcLayout.CABINET_WIDTH < CrcLayout.SCHEMATIC_RIGHT,
                        count + " cabinets, the " + i + "th at " + x);
                if (i > 0) {
                    assertTrue(x >= CrcLayout.cabinetX(i - 1, count) + CrcLayout.CABINET_WIDTH + 2);
                }
            }
        }
    }

    private static void assertClean(final GuiLayout layout) {
        assertTrue(layout.isClean(), "overlaps " + layout.overlaps() + ", outside " + layout.outOfBounds());
    }
}
