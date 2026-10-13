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

    private AudioDebugTexts() {
    }
}
