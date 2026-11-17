/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.ship;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class RollPathTest {

    private static final double EPSILON = 1.0e-9;
    /* The Side Door hold's way, as its rig lays it: up the flank from the deck, then the chamfer to the roof's edge. */
    private static final double DECK = 0.375;
    private static final double WALL_TOP = 2.1;
    private static final RollPath WAY = new RollPath(List.of(new RollPath.Point(1.9, DECK),
            new RollPath.Point(1.9, WALL_TOP), new RollPath.Point(1.0, 3.0)));
    private static final double WALL = WALL_TOP - DECK;
    private static final double CHAMFER = 0.9 * Math.sqrt(2);

    @Test
    void length_addsUpItsSegments() {
        assertEquals(WALL + CHAMFER, WAY.length(), EPSILON);
    }

    @Test
    void at_onTheWallStandsUpright() {
        final RollPath.Place place = WAY.at(1.0);
        assertEquals(1.9, place.x(), EPSILON);
        assertEquals(DECK + 1.0, place.y(), EPSILON);
        assertEquals(90, place.angle(), EPSILON);
    }

    @Test
    void at_pastTheCornerLeansInWithTheChamfer() {
        final RollPath.Place place = WAY.at(WALL + 0.5);
        assertEquals(1.9 - 0.5 / Math.sqrt(2), place.x(), EPSILON);
        assertEquals(WALL_TOP + 0.5 / Math.sqrt(2), place.y(), EPSILON);
        assertEquals(135, place.angle(), EPSILON);
    }

    @Test
    void shift_shutLeavesASlatWhereItRests() {
        final RollPath.Shift shift = WAY.shift(1.0, WAY.length(), 0).orElseThrow();
        assertEquals(0, shift.dx(), EPSILON);
        assertEquals(0, shift.dy(), EPSILON);
        assertEquals(0, shift.turn(), EPSILON);
    }

    @Test
    void shift_carriesASlatOffTheWallOntoTheChamferTurned() {
        final double rest = 1.0;
        final double share = (WALL + 0.2 - rest) / WAY.length();
        final RollPath.Shift shift = WAY.shift(rest, WAY.length(), share).orElseThrow();
        assertEquals(-0.2 / Math.sqrt(2), shift.dx(), EPSILON);
        assertEquals(WALL_TOP + 0.2 / Math.sqrt(2) - (DECK + rest), shift.dy(), EPSILON);
        assertEquals(45, shift.turn(), EPSILON, "leaning in with the chamfer");
    }

    @Test
    void shift_windsASlatCarriedPastTheEndIntoTheDrum() {
        assertTrue(WAY.shift(2.5, WAY.length(), 0.5).isEmpty());
        assertTrue(WAY.shift(0.05, WAY.length(), 1).isEmpty(), "open, the lowest slat is wound too");
    }

    @Test
    void path_needsTwoPointsApart() {
        assertThrows(IllegalArgumentException.class, () -> new RollPath(List.of(new RollPath.Point(0, 0))));
        assertThrows(IllegalArgumentException.class, () -> new RollPath(List.of(new RollPath.Point(1, 1),
                new RollPath.Point(1, 1))));
    }
}
