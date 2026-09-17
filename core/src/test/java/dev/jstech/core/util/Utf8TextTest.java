/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class Utf8TextTest {

    /* Two bytes, one char: a Greek letter. Four bytes, two chars: a mathematical letter, a surrogate pair. */
    private static final String TWO_BYTE = "Ω";
    private static final String FOUR_BYTE = "𝚫";

    @Test
    void byteLength_countsBytesNotCharacters() {
        assertEquals(1, Utf8Text.byteLength("a"));
        assertEquals(2, Utf8Text.byteLength(TWO_BYTE));
        assertEquals(4, Utf8Text.byteLength(FOUR_BYTE));
        assertEquals(2, FOUR_BYTE.length(), "the pair is two chars, which is the whole trap");
    }

    @Test
    void byteLength_treatsNothingAsEmpty() {
        assertEquals(0, Utf8Text.byteLength(null));
        assertEquals(0, Utf8Text.byteLength(""));
    }

    @Test
    void fits_answersByBytes() {
        assertTrue(Utf8Text.fits(TWO_BYTE, 2));
        assertFalse(Utf8Text.fits(TWO_BYTE, 1), "one char is not one byte");
    }

    @Test
    void require_givesBackTextThatFits() {
        assertEquals("abc", Utf8Text.require("abc", 3, "a name"));
    }

    @Test
    void require_refusesTextThatDoesNotFit() {
        final IllegalArgumentException refused =
                assertThrows(IllegalArgumentException.class, () -> Utf8Text.require(TWO_BYTE, 1, "a name"));
        assertTrue(refused.getMessage().contains("a name"), "the message must say which text it was");
        assertTrue(refused.getMessage().contains("1"), "and how much room there was");
    }

    @Test
    void clamp_leavesTextThatFitsAlone() {
        assertEquals("abc", Utf8Text.clamp("abc", 3));
        assertEquals("abc", Utf8Text.clamp("abc", 100));
    }

    @Test
    void clamp_cutsToTheByteLimit() {
        final String cut = Utf8Text.clamp("abcdef", 3);
        assertEquals("abc", cut);
        assertTrue(Utf8Text.fits(cut, 3));
    }

    /*
     * The case the whole class exists for: room for three bytes, and the second character takes two. Cutting
     * by characters would give back four bytes, and the write would throw on a string that looked short enough.
     */
    @Test
    void clamp_neverSplitsAMultiByteCharacter() {
        final String cut = Utf8Text.clamp("a" + TWO_BYTE + "b", 2);
        assertEquals("a", cut);
        assertEquals(1, cut.getBytes(StandardCharsets.UTF_8).length);
    }

    @Test
    void clamp_neverSplitsASurrogatePair() {
        final String room = Utf8Text.clamp("ab" + FOUR_BYTE, 5);
        assertEquals("ab", room, "three bytes left is not enough for a four-byte character");
        assertEquals(6, Utf8Text.byteLength("ab" + FOUR_BYTE));
        assertEquals("ab" + FOUR_BYTE, Utf8Text.clamp("ab" + FOUR_BYTE, 6));
    }

    @Test
    void clamp_treatsNoRoomAsNothing() {
        assertEquals("", Utf8Text.clamp("abc", 0));
        assertEquals("", Utf8Text.clamp("abc", -1));
        assertEquals("", Utf8Text.clamp(null, 10));
    }

    /* Half a pair is not a character at all, and anything cut out of it would be worse than nothing. */
    @Test
    void clamp_givesNothingBackForTextThatIsNotCharacters() {
        assertEquals("", Utf8Text.clamp("a\ud835b", 2));
    }

    @Test
    void field_tidiesAndCutsWhatSomebodyTyped() {
        assertEquals("a name", Utf8Text.field("  a name  ", 64));
        assertEquals("", Utf8Text.field(null, 64));
        assertEquals("", Utf8Text.field("   ", 64));
        assertEquals("abc", Utf8Text.field(" abcdef ", 3));
    }

    /* The trimming happens first, so a name that only looks too long because of its spaces still fits. */
    @Test
    void field_trimsBeforeItCuts() {
        assertEquals("abcd", Utf8Text.field("   abcd   ", 4));
    }
}
