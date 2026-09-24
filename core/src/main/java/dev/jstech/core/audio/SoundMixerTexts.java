/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * What the Sound Mixer says: the screen where a player sets each channel's volume, turns any sound of the game off
 * and chooses how the series' sounds behave. It is a client's screen, and its sentences live here, where a server
 * can load them too, so the language generator finds them.
 */
@TextHolder
public final class SoundMixerTexts {

    public static final TextKey TITLE = TextKey.of("jscore.sound_mixer.title", "Sound Mixer");
    /** The button in the game's own Music and Sound Options that opens the mixer. */
    public static final TextKey OPEN = TextKey.of("jscore.sound_mixer.open", "Sound Mixer...");
    public static final TextKey TAB_CHANNELS = TextKey.of("jscore.sound_mixer.tab.channels", "Channels");
    public static final TextKey TAB_SOUNDS = TextKey.of("jscore.sound_mixer.tab.sounds", "Sounds");
    public static final TextKey TAB_OPTIONS = TextKey.of("jscore.sound_mixer.tab.options", "Options");
    /** A volume as a share: the number, then the percent sign. */
    public static final TextKey PERCENT = TextKey.of("jscore.sound_mixer.percent", "%s%%");
    /** A channel's tooltip, after what it carries: the name of the game's own category it rides on. */
    public static final TextKey ALSO_FOLLOWS = TextKey.of("jscore.sound_mixer.also_follows",
            "Also follows the game's %s volume.");
    public static final TextKey RESET_CHANNELS = TextKey.of("jscore.sound_mixer.reset_channels", "Reset Channels");
    public static final TextKey CHANNELS_NOTE = TextKey.of("jscore.sound_mixer.channels_note",
            "A channel plays at its own volume times the game's volume for the category it belongs to.");
    public static final TextKey SEARCH_HINT = TextKey.of("jscore.sound_mixer.search_hint", "Search by name or id");
    /** How many sounds are off, then how many there are. */
    public static final TextKey TURNED_OFF_COUNT = TextKey.of("jscore.sound_mixer.turned_off_count",
            "%s of %s turned off");
    public static final TextKey SHOW = TextKey.of("jscore.sound_mixer.show", "Show");
    public static final TextKey SHOW_ALL = TextKey.of("jscore.sound_mixer.show.all", "All");
    public static final TextKey SHOW_RECENT = TextKey.of("jscore.sound_mixer.show.recent", "Recent");
    public static final TextKey SHOW_SERIES = TextKey.of("jscore.sound_mixer.show.series", "Series");
    public static final TextKey SHOW_GAME = TextKey.of("jscore.sound_mixer.show.game", "Game");
    public static final TextKey SHOW_TURNED_OFF = TextKey.of("jscore.sound_mixer.show.turned_off", "Turned Off");
    public static final TextKey TURN_ALL_BACK_ON = TextKey.of("jscore.sound_mixer.turn_all_back_on",
            "Turn All Back On");
    public static final TextKey PLAY = TextKey.of("jscore.sound_mixer.play", "Play");
    public static final TextKey NO_PREVIEW = TextKey.of("jscore.sound_mixer.no_preview",
            "This sound is made as it plays, so there is nothing to play here.");
    public static final TextKey MUFFLE = TextKey.of("jscore.sound_mixer.muffle", "Muffle Behind Walls");
    public static final TextKey MUFFLE_TIP = TextKey.of("jscore.sound_mixer.muffle.tip",
            "Walls between you and a sound muffle it, more with every wall.");
    public static final TextKey ALERTS_ON_SCREEN = TextKey.of("jscore.sound_mixer.alerts_on_screen",
            "Alerts on Screen");
    public static final TextKey ALERTS_ON_SCREEN_TIP = TextKey.of("jscore.sound_mixer.alerts_on_screen.tip",
            "Shows a sign at the top of the screen, pointing where an alert comes from.");
    public static final TextKey LOWER_UNDER_ALERTS = TextKey.of("jscore.sound_mixer.lower_under_alerts",
            "Lower Under Alerts");
    public static final TextKey LOWER_UNDER_ALERTS_TIP = TextKey.of("jscore.sound_mixer.lower_under_alerts.tip",
            "While an alert plays, the other channels are turned down so it is heard.");
    public static final TextKey KEY_NOTE = TextKey.of("jscore.sound_mixer.key_note",
            "Turn Off Last Sound silences whatever you heard last. Press it again within 5 seconds to bring it back.");

    private SoundMixerTexts() {
    }
}
