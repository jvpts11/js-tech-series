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
    void put_takesAnItemAGapBehindAnotherWhereTheGapIsAThird() {
        // A third is no exact number: an item stacked a gap behind another stands a hair less than a gap from it.
        final BeltLane<String> lane = new BeltLane<>(1.0 / 3);
        lane.put("first", 1.0);
        lane.put("second", 0.0);
        lane.advance(1.0, 1.0, NONE_HELD);
        final double behind = lane.entries().get(1).at();
        final BeltLane<String> read = new BeltLane<>(1.0 / 3);
        assertTrue(read.put("first", 1.0) && read.put("second", behind),
                "both come back on as they stood, as a lane read from the save puts them: " + behind);
    }

    @Test
    void put_takesBackALanePackedTightAtAnyNumberOfItemsABlock() {
        for (final int perBlock : new int[] {3, 5, 6, 7}) {
            final double gap = 1.0 / perBlock;
            final BeltLane<Integer> lane = new BeltLane<>(gap);
            // Each comes on at the start and runs up to the one before it, a gap behind it, as advance packs them.
            for (int i = 0; i < perBlock; i++) {
                assertTrue(lane.put(i, 0.0), "item " + i + " comes on at " + perBlock + " a block");
                lane.advance(1.0, 1.0, NONE_HELD);
            }
            final BeltLane<Integer> read = new BeltLane<>(gap);
            for (final BeltLane.Entry<Integer> entry : lane.entries()) {
                assertTrue(read.put(entry.item(), entry.at()), "at " + perBlock + " a block, item " + entry.item()
                        + " comes back on where it stood, " + entry.at());
            }
            assertEquals(lane.size(), read.size(), perBlock + " a block");
        }
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
