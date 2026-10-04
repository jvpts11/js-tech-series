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

class WorkshopLayoutTest {

    /** The tallest content a desktop window on a standard monitor opens with, as the other programs keep under. */
    private static final int MOST_CONTENT_H = 230;

    @Test
    void layout_isCleanAtTheDefaultAndTheNarrowestWidth() {
        for (final int width : new int[] {WorkshopLayout.MIN_W, WorkshopLayout.DEFAULT_W}) {
            final GuiLayout l = WorkshopLayout.layout(width, WorkshopLayout.minContentHeight());
            assertTrue(l.isClean(), width + ": " + l.overlaps() + " " + l.outOfBounds());
        }
    }

    @Test
    void stations_areCleanAtTheNarrowestWidth() {
        final int station = WorkshopLayout.stationWidth(WorkshopLayout.MIN_W);
        for (final GuiLayout l : new GuiLayout[] {WorkshopLayout.crafting(station), WorkshopLayout.furnace(station),
                WorkshopLayout.enchanting(station), WorkshopLayout.anvil(station)}) {
            assertTrue(l.isClean(), l.overlaps() + " " + l.outOfBounds());
        }
    }

    @Test
    void minContentHeight_fitsTheWindowAStandardMonitorOpens() {
        assertTrue(WorkshopLayout.minContentHeight() <= MOST_CONTENT_H,
                "the window needs " + WorkshopLayout.minContentHeight());
    }

    @Test
    void band_sitsUnderTheStationAndOverTheStatusBar() {
        final int h = WorkshopLayout.minContentHeight();
        assertTrue(WorkshopLayout.bandTop(h) >= WorkshopLayout.STATION_TOP + WorkshopLayout.STATION_H
                + WorkshopLayout.LABEL_H);
        assertEquals(h - WorkshopLayout.STATUS_H - WorkshopLayout.PAD,
                WorkshopLayout.bandTop(h) + WorkshopLayout.BAND_H);
    }

    @Test
    void rowYOffset_leavesTheHotbarAGap() {
        assertEquals(36, WorkshopLayout.rowYOffset(2));
        assertEquals(58, WorkshopLayout.rowYOffset(3));
    }
}
