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

/** What Soundfoundry's window says: the words on its skin, its menus and its dialogs. */
@TextHolder
final class SoundfoundryAppTexts {

    /* The words on the skin, which the players of the time wrote in capitals. */
    static final TextKey PLAYER = TextKey.of("jsc.soundfoundry.skin.player", "SOUNDFOUNDRY");
    static final TextKey PLAYLIST = TextKey.of("jsc.soundfoundry.skin.playlist", "SOUNDFOUNDRY PLAYLIST");
    static final TextKey NO_SOUND = TextKey.of("jsc.soundfoundry.skin.no_sound", "NO SOUND");
    static final TextKey DEVICE = TextKey.of("jsc.soundfoundry.skin.device", "DEVICE");
    static final TextKey NO_CARD = TextKey.of("jsc.soundfoundry.skin.no_card", "NO SOUND CARD INSTALLED");
    static final TextKey KBPS = TextKey.of("jsc.soundfoundry.skin.kbps", "kbps");
    static final TextKey KHZ = TextKey.of("jsc.soundfoundry.skin.khz", "kHz");
    static final TextKey MONO = TextKey.of("jsc.soundfoundry.skin.mono", "MONO");
    static final TextKey STEREO = TextKey.of("jsc.soundfoundry.skin.stereo", "STEREO");
    static final TextKey PL = TextKey.of("jsc.soundfoundry.skin.pl", "PL");
    static final TextKey NET = TextKey.of("jsc.soundfoundry.skin.net", "NET");
    static final TextKey SHUFFLE = TextKey.of("jsc.soundfoundry.skin.shuffle", "SHUFFLE");
    static final TextKey REPEAT = TextKey.of("jsc.soundfoundry.skin.repeat", "REPEAT");
    static final TextKey OUT_NONE = TextKey.of("jsc.soundfoundry.skin.out_none", "OUT  -");
    static final TextKey OUT_MONITOR = TextKey.of("jsc.soundfoundry.skin.out_monitor", "OUT MON");
    static final TextKey OUT_SPEAKERS = TextKey.of("jsc.soundfoundry.skin.out_speakers", "OUT SPK");
    static final TextKey OUT_BOTH = TextKey.of("jsc.soundfoundry.skin.out_both", "OUT MON+SPK");
    static final TextKey ADD = TextKey.of("jsc.soundfoundry.skin.add", "ADD");
    static final TextKey REMOVE = TextKey.of("jsc.soundfoundry.skin.remove", "REM");
    static final TextKey SELECT = TextKey.of("jsc.soundfoundry.skin.select", "SEL");
    static final TextKey MISC = TextKey.of("jsc.soundfoundry.skin.misc", "MISC");
    static final TextKey LIST = TextKey.of("jsc.soundfoundry.skin.list", "LIST");
    /** What the song display scrolls while nothing is chosen. */
    static final TextKey IDLE = TextKey.of("jsc.soundfoundry.skin.idle", "SOUNDFOUNDRY %s");
    /** A song on the display: its place, what it is listed as and how long it runs. */
    static final TextKey SONG = TextKey.of("jsc.soundfoundry.skin.song", "%s. %s (%s)");
    static final TextKey ROW = TextKey.of("jsc.soundfoundry.skin.row", "%s. %s");
    static final TextKey BY = TextKey.of("jsc.soundfoundry.skin.by", "%s - %s");
    static final TextKey IMPORTING = TextKey.of("jsc.soundfoundry.skin.importing", "IMPORTING %s OF %s: %s%%");

    /* The menus. */
    static final TextKey OPEN_FILE = TextKey.of("jsc.soundfoundry.menu.open_file", "Open File...");
    static final TextKey OPEN_FOLDER = TextKey.of("jsc.soundfoundry.menu.open_folder", "Open Folder...");
    static final TextKey IMPORT = TextKey.of("jsc.soundfoundry.menu.import", "Import from Your Computer...");
    static final TextKey ADD_FILE = TextKey.of("jsc.soundfoundry.menu.add_file", "Add File...");
    static final TextKey ADD_FOLDER = TextKey.of("jsc.soundfoundry.menu.add_folder", "Add Folder...");
    static final TextKey REMOVE_SELECTED = TextKey.of("jsc.soundfoundry.menu.remove_selected", "Remove Selected");
    static final TextKey CROP = TextKey.of("jsc.soundfoundry.menu.crop", "Keep Only Selected");
    static final TextKey CLEAR = TextKey.of("jsc.soundfoundry.menu.clear", "Clear Playlist");
    static final TextKey SELECT_ALL = TextKey.of("jsc.soundfoundry.menu.select_all", "Select All");
    static final TextKey SELECT_NONE = TextKey.of("jsc.soundfoundry.menu.select_none", "Select None");
    static final TextKey INVERT = TextKey.of("jsc.soundfoundry.menu.invert", "Invert Selection");
    static final TextKey SORT_TITLE = TextKey.of("jsc.soundfoundry.menu.sort_title", "Sort by Title");
    static final TextKey SORT_FILE = TextKey.of("jsc.soundfoundry.menu.sort_file", "Sort by File Name");
    static final TextKey REVERSE = TextKey.of("jsc.soundfoundry.menu.reverse", "Reverse List");
    static final TextKey NEW_LIST = TextKey.of("jsc.soundfoundry.menu.new_list", "New Playlist");
    static final TextKey OPEN_LIST = TextKey.of("jsc.soundfoundry.menu.open_list", "Open Playlist...");
    static final TextKey SAVE_LIST = TextKey.of("jsc.soundfoundry.menu.save_list", "Save Playlist...");

    /* The dialogs. */
    static final TextKey OPEN_TITLE = TextKey.of("jsc.soundfoundry.dialog.open", "Open Song");
    static final TextKey FOLDER_TITLE = TextKey.of("jsc.soundfoundry.dialog.folder", "Open Folder");
    static final TextKey LIST_TITLE = TextKey.of("jsc.soundfoundry.dialog.list", "Open Playlist");
    static final TextKey SAVE_TITLE = TextKey.of("jsc.soundfoundry.dialog.save", "Save Playlist");
    static final TextKey SONGS = TextKey.of("jsc.soundfoundry.dialog.songs", "Songs");
    static final TextKey PLAYLISTS = TextKey.of("jsc.soundfoundry.dialog.playlists", "Playlists");

    private SoundfoundryAppTexts() {
    }
}
