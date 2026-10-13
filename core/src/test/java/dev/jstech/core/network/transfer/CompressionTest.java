/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.network.transfer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class CompressionTest {

    @Test
    void pack_makesRepeatsSmallAndUnpacksToTheSameBytes() throws IOException {
        final byte[] bytes = "the same line again ".repeat(500).getBytes(StandardCharsets.UTF_8);

        final byte[] packed = Compression.pack(bytes);

        assertTrue(packed.length < bytes.length / 10, "a repeating line packs small: " + packed.length);
        assertArrayEquals(bytes, Compression.unpack(packed, bytes.length));
    }

    @Test
    void pack_ofNothingUnpacksToNothing() throws IOException {
        assertArrayEquals(new byte[0], Compression.unpack(Compression.pack(new byte[0]), 0));
    }

    @Test
    void unpack_refusesWhatUnpacksPastTheLimit() {
        final byte[] packed = Compression.pack(new byte[1_000_000]);

        assertThrows(IOException.class, () -> Compression.unpack(packed, 1_000));
    }

    @Test
    void unpack_refusesBytesThatAreNotPacked() {
        final byte[] noise = new byte[64];
        Arrays.fill(noise, (byte) 0x5A);

        assertThrows(IOException.class, () -> Compression.unpack(noise, 1_000));
    }
}
