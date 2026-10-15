/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.probe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.core.gui.layout.GuiLayout;
import org.junit.jupiter.api.Test;

class ProbeLayoutTest {

    @Test
    void layout_keepsEveryPieceApartAndInside() {
        final GuiLayout layout = ProbeLayout.layout();

        assertTrue(layout.isClean(), () -> "overlaps " + layout.overlaps() + ", outside " + layout.outOfBounds());
    }

    @Test
    void softAt_findsEachSoftKeyAndNothingBetween() {
        for (int k = 0; k < ProbeLayout.PAGES; k++) {
            assertEquals(k, ProbeLayout.softAt(ProbeLayout.softX(k) + 2, ProbeLayout.SOFT_Y + 2));
        }
        assertEquals(-1, ProbeLayout.softAt(ProbeLayout.softX(0) + ProbeLayout.SOFT_WIDTH, ProbeLayout.SOFT_Y + 2));
        assertEquals(-1, ProbeLayout.softAt(ProbeLayout.softX(0) + 2, ProbeLayout.SOFT_Y - 1));
    }

    @Test
    void padAt_findsTheArrowsAndOk() {
        assertEquals(ProbeLayout.Pad.OK, ProbeLayout.padAt(ProbeLayout.PAD_X, ProbeLayout.PAD_Y));
        assertEquals(ProbeLayout.Pad.UP, ProbeLayout.padAt(ProbeLayout.PAD_X, ProbeLayout.PAD_Y - 18));
        assertEquals(ProbeLayout.Pad.DOWN, ProbeLayout.padAt(ProbeLayout.PAD_X, ProbeLayout.PAD_Y + 15));
        assertEquals(ProbeLayout.Pad.LEFT, ProbeLayout.padAt(ProbeLayout.PAD_X - 17, ProbeLayout.PAD_Y));
        assertEquals(ProbeLayout.Pad.RIGHT, ProbeLayout.padAt(ProbeLayout.PAD_X + 15, ProbeLayout.PAD_Y));
        assertEquals(ProbeLayout.Pad.NONE, ProbeLayout.padAt(ProbeLayout.PAD_X + 30, ProbeLayout.PAD_Y + 30));
    }

    @Test
    void keys_answerTheirClicks() {
        assertTrue(ProbeLayout.onKey(ProbeLayout.HOLD_X + 3, ProbeLayout.HOLD_Y + 3, ProbeLayout.HOLD_X,
                ProbeLayout.HOLD_Y));
        assertFalse(ProbeLayout.onKey(ProbeLayout.SHOW_X + 3, ProbeLayout.SHOW_Y + 3, ProbeLayout.HOLD_X,
                ProbeLayout.HOLD_Y));
    }

    @Test
    void membersPage_holdsSeveralRows() {
        assertTrue(ProbeLayout.rowsShown() >= 6, "rows shown: " + ProbeLayout.rowsShown());
    }
}
