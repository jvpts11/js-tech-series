/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli.sh;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class TextFiltersTest {

    @Test
    void ordered_uniqueKeepsEachLineOnceThoughItsCaseHasTwins() {
        assertEquals(List.of("A", "a", "b"), TextFilters.ordered(List.of("a", "A", "b", "a"), false, true));
    }

    @Test
    void linesOf_anEmptyFileHasNoLinesAndAFinalNewlineEndsTheLastOne() {
        assertEquals(List.of(), TextFilters.linesOf(""));
        assertEquals(List.of("a", "b"), TextFilters.linesOf("a\nb\n"));
        assertEquals(List.of("a", "", "b"), TextFilters.linesOf("a\r\n\r\nb"));
        assertEquals(List.of(""), TextFilters.linesOf("\n"), "a file of one empty line keeps it");
        assertEquals(new TextFilters.Counts(0, 0, 0), TextFilters.counted(TextFilters.linesOf("")));
    }

    @Test
    void ordered_sortsWithoutRegardToCase() {
        assertEquals(List.of("apple", "Banana", "cherry"),
                TextFilters.ordered(List.of("cherry", "apple", "Banana"), false, false));
    }
}
