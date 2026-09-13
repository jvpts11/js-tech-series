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
}
