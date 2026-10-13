/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.audio.mixer;

import dev.jstech.core.audio.SoundMixerTexts;
import dev.jstech.core.text.TextKey;

/**
 * Which sounds the list shows: all of them, the ones heard in the last minute (newest first, to find the one that has
 * just annoyed the player), the series' own, the game's and other mods', or the ones turned off.
 */
enum SoundFilter {

    ALL(SoundMixerTexts.SHOW_ALL),

    RECENT(SoundMixerTexts.SHOW_RECENT),

    SERIES(SoundMixerTexts.SHOW_SERIES),

    GAME(SoundMixerTexts.SHOW_GAME),

    TURNED_OFF(SoundMixerTexts.SHOW_TURNED_OFF);

    private final TextKey label;

    SoundFilter(final TextKey label) {
        this.label = label;
    }

    /** What the Show button says for it. */
    TextKey label() {
        return label;
    }
}
