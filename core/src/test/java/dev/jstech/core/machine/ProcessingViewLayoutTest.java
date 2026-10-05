/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProcessingViewLayoutTest {

    @Test
    void oneInOneOut_sitsOnOneRowWithTheArrowBetween() {
        final ProcessingViewLayout layout = new ProcessingViewLayout(1, 1);
        assertEquals(new ProcessingViewLayout.Point(0, 0), layout.input(0));
        assertEquals(new ProcessingViewLayout.Point(22, 0), layout.arrow());
        assertEquals(new ProcessingViewLayout.Point(50, 0), layout.output(0));
        assertEquals(28, layout.height());
        assertEquals(104, layout.width());
    }

    @Test
    void manyInputs_wrapInRowsOfThree() {
        final ProcessingViewLayout layout = new ProcessingViewLayout(4, 1);
        assertEquals(new ProcessingViewLayout.Point(0, 0), layout.input(0));
        assertEquals(new ProcessingViewLayout.Point(36, 0), layout.input(2));
        assertEquals(new ProcessingViewLayout.Point(0, 18), layout.input(3));
        assertEquals(36 + 10, layout.height());
    }

    @Test
    void theShorterSide_isCentredDownTheTaller() {
        final ProcessingViewLayout layout = new ProcessingViewLayout(6, 1);
        assertEquals(9, layout.output(0).y());
        assertEquals((36 - ProcessingViewLayout.ARROW_HEIGHT) / 2, layout.arrow().y());
    }

    @Test
    void slots_neverOverlapOneAnotherOrTheArrow() {
        for (int in = 1; in <= 9; in++) {
            for (int out = 1; out <= 6; out++) {
                final ProcessingViewLayout layout = new ProcessingViewLayout(in, out);
                final List<int[]> boxes = new ArrayList<>();
                for (int i = 0; i < in; i++) {
                    boxes.add(box(layout.input(i), ProcessingViewLayout.SLOT, ProcessingViewLayout.SLOT));
                }
                for (int i = 0; i < out; i++) {
                    boxes.add(box(layout.output(i), ProcessingViewLayout.SLOT, ProcessingViewLayout.SLOT));
                }
                boxes.add(box(layout.arrow(), ProcessingViewLayout.ARROW_WIDTH, ProcessingViewLayout.ARROW_HEIGHT));
                for (int a = 0; a < boxes.size(); a++) {
                    final int[] box = boxes.get(a);
                    assertTrue(box[0] >= 0 && box[1] >= 0 && box[2] <= layout.width()
                            && box[3] <= layout.textLine().y(), "inside the recipe, above the text: " + in + "/" + out);
                    for (int b = a + 1; b < boxes.size(); b++) {
                        assertTrue(!overlap(box, boxes.get(b)), "no overlap at " + in + " in, " + out + " out");
                    }
                }
            }
        }
    }

    @Test
    void noInputs_stillKeepsItsPlace() {
        final ProcessingViewLayout layout = new ProcessingViewLayout(0, 1);
        assertEquals(22, layout.arrow().x());
        assertThrows(IndexOutOfBoundsException.class, () -> layout.input(0));
    }

    @Test
    void seconds_sayTheTimeInAsFewDigitsAsItNeeds() {
        assertEquals("10", ProcessingViewLayout.seconds(200));
        assertEquals("6", ProcessingViewLayout.seconds(120));
        assertEquals("0.5", ProcessingViewLayout.seconds(10));
        assertEquals("0.05", ProcessingViewLayout.seconds(1));
        assertEquals("1.35", ProcessingViewLayout.seconds(27));
    }

    @Test
    void constructor_refusesNegativeCounts() {
        assertThrows(IllegalArgumentException.class, () -> new ProcessingViewLayout(-1, 1));
    }

    private static int[] box(final ProcessingViewLayout.Point at, final int width, final int height) {
        return new int[]{at.x(), at.y(), at.x() + width, at.y() + height};
    }

    private static boolean overlap(final int[] a, final int[] b) {
        return a[0] < b[2] && b[0] < a[2] && a[1] < b[3] && b[1] < a[3];
    }
}
