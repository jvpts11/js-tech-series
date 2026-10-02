/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.core.audio.media.MediaId;
import dev.jstech.core.network.DataLine;
import dev.jstech.core.network.DataLink;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.core.text.Text;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SongDownloadTest {

    private SongDownload download;

    private static final long BYTES = 10_000L;
    private static final DataLink ETHERNET = new DataLink(DataLine.ACCESS, HardwareEra.LEGACY);
    private static final DataLink HBW = new DataLink(DataLine.BACKBONE, HardwareEra.LEGACY);
    private static final DataLink HPC = new DataLink(DataLine.HPC, HardwareEra.STANDARD);

    @BeforeEach
    void setUp() {
        download = new SongDownload("song.ogg", new MediaId("a".repeat(MediaId.HASH_DIGITS), "ogg", BYTES), 42L,
                "Users/Public/Music/Shared/song.ogg", "studio", true);
    }

    @Test
    void advance_bringsInWhatArrivesAtThatSpeedAndNeverPastTheEnd() {
        download.advance(1_000L, 500L);
        assertEquals(500L, download.done());
        download.advance(1_000_000L, 1_000L);
        assertEquals(BYTES, download.done());
        assertTrue(download.complete());
    }

    @Test
    void advance_bringsNothingInWhileItWaits() {
        download.unreachable();
        download.advance(1_000L, 1_000L);
        assertEquals(0L, download.done());
        assertEquals(SongDownload.Status.WAITING, download.status());
        download.reached(HBW);
        assertEquals(SongDownload.Status.RUNNING, download.status(), "a way back makes it come in again");
        assertEquals(HBW, download.link());
    }

    @Test
    void millisLeft_isWhatIsLeftAtThatSpeedRoundedUp() {
        download.advance(1_000L, 2_500L);
        assertEquals(7_500L, download.millisLeft(1_000L));
        assertEquals(2_500L, download.millisLeft(3_000L));
        download.unreachable();
        assertEquals(-1L, download.millisLeft(1_000L), "a waiting song has no time left to tell");
    }

    @Test
    void failed_saysWhyAndStaysFinished() {
        download.failed(Text.literal("gone"));
        assertEquals(SongDownload.Status.FAILED, download.status());
        assertEquals("gone", download.trouble().english());
        download.reached(ETHERNET);
        assertEquals(SongDownload.Status.FAILED, download.status(), "a way back does not bring a failed song back");
    }

    @Test
    void restore_putsBackHowItStoodWithinItsSize() {
        download.restore(BYTES * 3, SongDownload.Status.WAITING, HPC, Text.EMPTY);
        assertEquals(BYTES, download.done());
        assertEquals(SongDownload.Status.WAITING, download.status());
        assertEquals(HPC, download.link());
    }
}
