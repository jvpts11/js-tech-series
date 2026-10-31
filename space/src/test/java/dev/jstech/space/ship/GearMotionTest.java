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

class GearMotionTest {

    private static final double CLOSE = 1e-9;

    @Test
    void standing_startsAllTheWayDownOrAllTheWayUp() {
        final GearMotion down = GearMotion.standing(true);
        assertEquals(0, down.folded(), CLOSE);
        assertEquals(0, down.shut(), CLOSE);
        assertFalse(down.stowed());
        final GearMotion up = GearMotion.standing(false);
        assertEquals(1, up.folded(), CLOSE);
        assertEquals(1, up.shut(), CLOSE);
        assertTrue(up.stowed());
    }

    @Test
    void advance_foldsTheStrutsFirstAndShutsTheDoorsAfter() {
        final GearMotion gear = GearMotion.standing(true);
        gear.advance(GearMotion.CYCLE * 0.4, false);
        assertTrue(gear.folded() > 0 && gear.folded() < 1, "halfway the struts are folding: " + gear.folded());
        assertEquals(0, gear.shut(), CLOSE);
        gear.advance(GearMotion.CYCLE * 0.4, false);
        assertEquals(1, gear.folded(), CLOSE);
        assertEquals(0, gear.shut(), CLOSE);
        gear.advance(GearMotion.CYCLE * 0.1, false);
        assertEquals(0.5, gear.shut(), CLOSE);
        assertFalse(gear.stowed());
        gear.advance(GearMotion.CYCLE, false);
        assertTrue(gear.stowed());
        assertEquals(1, gear.up(), CLOSE);
    }

    @Test
    void advance_comingDownOpensTheDoorsFirstAndUnfoldsTheStrutsAfter() {
        final GearMotion gear = GearMotion.standing(false);
        gear.advance(GearMotion.CYCLE * 0.1, true);
        assertEquals(0.5, gear.shut(), CLOSE);
        assertEquals(1, gear.folded(), CLOSE);
        gear.advance(GearMotion.CYCLE * 0.1, true);
        assertEquals(0, gear.shut(), CLOSE);
        assertEquals(1, gear.folded(), CLOSE);
        gear.advance(GearMotion.CYCLE, true);
        assertEquals(0, gear.folded(), CLOSE);
        assertEquals(0, gear.up(), CLOSE);
    }

    @Test
    void advance_turnsBackHalfwayWhenTheGearIsCalledTheOtherWay() {
        final GearMotion gear = GearMotion.standing(true);
        gear.advance(GearMotion.CYCLE * 0.3, false);
        final double halfway = gear.up();
        gear.advance(GearMotion.CYCLE * 0.1, true);
        assertEquals(halfway - 0.1, gear.up(), CLOSE);
        gear.advance(-1, false);
        assertEquals(halfway - 0.1, gear.up(), CLOSE);
    }

    @Test
    void folded_easesInAndOut() {
        final GearMotion gear = GearMotion.standing(true);
        gear.advance(GearMotion.CYCLE * 0.04, false);
        final double start = gear.folded();
        gear.advance(GearMotion.CYCLE * 0.36, false);
        final double middle = gear.folded();
        assertTrue(start < 0.05 / 0.8 * 0.5, "it starts slowly: " + start);
        assertEquals(0.5, middle, CLOSE);
    }

    @Test
    void turned_isHowFarAWheelOfItsRadiusHasRolledWithinAWholeTurn() {
        final GearMotion gear = GearMotion.standing(true);
        gear.roll(Math.PI * 0.5);
        assertEquals(Math.PI, gear.turned(0.5), CLOSE);
        assertEquals(Math.PI * 0.5, gear.turned(1), CLOSE);
        gear.roll(Math.PI * 0.75);
        assertEquals(Math.PI * 0.5, gear.turned(0.5), CLOSE);
        assertEquals(0, gear.turned(0), CLOSE);
    }
}
