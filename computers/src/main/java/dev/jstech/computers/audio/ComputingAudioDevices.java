/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.audio;

import dev.jstech.computers.hardware.SoundCardSpec;
import dev.jstech.core.audio.AudioDevice;
import dev.jstech.core.audio.AudioDevices;
import dev.jstech.core.audio.pcm.Waveform;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

import java.util.EnumSet;
import java.util.Set;

/**
 * What a computer's sound can come out of: the speaker inside its case, which beeps one square note at a time and
 * plays no recording; an FM sound card; a wavetable sound card; and the sound built into a Standard board.
 */
@TextHolder
public final class ComputingAudioDevices {

    private static final TextKey PC_SPEAKER_NAME = TextKey.of("jsc.audio_device.pc_speaker", "PC speaker");
    private static final TextKey FM_CARD_NAME = TextKey.of("jsc.audio_device.fm_card", "FM sound card");
    private static final TextKey WAVETABLE_CARD_NAME =
            TextKey.of("jsc.audio_device.wavetable_card", "Wavetable sound card");
    private static final TextKey ON_BOARD_NAME = TextKey.of("jsc.audio_device.on_board", "On-board audio");

    public static final AudioDevice PC_SPEAKER = AudioDevices.register(
            new AudioDevice("jsc:pc_speaker", PC_SPEAKER_NAME, Set.of(Waveform.SQUARE), false, 1));
    public static final AudioDevice FM_CARD = AudioDevices.register(
            new AudioDevice("jsc:fm_card", FM_CARD_NAME, EnumSet.allOf(Waveform.class), true, 9));
    public static final AudioDevice WAVETABLE_CARD = AudioDevices.register(
            new AudioDevice("jsc:wavetable_card", WAVETABLE_CARD_NAME, EnumSet.allOf(Waveform.class), true, 32));
    public static final AudioDevice ON_BOARD = AudioDevices.register(
            new AudioDevice("jsc:on_board", ON_BOARD_NAME, EnumSet.allOf(Waveform.class), true, 64));

    private ComputingAudioDevices() {
    }

    /** Loads the devices, so both sides know them from the start. */
    public static void init() {
        // Nothing to do: loading the class is what registers the devices.
    }

    /** The device a sound card is: the kind of notes it makes decides it. */
    public static AudioDevice of(final SoundCardSpec card) {
        return switch (card.synthesis()) {
            case FM -> FM_CARD;
            case WAVETABLE -> WAVETABLE_CARD;
        };
    }
}
