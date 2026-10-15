/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.signal;

import dev.jstech.core.id.IStableName;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * Whose frequencies a radio is tuned among: its owner's own, its owner's team's, or a pair's of its own. Each is a
 * space of its own, so frequency 7 of a player and frequency 7 of their team are two frequencies.
 */
@TextHolder
public enum Band implements IStableName {

    /** The owner's own frequencies, which nobody else's radio hears. */
    PRIVATE("private", TextKey.of("jscore.signal.band.private", "Private")),
    /** The frequencies of the owner's team, which every radio of a player of the team hears. */
    TEAM("team", TextKey.of("jscore.signal.band.team", "Team")),
    /** A link of its own between one transmitter and the receivers paired with it, with no number. */
    PAIR("pair", TextKey.of("jscore.signal.band.pair", "Pair"));

    private final String serializedName;
    private final TextKey name;

    Band(final String serializedName, final TextKey name) {
        this.serializedName = serializedName;
        this.name = name;
    }

    @Override
    public String serializedName() {
        return this.serializedName;
    }

    /** What a player calls it. */
    public TextKey text() {
        return this.name;
    }
}
