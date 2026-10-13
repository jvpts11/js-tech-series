/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class IsmsQueryEscapingTest {

    @Test
    void unescape_keepsABackslashFollowedByNAsTwoCharacters() {
        final String text = "SELECT 'a\\nb'";
        assertEquals(text, IsmsQueryEscaping.unescape(IsmsQueryEscaping.escape(text)));
    }

    @Test
    void unescape_restoresLineBreaksAndBackslashes() {
        final String text = "one\ntwo\\three\n\\n";
        assertEquals(text, IsmsQueryEscaping.unescape(IsmsQueryEscaping.escape(text)));
    }

    @Test
    void escape_leavesNoLineBreakBehind() {
        assertFalse(IsmsQueryEscaping.escape("a\nb").contains("\n"));
    }

    @Test
    void unescape_copiesALoneTrailingBackslash() {
        assertEquals("a\\", IsmsQueryEscaping.unescape("a\\"));
    }
}
