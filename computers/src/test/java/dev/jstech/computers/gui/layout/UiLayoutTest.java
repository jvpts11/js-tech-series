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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class UiLayoutTest {

    /** A widget that holds nothing: as wide and as tall as it says, with the weight it was given. */
    private static UiLayout.Box leaf(final long id, final long parent, final int weight, final int wide,
                                     final int tall) {
        return new UiLayout.Box(id, parent, "Label", weight, wide, tall, 0, 0, 0, UiLayout.LAID_OUT,
                UiLayout.LAID_OUT);
    }

    private static UiLayout.Box box(final long id, final long parent, final String kind, final int weight) {
        return new UiLayout.Box(id, parent, kind, weight, 0, 0, 0, 0, 0, UiLayout.LAID_OUT, UiLayout.LAID_OUT);
    }

    private static Map<Long, UiLayout.Rect> places(final List<UiLayout.Box> boxes, final int width,
                                                   final int height) {
        final Map<Long, UiLayout.Rect> out = new HashMap<>();
        for (final UiLayout.Rect rect : UiLayout.lay(boxes, 0, 0, width, height)) {
            out.put(rect.id(), rect);
        }
        return out;
    }

    @Test
    void lay_keepsTheWindowsPaddingRoundWhatItHolds() {
        final Map<Long, UiLayout.Rect> where = places(List.of(box(1, 0, "Column", 0), leaf(2, 1, 0, 30, 9)),
                200, 100);
        assertEquals(UiLayout.PAD, where.get(1L).x());
        assertEquals(200 - UiLayout.PAD * 2, where.get(1L).w());
        assertEquals(UiLayout.PAD, where.get(2L).y(), "the only widget starts at the top of the column");
    }

    @Test
    void row_givesEachWidgetTheWidthItAsksForAndTheGapBetween() {
        final Map<Long, UiLayout.Rect> where = places(
                List.of(box(1, 0, "Row", 0), leaf(2, 1, 0, 40, 9), leaf(3, 1, 0, 30, 9)), 200, 40);
        assertEquals(40, where.get(2L).w());
        assertEquals(30, where.get(3L).w());
        assertEquals(where.get(2L).x() + 40 + UiLayout.GAP, where.get(3L).x());
    }

    @Test
    void weight_sharesOutWhatIsLeftOverAndLosesNoPixel() {
        final List<UiLayout.Box> boxes = List.of(box(1, 0, "Row", 0), leaf(2, 1, 0, 40, 9), leaf(3, 1, 1, 0, 9),
                leaf(4, 1, 2, 0, 9));
        final Map<Long, UiLayout.Rect> where = places(boxes, 200, 40);
        final int room = 200 - UiLayout.PAD * 2 - UiLayout.GAP * 2 - 40;
        assertEquals(room, where.get(3L).w() + where.get(4L).w(), "the weights take exactly what is left");
        assertTrue(where.get(4L).w() > where.get(3L).w(), "a weight of two takes more than a weight of one");
    }

    @Test
    void column_stacksDownAndAWeightedWidgetTakesTheRestOfTheHeight() {
        final Map<Long, UiLayout.Rect> where = places(
                List.of(box(1, 0, "Column", 0), leaf(2, 1, 0, 30, 16), leaf(3, 1, 1, 30, 9)), 120, 100);
        assertEquals(16, where.get(2L).h());
        assertEquals(100 - UiLayout.PAD * 2 - 16 - UiLayout.GAP, where.get(3L).h());
        assertEquals(where.get(2L).y() + 16 + UiLayout.GAP, where.get(3L).y());
    }

    @Test
    void rowsAndColumns_goInsideEachOther() {
        final List<UiLayout.Box> boxes = new ArrayList<>(List.of(box(1, 0, "Column", 0), box(2, 1, "Row", 0),
                leaf(3, 2, 0, 20, 9), leaf(4, 2, 1, 0, 9), box(5, 1, "Row", 1), leaf(6, 5, 1, 0, 9)));
        final Map<Long, UiLayout.Rect> where = places(boxes, 200, 120);
        assertEquals(where.get(1L).w(), where.get(2L).w(), "a row in a column is as wide as the column");
        assertEquals(where.get(2L).x() + 20 + UiLayout.GAP, where.get(4L).x());
        assertEquals(where.get(5L).h(), where.get(6L).h(), "the widget in the weighted row fills it");
    }

    @Test
    void askedSizes_areKeptAndTheWidgetIsCentredAcross() {
        final UiLayout.Box asked = new UiLayout.Box(2, 1, "ProgressBar", 0, 60, 10, 40, 6, 0,
                UiLayout.LAID_OUT, UiLayout.LAID_OUT);
        final Map<Long, UiLayout.Rect> where = places(List.of(box(1, 0, "Row", 0), asked), 120, 40);
        assertEquals(40, where.get(2L).w());
        assertEquals(6, where.get(2L).h());
        assertEquals(where.get(1L).y() + (where.get(1L).h() - 6) / 2, where.get(2L).y());
    }

    @Test
    void placed_putsAWidgetExactlyWhereTheProgramSaid() {
        final UiLayout.Box placed = new UiLayout.Box(1, 0, "Button", 0, 40, 16, 50, 20, 0, 12, 30);
        final Map<Long, UiLayout.Rect> where = places(List.of(placed), 200, 100);
        assertEquals(12, where.get(1L).x());
        assertEquals(30, where.get(1L).y());
        assertEquals(50, where.get(1L).w());
        assertEquals(20, where.get(1L).h());
    }

    @Test
    void tabView_placesOnlyThePickedPageUnderItsStrip() {
        final UiLayout.Box tabs = new UiLayout.Box(1, 0, "TabView", 0, 0, 0, 0, 0, 0, UiLayout.LAID_OUT,
                UiLayout.LAID_OUT, 2);
        final Map<Long, UiLayout.Rect> where = places(List.of(tabs, leaf(2, 1, 0, 30, 9), leaf(3, 1, 0, 30, 9)),
                200, 100);

        assertTrue(!where.containsKey(2L), "the first page is not the picked one, so it is not placed");
        assertEquals(where.get(1L).y() + UiLayout.TAB_H + 2, where.get(3L).y());
    }

    @Test
    void groupBox_placesItsWidgetInsideItsFrameUnderItsCaption() {
        final Map<Long, UiLayout.Rect> where = places(List.of(box(1, 0, "GroupBox", 0), leaf(2, 1, 0, 30, 9)),
                200, 100);

        assertEquals(where.get(1L).x() + UiLayout.PAD, where.get(2L).x());
        assertEquals(where.get(1L).y() + UiLayout.CAPTION_H + 2, where.get(2L).y());
        assertEquals(where.get(1L).w() - UiLayout.PAD * 2, where.get(2L).w());
    }

    @Test
    void scrollView_givesItsWidgetItsHeightAndMovesItByTheScroll() {
        final UiLayout.Box view = new UiLayout.Box(1, 0, "ScrollView", 0, 60, 40, 0, 40, 0, UiLayout.LAID_OUT,
                UiLayout.LAID_OUT);
        final List<UiLayout.Box> boxes = List.of(view, leaf(2, 1, 0, 30, 120));
        final Map<Long, UiLayout.Rect> still = new HashMap<>();
        for (final UiLayout.Rect rect : UiLayout.lay(boxes, 0, 0, 200, 100, Map.of())) {
            still.put(rect.id(), rect);
        }
        final Map<Long, UiLayout.Rect> moved = new HashMap<>();
        for (final UiLayout.Rect rect : UiLayout.lay(boxes, 0, 0, 200, 100, Map.of(1L, 30))) {
            moved.put(rect.id(), rect);
        }

        assertEquals(120, still.get(2L).h(), "the widget is as tall as it asks, however short the view");
        assertEquals(still.get(2L).y() - 30, moved.get(2L).y());
        assertEquals(120 - (40 - 2), UiLayout.scrollRoom(view, boxes, 40));
    }

    @Test
    void floating_menusAndDialogsTakeNoPlace() {
        final Map<Long, UiLayout.Rect> where = places(List.of(box(1, 0, "Column", 0), leaf(2, 1, 0, 30, 9),
                box(3, 2, "ContextMenu", 0), box(4, 0, "OpenFileDialog", 0)), 200, 100);

        assertTrue(!where.containsKey(3L) && !where.containsKey(4L));
        assertEquals(2, where.size());
    }

    @Test
    void cells_layAWindowInLettersWithACellBetweenWidgets() {
        final List<UiLayout.Box> boxes = List.of(box(1, 0, "Column", 0), leaf(2, 1, 0, 10, 1),
                leaf(3, 1, 0, 8, 1));
        final Map<Long, UiLayout.Rect> where = new HashMap<>();
        for (final UiLayout.Rect rect : UiLayout.lay(boxes, 0, 0, 40, 10, Map.of(), UiLayout.CELLS)) {
            where.put(rect.id(), rect);
        }

        assertEquals(1, where.get(2L).y(), "one cell round the window");
        assertEquals(3, where.get(3L).y(), "one row between two widgets");
    }
}
