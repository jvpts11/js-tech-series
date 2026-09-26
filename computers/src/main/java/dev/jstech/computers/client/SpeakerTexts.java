/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** What a speaker's screen says. */
@TextHolder
final class SpeakerTexts {

    static final TextKey TITLE = TextKey.of("jsc.speaker.title", "SPEAKER");
    static final TextKey MODEL_LEGACY = TextKey.of("jsc.speaker.model_legacy", "TONEWORKS");
    static final TextKey MODEL_STANDARD = TextKey.of("jsc.speaker.model_standard", "COBBLE");
    static final TextKey NAME = TextKey.of("jsc.speaker.name", "NAME");
    static final TextKey NAME_FIELD = TextKey.of("jsc.speaker.name_field", "Speaker name");
    /** What a speaker is called until a player names it. */
    static final TextKey DEFAULT_NAME = TextKey.of("jsc.speaker.default_name", "Speaker");
    static final TextKey HINT = TextKey.of("jsc.speaker.hint", "Programs find this speaker by its name");
    static final TextKey CLASH = TextKey.of("jsc.speaker.clash", "Another speaker already has this name");
    static final TextKey COMPUTER = TextKey.of("jsc.speaker.computer", "COMPUTER");
    static final TextKey NOT_LINKED = TextKey.of("jsc.speaker.not_linked", "Not linked");
    static final TextKey CHANNEL = TextKey.of("jsc.speaker.channel", "CHANNEL");
    static final TextKey CHANNEL_NONE = TextKey.of("jsc.speaker.channel_none", "None");
    static final TextKey CHANNEL_ALONE = TextKey.of("jsc.speaker.channel_alone", "Mono, alone");
    static final TextKey CHANNEL_BOTH = TextKey.of("jsc.speaker.channel_both", "Mono");
    static final TextKey CHANNEL_LEFT = TextKey.of("jsc.speaker.channel_left", "Left, by position");
    static final TextKey CHANNEL_RIGHT = TextKey.of("jsc.speaker.channel_right", "Right, by position");
    static final TextKey PLAYS = TextKey.of("jsc.speaker.plays", "PLAYS");
    static final TextKey PLAYS_LEGACY = TextKey.of("jsc.speaker.plays_legacy", "22 kHz, bass and treble cut");
    static final TextKey PLAYS_WHOLE = TextKey.of("jsc.speaker.plays_whole", "The whole range");

    private SpeakerTexts() {
    }
}
