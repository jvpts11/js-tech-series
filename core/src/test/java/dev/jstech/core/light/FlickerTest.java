/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.light;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FlickerTest {

    private static final double EPSILON = 1.0e-9;

    @Test
    void brightness_readsItsLetters() {
        final Flicker blink = new Flicker("maz", 1, true);
        assertEquals(1.0, blink.brightness(0.0, 0), EPSILON, "m is as declared");
        assertEquals(0.0, blink.brightness(1.0, 0), EPSILON, "a is dark");
        assertEquals(25.0 / 12.0, blink.brightness(2.0, 0), EPSILON, "z is about twice");
        assertEquals(1.0, blink.brightness(3.0, 0), EPSILON, "round again");
    }

    @Test
    void brightness_startsEachLightAtItsOwnPoint() {
        final Flicker blink = new Flicker("ma", 1, true);
        assertEquals(1.0, blink.brightness(0.0, 0), EPSILON);
        assertEquals(0.0, blink.brightness(0.0, 1), EPSILON, "another lamp starts a letter on");
    }

    @Test
    void brightness_holdsItsLastLetterWhenItDoesNotLoop() {
        assertEquals(1.0, Flicker.STARTING.brightness(1_000.0, 7), EPSILON, "a tube that has started stays lit");
        assertEquals(0.0, Flicker.STARTING.brightness(0.0, 7), EPSILON, "and starts dark");
    }

    @Test
    void steady_isTrueOnlyForOneLetter() {
        assertTrue(Flicker.STEADY.steady());
        assertTrue(new Flicker("mmmm", 3, true).steady());
        assertFalse(Flicker.CANDLE.steady());
    }

    @Test
    void named_holdsTheBuiltInFlickers() {
        assertEquals(Flicker.FAILING, Flicker.NAMED.get("failing"));
        assertEquals(6, Flicker.NAMED.size());
    }

    @Test
    void flicker_refusesWhatDoesNotRead() {
        assertThrows(IllegalArgumentException.class, () -> new Flicker("", 1, true));
        assertThrows(IllegalArgumentException.class, () -> new Flicker("mM1", 1, true));
        assertThrows(IllegalArgumentException.class, () -> new Flicker("m", 0, true));
    }
}
