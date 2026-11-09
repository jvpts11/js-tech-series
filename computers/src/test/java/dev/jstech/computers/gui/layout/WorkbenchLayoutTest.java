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

class WorkbenchLayoutTest {

    @Test
    void layout_keepsEveryPartApartAndInsideTheWindow() {
        final GuiLayout l = WorkbenchLayout.layout();
        assertTrue(l.isClean(), l.overlaps() + " " + l.outOfBounds());
    }

    @Test
    void trays_lieThreeByThreeRowByRow() {
        assertEquals(WorkbenchLayout.TRAY_X, WorkbenchLayout.trayX(0));
        assertEquals(WorkbenchLayout.TRAY_X + 2 * WorkbenchLayout.TRAY_PITCH, WorkbenchLayout.trayX(2));
        assertEquals(WorkbenchLayout.TRAY_X, WorkbenchLayout.trayX(3));
        assertEquals(WorkbenchLayout.TRAY_Y + WorkbenchLayout.TRAY_PITCH, WorkbenchLayout.trayY(3));
        assertEquals(WorkbenchLayout.TRAY_Y + 2 * WorkbenchLayout.TRAY_PITCH, WorkbenchLayout.trayY(8));
    }

    @Test
    void trays_endBeforeTheEraToolAndThePartBeforeTheGear() {
        assertTrue(WorkbenchLayout.trayX(8) + 16 < WorkbenchLayout.GROOVE_X);
        assertTrue(WorkbenchLayout.PART_TRAY_X + WorkbenchLayout.PART_TRAY_SIZE < WorkbenchLayout.GEAR_X);
        assertTrue(WorkbenchLayout.GEAR_X + WorkbenchLayout.GEAR_WIDTH < WorkbenchLayout.STRIP_X);
    }
}
