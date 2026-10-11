/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class GuideListingTest {

    @Test
    void rows_keepALineThatFitsWholeAndNumbered() {
        final List<String> lines = List.of("class Program {", "}");

        final List<GuideListing.Row> rows = GuideListing.rows(lines, GuideCode.plain(lines), 20);

        assertEquals(2, rows.size());
        assertEquals(1, rows.get(0).number());
        assertEquals("class Program {", rows.get(0).text());
        assertEquals(2, rows.get(1).number());
    }

    @Test
    void rows_breakALongLineAfterASpaceAndSetTheRestIn() {
        final List<String> lines = List.of("  return first + second + third;");

        final List<GuideListing.Row> rows = GuideListing.rows(lines, GuideCode.plain(lines), 20);

        assertEquals("  return first +", rows.get(0).text());
        assertEquals(0, rows.get(1).number(), "the row it goes on in has no number of its own");
        assertTrue(rows.get(1).text().startsWith("    second"), "set in two past where the line began: "
                + rows.get(1).text());
        for (final GuideListing.Row row : rows) {
            assertTrue(row.text().length() <= 20, () -> "too long: " + row.text());
        }
        assertEquals("return first + second + third;", String.join(" ", rows.stream()
                .map(row -> row.text().strip()).toList()), "every word is still there, and none was cut");
    }

    @Test
    void rows_breakAfterAnOpeningBracketWhenNoSpaceFits() {
        final List<String> lines = List.of("Console.PrintLine(\"long\")");

        final List<GuideListing.Row> rows = GuideListing.rows(lines, GuideCode.plain(lines), 20);

        assertEquals("Console.PrintLine(", rows.get(0).text());
    }

    @Test
    void rows_breakAfterAPointRatherThanInsideAName() {
        final List<String> lines = List.of("Console.PrintLine(\"long\")");

        final List<GuideListing.Row> rows = GuideListing.rows(lines, GuideCode.plain(lines), 12);

        assertEquals("Console.", rows.get(0).text());
        assertTrue(rows.get(1).text().strip().startsWith("PrintLine("), () -> "the name goes on whole: "
                + rows.get(1).text());
    }

    @Test
    void rows_cutAWordWiderThanTheWholeRowWhereItMust() {
        final List<String> lines = List.of("abcdefghijklmnopqrstuvwxyz");

        final List<GuideListing.Row> rows = GuideListing.rows(lines, GuideCode.plain(lines), 10);

        assertEquals("abcdefghij", rows.get(0).text());
        assertTrue(rows.size() > 1);
    }

    @Test
    void rows_keepTheColoursOfWhatGoesOn() {
        final List<String> lines = List.of("x = \"a long string\"");
        final List<List<GuideCode.Run>> colours = GuideCode.runs(lines, List.of(
                new GuideCode.Span(1, 5, 15, GuideCode.Ink.TEXT)));

        final List<GuideListing.Row> rows = GuideListing.rows(lines, colours, 12);

        assertEquals(GuideCode.Ink.TEXT, rows.get(1).segments().getLast().ink(),
                "the quoted text that went on is still coloured as text");
        assertEquals(GuideCode.Ink.PLAIN, rows.get(1).segments().getFirst().ink(),
                "the room it is set in by is plain");
    }
}
