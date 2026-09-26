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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MediaStoreTest {

    private static final int RATE = 8000;

    @Test
    void put_keepsARecordingOnceUnderItsHash(@TempDir final Path root) throws IOException {
        final MediaStore store = new MediaStore(root);
        final byte[] wav = wav(RATE);
        final MediaId first = store.put(wav, "wav");
        final MediaId again = store.put(wav, "wav");
        assertEquals(first, again, "the same bytes are the same recording");
        assertTrue(store.has(first));
        assertArrayEquals(wav, Files.readAllBytes(store.path(first)));
        assertEquals(1000, store.info(first).millis());
        assertEquals(1, countFiles(root), "kept once, with nothing left in the incoming folder");
    }

    @Test
    void put_refusesAFileThatIsNotWhatItsKindSays(@TempDir final Path root) throws IOException {
        final MediaStore store = new MediaStore(root);
        final byte[] notOgg = "this is not a recording".getBytes(StandardCharsets.US_ASCII);
        assertThrows(IOException.class, () -> store.put(notOgg, "ogg"));
        assertFalse(store.has(MediaId.of(notOgg, "ogg")));
        assertEquals(0, countFiles(root));
    }

    @Test
    void adopt_takesAnArrivedFileIntoPlace(@TempDir final Path root) throws IOException {
        final MediaStore store = new MediaStore(root);
        final byte[] wav = wav(RATE * 2);
        final MediaId id = MediaId.of(wav, "wav");
        final Path arrived = store.newIncoming(id);
        Files.write(arrived, wav);
        store.adopt(arrived, id);
        assertTrue(store.has(id));
        assertFalse(Files.exists(arrived), "the incoming file is gone once it is in place");
        assertEquals(2000, store.info(id).millis());
    }

    @Test
    void sweepIncoming_clearsWhatAStoppedServerLeftHalfArrived(@TempDir final Path root) throws IOException {
        final MediaStore store = new MediaStore(root);
        Files.write(store.newIncoming(MediaId.of(new byte[] {1}, "wav")), new byte[] {1});
        store.sweepIncoming();
        assertEquals(0, countFiles(root));
    }

    /* Every file under the store, the incoming folder's included. */
    private static long countFiles(final Path root) throws IOException {
        try (Stream<Path> all = Files.walk(root)) {
            return all.filter(Files::isRegularFile).count();
        }
    }

    /* A mono 16-bit WAV of that many frames of silence. */
    private static byte[] wav(final int frames) {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final int data = frames * 2;
        out.writeBytes("RIFF".getBytes(StandardCharsets.US_ASCII));
        le32(out, 36 + data);
        out.writeBytes("WAVEfmt ".getBytes(StandardCharsets.US_ASCII));
        le32(out, 16);
        le16(out, 1);
        le16(out, 1);
        le32(out, RATE);
        le32(out, RATE * 2);
        le16(out, 2);
        le16(out, 16);
        out.writeBytes("data".getBytes(StandardCharsets.US_ASCII));
        le32(out, data);
        out.writeBytes(new byte[data]);
        return out.toByteArray();
    }

    private static void le16(final ByteArrayOutputStream out, final int value) {
        out.write(value & 0xFF);
        out.write(value >> 8 & 0xFF);
    }

    private static void le32(final ByteArrayOutputStream out, final int value) {
        le16(out, value & 0xFFFF);
        le16(out, value >>> 16);
    }
}
