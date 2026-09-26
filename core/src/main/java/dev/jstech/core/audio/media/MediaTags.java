/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio.media;

/**
 * What a recording says about itself: its title, who made it, the album it is on, the year and its place on the album.
 * Any of them may be missing, which is an empty string, or a track of 0.
 *
 * @param title  the song's name
 * @param artist who made it
 * @param album  the album it is on
 * @param year   the year it came out, as four digits
 * @param track  its place on the album, counted from 1
 */
public record MediaTags(String title, String artist, String album, String year, int track) {

    /** The longest a tag is kept; a longer one is cut. */
    public static final int MAX_TEXT = 128;

    /** A recording that says nothing about itself. */
    public static final MediaTags EMPTY = new MediaTags("", "", "", "", 0);

    public MediaTags {
        title = clean(title);
        artist = clean(artist);
        album = clean(album);
        year = clean(year);
        track = Math.max(0, track);
    }

    /** Whether it says nothing at all. */
    public boolean isEmpty() {
        return title.isEmpty() && artist.isEmpty() && album.isEmpty() && year.isEmpty() && track == 0;
    }

    private static String clean(final String text) {
        final String trimmed = text == null ? "" : text.strip();
        return trimmed.length() > MAX_TEXT ? trimmed.substring(0, MAX_TEXT) : trimmed;
    }
}
