/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.audio.catalog;

import java.util.List;

/**
 * An album in the server's catalogue: a folder of songs the server owner put in the catalogue's folder or a data pack
 * carries, listed under a title, an artist and a year.
 *
 * <p>This record is pure and carries no Minecraft dependency.
 *
 * @param id     where it comes from and its folder, which tells two albums of the same name apart: {@code config/One-X}
 *               for one in the server's folder, {@code <namespace>/<folder>} for one a data pack carries
 * @param title  its title
 * @param artist who made it, or empty when nothing says
 * @param year   the year it came out, or empty when nothing says
 * @param tracks its songs, in the order they are played
 */
public record CatalogAlbum(String id, String title, String artist, String year, List<CatalogTrack> tracks) {

    public CatalogAlbum {
        tracks = List.copyOf(tracks);
    }

    /** How long the whole album runs, in milliseconds. */
    public long millis() {
        long total = 0L;
        for (final CatalogTrack track : tracks) {
            total += track.info().millis();
        }
        return total;
    }
}
