/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.iql;

import dev.jstech.computers.os.edit.CodeRuns;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IqlColouringTest {

    @Test
    void spans_coloursWordsTextAndNumbers() {
        final List<CodeRuns.Span> spans = IqlColouring.spans(List.of("QUERY items WHERE tag = \"c:ingots\""));
        assertEquals(new CodeRuns.Span(1, 1, 5, CodeRuns.Ink.KEYWORD), spans.get(0));
        assertEquals(new CodeRuns.Span(1, 7, 5, CodeRuns.Ink.NAME), spans.get(1));
        assertEquals(CodeRuns.Ink.KEYWORD, spans.get(2).ink());
        assertEquals(CodeRuns.Ink.SYMBOL, spans.get(4).ink());
        assertEquals(new CodeRuns.Span(1, 25, 10, CodeRuns.Ink.TEXT), spans.get(5));
    }

    @Test
    void spans_runsACommentToTheEndOfItsLine() {
        final List<CodeRuns.Span> spans = IqlColouring.spans(List.of("CRAFT 64 torch -- for the base"));
        final CodeRuns.Span last = spans.get(spans.size() - 1);
        assertEquals(CodeRuns.Ink.COMMENT, last.ink());
        assertEquals(16, last.column());
        assertEquals(CodeRuns.Ink.NUMBER, spans.get(1).ink());
    }

    @Test
    void spans_countsLinesFromOne() {
        final List<CodeRuns.Span> spans = IqlColouring.spans(List.of("", "QUERY servers;"));
        assertTrue(spans.stream().allMatch(span -> span.line() == 2));
    }
}
