/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.persistor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

class PersistorShapesTest {

    @Test
    void pack_keepsNegativeOffsetsApart() {
        for (final int[] offset : new int[][] {{0, 0}, {-1, 0}, {0, -1}, {-99, 99}, {99, -99}}) {
            final long packed = PersistorShapes.pack(offset[0], offset[1]);
            assertEquals(offset[0], PersistorShapes.dx(packed));
            assertEquals(offset[1], PersistorShapes.dz(packed));
        }
        assertFalse(PersistorShapes.pack(-1, 0) == PersistorShapes.pack(0, -1), "west is not north");
    }

    @Test
    void base_squareCoversItsWholeSide() {
        assertEquals(1, PersistorShapes.base(PersistorShape.SQUARE, 0, true).size(), "radius none is its own chunk");
        assertEquals(9, PersistorShapes.base(PersistorShape.SQUARE, 1, true).size());
        assertEquals(25, PersistorShapes.base(PersistorShape.SQUARE, 2, true).size());
    }

    @Test
    void base_circleIsAPlusAtOneAndRoundsOutAfter() {
        final Set<Long> one = PersistorShapes.base(PersistorShape.CIRCLE, 1, true);
        assertEquals(5, one.size(), "a radius of one is the plus round the middle");
        assertFalse(one.contains(PersistorShapes.pack(1, 1)), "without its corners");
        final Set<Long> two = PersistorShapes.base(PersistorShape.CIRCLE, 2, true);
        assertTrue(two.contains(PersistorShapes.pack(1, 1)) && two.contains(PersistorShapes.pack(2, 1)),
                "two takes in the corners beside it");
        assertFalse(two.contains(PersistorShapes.pack(2, 2)), "but not the far ones");
        assertEquals(21, two.size());
    }

    @Test
    void base_lineRunsAcrossTheWayItFaces() {
        final Set<Long> alongX = PersistorShapes.base(PersistorShape.LINE, 2, true);
        assertEquals(5, alongX.size());
        assertTrue(alongX.contains(PersistorShapes.pack(-2, 0)) && alongX.contains(PersistorShapes.pack(2, 0)));
        final Set<Long> alongZ = PersistorShapes.base(PersistorShape.LINE, 2, false);
        assertTrue(alongZ.contains(PersistorShapes.pack(0, -2)) && !alongZ.contains(PersistorShapes.pack(1, 0)));
    }

    @Test
    void base_freeIsItsOwnChunkAlone() {
        assertEquals(Set.of(PersistorShapes.pack(0, 0)), PersistorShapes.base(PersistorShape.FREE, 5, true));
    }

    @Test
    void click_addsOutsideTakesOffInsideAndUndoesOnASecondClick() {
        final PersistorShapes.Clicks added = PersistorShapes.click(PersistorShape.SQUARE, 1, true, Set.of(),
                Set.of(), 3, 0);
        assertEquals(Set.of(PersistorShapes.pack(3, 0)), added.added(), "a chunk outside the square is added");
        assertTrue(PersistorShapes.area(PersistorShape.SQUARE, 1, true, added.added(), added.removed())
                .contains(PersistorShapes.pack(3, 0)));
        final PersistorShapes.Clicks undone = PersistorShapes.click(PersistorShape.SQUARE, 1, true, added.added(),
                added.removed(), 3, 0);
        assertTrue(undone.added().isEmpty() && undone.removed().isEmpty(), "clicking it again takes it off");
        final PersistorShapes.Clicks removed = PersistorShapes.click(PersistorShape.SQUARE, 1, true, Set.of(),
                Set.of(), 1, 1);
        assertEquals(Set.of(PersistorShapes.pack(1, 1)), removed.removed(), "a corner of the square is taken off");
        assertEquals(8, PersistorShapes.area(PersistorShape.SQUARE, 1, true, removed.added(), removed.removed())
                .size());
    }

    @Test
    void click_neverTakesItsOwnChunkOff() {
        final PersistorShapes.Clicks clicks = PersistorShapes.click(PersistorShape.SQUARE, 1, true, Set.of(),
                Set.of(), 0, 0);
        assertTrue(clicks.added().isEmpty() && clicks.removed().isEmpty());
        assertTrue(PersistorShapes.area(PersistorShape.FREE, 0, true, Set.of(), Set.of(PersistorShapes.pack(0, 0)))
                .contains(PersistorShapes.pack(0, 0)), "even an area that says to leave it");
    }
}
