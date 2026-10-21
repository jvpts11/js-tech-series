/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ManualScreenLayoutTest {

    @Test
    void of_givesTheBinderOfTheApprovedSizeForTwoPages() {
        final ManualScreenLayout.Geometry binder = ManualScreenLayout.of(166, 201, true);

        assertEquals(364, binder.width());
        assertEquals(218, binder.height());
        assertEquals(181, binder.right().x());
    }

    @Test
    void bookmarks_standBesideTheSearchTheirListInsideTheBinder() {
        final ManualScreenLayout.Geometry binder = ManualScreenLayout.of(166, 201, true);
        final ManualScreenLayout.Rect button = ManualScreenLayout.bookmarks(binder);
        final ManualScreenLayout.Rect list = ManualScreenLayout.bookmarkList(binder, 8);

        assertEquals(binder.search().x() - ManualScreenLayout.BUTTON_GAP, button.x() + button.width());
        assertTrue(list.x() >= 0 && list.x() + list.width() <= binder.coverRight(), "the list stays on the binder");
        assertTrue(list.y() + list.height() <= binder.height(), "eight bookmarks fit down the binder");
        final ManualScreenLayout.Rect add = ManualScreenLayout.bookmarkRow(list, 8, 8);
        assertTrue(add.y() >= ManualScreenLayout.bookmarkRow(list, 7, 8).y() + ManualScreenLayout.LIST_ROW
                && add.y() + add.height() <= list.y() + list.height(), "the row that marks a page closes the list");
    }

    @Test
    void ribbons_hangApartAndClearOfTheButtons() {
        final ManualScreenLayout.Geometry binder = ManualScreenLayout.of(166, 201, true);
        final GuiLayout layout = new GuiLayout(binder.width(), binder.height());
        for (final ManualScreenLayout.Rect button : new ManualScreenLayout.Rect[] {binder.contents(), binder.back(),
                binder.forward(), binder.search(), ManualScreenLayout.bookmarks(binder)}) {
            layout.box("button " + button.x(), button.x(), button.y(), button.width(), button.height());
        }
        for (int slot = 0; slot < 4; slot++) {
            for (final boolean right : new boolean[] {false, true}) {
                final ManualScreenLayout.Rect ribbon = ManualScreenLayout.ribbon(binder, right, slot);
                layout.box("ribbon " + right + " " + slot, ribbon.x(), ribbon.y(), ribbon.width(), ribbon.height());
            }
        }
        assertTrue(layout.isClean(), () -> layout.overlaps() + " " + layout.outOfBounds());
    }

    @Test
    void note_staysInsideItsPageAboveItsFoot() {
        final ManualScreenLayout.Rect page = ManualScreenLayout.of(166, 201, true).right();
        final ManualScreenLayout.Rect note = ManualScreenLayout.note(page, 4);

        assertTrue(note.x() >= page.x() && note.x() + note.width() <= page.x() + page.width());
        assertEquals(page.y() + page.height() - ManualScreenLayout.NOTE_FOOT, note.y() + note.height());
        assertEquals(2 * ManualScreenLayout.NOTE_PAD + 4 * ManualScreenLayout.NOTE_LINE, note.height());
    }

    @Test
    void layout_isCleanForTwoPagesAndForOne() {
        for (final boolean spread : new boolean[] {true, false}) {
            final GuiLayout layout = ManualScreenLayout.layout(ManualScreenLayout.of(166, 201, spread), 10);

            assertTrue(layout.isClean(), () -> "spread " + spread + ": " + layout.overlaps() + " "
                    + layout.outOfBounds());
        }
    }

    @Test
    void thumb_printsABlockForEachChapterDownTheRightPagesEdgeTheOpenOneWidest() {
        final ManualScreenLayout.Geometry binder = ManualScreenLayout.of(166, 201, true);
        final ManualScreenLayout.Rect page = binder.right();

        final ManualScreenLayout.Rect first = ManualScreenLayout.thumb(binder, 0, 6, true);
        final ManualScreenLayout.Rect second = ManualScreenLayout.thumb(binder, 1, 6, false);
        assertEquals(page.x() + page.width(), first.x() + first.width(), "flush with the page's edge");
        assertEquals(page.x() + page.width(), second.x() + second.width());
        assertTrue(first.width() > second.width(), "the open chapter's block is the widest");
        assertTrue(second.y() >= first.y() + first.height(), "each under the one before");
        assertTrue(ManualScreenLayout.thumb(binder, 12, 13, false).y() + ManualScreenLayout.thumb(binder, 12, 13,
                false).height() <= page.y() + page.height(), "thirteen fit on the page");
    }

    @Test
    void flag_standsOutPastTheCoverToTheEdgeTheTabsReach() {
        final ManualScreenLayout.Geometry binder = ManualScreenLayout.of(166, 201, true);

        final ManualScreenLayout.Rect open = ManualScreenLayout.flag(binder, 0, true);
        final ManualScreenLayout.Rect shut = ManualScreenLayout.flag(binder, 1, false);
        assertTrue(open.x() < binder.right().x() + binder.right().width(), "a flag is stuck under the page's edge");
        assertEquals(binder.width(), open.x() + open.width(), "and stands out as far as a tab");
        assertTrue(open.x() < shut.x(), "the open chapter's reaches further onto the page");
        assertTrue(shut.y() >= open.y() + open.height(), "each under the one before");
    }

    @Test
    void of_fitsThirteenTabsAboveTheArrow() {
        final ManualScreenLayout.Geometry binder = ManualScreenLayout.of(166, 201, true);

        final ManualScreenLayout.Rect last = ManualScreenLayout.tab(binder, binder.tabs() - 1);
        assertTrue(last.y() + last.height() <= binder.next().y(), () -> "the last tab ends at "
                + (last.y() + last.height()));
        assertEquals(13, binder.tabs(), "a tab for each chapter of the Guide to Operations");
    }

    @Test
    void of_putsContentsBackAndForwardAtTheLeftOfTheTopEdgeAndSearchAtItsRight() {
        final ManualScreenLayout.Geometry binder = ManualScreenLayout.of(166, 201, true);

        assertEquals(9, binder.contents().x());
        assertEquals(25, binder.back().x());
        assertEquals(41, binder.forward().x());
        assertEquals(331, binder.search().x());
        assertTrue(binder.contents().y() + binder.contents().height() <= binder.left().y(),
                "the buttons stand on the cover above the pages");
    }

    @Test
    void of_fitsTheBinderOnAFullHdScreenAtTheLargestGuiScale() {
        final ManualScreenLayout.Geometry binder = ManualScreenLayout.of(166, 201, true);

        assertTrue(binder.width() <= 480 && binder.height() <= 270, () -> binder.width() + " by " + binder.height());
    }
}
