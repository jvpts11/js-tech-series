/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.physics;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PhysicsMathTest {

    private static final double CLOSE = 1e-9;
    private static final double FALL = 0.08;

    @Test
    void scaled_undoesThePartOfTheFallTheLawTakesAway() {
        assertEquals(FALL, PhysicsMath.scaled(0, FALL, FALL), CLOSE, "weightless: the whole fall undone");
        assertEquals(FALL / 2, PhysicsMath.scaled(0.5, FALL, FALL), CLOSE, "half a g: half undone");
        assertEquals(0, PhysicsMath.scaled(1, FALL, FALL), CLOSE, "the usual pull: nothing");
        assertEquals(-FALL, PhysicsMath.scaled(2, FALL, FALL), CLOSE, "twice the pull: as much again, down");
        assertEquals(0.38 * FALL - FALL, PhysicsMath.scaled(1, FALL, 0.38 * FALL), CLOSE,
                "a room of 1 g on Mars adds what Mars leaves out");
    }

    @Test
    void towards_pullsAlongItsDirectionOnly() {
        assertArrayEquals(new double[] {0, 2 * FALL, 0}, PhysicsMath.towards(0, 1, 0, 1, FALL, FALL), CLOSE,
                "falling up: the fall undone and as much again upward");
        assertArrayEquals(new double[] {FALL, FALL, 0}, PhysicsMath.towards(5, 0, 0, 1, FALL, FALL), CLOSE,
                "sideways, whatever the direction's length");
    }

    @Test
    void centre_pullsHarderNearerToFourTimesAtMost() {
        final double[] atRadius = PhysicsMath.centre(4, 0, 0, 0, 0, 0, 1, 4, FALL, FALL);
        assertEquals(-FALL, atRadius[0], CLOSE, "at its radius, a g towards it");
        assertEquals(FALL, atRadius[1], CLOSE, "the usual fall undone");
        final double[] twiceOut = PhysicsMath.centre(8, 0, 0, 0, 0, 0, 1, 4, FALL, FALL);
        assertEquals(-FALL / 4, twiceOut[0], CLOSE, "twice as far, a quarter as hard");
        final double[] close = PhysicsMath.centre(0.5, 0, 0, 0, 0, 0, 1, 4, FALL, FALL);
        assertEquals(-PhysicsMath.CLOSEST * FALL, close[0], CLOSE, "close in, no harder than four times");
    }

    @Test
    void ticks_carriesWhatIsLeftOfATick() {
        assertArrayEquals(new double[] {0, 0.5}, PhysicsMath.ticks(0.5, 0), CLOSE, "half speed: no tick yet");
        assertArrayEquals(new double[] {1, 0}, PhysicsMath.ticks(0.5, 0.5), CLOSE, "then one");
        assertArrayEquals(new double[] {2, 0}, PhysicsMath.ticks(2, 0), CLOSE, "twice as fast: two a tick");
        assertArrayEquals(new double[] {0, 0}, PhysicsMath.ticks(0, 0), CLOSE, "stopped: none ever");
    }

    @Test
    void wrap_comesBackInThroughTheOppositeSide() {
        assertEquals(10.25, PhysicsMath.wrap(15.25, 10, 15), CLOSE);
        assertEquals(14.5, PhysicsMath.wrap(9.5, 10, 15), CLOSE);
        assertEquals(12, PhysicsMath.wrap(12, 10, 15), CLOSE, "inside, it stays");
    }

    @Test
    void map_scalesAPlaceOntoAnotherSpan() {
        assertEquals(110, PhysicsMath.map(1, 0, 3, 100, 30), CLOSE, "a third of a small box is a third of a big one");
        assertEquals(99, PhysicsMath.map(-0.1, 0, 3, 100, 30), CLOSE, "just outside stays just outside, scaled");
    }
}
