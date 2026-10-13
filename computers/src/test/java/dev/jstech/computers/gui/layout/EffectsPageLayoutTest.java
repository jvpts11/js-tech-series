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

import dev.jstech.computers.gui.EffectsPages;
import dev.jstech.core.gui.layout.GuiLayout;
import org.junit.jupiter.api.Test;

class EffectsPageLayoutTest {

    /** The Settings window's page area at its smallest, and as the window opens. */
    private static final int[][] AREAS = {{135, 126}, {161, 190}};

    @Test
    void layout_everyPageIsCleanAtEverySizeWithItsWordsAtTheMostLines() {
        for (final EffectsPages.Page page : EffectsPages.ALL) {
            for (final int[] area : AREAS) {
                final GuiLayout layout = EffectsPageLayout.layout(page, area[0], area[1],
                        row -> EffectsPageLayout.MOST_LINES, page.title().english().length());
                assertTrue(layout.isClean(), page.title().key() + " at " + area[0] + "x" + area[1] + ": "
                        + layout.overlaps() + " " + layout.outOfBounds());
            }
        }
    }

    @Test
    void place_rowsFollowOneAnotherWithoutOverlapping() {
        for (final EffectsPages.Page page : EffectsPages.ALL) {
            final EffectsPageLayout.Placed placed = EffectsPageLayout.place(page, 161, 190,
                    row -> EffectsPageLayout.MOST_LINES);
            for (int i = 1; i < placed.rowY().size(); i++) {
                assertTrue(placed.rowY().get(i) >= placed.rowY().get(i - 1) + placed.rowH().get(i - 1),
                        page.title().key() + " row " + i);
            }
        }
    }

    @Test
    void place_aPageThatWasADialogKeepsItsButtonsAtTheFoot() {
        final EffectsPageLayout.Placed placed = EffectsPageLayout.place(EffectsPages.FRAMES_XP, 161, 190, row -> 1);
        assertEquals(190 - EffectsPageLayout.CONTROL_H, placed.footerY());
        assertTrue(placed.scrollTop() + placed.scrollHeight() <= placed.footerY());
    }

    @Test
    void place_aPageWithNoDialogButtonsScrollsDownToTheFoot() {
        final EffectsPageLayout.Placed placed = EffectsPageLayout.place(EffectsPages.CINNAMON, 161, 190, row -> 1);
        assertEquals(-1, placed.footerY());
        assertEquals(190, placed.scrollTop() + placed.scrollHeight());
    }

    @Test
    void rowHeight_aBoxGrowsWithTheLinesItsWordsTake() {
        final EffectsPages.IRow check = EffectsPages.FRAMES_95.rows().get(1);
        assertTrue(EffectsPageLayout.rowHeight(check, 2) > EffectsPageLayout.rowHeight(check, 1));
    }

    @Test
    void footerX_buttonsEndAtTheRightAndStandApart() {
        final int last = EffectsPageLayout.footerX(161, 2, 3);
        assertEquals(161, last + EffectsPageLayout.FOOTER_BUTTON_W);
        assertEquals(EffectsPageLayout.FOOTER_BUTTON_W + EffectsPageLayout.FOOTER_GAP,
                EffectsPageLayout.footerX(161, 1, 3) - EffectsPageLayout.footerX(161, 0, 3));
    }
}
