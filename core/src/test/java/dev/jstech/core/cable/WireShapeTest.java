/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.cable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WireShapeTest {

    private static final int DOWN = 1;
    private static final int UP = 1 << 1;
    private static final int NORTH = 1 << 2;
    private static final int SOUTH = 1 << 3;
    private static final int WEST = 1 << 4;
    private static final int EAST = 1 << 5;

    @Test
    void select_aPlainWireJoinsEveryFace() {
        assertEquals(NORTH | EAST | UP, WireShape.select(false, 0, NORTH | EAST | UP, 0));
    }

    @Test
    void select_aStraightWireKeepsTheAxisItAlreadyRunsAlong() {
        assertEquals(WEST | EAST, WireShape.select(true, 0, WEST | EAST | NORTH, WEST | EAST));
        assertEquals(WEST, WireShape.select(true, 0, WEST | NORTH, WEST));
    }

    @Test
    void select_aNewStraightWireTakesTheAxisWithTheMostFaces() {
        assertEquals(NORTH | SOUTH, WireShape.select(true, 0, NORTH | SOUTH | EAST, 0));
    }

    @Test
    void select_aTieGoesUpAndDownFirst() {
        assertEquals(UP, WireShape.select(true, 0, UP | NORTH | EAST, 0));
        assertEquals(NORTH, WireShape.select(true, 0, NORTH | EAST, 0));
    }

    @Test
    void select_aWireOfTwoEndsKeepsItsJoinsAndTakesNoThird() {
        assertEquals(WEST | EAST, WireShape.select(false, 2, WEST | EAST | NORTH, WEST | EAST));
        assertEquals(DOWN | NORTH, WireShape.select(false, 2, DOWN | NORTH | EAST, 0));
    }

    @Test
    void allows_aStraightWireNeverTurns() {
        assertTrue(WireShape.allows(true, 0, NORTH | SOUTH));
        assertFalse(WireShape.allows(true, 0, NORTH | EAST));
        assertTrue(WireShape.allows(true, 0, 0));
    }

    @Test
    void allows_aWireOfTwoEndsNeverBranches() {
        assertTrue(WireShape.allows(false, 2, NORTH | EAST));
        assertFalse(WireShape.allows(false, 2, NORTH | EAST | UP));
    }
}
