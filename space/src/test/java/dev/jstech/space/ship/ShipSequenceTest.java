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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ShipSequenceTest {

    private static final long BEGAN = 1_000;
    private static final int TICKS_A_SECOND = 20;
    private static final double CLOSE = 1.0e-9;
    /* How fast a ship hovering comes down as its pilot holds DOWN, in metres a second. */
    private static final double HOVERING_DOWN = 6;

    @Test
    void landing_fromItsFullHeightTakesFourSecondsAndLessFromNearer() {
        assertEquals(4 * TICKS_A_SECOND, ShipSequence.landing(BEGAN, ShipSequence.LANDS_FROM, 0).ticks());
        assertEquals(2 * TICKS_A_SECOND, ShipSequence.landing(BEGAN, ShipSequence.LANDS_FROM / 2, 0).ticks());
        assertEquals(TICKS_A_SECOND, ShipSequence.landing(BEGAN, 1, 0).ticks(), "a second at the least");
    }

    @Test
    void gone_isNoughtAtItsBeginningItsDepthAtItsEndAndNeverGoesBack() {
        final ShipSequence down = ShipSequence.landing(BEGAN, 20, HOVERING_DOWN);
        assertEquals(0, down.gone(BEGAN), CLOSE);
        assertEquals(20, down.gone(BEGAN + down.ticks()), CLOSE);
        assertEquals(20, down.gone(BEGAN + down.ticks() * 2L), CLOSE, "and stays there after");
        double was = 0;
        for (long tick = BEGAN; tick <= BEGAN + down.ticks(); tick++) {
            final double now = down.gone(tick);
            assertTrue(now >= was - CLOSE && now <= 20 + CLOSE, "on its way, never back nor past the land: " + now);
            was = now;
        }
    }

    @Test
    void landing_goesOnAtTheSpeedItCameDownAtAndTouchesAlmostStill() {
        final ShipSequence down = ShipSequence.landing(BEGAN, ShipSequence.LANDS_FROM, HOVERING_DOWN);
        final double first = (down.gone(BEGAN + 1) - down.gone(BEGAN)) * TICKS_A_SECOND;
        final double last = (down.gone(BEGAN + down.ticks()) - down.gone(BEGAN + down.ticks() - 1)) * TICKS_A_SECOND;
        assertEquals(HOVERING_DOWN, first, 0.5, "it goes on at the speed it came down at: " + first);
        assertTrue(last < 1, "and touches under a metre a second: " + last);
    }

    @Test
    void landing_fromNearAtSpeedNeverGoesPastTheLand() {
        // A metre over the land, coming down fast: the cubic's lead is held down so it never overshoots.
        final ShipSequence down = ShipSequence.landing(BEGAN, 1, 20);
        for (long tick = BEGAN; tick <= BEGAN + down.ticks(); tick++) {
            assertTrue(down.gone(tick) <= 1 + CLOSE, "never past the land: " + down.gone(tick));
        }
    }

    @Test
    void takingOff_goesUpItsHeightFromRestToRestInThreeSeconds() {
        final ShipSequence up = ShipSequence.takingOff(BEGAN);
        assertFalse(up.landing());
        assertEquals(3 * TICKS_A_SECOND, up.ticks());
        assertEquals(ShipSequence.RISES, up.gone(BEGAN + up.ticks()), CLOSE);
        assertTrue((up.gone(BEGAN + 1) - up.gone(BEGAN)) * TICKS_A_SECOND < 1, "it leaves the land gently");
        assertTrue(!up.over(BEGAN + up.ticks() - 1) && up.over(BEGAN + up.ticks()), "its time up at its end");
    }

    @Test
    void sequence_refusesNoTimeAndAWayBack() {
        assertThrows(IllegalArgumentException.class, () -> new ShipSequence(true, BEGAN, 0, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> new ShipSequence(true, BEGAN, 1, -1, 0));
    }
}
