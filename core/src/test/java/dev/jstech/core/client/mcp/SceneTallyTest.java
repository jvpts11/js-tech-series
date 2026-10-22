/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class SceneTallyTest {

    /* The game's turn of a player facing south, and facing east. */
    private static final double SOUTH = 0;
    private static final double EAST = -90;

    @Test
    void bearing_namesWhereAThingLiesFromTheWayThePlayerFaces() {
        assertEquals("ahead", SceneTally.bearing(SOUTH, 0, 5));
        assertEquals("right", SceneTally.bearing(SOUTH, -5, 0));
        assertEquals("left", SceneTally.bearing(SOUTH, 5, 0));
        assertEquals("behind", SceneTally.bearing(SOUTH, 0, -5));
        assertEquals("ahead_left", SceneTally.bearing(SOUTH, 3, 3));
        assertEquals("ahead", SceneTally.bearing(EAST, 5, 0));
        assertEquals("right", SceneTally.bearing(EAST, 0, 5));
    }

    @Test
    void bearing_takesAThingAtTheEyesAsAhead() {
        assertEquals("ahead", SceneTally.bearing(SOUTH, 0, 0));
    }

    @Test
    void height_isLevelUnlessTheThingLiesSteeplyUpOrDown() {
        assertEquals("level", SceneTally.height(1, 10));
        assertEquals("above", SceneTally.height(10, 2));
        assertEquals("below", SceneTally.height(-10, 2));
    }

    @Test
    void facing_roundsTheTurnToTheNearestQuarter() {
        assertEquals("south", SceneTally.facing(10));
        assertEquals("west", SceneTally.facing(95));
        assertEquals("north", SceneTally.facing(-180));
        assertEquals("east", SceneTally.facing(-80));
        assertEquals("south", SceneTally.facing(350));
    }

    @Test
    void partOfDay_followsTheGamesDay() {
        assertEquals("morning", SceneTally.partOfDay(1_000));
        assertEquals("afternoon", SceneTally.partOfDay(6_000));
        assertEquals("evening", SceneTally.partOfDay(12_500));
        assertEquals("night", SceneTally.partOfDay(18_000));
        assertEquals("dawn", SceneTally.partOfDay(23_000));
        assertEquals("morning", SceneTally.partOfDay(24_000 * 3 + 10));
    }

    @Test
    void most_putsWhatMostOfTheViewEndedOnFirstWithItsNearest() {
        final SceneTally tally = new SceneTally();
        tally.block("minecraft:stone", 9, "left", "level");
        tally.block("minecraft:grass_block", 4, "ahead", "below");
        tally.block("minecraft:stone", 3, "right", "below");
        tally.block("minecraft:stone", 6, "ahead", "level");

        final List<SceneTally.Seen> most = tally.most(10);

        assertEquals(new SceneTally.Seen("minecraft:stone", 3, 3, "right", "below"), most.getFirst());
        assertEquals("minecraft:grass_block", most.get(1).id());
        assertEquals(1, tally.most(1).size());
        assertEquals(2, tally.kinds());
    }

    @Test
    void openShare_isHowMuchOfTheViewMetNothing() {
        final SceneTally tally = new SceneTally();
        tally.nothing();
        tally.nothing();
        tally.nothing();
        tally.block("minecraft:stone", 2, "ahead", "level");

        assertEquals(0.75, tally.openShare());
        assertEquals(0, new SceneTally().openShare());
    }
}
