/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.cannon.lua.lib;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class LuaPatternsTest {

    private static List<Object> results(final String subject, final String pattern) {
        final LuaPatterns.Match match = LuaPatterns.find(subject, pattern, 0);
        return match == null ? null : match.results(subject);
    }

    @Test
    void find_matchesClassesAndTheirComplements() {
        assertEquals(List.of("123"), results("abc123def", "%d+"));
        assertEquals(List.of("abc"), results("abc123def", "%D+"));
        assertEquals(List.of("  "), results("a  b", "%s+"));
        assertEquals(List.of("A"), results("xyzA", "%u"));
    }

    @Test
    void find_matchesSetsWithRangesAndNegation() {
        assertEquals(List.of("bca"), results("xbcay", "[a-c]+"));
        assertEquals(List.of("xy"), results("xyabc", "[^a-c]+"));
        assertEquals(List.of("-"), results("a-b", "[%-]"));
    }

    @Test
    void find_takesAsLittleAsItCanWithTheLazyQuantifier() {
        assertEquals(List.of("<a>"), results("<a><b>", "<.->"));
        assertEquals(List.of("<a><b>"), results("<a><b>", "<.*>"));
    }

    @Test
    void find_returnsCapturesAndPositions() {
        assertEquals(List.of("key", "value"), results("key = value", "(%w+)%s*=%s*(%w+)"));
        assertEquals(List.of(1L, 3L), results("abc", "()a.-()c"));
    }

    @Test
    void find_honoursAnchorsAtBothEnds() {
        assertEquals(List.of("ab"), results("abab", "^ab"));
        assertNull(results("xab", "^ab"));
        assertEquals(List.of("ab"), results("xab", "ab$"));
        assertNull(results("abx", "ab$"));
    }

    @Test
    void find_matchesBalancedPairsAndFrontiers() {
        assertEquals(List.of("(a(b)c)"), results("x(a(b)c)y", "%b()"));
        assertEquals(List.of("the"), results("other the", "%f[%a]the%f[%A]"));
    }

    @Test
    void find_matchesABackReference() {
        assertEquals(List.of("'", "hi"), results("say 'hi' now", "(['\"])(.-)%1"));
    }

    @Test
    void find_startsWhereItIsToldAndMatchesEmpty() {
        final LuaPatterns.Match match = LuaPatterns.find("aXbX", "X", 2);
        assertEquals(3, match.start());
        assertEquals(0, LuaPatterns.find("abc", "x*", 0).end());
    }

    @Test
    void find_refusesAMalformedPattern() {
        assertThrows(LuaPatterns.BadPattern.class, () -> LuaPatterns.find("abc", "[a", 0));
        assertThrows(LuaPatterns.BadPattern.class, () -> LuaPatterns.find("abc", "%", 0));
        assertThrows(LuaPatterns.BadPattern.class, () -> LuaPatterns.find("abc", "(a", 0));
    }

    @Test
    void isPlain_isTrueOnlyForTextWithNothingSpecial() {
        assertTrue(LuaPatterns.isPlain("hello world"));
        assertEquals(false, LuaPatterns.isPlain("a.b"));
    }
}
