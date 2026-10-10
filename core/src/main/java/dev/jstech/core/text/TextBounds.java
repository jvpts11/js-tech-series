/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.text;

/**
 * Text measured and cut against the fixed limits of the forms it travels and is kept in.
 *
 * <p>A string past the cap of its wire field does not trim itself: writing it throws, and takes the player's
 * connection with it. A string tag holds at most 65,535 bytes of modified UTF-8; one past that is written to a save
 * as an empty string and throws on the wire. So text that comes from a player, a program or a file is cut or checked
 * here before it reaches either, in one place, the same way everywhere.
 */
public final class TextBounds {

    /** The most bytes of modified UTF-8 one string tag holds, its length being written in two bytes. */
    public static final int MOST_TAG_BYTES = 65_535;

    /* No character takes more than three bytes of modified UTF-8, so a text this short always fits a tag. */
    private static final int ALWAYS_FITS_TAG = MOST_TAG_BYTES / 3;

    private TextBounds() {
    }

    /**
     * The text cut to at most {@code most} UTF-16 units, and empty for none.
     *
     * <p>The cut never falls between the two halves of a character outside the basic plane (an emoji, a rare
     * ideograph): a lone half would reach the other side as a question mark.
     */
    public static String clip(final String text, final int most) {
        if (text == null || most <= 0) {
            return "";
        }
        if (text.length() <= most) {
            return text;
        }
        int end = most;
        if (Character.isHighSurrogate(text.charAt(end - 1)) && Character.isLowSurrogate(text.charAt(end))) {
            end--;
        }
        return text.substring(0, end);
    }

    /** How many bytes the text takes as modified UTF-8, the form a string tag is written in. */
    public static long tagBytes(final String text) {
        long bytes = 0;
        for (int i = 0; i < text.length(); i++) {
            final char c = text.charAt(i);
            if (c >= 0x0001 && c <= 0x007F) {
                bytes += 1;
            } else if (c <= 0x07FF) {
                // The null character is two bytes in modified UTF-8, so that no zero byte appears inside a string.
                bytes += 2;
            } else {
                bytes += 3;
            }
        }
        return bytes;
    }

    /** Whether the text fits in one string tag. */
    public static boolean fitsTag(final String text) {
        return text.length() <= ALWAYS_FITS_TAG || tagBytes(text) <= MOST_TAG_BYTES;
    }
}
