/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class Lz4Test {

    @Test
    void decompress_unpacksABlockOfLiteralsAlone() {
        assertEquals("hello", text(Lz4.decompress(bytes(0, 0x50, 'h', 'e', 'l', 'l', 'o'), 5)));
    }

    @Test
    void decompress_countsALongRunOfLiteralsInItsExtraBytes() {
        final byte[] packed = new byte[3 + 20];
        packed[1] = (byte) 0xF0;
        packed[2] = 5;
        for (int i = 0; i < 20; i++) {
            packed[3 + i] = (byte) ('a' + i);
        }
        assertEquals("abcdefghijklmnopqrst", text(Lz4.decompress(packed, 20)), "15 in the token, 5 after it");
    }

    @Test
    void decompress_copiesAMatchThatOverlapsWhatItCopies() {
        // Two literals, then six bytes copied from two back: each copied byte is one just written.
        assertEquals("abababab", text(Lz4.decompress(bytes(0, 0x22, 'a', 'b', 2, 0), 8)));
    }

    @Test
    void decompress_unpacksSeveralBlocksEachOntoWhatCameBefore() {
        final byte[] packed = bytes(2, 4, 0, 0, 0, 0x30, 'a', 'b', 'c', 4, 0, 0, 0, 0x13, 'd', 4, 0);
        assertEquals("abcdabcdabc", text(Lz4.decompress(packed, 11)), "the second block copies from the first");
    }

    @Test
    void decompress_givesNothingForNothing() {
        assertArrayEquals(new byte[0], Lz4.decompress(new byte[0], 0));
    }

    @Test
    void decompress_refusesACopyFromBeforeItsStart() {
        assertThrows(ModelFormatException.class, () -> Lz4.decompress(bytes(0, 0x10, 'a', 5, 0), 8));
    }

    @Test
    void decompress_refusesABlockThatRunsPastItsData() {
        assertThrows(ModelFormatException.class, () -> Lz4.decompress(bytes(1, 10, 0, 0, 0, 0x10, 'a'), 1));
    }

    private static byte[] bytes(final int... values) {
        final byte[] out = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            out[i] = (byte) values[i];
        }
        return out;
    }

    private static String text(final byte[] bytes) {
        return new String(bytes, StandardCharsets.US_ASCII);
    }
}
