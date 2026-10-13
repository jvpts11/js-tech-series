/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SongRefsTest {

    @Test
    void catalog_namesTheAlbumAndFileAndReadsThemBack() {
        final String ref = SongRefs.catalog("config/one_x", "02 Pain.ogg");
        assertTrue(SongRefs.streamed(ref) && SongRefs.fromCatalog(ref) && !SongRefs.fromNetwork(ref));
        assertEquals("config/one_x/02 Pain.ogg", SongRefs.pathOf(ref));
    }

    @Test
    void network_namesThePathOnTheServer() {
        final String ref = SongRefs.network("Users/Public/Music/ATTIC-PC/3 AM Backup.ogg");
        assertTrue(SongRefs.streamed(ref) && SongRefs.fromNetwork(ref));
        assertEquals("Users/Public/Music/ATTIC-PC/3 AM Backup.ogg", SongRefs.pathOf(ref));
    }

    @Test
    void streamed_isFalseForAFileOnTheDisk() {
        assertFalse(SongRefs.streamed("Users/Public/Music/Pain.ogg"));
        assertFalse(SongRefs.streamed("/home/user/Music/Pain.ogg"));
        assertEquals("Users/Public/Music/Pain.ogg", SongRefs.pathOf("Users/Public/Music/Pain.ogg"));
    }
}
