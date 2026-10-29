/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.geology;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.space.geology.StoneArrangements.Box;
import java.util.List;
import org.junit.jupiter.api.Test;

class StoneArrangementsTest {

    @Test
    void all_keepEveryStoneSmallerThanTheBlockAndInsideIt() {
        for (final List<Box> arrangement : StoneArrangements.ALL) {
            for (final Box box : arrangement) {
                assertTrue(box.x0() >= 0 && box.y0() >= 0 && box.z0() >= 0, box + " starts inside the block");
                assertTrue(box.x1() <= StoneArrangements.BLOCK && box.y1() <= StoneArrangements.BLOCK
                        && box.z1() <= StoneArrangements.BLOCK, box + " ends inside it");
                assertTrue(box.x0() < box.x1() && box.y0() < box.y1() && box.z0() < box.z1(), box + " has a size");
                assertTrue(box.y1() - box.y0() < StoneArrangements.BLOCK / 2, box + " is a stone, never a block");
            }
        }
    }

    @Test
    void all_layEveryStoneOnTheGroundOrOnAnotherStoneWithoutOverlapping() {
        for (final List<Box> arrangement : StoneArrangements.ALL) {
            for (final Box box : arrangement) {
                assertTrue(box.y0() == 0 || arrangement.stream().anyMatch(under -> under.y1() == box.y0()
                        && under.x0() <= box.x0() && box.x1() <= under.x1() && under.z0() <= box.z0()
                        && box.z1() <= under.z1()), box + " lies on the ground or on a stone under it");
                for (final Box other : arrangement) {
                    assertFalse(other != box && box.overlaps(other), box + " does not go through " + other);
                }
            }
        }
    }

    @Test
    void turned_takesTheNorthSideToTheEastWithAQuarterTurn() {
        final Box north = new Box(4, 0, 0, 12, 2, 3);
        assertEquals(new Box(13, 0, 4, 16, 2, 12), north.turned(1), "a quarter turn takes it to the east side");
        assertEquals(new Box(4, 0, 13, 12, 2, 16), north.turned(2), "half a turn to the south");
        assertEquals(new Box(0, 0, 4, 3, 2, 12), north.turned(3), "three quarters to the west");
        assertEquals(north, north.turned(4), "and a whole turn back where it was");
        assertEquals(north.turned(3), north.turned(-1), "a quarter turn back is three forward");
    }
}
