/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/** One value read off what is left of a typed line, the way scanf reads one. */
class ScanReadingTest {

    @Test
    void of_aWholeNumberSkipsTheSpacesBeforeItAndLeavesWhatFollows() {
        final ScanReading.Read read = ScanReading.of("int", "  -12 34\n");
        assertEquals(-12, read.value());
        assertEquals(" 34\n", read.rest());
        assertEquals(34L, ScanReading.of("long", read.rest()).value());
    }

    @Test
    void of_aNumberTakesAFractionAndAnExponent() {
        assertEquals(2.5, ScanReading.of("double", "2.5kg\n").value());
        assertEquals("kg\n", ScanReading.of("double", "2.5kg\n").rest());
        assertEquals(1500.0, ScanReading.of("double", "1.5e3\n").value());
        assertEquals(7.0, ScanReading.of("double", "7\n").value());
    }

    @Test
    void of_aWordStopsAtTheFirstSpace() {
        final ScanReading.Read read = ScanReading.of("string", " Iron Ingot\n");
        assertEquals("Iron", read.value());
        assertEquals(" Ingot\n", read.rest());
    }

    @Test
    void of_aCharacterIsTakenAsItComesSpacesAndLineBreakIncluded() {
        assertEquals(' ', ScanReading.of("char", " x\n").value());
        assertEquals('\n', ScanReading.of("char", "\n").value());
        assertEquals("", ScanReading.of("char", "\n").rest());
    }

    @Test
    void of_textThatIsNotTheValueLetsTheLineGoRatherThanStopEveryReadAfterIt() {
        final ScanReading.Read read = ScanReading.of("int", "twelve 12\n");
        assertNull(read.value());
        assertEquals("", read.rest());
        assertNull(ScanReading.of("int", "   \n").value(), "a line of nothing but spaces holds no value");
        assertNull(ScanReading.of("int", "99999999999\n").value(), "nor one too big for the variable");
    }
}
