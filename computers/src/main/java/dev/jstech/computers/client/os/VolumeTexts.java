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

/**
 * What the volume control on a panel says. Each desktop words it its own way, so the same thing has more than one
 * key where the systems it imitates wrote it differently.
 */
@TextHolder
final class VolumeTexts {

    static final TextKey VOLUME = TextKey.of("jsc.volume.volume", "Volume");
    static final TextKey VOLUME_AT = TextKey.of("jsc.volume.volume_at", "Volume: %s%%");
    static final TextKey MUTED = TextKey.of("jsc.volume.muted", "Muted");
    static final TextKey MUTE = TextKey.of("jsc.volume.mute", "Mute");
    static final TextKey MUTE_OUTPUT = TextKey.of("jsc.volume.mute_output", "Mute output");
    static final TextKey MIXER = TextKey.of("jsc.volume.mixer", "Mixer...");
    static final TextKey AUDIO_VOLUME = TextKey.of("jsc.volume.audio_volume", "Audio Volume");
    static final TextKey PLAY_THROUGH = TextKey.of("jsc.volume.play_through", "Play through");
    static final TextKey CONFIGURE_DEVICES = TextKey.of("jsc.volume.configure_devices", "Configure Audio Devices...");
    /** The heading of the newest Frames' list, in its sentence case. */
    static final TextKey SOUND_OUTPUT = TextKey.of("jsc.volume.sound_output", "Sound output");
    /** The heading of GNOME's list, in its title case. */
    static final TextKey SOUND_OUTPUT_TITLE = TextKey.of("jsc.volume.sound_output_title", "Sound Output");
    static final TextKey OUTPUT_DEVICE = TextKey.of("jsc.volume.output_device", "Output device");
    static final TextKey MORE_SOUND_SETTINGS = TextKey.of("jsc.volume.more_sound_settings", "More sound settings");
    /** The entry of the menu the speaker on the panel opens with the right button. */
    static final TextKey SOUND_SETTINGS = TextKey.of("jsc.volume.sound_settings", "Sound settings");
    static final TextKey SOUND_SETTINGS_TITLE = TextKey.of("jsc.volume.sound_settings_title", "Sound Settings");
    static final TextKey OUTPUT_MONITOR = TextKey.of("jsc.volume.output_monitor", "Monitor");
    static final TextKey OUTPUT_SPEAKERS = TextKey.of("jsc.volume.output_speakers", "Speakers");
    static final TextKey OUTPUT_BOTH = TextKey.of("jsc.volume.output_both", "Monitor and speakers");
    /** A speaker nobody has named yet, the way its own screen calls it. */
    static final TextKey UNNAMED_SPEAKER = TextKey.of("jsc.volume.unnamed_speaker", "Speaker");
    static final TextKey NO_SPEAKERS = TextKey.of("jsc.volume.no_speakers", "None linked");

    private VolumeTexts() {
    }
}
