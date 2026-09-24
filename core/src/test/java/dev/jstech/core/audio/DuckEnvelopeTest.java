/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DuckEnvelopeTest {

    @Test
    void tick_goesDownQuicklyUnderAnAlertAndStopsAtTheDepth() {
        final DuckEnvelope duck = new DuckEnvelope(0.6F, 4, 20);
        assertEquals(1.0F, duck.factor());
        assertEquals(0.9F, duck.tick(true), 0.0001F);
        for (int i = 0; i < 3; i++) {
            duck.tick(true);
        }
        assertEquals(0.6F, duck.factor(), 0.0001F);
        assertEquals(0.6F, duck.tick(true), 0.0001F);
    }

    @Test
    void tick_comesBackSlowlyOnceTheAlertsEnd() {
        final DuckEnvelope duck = new DuckEnvelope(0.6F, 4, 20);
        for (int i = 0; i < 4; i++) {
            duck.tick(true);
        }
        assertEquals(0.62F, duck.tick(false), 0.0001F);
        for (int i = 0; i < 18; i++) {
            assertTrue(duck.tick(false) < 1.0F);
        }
        assertEquals(1.0F, duck.tick(false), 0.0001F);
        assertEquals(1.0F, duck.tick(false));
    }

    @Test
    void tick_staysWholeWithNoAlert() {
        final DuckEnvelope duck = DuckEnvelope.standard();
        for (int i = 0; i < 10; i++) {
            assertEquals(1.0F, duck.tick(false));
        }
    }

    @Test
    void constructor_refusesWhatCannotBeAnEnvelope() {
        assertThrows(IllegalArgumentException.class, () -> new DuckEnvelope(1.5F, 4, 20));
        assertThrows(IllegalArgumentException.class, () -> new DuckEnvelope(0.5F, 0, 20));
        assertThrows(IllegalArgumentException.class, () -> new DuckEnvelope(0.5F, 4, 0));
    }
}
