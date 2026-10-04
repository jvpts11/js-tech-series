/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.printer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class PrintLayoutTest {

    @Test
    void pages_openWithTheTitleAndABlankLine() {
        final List<String> pages = PrintLayout.pages("README.TXT", "first\nsecond", false);
        assertEquals(1, pages.size());
        assertEquals(List.of("README.TXT", "", "first", "second"), List.of(pages.getFirst().split("\n", -1)));
    }

    @Test
    void pages_holdThirtyLinesUpright() {
        final String text = "line\n".repeat(PrintLayout.PORTRAIT_LINES * 2);
        final List<String> pages = PrintLayout.pages("", text, false);
        assertEquals(2, pages.size());
        assertEquals(PrintLayout.PORTRAIT_LINES, pages.getFirst().split("\n", -1).length);
    }

    @Test
    void pages_onTheirSideHoldFewerLongerLines() {
        final String line = "x".repeat(PrintLayout.LANDSCAPE_COLUMNS);
        final List<String> upright = PrintLayout.pages("", line, false);
        final List<String> lying = PrintLayout.pages("", line, true);
        assertEquals(2, upright.getFirst().split("\n").length);
        assertEquals(1, lying.getFirst().split("\n").length);
    }

    @Test
    void pages_stopAtTheMostADocumentHolds() {
        final String text = "line\n".repeat(PrintLayout.PORTRAIT_LINES * (PrintedDocument.MAX_PAGES + 5));
        assertEquals(PrintedDocument.MAX_PAGES, PrintLayout.pages("", text, false).size());
    }

    @Test
    void pages_ofNothingAreOneBlankPage() {
        assertEquals(List.of(""), PrintLayout.pages("", "", false));
    }

    @Test
    void wrap_breaksAtTheLastSpaceBeforeTheEdge() {
        assertEquals(List.of("one two", "three"), PrintLayout.wrap("one two three", 9));
    }

    @Test
    void wrap_cutsAWordLongerThanTheLine() {
        assertEquals(List.of("abcde", "fgh"), PrintLayout.wrap("abcdefgh", 5));
    }

    @Test
    void table_padsColumnsToTheWidestCellAndUnderlinesTheHeader() {
        final String table = PrintLayout.table(List.of(List.of("item", "qty"), List.of("Iron Ingot", "64")), true);
        final String[] lines = table.split("\n");
        assertEquals("item        qty", lines[0]);
        assertEquals("---------------", lines[1]);
        assertEquals("Iron Ingot  64", lines[2]);
    }

    @Test
    void table_cutsACellPastTheWidestColumn() {
        final String wide = "y".repeat(PrintLayout.MAX_COLUMN + 10);
        final String table = PrintLayout.table(List.of(List.of(wide, "1")), false);
        assertTrue(table.startsWith("y".repeat(PrintLayout.MAX_COLUMN) + "  1"));
    }

    @Test
    void sheets_countThePages() {
        assertEquals(2, PrintLayout.sheets("", "a\n".repeat(PrintLayout.PORTRAIT_LINES + 1), false));
    }
}
