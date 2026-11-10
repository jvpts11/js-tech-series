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

class UpdateManagerLayoutTest {

    /* The toolbar's words at the game font's six pixels a letter, in English and in the longest translation. */
    private static final int[] ENGLISH = {5 * 6, 10 * 6, 7 * 6, 15 * 6};
    private static final int[] PORTUGUESE = {6 * 6, 12 * 6, 9 * 6, 8 * 6};

    @Test
    void layout_fitsTheToolbarAndEveryRowInEnglish() {
        assertClean(UpdateManagerLayout.layout(ENGLISH));
    }

    @Test
    void layout_fitsTheToolbarInPortugueseToo() {
        assertClean(UpdateManagerLayout.layout(PORTUGUESE));
    }

    @Test
    void buttons_runLeftToRightEachAsWideAsItsWord() {
        final SoftwareManagerLayout.Rect[] buttons = UpdateManagerLayout.buttons(new int[] {20, 40});
        assertEquals(new SoftwareManagerLayout.Rect(5, 4, 30, 12), buttons[0]);
        assertEquals(39, buttons[1].x(), "the next starts past the gap");
    }

    @Test
    void check_sitsInsideItsRow() {
        final SoftwareManagerLayout.Rect row = UpdateManagerLayout.row(3);
        final SoftwareManagerLayout.Rect check = UpdateManagerLayout.check(3);
        assertTrue(check.y() >= row.y() && check.bottom() <= row.bottom());
        assertEquals(3, UpdateManagerLayout.rowAt(check.x() + 1, check.y() + 1));
    }

    private static void assertClean(final GuiLayout layout) {
        assertTrue(layout.isClean(), "overlaps " + layout.overlaps() + ", out of bounds " + layout.outOfBounds());
    }
}
