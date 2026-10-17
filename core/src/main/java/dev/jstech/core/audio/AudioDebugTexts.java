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
 * The lines the sound system adds to the game's debug screen (F3), under the Sound Mixer's name: what the director
 * keeps, the rooms, what is muffled, the lowering under alerts, the last sound heard and how many are turned off.
 */
@TextHolder
public final class AudioDebugTexts {

    /** Short sounds kept and their budget, then long ones and theirs. */
    public static final TextKey RUNNING = TextKey.of("jscore.audio.debug.running", "Running: %s/%s short, %s/%s long");
    public static final TextKey ROOMS = TextKey.of("jscore.audio.debug.rooms", "Rooms: %s");
    /** How many rooms, then the first one's field, how many machines it stands for and how loud it plays. */
    public static final TextKey ROOMS_FIRST = TextKey.of("jscore.audio.debug.rooms_first", "Rooms: %s (%s x%s, %s%%)");
    public static final TextKey MUFFLED = TextKey.of("jscore.audio.debug.muffled", "Muffled: %s");
    /** How many sounds are muffled, then the walls in the way of each, as a list. */
    public static final TextKey MUFFLED_WALLS = TextKey.of("jscore.audio.debug.muffled_walls",
            "Muffled: %s (%s walls)");
    /** A list's last two items. */
    public static final TextKey LIST_LAST = TextKey.of("jscore.audio.debug.list_last", "%s and %s");
    /** A list's items before its last two. */
    public static final TextKey LIST_MORE = TextKey.of("jscore.audio.debug.list_more", "%s, %s");
    public static final TextKey LOWERED_NO = TextKey.of("jscore.audio.debug.lowered_no", "Lowered under an alert: no");
    public static final TextKey LOWERED = TextKey.of("jscore.audio.debug.lowered", "Lowered under an alert: %s%%");
    /** How loud the other channels play, then how many seconds until they are back to full. */
    public static final TextKey LOWERED_BACK = TextKey.of("jscore.audio.debug.lowered_back",
            "Lowered under an alert: %s%%, %s s");
    public static final TextKey LAST_HEARD = TextKey.of("jscore.audio.debug.last_heard", "Last heard: %s");
    public static final TextKey LAST_HEARD_NONE = TextKey.of("jscore.audio.debug.last_heard_none",
            "Last heard: nothing");
    /** How many sounds are turned off, then how many the game knows. */
    public static final TextKey TURNED_OFF = TextKey.of("jscore.audio.debug.turned_off", "Turned off: %s of %s");
    /** How long the place's echo rings, in seconds, then how loud it is, in percent. */
    public static final TextKey ECHO = TextKey.of("jscore.audio.debug.echo", "Echo: %s s, %s%% loud");
    /** How walled in the place is, in percent, how far to a wall, in metres, and how much the walls drink. */
    public static final TextKey PLACE = TextKey.of("jscore.audio.debug.place",
            "Place: %s%% walled, %s m to a wall, drinks %s%%");
    /** The sealed room's air in cubic metres, then its walls in square metres. */
    public static final TextKey ROOM = TextKey.of("jscore.audio.debug.room", "Sealed room: %s m3, %s m2 of walls");
    public static final TextKey MEDIUM_VACUUM = TextKey.of("jscore.audio.debug.medium_vacuum",
            "Through: a vacuum, only what is touched");
    /** How fast sound goes through water, in metres a second. */
    public static final TextKey MEDIUM_WATER = TextKey.of("jscore.audio.debug.medium_water",
            "Through: water, muffled, %s m/s");
    /** The air's pressure in kilopascals, how fast sound goes in it and how loud it carries a sound, in percent. */
    public static final TextKey MEDIUM_AIR = TextKey.of("jscore.audio.debug.medium_air",
            "Through: air at %s kPa, %s m/s, %s%% loud");
    /** How much the ringing ears take away of a sound's loudness, in percent. */
    public static final TextKey RINGING = TextKey.of("jscore.audio.debug.ringing", "Ears ringing: %s%% muffled");

    private AudioDebugTexts() {
    }
}
