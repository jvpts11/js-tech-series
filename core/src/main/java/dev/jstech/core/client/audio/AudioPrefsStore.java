/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.audio;

import dev.jstech.core.audio.AudioPrefs;
import dev.jstech.core.audio.AudioSettings;

/**
 * The player's sound preferences, as the sound options and the mixer reach them: read from the player's settings file
 * as the game starts ({@link AudioSettings}), and written back each time they change.
 */
public final class AudioPrefsStore {

    private AudioPrefsStore() {
    }

    /** The preferences in force. */
    public static AudioPrefs prefs() {
        return AudioSettings.prefs();
    }

    /** Writes the preferences to the player's settings file; one that cannot be written is said in the log. */
    public static void save() {
        AudioSettings.save(AudioSettings.prefs(), AudioSettings.FILE);
    }
}
