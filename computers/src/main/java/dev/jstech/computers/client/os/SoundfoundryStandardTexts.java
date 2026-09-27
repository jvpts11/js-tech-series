/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** What the Standard Soundfoundry's window says. */
@TextHolder
final class SoundfoundryStandardTexts {

    static final TextKey HOME = TextKey.of("jsc.soundfoundry.standard.home", "Home");
    static final TextKey SEARCH = TextKey.of("jsc.soundfoundry.standard.search", "Search");
    static final TextKey LIBRARY = TextKey.of("jsc.soundfoundry.standard.library", "Your Library");
    static final TextKey PLAYLISTS = TextKey.of("jsc.soundfoundry.standard.playlists", "PLAYLISTS");
    static final TextKey LIKED = TextKey.of("jsc.soundfoundry.standard.liked", "Liked Songs");
    static final TextKey NEW_PLAYLIST = TextKey.of("jsc.soundfoundry.standard.new_playlist", "My Playlist #%s");
    static final TextKey MORNING = TextKey.of("jsc.soundfoundry.standard.morning", "Good morning");
    static final TextKey AFTERNOON = TextKey.of("jsc.soundfoundry.standard.afternoon", "Good afternoon");
    static final TextKey EVENING = TextKey.of("jsc.soundfoundry.standard.evening", "Good evening");
    static final TextKey FROM_CATALOG = TextKey.of("jsc.soundfoundry.standard.from_catalog", "From the catalog");
    static final TextKey SHOW_ALL = TextKey.of("jsc.soundfoundry.standard.show_all", "Show all");
    static final TextKey ON_NETWORK = TextKey.of("jsc.soundfoundry.standard.on_network", "On your network");
    static final TextKey DOWNLOADED = TextKey.of("jsc.soundfoundry.standard.downloaded", "Downloaded");
    static final TextKey FROM = TextKey.of("jsc.soundfoundry.standard.from", "%s, from %s");
    static final TextKey FROM_ALONE = TextKey.of("jsc.soundfoundry.standard.from_alone", "From %s");
    static final TextKey ALBUM = TextKey.of("jsc.soundfoundry.standard.album", "ALBUM");
    static final TextKey PLAYLIST = TextKey.of("jsc.soundfoundry.standard.playlist", "PLAYLIST");
    static final TextKey ALBUM_META = TextKey.of("jsc.soundfoundry.standard.album_meta", "%s, %s, %s songs, %s min");
    static final TextKey ALBUM_META_UNDATED = TextKey.of("jsc.soundfoundry.standard.album_meta_undated",
            "%s, %s songs, %s min");
    static final TextKey SONGS = TextKey.of("jsc.soundfoundry.standard.songs", "%s songs");
    static final TextKey CATALOG = TextKey.of("jsc.soundfoundry.standard.catalog", "Voidsoft Music catalog");
    static final TextKey DOWNLOAD_ALBUM = TextKey.of("jsc.soundfoundry.standard.download_album", "Download album");
    static final TextKey DOWNLOADED_OF = TextKey.of("jsc.soundfoundry.standard.downloaded_of", "%s of %s downloaded");
    static final TextKey COLUMN_TITLE = TextKey.of("jsc.soundfoundry.standard.column_title", "TITLE");
    static final TextKey COLUMN_ARTIST = TextKey.of("jsc.soundfoundry.standard.column_artist", "ARTIST");
    static final TextKey COLUMN_TIME = TextKey.of("jsc.soundfoundry.standard.column_time", "TIME");
    static final TextKey LOCAL = TextKey.of("jsc.soundfoundry.standard.local", "Local files");
    static final TextKey NO_SERVER = TextKey.of("jsc.soundfoundry.standard.no_server",
            "No Soundfoundry Server on this network.");
    static final TextKey NO_SERVER_HOW = TextKey.of("jsc.soundfoundry.standard.no_server_how",
            "Install Soundfoundry Server on a Server to stream the catalog");
    static final TextKey NO_SERVER_HOW_2 = TextKey.of("jsc.soundfoundry.standard.no_server_how_2",
            "and your network's library.");
    static final TextKey IMPORT = TextKey.of("jsc.soundfoundry.standard.import", "Import from Your Computer...");
    static final TextKey UPLOAD = TextKey.of("jsc.soundfoundry.standard.upload", "Upload to network");
    static final TextKey OUT = TextKey.of("jsc.soundfoundry.standard.out", "OUT");
    static final TextKey ONLINE = TextKey.of("jsc.soundfoundry.standard.online", "Online, %s songs");
    static final TextKey NO_SERVER_BOX = TextKey.of("jsc.soundfoundry.standard.no_server_box", "No Soundfoundry");
    static final TextKey NO_SERVER_BOX_2 = TextKey.of("jsc.soundfoundry.standard.no_server_box_2", "Server found");
    static final TextKey SERVER_CHOICE = TextKey.of("jsc.soundfoundry.standard.server_choice", "%s, %s songs");
    static final TextKey SEARCH_HINT = TextKey.of("jsc.soundfoundry.standard.search_hint",
            "What do you want to listen to?");
    static final TextKey NOTHING_FOUND = TextKey.of("jsc.soundfoundry.standard.nothing_found",
            "Nothing found for \"%s\"");
    static final TextKey EMPTY_PLAYLIST = TextKey.of("jsc.soundfoundry.standard.empty_playlist",
            "Songs you add to this playlist show up here.");
    static final TextKey NO_LOCAL = TextKey.of("jsc.soundfoundry.standard.no_local",
            "No songs on this computer yet.");
    static final TextKey LIKE = TextKey.of("jsc.soundfoundry.standard.like", "Save to Liked Songs");
    static final TextKey UNLIKE = TextKey.of("jsc.soundfoundry.standard.unlike", "Remove from Liked Songs");
    static final TextKey ADD_TO = TextKey.of("jsc.soundfoundry.standard.add_to", "Add to %s");
    static final TextKey REMOVE_FROM = TextKey.of("jsc.soundfoundry.standard.remove_from",
            "Remove from this playlist");
    static final TextKey DOWNLOAD = TextKey.of("jsc.soundfoundry.standard.download", "Download");
    static final TextKey DELETE_PLAYLIST = TextKey.of("jsc.soundfoundry.standard.delete_playlist", "Delete playlist");
    static final TextKey IMPORTING = TextKey.of("jsc.soundfoundry.standard.importing", "Importing %s of %s: %s%%");

    private SoundfoundryStandardTexts() {
    }
}
