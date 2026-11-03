/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.ship;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DoorMotionTest {

    private static final double EPSILON = 1.0e-9;
    /* A quarter of the stair's time: one frame of the storyboard to the next. */
    private static final double FRAME = DoorMotion.STAIR / 4;

    @Test
    void advance_letsTheStairDownThroughTheStoryboardsFrames() {
        final DoorMotion door = DoorMotion.standing(false, false);
        door.advance(FRAME, true, true, DoorMotion.OUTER_DOOR);
        assertEquals(70, door.bay(), EPSILON, "the bay's doors opening as the leaf slides");
        assertEquals(0.5, door.leaf(), EPSILON);
        assertEquals(0, door.slid(), EPSILON);
        door.advance(FRAME, true, true, DoorMotion.OUTER_DOOR);
        assertEquals(90, door.bay(), EPSILON);
        assertEquals(1, door.slid(), EPSILON, "the folded stair out under the sill");
        assertEquals(1, door.leaf(), EPSILON);
        assertEquals(1, door.fold(), EPSILON);
        door.advance(FRAME, true, true, DoorMotion.OUTER_DOOR);
        assertEquals(-90, door.firstFlight(), EPSILON, "the first flight swung down, the others unfolding");
        assertEquals(0.5, door.fold(), EPSILON);
        assertEquals(0, door.rails(), EPSILON);
        door.advance(FRAME, true, true, DoorMotion.OUTER_DOOR);
        assertEquals(-125, door.firstFlight(), EPSILON, "down at its slope to the land");
        assertEquals(0, door.fold(), EPSILON);
        assertEquals(1, door.rails(), EPSILON, "its handrails standing last");
    }

    @Test
    void advance_shutsTheBayBeforeTheLeaf() {
        final DoorMotion door = DoorMotion.standing(true, true);
        door.advance(DoorMotion.STAIR - FRAME, false, false, DoorMotion.OUTER_DOOR);
        assertEquals(1, door.leaf(), EPSILON, "the leaf open while the stair folds back into its bay");
        assertEquals(70, door.bay(), EPSILON);
        door.advance(FRAME, false, false, DoorMotion.OUTER_DOOR);
        assertTrue(door.stowed());
        assertEquals(1, door.leaf(), EPSILON, "the bay shut, the leaf not yet moved");
        door.advance(DoorMotion.OUTER_DOOR / 2, false, false, DoorMotion.OUTER_DOOR);
        assertEquals(0.5, door.leaf(), EPSILON, "then the leaf slides shut last");
    }

    @Test
    void advance_opensWithNoStairOffTheLand() {
        final DoorMotion door = DoorMotion.standing(false, false);
        door.advance(DoorMotion.OUTER_DOOR / 2, true, false, DoorMotion.OUTER_DOOR);
        assertEquals(0.5, door.leaf(), EPSILON);
        assertTrue(door.stowed(), "in the air the bay stays shut");
    }

    @Test
    void advance_takesTheStairUpAsTheShipLiftsOffWithItsDoorOpen() {
        final DoorMotion door = DoorMotion.standing(true, true);
        door.advance(FRAME, true, false, DoorMotion.OUTER_DOOR);
        assertEquals(DoorMotion.STAIR - FRAME, door.stair(), EPSILON);
        assertEquals(1, door.leaf(), EPSILON, "the door stays open");
        assertFalse(door.stowed());
    }

    @Test
    void advance_slidesAPressureDoorInHalfASecond() {
        final DoorMotion door = DoorMotion.standing(false, false);
        door.advance(DoorMotion.PRESSURE_DOOR / 2, true, false, DoorMotion.PRESSURE_DOOR);
        assertEquals(0.5, door.leaf(), EPSILON);
        door.advance(DoorMotion.PRESSURE_DOOR, true, false, DoorMotion.PRESSURE_DOOR);
        assertEquals(1, door.leaf(), EPSILON);
        door.advance(DoorMotion.PRESSURE_DOOR, false, false, DoorMotion.PRESSURE_DOOR);
        assertEquals(0, door.leaf(), EPSILON);
    }
}
