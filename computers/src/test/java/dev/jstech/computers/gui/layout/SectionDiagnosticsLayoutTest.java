/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SectionDiagnosticsLayoutTest {

    @Test
    void layout_defaultPaneWithFourAlerts_isClean() {
        assertTrue(SectionDiagnosticsLayout.layout(SectionDiagnosticsLayout.DEFAULT_W,
                SectionDiagnosticsLayout.DEFAULT_H, 4).isClean());
    }

    @Test
    void layout_smallestPane_isClean() {
        assertTrue(SectionDiagnosticsLayout.layout(SectionDiagnosticsLayout.MIN_W, SectionDiagnosticsLayout.MIN_H, 0)
                .isClean());
        assertTrue(SectionDiagnosticsLayout.layout(SectionDiagnosticsLayout.MIN_W, SectionDiagnosticsLayout.MIN_H, 9)
                .isClean());
    }

    @Test
    void place_defaultPane_showsFourAlertsAndBothNotes() {
        final SectionDiagnosticsLayout.Placed placed = SectionDiagnosticsLayout.place(
                SectionDiagnosticsLayout.DEFAULT_W, SectionDiagnosticsLayout.DEFAULT_H, 4);
        assertEquals(4, placed.rows());
        assertEquals(SectionDiagnosticsLayout.NOTES, placed.notes());
        assertTrue(placed.chartH() >= SectionDiagnosticsLayout.MIN_CHART_H);
    }

    @Test
    void place_manyAlerts_showsWhatFitsAndKeepsTheChart() {
        final SectionDiagnosticsLayout.Placed placed = SectionDiagnosticsLayout.place(
                SectionDiagnosticsLayout.DEFAULT_W, SectionDiagnosticsLayout.DEFAULT_H, 40);
        assertTrue(placed.rows() < 40);
        assertTrue(placed.chartH() >= SectionDiagnosticsLayout.MIN_CHART_H);
        assertTrue(placed.rowsY() + placed.rows() * SectionDiagnosticsLayout.ROW_H <= placed.notesY());
    }

    @Test
    void place_smallestPane_dropsTheNotesFirst() {
        final SectionDiagnosticsLayout.Placed placed = SectionDiagnosticsLayout.place(
                SectionDiagnosticsLayout.MIN_W, SectionDiagnosticsLayout.MIN_H, 3);
        assertEquals(0, placed.notes());
        assertTrue(placed.chartH() >= SectionDiagnosticsLayout.MIN_CHART_H);
    }
}
