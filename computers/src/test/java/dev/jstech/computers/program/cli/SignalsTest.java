/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SignalsTest {

    @Test
    void parse_readsANumberANameAndANameWithItsPrefixAlike() {
        assertEquals(9, Signals.parse("-9"));
        assertEquals(9, Signals.parse("-KILL"));
        assertEquals(9, Signals.parse("-SIGKILL"));
        assertEquals(9, Signals.parse("-sigkill"));
        assertEquals(9, Signals.parse("KILL"));
        assertEquals(15, Signals.parse("-TERM"));
        assertEquals(1, Signals.parse("-HUP"));
    }

    @Test
    void parse_refusesWhatNamesNoSignal() {
        assertEquals(-1, Signals.parse("-"));
        assertEquals(-1, Signals.parse("-7"));
        assertEquals(-1, Signals.parse("-NOPE"));
        assertEquals(-1, Signals.parse("-99999999999"));
    }

    @Test
    void listing_namesEachNumberWithItsWholeName() {
        assertEquals("1) SIGHUP  2) SIGINT  3) SIGQUIT  9) SIGKILL  15) SIGTERM", Signals.listing());
    }
}
