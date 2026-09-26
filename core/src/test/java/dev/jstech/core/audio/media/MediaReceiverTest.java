/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio.media;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MediaReceiverTest {

    private static final byte[] SONG = "a song in three pieces".getBytes(StandardCharsets.US_ASCII);

    @Test
    void accept_piecesInOrderMakeTheRecording() throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final MediaReceiver receiver = new MediaReceiver(MediaId.of(SONG, "ogg"), 3, out);
        receiver.accept(0, slice(0, 7));
        receiver.accept(1, slice(7, 14));
        assertFalse(receiver.complete());
        receiver.accept(2, slice(14, SONG.length));
        assertTrue(receiver.verified());
        assertArrayEquals(SONG, out.toByteArray());
    }

    @Test
    void accept_refusesAPieceOutOfPlace() throws IOException {
        final MediaReceiver receiver = new MediaReceiver(MediaId.of(SONG, "ogg"), 3, new ByteArrayOutputStream());
        receiver.accept(0, slice(0, 7));
        assertThrows(IOException.class, () -> receiver.accept(2, slice(14, SONG.length)));
    }

    @Test
    void accept_refusesMoreBytesThanTheRecordingHas() {
        final MediaReceiver receiver = new MediaReceiver(MediaId.of(SONG, "ogg"), 1, new ByteArrayOutputStream());
        final byte[] longer = new byte[SONG.length + 1];
        assertThrows(IOException.class, () -> receiver.accept(0, longer));
    }

    @Test
    void verified_isFalseForOtherBytesOfTheSameLength() throws IOException {
        final MediaReceiver receiver = new MediaReceiver(MediaId.of(SONG, "ogg"), 1, new ByteArrayOutputStream());
        final byte[] other = SONG.clone();
        other[0] ^= 1;
        receiver.accept(0, other);
        assertTrue(receiver.complete());
        assertFalse(receiver.verified(), "bytes that are not the ones named are not the recording");
    }

    private static byte[] slice(final int from, final int to) {
        final byte[] out = new byte[to - from];
        System.arraycopy(SONG, from, out, 0, out.length);
        return out;
    }
}
