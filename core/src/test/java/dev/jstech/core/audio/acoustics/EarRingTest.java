/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio.acoustics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EarRingTest {

    @Test
    void from_ringsHardestAtTheBlast() {
        final EarRing ring = EarRing.from(4, 0, false);
        assertEquals(1, ring.strength(), 1e-9);
        assertEquals(EarRing.LONGEST, ring.ticks());
    }

    @Test
    void from_ringsLessAndShorterFartherAway() {
        final EarRing near = EarRing.from(4, 3, false);
        final EarRing far = EarRing.from(4, 9, false);
        assertTrue(far.strength() < near.strength());
        assertTrue(far.ticks() < near.ticks());
        assertTrue(far.ticks() >= EarRing.SHORTEST);
    }

    @Test
    void from_doesNotRingBeyondTheReachOrThroughProtection() {
        assertSame(EarRing.NONE, EarRing.from(4, 12, false));
        assertSame(EarRing.NONE, EarRing.from(4, 0, true));
        assertFalse(EarRing.NONE.rings());
    }

    @Test
    void strengthAt_fadesSmoothlyToNothing() {
        final EarRing ring = EarRing.from(4, 0, false);
        assertEquals(1, ring.strengthAt(ring.ticks()), 1e-9);
        assertTrue(ring.strengthAt(ring.ticks() / 2) < 1);
        assertEquals(0, ring.strengthAt(0));
        assertEquals(1, ring.heardAt(0), 1e-9);
        assertTrue(ring.heardAt(ring.ticks()) < 0.2 && ring.highsAt(ring.ticks()) < ring.heardAt(ring.ticks()));
    }

    @Test
    void louder_keepsTheHarderRinging() {
        final EarRing first = EarRing.from(4, 0, false);
        final EarRing faint = EarRing.from(4, 10, false);
        assertSame(faint, EarRing.NONE.louder(faint, 0));
        assertEquals(first.strength(), first.louder(faint, first.ticks()).strength(), 1e-9);
        assertSame(first, faint.louder(first, 1));
    }
}
