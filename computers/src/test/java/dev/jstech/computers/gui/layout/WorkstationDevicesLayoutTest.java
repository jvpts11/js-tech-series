/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.gui.layout.CdeFrontPanelLayout.Rect;
import org.junit.jupiter.api.Test;

class WorkstationDevicesLayoutTest {

    @Test
    void window_fitsTheWorkAreaOfTheGlass() {
        assertTrue(WorkstationDevicesLayout.H + WorkstationDevicesLayout.FRAME_H <= 256 - CdeFrontPanelLayout.BAND_H);
        assertTrue(WorkstationDevicesLayout.W + WorkstationDevicesLayout.FRAME_W <= 384);
    }

    @Test
    void everyRow_standsInsideTheWell() {
        final Rect well = WorkstationDevicesLayout.well();
        final int last = WorkstationDevicesLayout.rowY(WorkstationDevicesLayout.ROWS - 1);
        assertTrue(WorkstationDevicesLayout.headY() >= well.y());
        assertTrue(last + WorkstationDevicesLayout.ROW_H <= well.y() + well.h());
    }

    @Test
    void buttons_standUnderTheWellInARow() {
        final Rect well = WorkstationDevicesLayout.well();
        final Rect disable = WorkstationDevicesLayout.disable();
        final Rect enable = WorkstationDevicesLayout.enable();
        final Rect close = WorkstationDevicesLayout.close();
        assertTrue(well.y() + well.h() < close.y() - 2, "the default ring clears the well");
        assertTrue(disable.y() == enable.y() && enable.y() == close.y());
        assertTrue(disable.x() + disable.w() < enable.x() && enable.x() + enable.w() < close.x() - 2);
    }
}
