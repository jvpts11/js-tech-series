/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.engine.nextgre;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class NextgreStatementTest {

    @Test
    void parse_plainStatementHasNoExplainAndNoHints() {
        final NextgreStatement s = NextgreStatement.parse("CRAFT 64 processor", List.of());

        assertFalse(s.explain());
        assertFalse(s.analyze());
        assertEquals("CRAFT 64 processor", s.core());
        assertTrue(s.hints().isEmpty());
    }

    @Test
    void parse_explainAnalyzeAndTrailingHintsAreTakenOff() {
        final NextgreStatement s = NextgreStatement.parse(
                "EXPLAIN ANALYZE CRAFT 64 processor PREFER SOURCE 'Storage B' MAX PARALLEL 2;", List.of());

        assertTrue(s.explain());
        assertTrue(s.analyze());
        assertEquals("CRAFT 64 processor", s.core());
        assertEquals("Storage B", s.value(NextgreStatement.PREFER_SOURCE));
        assertEquals(2, s.hints().get(1).number());
    }

    @Test
    void parse_hintsAreReadInAnyCase() {
        final NextgreStatement s = NextgreStatement.parse("craft 8 piston prefer machine max parallel 4", List.of());

        assertEquals("craft 8 piston", s.core());
        assertTrue(s.has(NextgreStatement.PREFER_MACHINE));
        assertEquals("4", s.value(NextgreStatement.MAX_PARALLEL));
    }

    @Test
    void parse_hintWordsInsideQuotesStayPartOfTheStatement() {
        final NextgreStatement s = NextgreStatement.parse("SELECT 4 stone FROM 'PREFER BENCH'", List.of());

        assertEquals("SELECT 4 stone FROM 'PREFER BENCH'", s.core());
        assertTrue(s.hints().isEmpty());
    }

    @Test
    void parse_aHintMissingItsValueLeavesTheStatementWhole() {
        final NextgreStatement s = NextgreStatement.parse("CRAFT 4 torch MAX PARALLEL", List.of());

        assertEquals("CRAFT 4 torch MAX PARALLEL", s.core());
        assertNull(s.value(NextgreStatement.MAX_PARALLEL));
    }

    @Test
    void parse_anotherModsHintIsReadLikeTheEnginesOwn() {
        final NextgreStatement s = NextgreStatement.parse("CRAFT 4 torch prefer computer 'Bench A'",
                List.of(new NextgreStatement.Operator("prefer  computer", true)));

        assertEquals("CRAFT 4 torch", s.core());
        assertEquals("Bench A", s.value("PREFER COMPUTER"));
    }

    @Test
    void parse_explainWithNothingToExplainIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> NextgreStatement.parse("EXPLAIN ANALYZE ;", List.of()));
    }

    @Test
    void parse_aWordThatOnlyStartsWithExplainIsNotOne() {
        final NextgreStatement s = NextgreStatement.parse("EXPLAINED", List.of());

        assertFalse(s.explain());
        assertEquals("EXPLAINED", s.core());
    }

    @Test
    void written_showsTheHintAsItWasWritten() {
        assertEquals("PREFER SOURCE Storage B",
                new NextgreStatement.Hint(NextgreStatement.PREFER_SOURCE, "Storage B").written());
        assertEquals("PREFER BENCH", new NextgreStatement.Hint(NextgreStatement.PREFER_BENCH, "").written());
    }

    @Test
    void number_isMinusOneForAValueThatIsNotANumber() {
        assertEquals(-1, new NextgreStatement.Hint(NextgreStatement.MAX_PARALLEL, "many").number());
    }
}
