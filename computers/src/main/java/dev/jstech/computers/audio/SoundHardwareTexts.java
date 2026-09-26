/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.audio;

import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.tier.HardwareEra;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * What the tooltips say about where a computer's sound comes from: the speaker in its case, the sound built into a
 * Standard board, and the monitor it comes out of. One place, so the board, the case and the monitor say it alike.
 */
@TextHolder
public final class SoundHardwareTexts {

    /** A Standard board's line: its sound is built in, so it needs no card. */
    public static final TextKey ON_BOARD_AUDIO = TextKey.of("jsc.audio.on_board", "On-board audio");

    private static final TextKey MONITOR_PLAYS =
            TextKey.of("jsc.audio.monitor_plays", "Plays the sound of its computer");
    private static final TextKey PC_SPEAKER_ONLY = TextKey.of("jsc.audio.pc_speaker_only", "PC speaker: beeps only");
    private static final TextKey SOUND_ON_BOARD =
            TextKey.of("jsc.audio.sound_on_board", "Sound on the board: plays everything");

    private SoundHardwareTexts() {
    }

    /** A computer case's line: a Vintage or Legacy case only beeps, a Standard one has its sound on the board. */
    public static void appendComputer(final List<Component> tooltip, final HardwareEra era) {
        final TextKey line = era == HardwareEra.STANDARD ? SOUND_ON_BOARD : PC_SPEAKER_ONLY;
        tooltip.add(GameText.component(line).withStyle(ChatFormatting.GRAY));
    }

    /** A monitor's line: what its computer plays comes out of it. */
    public static void appendMonitor(final List<Component> tooltip) {
        tooltip.add(GameText.component(MONITOR_PLAYS).withStyle(ChatFormatting.GRAY));
    }
}
