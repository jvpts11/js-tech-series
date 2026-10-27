/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.integration.computers;

import dev.jstech.core.gui.layout.GuiLayout;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MachineMonitorLayoutTest {

    @Test
    void window_isCleanInTheClassicLook() {
        final GuiLayout layout = MachineMonitorLayout.window(MachineMonitorLayout.CLASSIC_W,
                MachineMonitorLayout.CLASSIC_H, true);

        assertTrue(layout.isClean(), "overlaps " + layout.overlaps() + ", out of bounds " + layout.outOfBounds());
    }

    @Test
    void window_isCleanInTheModernLook() {
        final GuiLayout layout = MachineMonitorLayout.window(MachineMonitorLayout.MODERN_W,
                MachineMonitorLayout.MODERN_H, false);

        assertTrue(layout.isClean(), "overlaps " + layout.overlaps() + ", out of bounds " + layout.outOfBounds());
    }

    @Test
    void columns_holdTheTimeTheLongestAlertAndAPlace() {
        assertTrue(width("14:02") <= MachineMonitorLayout.TIME_W, "the time");
        assertTrue(width("Undervoltage") <= MachineMonitorLayout.ALERT_W, "the longest alert");
        assertTrue(width("1204, 64, -380") <= MachineMonitorLayout.WHERE_W, "where a machine stands");
    }

    @Test
    void reading_hasRoomInEitherLook() {
        assertTrue(MachineMonitorLayout.readingW(MachineMonitorLayout.tableW(MachineMonitorLayout.CLASSIC_W, true))
                >= width("1840 °C, limit 1750 °C"), "the classic list shows a reading whole");
        assertTrue(MachineMonitorLayout.readingW(MachineMonitorLayout.tableW(MachineMonitorLayout.MODERN_W, false))
                >= width("on a port"), "the modern list shows the start of one; the panel shows it whole");
    }

    @Test
    void list_showsSeveralAlertsInEitherLook() {
        assertTrue(MachineMonitorLayout.listH(MachineMonitorLayout.CLASSIC_H, true) / MachineMonitorLayout.ROW_H >= 6,
                "the classic list");
        assertTrue(MachineMonitorLayout.listH(MachineMonitorLayout.MODERN_H, false) / MachineMonitorLayout.ROW_H
                >= 10, "the modern list");
    }

    @Test
    void counts_fitOnOneLineInTheNarrowerWindow() {
        assertTrue(MachineMonitorLayout.chipX(4) + MachineMonitorLayout.LAMP + 2 + width("Damaged 99")
                <= MachineMonitorLayout.CLASSIC_W, "the five counts at their longest");
    }

    private static int width(final String text) {
        return Math.round(text.length() * GuiLayout.GLYPH_WIDTH * MachineMonitorLayout.SMALL);
    }
}
