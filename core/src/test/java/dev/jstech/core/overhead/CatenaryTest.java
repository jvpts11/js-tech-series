/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.overhead;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class CatenaryTest {

    private static final double CLOSE = 1e-9;
    private static final Catenary.Point A = new Catenary.Point(0, 10, 0);
    private static final Catenary.Point B = new Catenary.Point(20, 10, 0);

    @Test
    void at_endsAtItsInsulatorsAndSagsMostMidway() {
        assertPoint(A, Catenary.at(A, B, 0));
        assertPoint(B, Catenary.at(A, B, 1));
        final Catenary.Point middle = Catenary.at(A, B, 0.5);
        assertEquals(10 - 20 * Catenary.SAG, middle.y(), CLOSE, "a twenty-block wire sags a block midway");
        assertTrue(Catenary.at(A, B, 0.25).y() > middle.y(), "and less nearer its ends");
    }

    @Test
    void length_isTheStraightDistance() {
        assertEquals(20.0, Catenary.length(A, B), CLOSE);
        assertEquals(5.0, Catenary.length(new Catenary.Point(0, 0, 0), new Catenary.Point(3, 4, 0)), CLOSE);
    }

    @Test
    void points_coverTheWireInPiecesOfHalfABlock() {
        final List<Catenary.Point> points = Catenary.points(A, B);

        assertEquals(41, points.size(), "forty pieces for twenty blocks");
        assertEquals(Catenary.FEWEST_PIECES + 1, Catenary.points(A, new Catenary.Point(1, 10, 0)).size(),
                "and never fewer than four");
    }

    @Test
    void distance_isNearUnderTheSagAndFarAbove() {
        final Catenary.Point under = new Catenary.Point(10, 9, 0);
        final Catenary.Point above = new Catenary.Point(10, 12, 0);

        assertTrue(Catenary.distance(under, A, B) < 0.01, "the wire sags down to a block below its ends");
        assertTrue(Catenary.distance(above, A, B) > 2.9);
    }

    private static void assertPoint(final Catenary.Point expected, final Catenary.Point actual) {
        assertEquals(expected.x(), actual.x(), CLOSE);
        assertEquals(expected.y(), actual.y(), CLOSE);
        assertEquals(expected.z(), actual.z(), CLOSE);
    }
}
