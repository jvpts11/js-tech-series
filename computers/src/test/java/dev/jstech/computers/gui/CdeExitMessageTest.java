/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class CdeExitMessageTest {

    @Test
    void lines_sayTheCountInWordsAndWarn() {
        assertEquals(List.of("Three programs are still open", "on this workstation.",
                "What is not saved will be lost."), CdeExitMessage.lines(3));
    }

    @Test
    void lines_sayOneProgramInTheSingular() {
        assertEquals("One program is still open", CdeExitMessage.lines(1).get(0));
    }

    @Test
    void lines_leaveTheWarningOutWhenNothingIsOpen() {
        assertEquals(List.of("No program is open", "on this workstation."), CdeExitMessage.lines(0));
        assertEquals(CdeExitMessage.lines(0), CdeExitMessage.lines(-4));
    }

    @Test
    void lines_writeACountPastTwelveInFigures() {
        assertEquals("Twelve programs are still open", CdeExitMessage.lines(12).get(0));
        assertEquals("13 programs are still open", CdeExitMessage.lines(13).get(0));
    }

    @Test
    void lines_neverSayMoreThanTheDialogHasRoomFor() {
        for (int open = 0; open < 40; open++) {
            assertTrue(CdeExitMessage.lines(open).size() <= 3);
        }
    }
}
