/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.jstech.computers.gui.layout.SoundfoundryLayout.Rect;
import dev.jstech.core.network.DataLine;
import dev.jstech.core.network.DataLink;
import dev.jstech.core.tier.HardwareEra;
import java.util.List;
import org.junit.jupiter.api.Test;

class SoundfoundryShareLayoutTest {

    @Test
    void tabs_eachAsWideAsItsWordAndThePadInARow() {
        final List<Rect> tabs = SoundfoundryShareLayout.tabs(List.of("AB", "CDEF"), word -> word.length() * 6);
        assertEquals(new Rect(6, 16, 26, 13), tabs.get(0));
        assertEquals(new Rect(35, 16, 38, 13), tabs.get(1), "the next starts past the gap");
    }

    @Test
    void signalOf_lightsMoreBarsForFasterCables() {
        assertEquals(0, SoundfoundryShareLayout.signalOf(null));
        assertEquals(2, SoundfoundryShareLayout.signalOf(new DataLink(DataLine.ACCESS, HardwareEra.LEGACY)));
        assertEquals(4, SoundfoundryShareLayout.signalOf(new DataLink(DataLine.BACKBONE, HardwareEra.LEGACY)));
        assertEquals(5, SoundfoundryShareLayout.signalOf(new DataLink(DataLine.HPC, HardwareEra.STANDARD)));
        assertEquals(2, SoundfoundryShareLayout.signalOf(new DataLink(DataLine.CRAFTING, HardwareEra.VINTAGE)),
                "a cable songs go over at Ethernet's speed shows as Ethernet");
        assertEquals(2, SoundfoundryShareLayout.signalOf(new DataLink(DataLine.BACKBONE, HardwareEra.STANDARD)),
                "and so does a new cable with no song speed of its own");
    }

    @Test
    void resultAt_findsTheRowUnderAPointAndNothingOutsideTheList() {
        assertEquals(0, SoundfoundryShareLayout.resultAt(20, SoundfoundryShareLayout.ROW_TOP));
        assertEquals(2, SoundfoundryShareLayout.resultAt(20, SoundfoundryShareLayout.ROW_TOP + 2 * 11 + 3));
        assertEquals(-1, SoundfoundryShareLayout.resultAt(2, SoundfoundryShareLayout.ROW_TOP));
        assertEquals(-1, SoundfoundryShareLayout.resultAt(20, SoundfoundryShareLayout.ROW_TOP + 9 * 11));
    }

    @Test
    void listDownloadAt_findsTheDownloadUnderAPoint() {
        final int top = SoundfoundryShareLayout.listDownloadTop(1);
        assertEquals(1, SoundfoundryShareLayout.listDownloadAt(30, top + 5));
        assertEquals(-1, SoundfoundryShareLayout.listDownloadAt(30, SoundfoundryShareLayout.STATUS.y()));
    }

    @Test
    void listDownloadAt_hitsARowOnItsFirstAndLastDrawnPixelRows() {
        for (int row = 0; row < 2; row++) {
            final int fillTop = SoundfoundryShareLayout.listDownloadTop(row) - 2;
            assertEquals(row, SoundfoundryShareLayout.listDownloadAt(30, fillTop));
            assertEquals(row, SoundfoundryShareLayout.listDownloadAt(30,
                    fillTop + SoundfoundryShareLayout.DOWNLOAD_H - 2));
        }
    }
}
