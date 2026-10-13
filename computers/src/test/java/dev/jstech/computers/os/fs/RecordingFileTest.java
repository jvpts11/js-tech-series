/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.fs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.core.audio.media.MediaId;
import dev.jstech.core.audio.media.MediaInfo;
import dev.jstech.core.audio.media.MediaTags;
import org.junit.jupiter.api.Test;

class RecordingFileTest {

    private static final String HASH = "0123456789abcdef".repeat(4);

    @Test
    void write_readsBackAsTheSameRecording() {
        final RecordingFile song = song(new MediaTags("Never Too Late", "Three Days Grace", "One-X", "2006", 3));
        assertEquals(song, RecordingFile.read(song.write()));
    }

    @Test
    void write_keepsEachValueToOneLine() {
        final RecordingFile song = song(new MediaTags("Two\nLines", "", "", "", 0));
        final RecordingFile back = RecordingFile.read(song.write());
        assertNotNull(back);
        assertEquals("Two Lines", back.info().tags().title());
        assertEquals(song.media(), back.media());
    }

    @Test
    void read_needsOnlyTheRecordingsName() {
        final RecordingFile back = RecordingFile.read(RecordingFile.MAGIC + "\nmedia " + HASH + ".wav 4410\n");
        assertNotNull(back);
        assertEquals(new MediaId(HASH, "wav", 4410L), back.media());
        assertEquals(0L, back.info().millis());
        assertTrue(back.info().tags().isEmpty());
    }

    @Test
    void read_isNullForWhatIsNotARecordingFile() {
        assertNull(RecordingFile.read(null));
        assertNull(RecordingFile.read("just some notes"));
        assertNull(RecordingFile.read(Archive.MAGIC + "\n"));
        assertNull(RecordingFile.read(RecordingFile.MAGIC + "\ntitle Nothing named\n"));
        assertNull(RecordingFile.read(RecordingFile.MAGIC + "\nmedia nothex.ogg 10\n"));
        assertNull(RecordingFile.read(RecordingFile.MAGIC + "\nmedia " + HASH + ".ogg many\n"));
    }

    @Test
    void bytes_isWhatTheRecordingWeighs() {
        assertEquals(5_242_880L, song(MediaTags.EMPTY).bytes());
    }

    @Test
    void stemOf_keepsTheNameWithoutItsFoldersOrExtension() {
        assertEquals("01 - Animal I Have Become",
                RecordingFile.stemOf("C:\\Music\\01 - Animal I Have Become.ogg", "x"));
        assertEquals("song", RecordingFile.stemOf("/home/me/song.wav", "x"));
        assertEquals(".hidden", RecordingFile.stemOf(".hidden", "x"));
    }

    @Test
    void stemOf_dropsControlCharactersAndShortensALongName() {
        assertEquals("ab", RecordingFile.stemOf("a\u0007b.ogg", "x"));
        final String stem = RecordingFile.stemOf("n".repeat(200) + ".ogg", "x");
        assertTrue(stem.length() + ".ogg_99".length() <= FsPaths.MAX_NAME_LENGTH, "room is left for a number");
    }

    @Test
    void stemOf_fallsBackWhenNothingOfTheNameIsLeft() {
        assertEquals("Track", RecordingFile.stemOf("   .ogg", "Track"));
        assertEquals("Track", RecordingFile.stemOf(null, "Track"));
    }

    private static RecordingFile song(final MediaTags tags) {
        return new RecordingFile(new MediaId(HASH, "ogg", 5_242_880L), new MediaInfo(245_000L, 44_100, 2, 160, tags));
    }
}
