/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class TextTableTest {

    @Test
    void shape_setsColumnsSideBySideAsWideAsTheirWidestCellWhenThereIsRoom() {
        final TextTable table = TextTable.shape(List.of("Command", "What it does"),
                List.of(List.of("dir", "Lists the folder."), List.of("del", "Deletes a file.")), 60, 2, 10,
                String::length);

        assertFalse(table.stacked());
        assertEquals(List.of("Command  What it does", "dir      Lists the folder.", "del      Deletes a file."),
                table.lines().stream().map(TextTable.Line::padded).toList());
        assertEquals(TextTable.Role.HEAD, table.lines().getFirst().cells().getFirst().role());
        assertEquals(TextTable.Role.NAME, table.lines().get(1).cells().getFirst().role());
    }

    @Test
    void shape_wrapsAColumnOfWordsBetweenWordsWhenTheRoomIsShort() {
        final TextTable table = TextTable.shape(List.of("Command", "What it does"),
                List.of(List.of("format", "Erases a whole drive, after asking first.")), 30, 2, 10, String::length);

        assertFalse(table.stacked());
        final List<String> lines = table.lines().stream().map(TextTable.Line::padded).toList();
        assertTrue(lines.size() > 2, "the long cell runs on to more lines: " + lines);
        assertTrue(lines.stream().allMatch(line -> line.length() <= 30), "and never past the room: " + lines);
        assertTrue(String.join(" ", lines).contains("Erases a whole"), "between whole words");
    }

    @Test
    void shape_setsRowsOutOneUnderAnotherWhenTheColumnsCannotStandSideBySide() {
        final TextTable table = TextTable.shape(List.of("Code", "Message", "Means"), List.of(List.of("S1001",
                "unterminated string", "A string was opened and the line ended before it was closed.")), 24, 2, 10,
                String::length);

        assertTrue(table.stacked());
        final List<String> lines = table.lines().stream().map(TextTable.Line::padded).toList();
        assertEquals("S1001", lines.getFirst());
        assertTrue(lines.get(1).startsWith("    Message:"), "each further cell set in, after its head: " + lines);
    }

    @Test
    void wrap_breaksBetweenWordsAndCutsAWordWiderThanALine() {
        assertEquals(List.of("one two", "three"), TextTable.wrap("one two three", 7, String::length));
        assertEquals(List.of("abcde", "fghij"), TextTable.wrap("abcdefghij", 5, String::length));
    }
}
