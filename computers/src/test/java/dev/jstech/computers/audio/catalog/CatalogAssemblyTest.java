/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.audio.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.jstech.core.audio.media.MediaId;
import dev.jstech.core.audio.media.MediaInfo;
import dev.jstech.core.audio.media.MediaTags;
import java.util.List;
import org.junit.jupiter.api.Test;

class CatalogAssemblyTest {

    @Test
    void album_takesItsNamesFromItsNotesFirst() {
        final CatalogAlbum album = CatalogAssembly.album("config/x", "x",
                new CatalogAssembly.Notes("One-X", "Three Days Grace", "2006"),
                List.of(found("a.ogg", 1, new MediaTags("Song", "Somebody Else", "Other", "1999", 1))));
        assertEquals("One-X", album.title());
        assertEquals("Three Days Grace", album.artist());
        assertEquals("2006", album.year());
        assertEquals("Somebody Else", album.tracks().getFirst().artist(), "a song keeps its own artist");
    }

    @Test
    void album_takesWhatMostOfItsSongsSayWithoutNotes() {
        final CatalogAlbum album = CatalogAssembly.album("config/x", "x", CatalogAssembly.Notes.NONE, List.of(
                found("a.ogg", 1, new MediaTags("A", "Band", "Record", "2001", 1)),
                found("b.ogg", 2, new MediaTags("B", "Band", "Record", "2001", 2)),
                found("c.ogg", 3, new MediaTags("C", "Guest", "Single", "", 3))));
        assertEquals("Record", album.title());
        assertEquals("Band", album.artist());
        assertEquals("2001", album.year());
    }

    @Test
    void album_isNamedAfterItsFolderWhenNothingSays() {
        final CatalogAlbum album = CatalogAssembly.album("config/Road Trip", "Road Trip", CatalogAssembly.Notes.NONE,
                List.of(found("01 - Highway.ogg", 1, MediaTags.EMPTY)));
        assertEquals("Road Trip", album.title());
        assertEquals("", album.artist());
        final CatalogTrack track = album.tracks().getFirst();
        assertEquals("01 - Highway", track.title(), "a song that names itself nothing is listed by its file");
        assertEquals("", track.artist());
    }

    @Test
    void tracks_numberedOnesFirstInTheirOrderThenTheRestByFileName() {
        final CatalogAlbum album = CatalogAssembly.album("config/x", "x", CatalogAssembly.Notes.NONE, List.of(
                found("z.ogg", 1, new MediaTags("Zed", "", "", "", 0)),
                found("m.ogg", 2, new MediaTags("Second", "", "", "", 2)),
                found("b.ogg", 3, new MediaTags("Bee", "", "", "", 0)),
                found("y.ogg", 4, new MediaTags("First", "", "", "", 1))));
        assertEquals(List.of("First", "Second", "Bee", "Zed"),
                album.tracks().stream().map(CatalogTrack::title).toList());
    }

    @Test
    void millis_isTheWholeAlbum() {
        final CatalogAlbum album = CatalogAssembly.album("config/x", "x", CatalogAssembly.Notes.NONE, List.of(
                found("a.ogg", 1, MediaTags.EMPTY), found("b.ogg", 2, MediaTags.EMPTY)));
        assertEquals(2 * 60_000L, album.millis());
    }

    private static CatalogAssembly.Found found(final String file, final int salt, final MediaTags tags) {
        final String hash = String.format("%064x", salt);
        return new CatalogAssembly.Found(file, new MediaId(hash, "ogg", 1000L + salt),
                new MediaInfo(60_000L, 44_100, 2, 128, tags));
    }
}
