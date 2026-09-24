/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SoundGateTest {

    @Test
    void allow_letsTheFirstThroughAndHoldsTheRestInsideTheCooldown() {
        final SoundGate gate = new SoundGate(10);
        assertTrue(gate.allow("machine@1", "jsc:done", 100));
        assertFalse(gate.allow("machine@1", "jsc:done", 100));
        assertFalse(gate.allow("machine@1", "jsc:done", 109));
        assertTrue(gate.allow("machine@1", "jsc:done", 110));
    }

    @Test
    void allow_keepsSourcesAndSoundsApart() {
        final SoundGate gate = new SoundGate(10);
        assertTrue(gate.allow("machine@1", "jsc:done", 0));
        assertTrue(gate.allow("machine@2", "jsc:done", 0));
        assertTrue(gate.allow("machine@1", "jsc:error", 0));
    }

    @Test
    void allow_forgetsWhatNoLongerMatters() {
        final SoundGate gate = new SoundGate(5);
        for (int i = 0; i < 50; i++) {
            gate.allow("machine@" + i, "jsc:done", 0);
        }
        assertEquals(50, gate.remembered());
        gate.allow("machine@late", "jsc:done", 1000);
        assertEquals(1, gate.remembered());
    }

    @Test
    void constructor_refusesACooldownOfNothing() {
        assertThrows(IllegalArgumentException.class, () -> new SoundGate(0));
    }
}
