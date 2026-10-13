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

import dev.jstech.core.text.Text;
import java.util.List;
import org.junit.jupiter.api.Test;

class CdeExitMessageTest {

    @Test
    void lines_sayTheCountInWordsAndWarn() {
        assertEquals(List.of("Three programs are still open", "on this workstation.",
                "What is not saved will be lost."), english(3));
    }

    @Test
    void lines_sayOneProgramInTheSingular() {
        assertEquals("One program is still open", english(1).get(0));
    }

    @Test
    void lines_leaveTheWarningOutWhenNothingIsOpen() {
        assertEquals(List.of("No program is open", "on this workstation."), english(0));
        assertEquals(english(0), english(-4));
    }

    @Test
    void lines_writeACountPastTwelveInFigures() {
        assertEquals("Two programs are still open", english(2).get(0));
        assertEquals("Twelve programs are still open", english(12).get(0));
        assertEquals("13 programs are still open", english(13).get(0));
    }

    @Test
    void lines_neverSayMoreThanTheDialogHasRoomFor() {
        for (int open = 0; open < 40; open++) {
            assertTrue(CdeExitMessage.lines(open).size() <= 3);
        }
    }

    private static List<String> english(final int open) {
        return CdeExitMessage.lines(open).stream().map(Text::english).toList();
    }
}
