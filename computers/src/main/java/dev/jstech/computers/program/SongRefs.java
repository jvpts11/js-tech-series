/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program;

/**
 * How a song Soundfoundry plays is named on its list: a file on the machine's disks by its path, a song of the
 * server's catalogue as {@code catalog://<album>/<file>}, or a song of the network's library, kept by a Soundfoundry
 * Server, as {@code network://<path on the server>}. The two streamed kinds play through a Soundfoundry Server on the
 * network, unless the machine keeps the same recording on its own disk.
 *
 * <p>No path on a disk starts with either prefix: a path's folders are never empty, so it never holds a
 * {@code //}.
 *
 * <p>This class is pure and carries no Minecraft dependency.
 */
public final class SongRefs {

    /** What a song of the catalogue is named with, before its album and file. */
    public static final String CATALOG = "catalog://";
    /** What a song of the network's library is named with, before its path on the server. */
    public static final String NETWORK = "network://";

    private SongRefs() {
    }

    /** A song of the catalogue, by its album and its file. */
    public static String catalog(final String album, final String file) {
        return CATALOG + album + "/" + file;
    }

    /** A song of the network's library, by its path on the server keeping it. */
    public static String network(final String path) {
        return NETWORK + path;
    }

    /** Whether the song plays through a Soundfoundry Server rather than from a file on the machine's disks. */
    public static boolean streamed(final String ref) {
        return fromCatalog(ref) || fromNetwork(ref);
    }

    public static boolean fromCatalog(final String ref) {
        return ref.startsWith(CATALOG);
    }

    public static boolean fromNetwork(final String ref) {
        return ref.startsWith(NETWORK);
    }

    /**
     * What the name says where the song is: its album and file in the catalogue, its path on the server, or the path
     * of the file itself.
     */
    public static String pathOf(final String ref) {
        if (fromCatalog(ref)) {
            return ref.substring(CATALOG.length());
        }
        return fromNetwork(ref) ? ref.substring(NETWORK.length()) : ref;
    }
}
