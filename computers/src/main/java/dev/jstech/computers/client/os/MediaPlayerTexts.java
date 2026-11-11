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

/** What the Media Player says, in every desktop's look. */
@TextHolder
final class MediaPlayerTexts {

    // Menus, as each player's bar names them.
    static final TextKey FILE = TextKey.of("jsc.media_player.menu.file", "File");
    static final TextKey EDIT = TextKey.of("jsc.media_player.menu.edit", "Edit");
    static final TextKey DEVICE = TextKey.of("jsc.media_player.menu.device", "Device");
    static final TextKey SCALE = TextKey.of("jsc.media_player.menu.scale", "Scale");
    static final TextKey HELP = TextKey.of("jsc.media_player.menu.help", "Help");
    static final TextKey VIEW = TextKey.of("jsc.media_player.menu.view", "View");
    static final TextKey PLAY = TextKey.of("jsc.media_player.menu.play", "Play");
    static final TextKey TOOLS = TextKey.of("jsc.media_player.menu.tools", "Tools");
    static final TextKey OPTIONS = TextKey.of("jsc.media_player.menu.options", "Options");
    static final TextKey WINDOW = TextKey.of("jsc.media_player.menu.window", "Window");
    // What a menu holds.
    static final TextKey OPEN = TextKey.of("jsc.media_player.open", "Open...");
    static final TextKey PLAY_PAUSE = TextKey.of("jsc.media_player.play_pause", "Play or pause");
    static final TextKey STOP = TextKey.of("jsc.media_player.stop", "Stop");
    static final TextKey SPEED = TextKey.of("jsc.media_player.speed", "Pictures a second...");
    static final TextKey CLOSE = TextKey.of("jsc.media_player.close", "Close");
    // The views and lists.
    static final TextKey NOW_PLAYING = TextKey.of("jsc.media_player.now_playing", "Now Playing");
    static final TextKey LIBRARY = TextKey.of("jsc.media_player.library", "Library");
    static final TextKey IMAGE_SEQUENCES = TextKey.of("jsc.media_player.image_sequences", "Image sequences");
    static final TextKey SONGS_HERE = TextKey.of("jsc.media_player.songs_here", "Songs on this computer");
    static final TextKey TOTAL_TIME = TextKey.of("jsc.media_player.total_time", "Total time: %s");
    static final TextKey PLAYING = TextKey.of("jsc.media_player.playing", "Playing: %s");
    static final TextKey MUSIC = TextKey.of("jsc.media_player.music", "Music");
    static final TextKey ALL_MUSIC = TextKey.of("jsc.media_player.all_music", "All music");
    static final TextKey PLAYLISTS = TextKey.of("jsc.media_player.playlists", "Playlists");
    static final TextKey HOME = TextKey.of("jsc.media_player.home", "Home");
    static final TextKey MUSIC_LIBRARY = TextKey.of("jsc.media_player.music_library", "Music library");
    static final TextKey VIDEO_LIBRARY = TextKey.of("jsc.media_player.video_library", "Video library");
    static final TextKey PLAY_QUEUE = TextKey.of("jsc.media_player.play_queue", "Play queue");
    static final TextKey SONGS = TextKey.of("jsc.media_player.songs", "Songs");
    static final TextKey VIDEOS = TextKey.of("jsc.media_player.videos", "Videos");
    static final TextKey PLAYLIST = TextKey.of("jsc.media_player.playlist", "Playlist");
    static final TextKey TITLE_COLUMN = TextKey.of("jsc.media_player.column.title", "Title");
    static final TextKey LENGTH_COLUMN = TextKey.of("jsc.media_player.column.length", "Length");
    static final TextKey FILE_COLUMN = TextKey.of("jsc.media_player.column.file", "File");
    static final TextKey PICTURE_OF = TextKey.of("jsc.media_player.picture_of", "Picture %s of %s");
    static final TextKey PICTURE_RATE = TextKey.of("jsc.media_player.picture_rate", "Picture %s of %s, %s a second");
    static final TextKey SECONDS_OF = TextKey.of("jsc.media_player.seconds_of", "%s of %s s");
    static final TextKey PICTURES = TextKey.of("jsc.media_player.pictures", "%s pictures, %s");
    static final TextKey NOTHING = TextKey.of("jsc.media_player.nothing",
            "Nothing to play yet: songs in your Music folder, and folders of pictures in Pictures.");
    static final TextKey PLAY_FILE = TextKey.of("jsc.media_player.play_file", "Play File");
    static final TextKey PAUSE = TextKey.of("jsc.media_player.pause", "Pause");
    static final TextKey VOLUME = TextKey.of("jsc.media_player.volume", "Volume");
    // The options and the refusal.
    static final TextKey SPEED_TITLE = TextKey.of("jsc.media_player.speed_title", "Image sequences");
    static final TextKey SPEED_LINE = TextKey.of("jsc.media_player.speed_line", "%s pictures a second");
    static final TextKey OK = TextKey.of("jsc.media_player.ok", "OK");
    static final TextKey NO_CARD = TextKey.of("jsc.media_player.no_card",
            "This computer has no sound card that can play %s. A recording needs a sound card; pictures play"
                    + " without one.");
    static final TextKey OPEN_TITLE = TextKey.of("jsc.media_player.open_title", "Open a song or a picture");
    static final TextKey MEDIA_FILES = TextKey.of("jsc.media_player.media_files", "Songs and pictures");

    private MediaPlayerTexts() {
    }
}
