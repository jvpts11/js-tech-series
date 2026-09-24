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
 * What the sound system says to the player on their own client: the name of its key and of the group of keys the
 * series' mods share, and what the key tells them it did. They live here, where a server can load them too, so the
 * language generator finds them.
 */
@TextHolder
public final class AudioTexts {

    /** The group the series' keys are listed under in the game's controls. */
    public static final TextKey KEY_CATEGORY = TextKey.of("key.categories.jstech", "J's Tech Series");
    public static final TextKey TURN_OFF_LAST_SOUND = TextKey.of("key.jscore.turn_off_last_sound",
            "Turn Off Last Sound");
    /** The sound turned off, then the key that brings it back. */
    public static final TextKey TURNED_OFF = TextKey.of("jscore.audio.turned_off",
            "Turned off: %s. Press %s again to undo.");
    public static final TextKey BACK_ON = TextKey.of("jscore.audio.back_on", "Turned back on: %s.");
    public static final TextKey NOTHING_HEARD = TextKey.of("jscore.audio.nothing_heard", "No sound has played yet.");

    private AudioTexts() {
    }
}
