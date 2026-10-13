/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.boot;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BootPhasesTest {

    private BootPhases phases;

    @BeforeEach
    void setUp() {
        phases = new BootPhases();
    }

    @Test
    void beginDown_forARestartDoesNotSwitchOff() {
        phases.beginDown(100L, 40);
        assertTrue(phases.goingDown());
        assertFalse(phases.poweringOff());
    }

    @Test
    void beginDown_forAShutDownSaysItSwitchesOffUntilItEnds() {
        phases.beginDown(100L, 40, true);
        assertTrue(phases.poweringOff());
        assertFalse(phases.downDone(139L), "still saying goodbye one tick before the end");
        assertTrue(phases.downDone(140L));
        phases.endDown();
        assertFalse(phases.goingDown());
        assertFalse(phases.poweringOff(), "a closing-down that has ended switches nothing off");
    }

    @Test
    void setNeedsPost_endsAShutDownUnderWay() {
        phases.beginDown(0L, 40, true);
        phases.setNeedsPost(true);
        assertFalse(phases.poweringOff(), "a self-test asked for outright takes the machine back to the start");
        assertEquals(0, phases.downTotal());
    }
}
