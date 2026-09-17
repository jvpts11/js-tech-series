/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.util;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.StandardCharsets;

/**
 * How long a piece of text is where it is measured in bytes rather than characters.
 *
 * <p>Everything written to a packet or to a save is measured in UTF-8 bytes, and a character is one to four of
 * them. Counting characters instead is right until somebody types an accent or an emoji, and then a name that
 * looked well within the limit is over it. What happens then is not a truncated name: the write throws where it
 * stands, and the packet is never sent at all.
 *
 * <p>So there are two answers here, for the two places the question comes up. Where the text comes from a player
 * or a program, {@link #require} refuses it with an explanation while the one who wrote it is still there to hear
 * it. Where the text is already in hand and has to go out whatever it is, {@link #clamp} cuts it to fit. A cut
 * never lands in the middle of a character, whether it is two bytes or a pair of surrogates four bytes long.
 */
public final class Utf8Text {

    private Utf8Text() {
    }

    /** How many bytes the text takes where it is written. */
    public static int byteLength(final String text) {
        return text == null ? 0 : text.getBytes(StandardCharsets.UTF_8).length;
    }

    /** Whether the text fits in that many bytes. */
    public static boolean fits(final String text, final int limit) {
        return byteLength(text) <= limit;
    }

    /**
     * The text, if it fits, and an explanation if it does not.
     *
     * <p>For the place the text arrives from outside: a field somebody typed, a name a program chose. Refusing
     * there is what lets the message say which text it was and how much room there is.
     *
     * @throws IllegalArgumentException if the text is longer than the limit
     */
    public static String require(final String text, final int limit, final String what) {
        final int length = byteLength(text);
        if (length > limit) {
            throw new IllegalArgumentException(
                    what + " is " + length + " bytes long and there is room for " + limit);
        }
        return text;
    }

    /**
     * The text cut down to fit, keeping as much of the front as will go.
     *
     * <p>For the place the text is written out: whatever is in hand has to go somewhere, and a line that reads
     * short is better than a packet that is never sent. Nothing is cut where a character would be split.
     */
    public static String clamp(final String text, final int limit) {
        if (text == null || text.isEmpty() || limit <= 0) {
            return "";
        }
        if (fits(text, limit)) {
            return text;
        }
        final CharsetEncoder encoder = StandardCharsets.UTF_8.newEncoder();
        final CharBuffer in = CharBuffer.wrap(text);
        final ByteBuffer out = ByteBuffer.allocate(limit);
        final CoderResult result = encoder.encode(in, out, true);
        /*
         * An overflow is the expected answer here: the encoder filled the room there was and left the reader
         * sitting on the first character it could not fit whole. Anything else means the text held something
         * that is not a character at all, such as half a surrogate pair, and then nothing of it can be trusted.
         */
        if (!result.isOverflow() && !result.isUnderflow()) {
            return "";
        }
        return text.substring(0, in.position());
    }
}
