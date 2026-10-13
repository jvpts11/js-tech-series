/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.audio.catalog;

import dev.jstech.computers.os.fs.RecordingFile;
import dev.jstech.core.audio.media.MediaId;
import dev.jstech.core.audio.media.MediaInfo;
import dev.jstech.core.audio.media.MediaTags;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * Puts an album of the catalogue together from the songs found in its folder and what its notes say.
 *
 * <p>An owner drops files in a folder and expects to see an album, so nothing has to be filled in: the album's
 * title, artist and year are what its notes say, else what most of its songs say about themselves, else the folder's
 * name for the title. A song is listed under its own title, else its file's name, by its own artist, else the
 * album's. Songs that say their place on the album are played in that order, and the rest after them by file name.
 *
 * <p>This class is pure and carries no Minecraft dependency.
 */
public final class CatalogAssembly {

    private CatalogAssembly() {
    }

    /** One recording found in an album's folder, before the album is put together. */
    public record Found(String file, MediaId media, MediaInfo info) {
    }

    /**
     * What an album's own notes say about it, each part empty when they do not say.
     *
     * @param title  the album's title
     * @param artist who made it
     * @param year   the year it came out
     */
    public record Notes(String title, String artist, String year) {

        /** Notes that say nothing. */
        public static final Notes NONE = new Notes("", "", "");

        public Notes {
            title = clean(title);
            artist = clean(artist);
            year = clean(year);
        }
    }

    /**
     * The album from the songs found in {@code folder}.
     *
     * @param id where it comes from and its folder; see {@link CatalogAlbum#id()}
     */
    public static CatalogAlbum album(final String id, final String folder, final Notes notes,
                                     final List<Found> found) {
        final List<Found> byFile = new ArrayList<>(found);
        byFile.sort(Comparator.comparing(one -> one.file().toLowerCase(Locale.ROOT)));
        final String title = either(notes.title(), mostSaid(byFile, MediaTags::album), folder);
        final String artist = either(notes.artist(), mostSaid(byFile, MediaTags::artist), "");
        final String year = either(notes.year(), mostSaid(byFile, MediaTags::year), "");
        final List<CatalogTrack> tracks = new ArrayList<>(byFile.size());
        for (final Found one : byFile) {
            final MediaTags tags = one.info().tags();
            tracks.add(new CatalogTrack(one.file(), one.media(), one.info(),
                    either(tags.title(), RecordingFile.stemOf(one.file(), one.file()), one.file()),
                    either(tags.artist(), artist, ""), tags.track()));
        }
        // Numbered songs first, in their order; a sort that keeps ties leaves the rest in the order of their files.
        tracks.sort(Comparator.comparingInt(CatalogAssembly::orderOf));
        return new CatalogAlbum(id, title, artist, year, tracks);
    }

    private static int orderOf(final CatalogTrack track) {
        return track.number() > 0 ? track.number() : Integer.MAX_VALUE;
    }

    /* What most of the songs say, the first of them winning a tie, or empty when none says anything. */
    private static String mostSaid(final List<Found> found, final Function<MediaTags, String> part) {
        final Map<String, Integer> counts = new LinkedHashMap<>();
        for (final Found one : found) {
            final String said = part.apply(one.info().tags());
            if (!said.isEmpty()) {
                counts.merge(said, 1, Integer::sum);
            }
        }
        String best = "";
        int most = 0;
        for (final Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > most) {
                best = entry.getKey();
                most = entry.getValue();
            }
        }
        return best;
    }

    private static String either(final String first, final String second, final String last) {
        if (!first.isEmpty()) {
            return first;
        }
        return second.isEmpty() ? last : second;
    }

    private static String clean(final String text) {
        final String trimmed = text == null ? "" : text.strip();
        return trimmed.length() > MediaTags.MAX_TEXT ? trimmed.substring(0, MediaTags.MAX_TEXT) : trimmed;
    }
}
