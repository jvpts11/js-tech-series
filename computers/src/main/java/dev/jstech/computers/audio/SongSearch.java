/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.audio;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * What a player searched Soundfoundry's network for: every word they typed has to be found somewhere in what is known
 * of a song, its title, its artist, its album or its file's name, in any case and any order. Nothing typed finds
 * every song.
 *
 * <p>This class is pure and carries no Minecraft dependency.
 */
public final class SongSearch {

    private final List<String> words = new ArrayList<>();

    /** The longest search a player can type. */
    public static final int MAX_QUERY = 64;

    public SongSearch(final String query) {
        final String cut = query.length() <= MAX_QUERY ? query : query.substring(0, MAX_QUERY);
        for (final String word : cut.toLowerCase(Locale.ROOT).split("\\s+")) {
            if (!word.isEmpty()) {
                words.add(word);
            }
        }
    }

    /** Whether nothing was typed, which finds every song. */
    public boolean everything() {
        return words.isEmpty();
    }

    /** Whether every word typed is found in what is known of a song. */
    public boolean matches(final String... about) {
        if (words.isEmpty()) {
            return true;
        }
        final String known = String.join(" ", about).toLowerCase(Locale.ROOT);
        for (final String word : words) {
            if (!known.contains(word)) {
                return false;
            }
        }
        return true;
    }
}
