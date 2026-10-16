/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.conveyor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BeltLaneTest {

    private static final double QUARTER = 0.25;
    private static final double EPSILON = 1.0e-9;
    private static final int NONE_HELD = -1;

    @Test
    void advance_movesItemsAlongAndStopsTheFirstAtTheEnd() {
        final BeltLane<String> lane = new BeltLane<>(QUARTER);
        lane.put("a", 0.5);
        assertFalse(lane.advance(0.3, 1.0, NONE_HELD), "not at the end yet");
        assertEquals(0.8, lane.entries().getFirst().at(), EPSILON);
        assertTrue(lane.advance(0.3, 1.0, NONE_HELD), "at the end, ready to be handed on");
        assertEquals(1.0, lane.entries().getFirst().at(), EPSILON, "and no further");
    }

    @Test
    void advance_keepsTheGapBehindAStoppedItem() {
        final BeltLane<String> lane = new BeltLane<>(QUARTER);
        lane.put("first", 1.0);
        lane.put("second", 0.5);
        lane.put("third", 0.0);
        lane.advance(1.0, 1.0, NONE_HELD);
        assertEquals(0.75, lane.entries().get(1).at(), EPSILON, "the second waits a gap behind the first");
        assertEquals(0.5, lane.entries().get(2).at(), EPSILON, "and the third a gap behind it");
        assertEquals(0.5, lane.lastAt(), EPSILON);
    }

    @Test
    void put_refusesAnItemWithinTheGapAndKeepsTheOrder() {
        final BeltLane<String> lane = new BeltLane<>(QUARTER);
        assertTrue(lane.put("b", 0.4));
        assertFalse(lane.roomAt(0.5), "too near");
        assertFalse(lane.put("c", 0.5));
        assertTrue(lane.put("a", 0.9), "ahead of it");
        assertEquals("a", lane.entries().getFirst().item(), "the first along the belt first");
        assertEquals("a", lane.takeFirst());
        assertEquals(1, lane.size());
    }

    @Test
    void advance_stopsAHeldItemAndThoseBehindIt() {
        final BeltLane<String> lane = new BeltLane<>(QUARTER);
        lane.put("worked", 0.5);
        lane.put("behind", 0.0);
        lane.advance(1.0, 1.0, 0);
        assertEquals(0.5, lane.entries().getFirst().at(), EPSILON, "the item a machine works stays");
        assertEquals(0.25, lane.entries().get(1).at(), EPSILON, "and the next waits behind it");
        lane.replace(0, "pressed");
        assertEquals("pressed", lane.entries().getFirst().item(), "what the machine made stands where it was");
    }

    @Test
    void advance_bringsAHeldItemUpToWhereItIsStoppedAndNoFurther() {
        final BeltLane<String> lane = new BeltLane<>(QUARTER);
        lane.put("ahead", 0.6);
        lane.put("worked", 0.3);
        lane.advance(0.1, 1.0, 1, 0.5);
        assertEquals(0.4, lane.entries().get(1).at(), EPSILON, "short of the middle, it moves as the belt does");
        lane.advance(0.3, 1.0, 1, 0.5);
        assertEquals(0.5, lane.entries().get(1).at(), EPSILON, "stopped at the middle");
        assertEquals(1.0, lane.entries().getFirst().at(), EPSILON, "while the one ahead goes on");
        lane.advance(0.3, 1.0, 1, 0.5);
        assertEquals(0.5, lane.entries().get(1).at(), EPSILON, "and stays there");
    }

    @Test
    void lastAt_isTheEndOfAnEmptyLane() {
        assertEquals(1.0, new BeltLane<String>(QUARTER).lastAt(), EPSILON);
    }
}
