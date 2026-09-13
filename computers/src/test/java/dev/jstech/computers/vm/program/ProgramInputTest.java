/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ProgramInputTest {

    @Test
    void take_givesTheLinesInTheOrderTheyWereTyped() {
        final ProgramInput input = new ProgramInput();
        input.offer("first");
        input.offer("second");
        assertEquals("first", input.take());
        assertEquals("second", input.take());
    }

    @Test
    void take_givesAnEmptyLineWhenNoneWasTyped() {
        final ProgramInput input = new ProgramInput();
        assertEquals("", input.take());
    }

    @Test
    void offer_letsALineGoOnceTheMostItKeepsAreWaiting() {
        final ProgramInput input = new ProgramInput();
        for (int i = 0; i < ProgramInput.MOST_LINES + 4; i++) {
            input.offer("line " + i);
        }
        for (int i = 0; i < ProgramInput.MOST_LINES; i++) {
            assertEquals("line " + i, input.take());
        }
        assertFalse(input.has(), "the lines typed past the limit were let go");
    }

    @Test
    void offer_keepsNothingTypedAsAnEmptyLine() {
        final ProgramInput input = new ProgramInput();
        input.offer(null);
        assertTrue(input.has());
        assertEquals("", input.take());
    }

    @Test
    void has_saysWhetherALineIsWaitingWithoutTakingIt() {
        final ProgramInput input = new ProgramInput();
        assertFalse(input.has());
        input.offer("one");
        assertTrue(input.has());
        assertTrue(input.has(), "asking takes nothing");
        assertEquals("one", input.take());
        assertFalse(input.has());
    }
}
