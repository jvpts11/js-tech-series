/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */

/**
 * Sound for any mod: sounds declared once and played from the server with one call, running sounds that follow a
 * machine's state, cues that pick their sound by where they happen, sounds made as they play, the channels a player
 * sets the volume of, and the hardware a machine plays through.
 *
 * <p>A sound is a {@link SoundKey}, declared with {@code ModContent.sound(path)}, from which its entry in
 * {@code sounds.json} and its subtitle are made. {@link Audio} plays it from the server, at a place for whoever is
 * near or on one player's screen, and never through the game's own calls, so every sound passes the same gate and is
 * mixed in its {@link AudioChannel}. A block entity that hums while it works is {@link IAudible} and answers which
 * {@link LoopRequest}s it wants now; many of one kind close together are heard as one {@link AmbientField}. A
 * {@link SoundCue} is something that happens, whose sound is picked by its {@link SoundContext} (the machine's era,
 * its device, its system) from rules a resource pack can replace. A machine with sound hardware of its own is an
 * {@link IAudioHost}, found through {@link AudioHosts}, and plays through an {@link AudioDevice}, with the waves it
 * makes, its voices and its {@link FrequencyResponse}. Each player's volumes and switches are their own
 * ({@link AudioPrefs}), kept beside the game's options.
 *
 * <p>How to use it, from zero, is in {@code core/docs/SOUND.md}. An example that compiles is the test mod's sounds,
 * its alarm cue and its buzzer ({@code tests/src/main/java/dev/jstech/tests/TestSounds.java}).
 */
@ApiStatus.Experimental
package dev.jstech.core.audio;

import org.jetbrains.annotations.ApiStatus;
