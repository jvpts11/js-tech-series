/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class TextBoundsTest {

    /* A character outside the basic plane, two UTF-16 units long. */
    private static final String EMOJI = new String(Character.toChars(0x1F600));
    /* The first half of such a pair, on its own. */
    private static final String HIGH_HALF = String.valueOf((char) 0xD83D);
    /* Characters of two and of three bytes, and the null character, which modified UTF-8 writes in two. */
    private static final String TWO_BYTES = String.valueOf((char) 0x00E9);
    private static final String THREE_BYTES = String.valueOf((char) 0x4E2D);
    private static final String NUL = String.valueOf((char) 0);

    @Test
    void clip_leavesAShortTextAsItIs() {
        final String text = "Mainframe";
        assertSame(text, TextBounds.clip(text, 9));
    }

    @Test
    void clip_cutsALongTextToTheCap() {
        assertEquals("Main", TextBounds.clip("Mainframe", 4));
    }

    @Test
    void clip_readsNothingAsEmpty() {
        assertEquals("", TextBounds.clip(null, 8));
    }

    @Test
    void clip_givesEmptyForACapOfZeroOrLess() {
        assertEquals("", TextBounds.clip("Mainframe", 0));
        assertEquals("", TextBounds.clip("Mainframe", -3));
    }

    @Test
    void clip_neverSplitsACharacterOutsideTheBasicPlane() {
        final String text = "ab" + EMOJI + "cd";
        // The cap falls between the two halves of the emoji, so the cut steps back before it.
        assertEquals("ab", TextBounds.clip(text, 3));
        assertEquals("ab" + EMOJI, TextBounds.clip(text, 4));
    }

    @Test
    void clip_keepsALoneHalfThatWasAlreadyThere() {
        // A first half not followed by its second is not a pair to protect.
        final String text = "ab" + HIGH_HALF + "cd";
        assertEquals("ab" + HIGH_HALF, TextBounds.clip(text, 3));
    }

    @Test
    void tagBytes_countsAsDataOutputWritesIt() throws IOException {
        final String text = "a" + NUL + TWO_BYTES + THREE_BYTES + EMOJI;
        assertEquals(writtenUtfBytes(text), TextBounds.tagBytes(text));
    }

    @Test
    void fitsTag_acceptsTheLongestAsciiATagHolds() {
        assertTrue(TextBounds.fitsTag("a".repeat(TextBounds.MOST_TAG_BYTES)));
        assertFalse(TextBounds.fitsTag("a".repeat(TextBounds.MOST_TAG_BYTES + 1)));
    }

    @Test
    void fitsTag_countsWideCharactersByTheirBytes() {
        // Each of these is three bytes, so a third of the cap in characters is all a tag holds.
        final int most = TextBounds.MOST_TAG_BYTES / 3;
        assertTrue(TextBounds.fitsTag(THREE_BYTES.repeat(most)));
        assertFalse(TextBounds.fitsTag(THREE_BYTES.repeat(most) + "a"));
    }

    /* The bytes DataOutput.writeUTF writes for the text, without its two-byte length. */
    private static long writtenUtfBytes(final String text) throws IOException {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeUTF(text);
        }
        return bytes.size() - 2L;
    }
}
