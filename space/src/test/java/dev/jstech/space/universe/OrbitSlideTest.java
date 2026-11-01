/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.universe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OrbitSlideTest {

    private static final double CLOSE = 1e-9;
    private static final double TURN = 2 * Math.PI;
    /* How many slides between none and five turns are tried, and how many steps the way down is taken in. */
    private static final int SLIDES = 997;
    private static final int STEPS = 400;
    private static final double TURNS = 5;

    @Test
    void slid_isAWholeTurnAGameDay() {
        assertEquals(0, OrbitSlide.slid(0), CLOSE);
        assertEquals(TURN, OrbitSlide.slid(24_000), CLOSE);
        assertEquals(TURN / 4, OrbitSlide.slid(6_000), CLOSE);
    }

    @Test
    void downTo_atTheOrbitIsTheSlideItselfAndAtTheFloorAWholeTurn() {
        for (int i = 0; i <= SLIDES; i++) {
            final double slide = TURNS * TURN * i / SLIDES;
            assertEquals(slide, OrbitSlide.downTo(slide, 0), CLOSE);
            final double floor = OrbitSlide.downTo(slide, 1);
            assertEquals(0, Math.IEEEremainder(floor, TURN), CLOSE, "at the floor " + floor + " from " + slide);
            assertTrue(floor >= slide && floor - slide < TURN + CLOSE, "the next whole turn on from " + slide);
        }
    }

    @Test
    void downTo_neverTurnsBackOnTheWayDown() {
        for (int i = 0; i <= SLIDES; i++) {
            final double slide = TURNS * TURN * i / SLIDES;
            double last = OrbitSlide.downTo(slide, 0);
            for (int step = 1; step <= STEPS; step++) {
                final double now = OrbitSlide.downTo(slide, (double) step / STEPS);
                assertTrue(now >= last - CLOSE, "from " + slide + ", " + now + " after " + last + " at step " + step);
                last = now;
            }
        }
    }

    @Test
    void downTo_holdsBeyondTheOrbitAndTheFloor() {
        final double slide = 1.3;
        assertEquals(OrbitSlide.downTo(slide, 0), OrbitSlide.downTo(slide, -0.5), CLOSE);
        assertEquals(OrbitSlide.downTo(slide, 1), OrbitSlide.downTo(slide, 1.5), CLOSE);
    }
}
