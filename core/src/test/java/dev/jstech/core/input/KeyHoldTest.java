/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class KeyHoldTest {

    private static final long HOLD = 400L;
    private static final String PRESS = "jstests:press";
    private static final String UPGRADE = "jstests:upgrade";

    @Test
    void follow_isDoneOnceTheKeyIsHeldLongEnough() {
        final KeyHold hold = new KeyHold(HOLD);
        assertFalse(hold.follow(PRESS, true, 1000L));
        assertFalse(hold.follow(PRESS, true, 1399L));
        assertTrue(hold.follow(PRESS, true, 1400L));
    }

    @Test
    void follow_startsAgainOverAnotherThing() {
        final KeyHold hold = new KeyHold(HOLD);
        hold.follow(PRESS, true, 1000L);
        assertFalse(hold.follow(UPGRADE, true, 1300L));
        assertFalse(hold.follow(UPGRADE, true, 1500L));
        assertTrue(hold.follow(UPGRADE, true, 1700L));
    }

    @Test
    void follow_startsAgainWhenTheKeyIsLetGo() {
        final KeyHold hold = new KeyHold(HOLD);
        hold.follow(PRESS, true, 1000L);
        hold.follow(PRESS, false, 1300L);
        assertFalse(hold.follow(PRESS, true, 1450L));
        assertFalse(hold.follow(PRESS, true, 1600L));
        assertTrue(hold.follow(PRESS, true, 1850L));
    }

    @Test
    void follow_waitsForTheKeyToBeLetGoAfterItIsDone() {
        final KeyHold hold = new KeyHold(HOLD);
        hold.follow(PRESS, true, 1000L);
        assertTrue(hold.follow(PRESS, true, 1400L));
        assertFalse(hold.follow(PRESS, true, 1500L));
        assertFalse(hold.follow(PRESS, true, 3000L));
        assertEquals("", hold.over());
        hold.follow(PRESS, false, 3100L);
        hold.follow(PRESS, true, 3200L);
        assertTrue(hold.follow(PRESS, true, 3600L));
    }

    @Test
    void follow_holdsOverNothingForAnEmptyThing() {
        final KeyHold hold = new KeyHold(HOLD);
        hold.follow(PRESS, true, 1000L);
        assertFalse(hold.follow("", true, 1200L));
        assertEquals("", hold.over());
        assertFalse(hold.follow(PRESS, true, 1500L));
    }

    @Test
    void progress_runsFromZeroToOne() {
        final KeyHold hold = new KeyHold(HOLD);
        assertEquals(0.0F, hold.progress(1000L));
        hold.follow(PRESS, true, 1000L);
        assertEquals(0.0F, hold.progress(1000L));
        assertEquals(0.5F, hold.progress(1200L), 1.0e-6F);
        assertEquals(1.0F, hold.progress(5000L));
    }

    @Test
    void constructor_refusesAHoldOfNoTime() {
        assertThrows(IllegalArgumentException.class, () -> new KeyHold(0L));
    }
}
