/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.signal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class FrequencyTest {

    @Test
    void frequency_ofOneOwnerAndNumberIsOne() {
        assertEquals(new Frequency(Band.PRIVATE, "a", 7), new Frequency(Band.PRIVATE, "a", 7));
    }

    @Test
    void frequency_ofTheTeamIsNotTheOwnersOwn() {
        assertNotEquals(new Frequency(Band.PRIVATE, "a", 7), new Frequency(Band.TEAM, "a", 7));
    }

    @Test
    void frequency_refusesANumberOutOfRange() {
        assertThrows(IllegalArgumentException.class, () -> new Frequency(Band.PRIVATE, "a", 0));
        assertThrows(IllegalArgumentException.class, () -> new Frequency(Band.TEAM, "a", Frequency.MOST + 1));
    }

    @Test
    void frequency_ofAPairHasNoNumber() {
        assertEquals(0, new Frequency(Band.PAIR, "p", 0).number());
        assertThrows(IllegalArgumentException.class, () -> new Frequency(Band.PAIR, "p", 3));
    }

    @Test
    void held_keepsANumberAmongThoseThereAre() {
        assertEquals(Frequency.LEAST, Frequency.held(-5));
        assertEquals(42, Frequency.held(42));
        assertEquals(Frequency.MOST, Frequency.held(1_000_000));
    }
}
