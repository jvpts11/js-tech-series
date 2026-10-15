/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.signal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.jstech.core.text.TextKey;
import org.junit.jupiter.api.Test;

class ReadingTest {

    private static final TextKey LIGHT = TextKey.of("test.reading.light", "Light");

    @Test
    void strength_isNothingAtTheLeastAndFullAtTheMost() {
        assertEquals(0, new Reading(LIGHT, 0, 0, 15, null).strength());
        assertEquals(Reading.FULL, new Reading(LIGHT, 15, 0, 15, null).strength());
    }

    @Test
    void strength_holdsAValueOutsideTheSpan() {
        assertEquals(0, Reading.strength(-40, 0, 100));
        assertEquals(Reading.FULL, Reading.strength(250, 0, 100));
    }

    @Test
    void strength_roundsTheShareOfTheSpan() {
        assertEquals(8, Reading.strength(50, 0, 100));
        assertEquals(1, Reading.strength(4, 0, 100));
        assertEquals(0, Reading.strength(3, 0, 100));
    }

    @Test
    void strength_followsASpanThatDoesNotStartAtZero() {
        assertEquals(0, Reading.strength(-64, -64, 320));
        assertEquals(Reading.FULL, Reading.strength(320, -64, 320));
    }

    @Test
    void strength_isNothingForNotANumber() {
        assertEquals(0, Reading.strength(Double.NaN, 0, 1));
    }

    @Test
    void reading_refusesASpanRunningBackwards() {
        assertThrows(IllegalArgumentException.class, () -> new Reading(LIGHT, 0, 10, 10, null));
        assertThrows(IllegalArgumentException.class, () -> new Reading(LIGHT, 0, 10, 0, null));
    }
}
