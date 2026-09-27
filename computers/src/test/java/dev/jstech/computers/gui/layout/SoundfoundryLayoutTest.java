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

import org.junit.jupiter.api.Test;

class SoundfoundryLayoutTest {

    @Test
    void layout_hasNothingOverlappingOrOutside() {
        assertTrue(SoundfoundryLayout.layout().isClean(), SoundfoundryLayout.layout().overlaps()
                + " " + SoundfoundryLayout.layout().outOfBounds());
    }

    @Test
    void heights_foldAndPutAwayThePlates() {
        assertEquals(SoundfoundryLayout.PLAYER_H, SoundfoundryLayout.playerHeight(false));
        assertEquals(SoundfoundryLayout.SHADE_H, SoundfoundryLayout.playerHeight(true));
        assertEquals(0, SoundfoundryLayout.playlistHeight(false, false));
        assertEquals(SoundfoundryLayout.SHADE_H, SoundfoundryLayout.playlistHeight(true, true));
        assertEquals(SoundfoundryLayout.PLAYLIST_H, SoundfoundryLayout.playlistHeight(true, false));
    }

    @Test
    void rowAt_findsTheRowsAndNothingElse() {
        assertEquals(0, SoundfoundryLayout.rowAt(20, SoundfoundryLayout.ROW_TOP));
        assertEquals(9, SoundfoundryLayout.rowAt(20, SoundfoundryLayout.ROW_TOP + 9 * SoundfoundryLayout.ROW_H + 5));
        assertEquals(-1, SoundfoundryLayout.rowAt(20, SoundfoundryLayout.ROW_TOP + 10 * SoundfoundryLayout.ROW_H));
        assertEquals(-1, SoundfoundryLayout.rowAt(SoundfoundryLayout.SCROLLBAR.x() + 1, 30), "the scrollbar is no row");
    }

    @Test
    void valueAt_readsBackWhereTheThumbStands() {
        final SoundfoundryLayout.Rect slider = SoundfoundryLayout.VOLUME;
        for (final double value : new double[] {0.0, 0.25, 0.78, 1.0}) {
            final int at = slider.x() + SoundfoundryLayout.thumbAt(slider, SoundfoundryLayout.THUMB, value)
                    + SoundfoundryLayout.THUMB / 2;
            assertEquals(value, SoundfoundryLayout.valueAt(slider, SoundfoundryLayout.THUMB, at), 0.02);
        }
        assertEquals(0.0, SoundfoundryLayout.valueAt(slider, SoundfoundryLayout.THUMB, -50));
        assertEquals(1.0, SoundfoundryLayout.valueAt(slider, SoundfoundryLayout.THUMB, 5000));
    }
}
