/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.fs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.jstech.core.audio.media.MediaId;
import dev.jstech.core.audio.media.MediaInfo;
import dev.jstech.core.audio.media.MediaTags;
import dev.jstech.core.tier.HardwareEra;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class StoredFileTest {

    private static final long SONG_BYTES = 5L * 1024L * 1024L;

    @Test
    void byteSize_isATextFilesText() {
        assertEquals(5L, new StoredFile("a.txt", FileType.TXT, "hello").byteSize());
    }

    @Test
    void byteSize_ofARecordingIsTheRecordingItNames() {
        assertEquals(SONG_BYTES, song("song.ogg").byteSize());
    }

    @Test
    void byteSize_ofARecordingFileThatNamesNothingIsItsText() {
        assertEquals(4L, new StoredFile("song.ogg", FileType.OGG, "junk").byteSize());
    }

    @Test
    void byteSize_ofAnArchiveAddsTheRecordingsPackedInIt() {
        final String packed = Archive.pack(List.of(song("song.ogg"), new StoredFile("a.txt", FileType.TXT, "hello")));
        final long text = packed.getBytes(StandardCharsets.UTF_8).length;
        assertEquals(text + SONG_BYTES, new StoredFile("box.ark", FileType.ARK, packed).byteSize());
    }

    @Test
    void weight_ofASongFollowsTheDisksEra() {
        final StoredFile song = song("song.ogg");
        assertEquals(FsPaths.sizeMbEq(SONG_BYTES, HardwareEra.VINTAGE), song.weight(HardwareEra.VINTAGE));
        assertEquals(313L, song.weight(HardwareEra.LEGACY));
        assertEquals(20L, song.weight(HardwareEra.STANDARD));
    }

    private static StoredFile song(final String path) {
        final RecordingFile recording = new RecordingFile(new MediaId("ab".repeat(32), "ogg", SONG_BYTES),
                new MediaInfo(300_000L, 44_100, 2, 128, MediaTags.EMPTY));
        return new StoredFile(path, FileType.OGG, recording.write());
    }
}
