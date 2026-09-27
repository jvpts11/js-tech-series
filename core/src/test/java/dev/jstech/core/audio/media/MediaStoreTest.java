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
import java.util.Set;
import java.util.UUID;
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
    private static final UUID PLAYER = new UUID(7L, 7L);
    private static final long DAY = 24L * 60L * 60L * 1000L;

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
    void has_isFalseForTheRightHashWithAnotherSize(@TempDir final Path root) throws IOException {
        final MediaStore store = new MediaStore(root);
        final MediaId kept = store.put(wav(RATE), "wav");
        assertTrue(store.has(kept));
        assertFalse(store.has(new MediaId(kept.hash(), kept.format(), 1L)),
                "a name that says the song is one byte long is not the song");
    }

    @Test
    void sweepIncoming_clearsWhatAStoppedServerLeftHalfArrived(@TempDir final Path root) throws IOException {
        final MediaStore store = new MediaStore(root);
        Files.write(store.newIncoming(MediaId.of(new byte[] {1}, "wav")), new byte[] {1});
        store.sweepIncoming();
        assertEquals(0, countFiles(root));
    }

    @Test
    void prune_takesOutWhatNothingUsedAndGivesTheRoomBack(@TempDir final Path root) throws IOException {
        final long[] now = {0L};
        final MediaStore store = new MediaStore(root, () -> now[0]);
        final MediaId old = store.put(wav(RATE), "wav");
        store.broughtBy(old, PLAYER);
        now[0] = 10 * DAY;
        final MediaId recent = store.put(wav(RATE * 2), "wav");
        final MediaStore.Held pruned = store.prune(5 * DAY, Set.of());
        assertEquals(1, pruned.count());
        assertEquals(old.bytes(), pruned.bytes());
        assertFalse(store.has(old), "the recording nothing used is gone");
        assertTrue(store.has(recent), "and the one used lately stays");
        assertEquals(0L, store.broughtBytes(PLAYER), "whoever brought it has its room back");
    }

    @Test
    void prune_leavesWhatAModStillNeeds(@TempDir final Path root) throws IOException {
        final long[] now = {0L};
        final MediaStore store = new MediaStore(root, () -> now[0]);
        final MediaId offered = store.put(wav(RATE), "wav");
        now[0] = 100 * DAY;
        assertEquals(0, store.prune(DAY, Set.of(offered)).count());
        assertTrue(store.has(offered));
    }

    @Test
    void flush_keepsWhoBroughtWhatForTheNextStart(@TempDir final Path root) throws IOException {
        final MediaStore store = new MediaStore(root);
        final MediaId song = store.put(wav(RATE), "wav");
        store.broughtBy(song, PLAYER);
        store.flush();
        final MediaStore again = new MediaStore(root);
        assertEquals(song.bytes(), again.broughtBytes(PLAYER));
        assertEquals(1, again.size().count());
    }

    @Test
    void newStore_writesDownRecordingsKeptBeforeThereWasALedger(@TempDir final Path root) throws IOException {
        final MediaStore first = new MediaStore(root);
        first.put(wav(RATE), "wav");
        first.put(wav(RATE * 3), "wav");
        final MediaStore.Held held = new MediaStore(root).size();
        assertEquals(2, held.count());
        assertEquals(wav(RATE).length + wav(RATE * 3).length, held.bytes());
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
