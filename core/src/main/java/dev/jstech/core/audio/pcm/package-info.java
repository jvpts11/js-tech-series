/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */

/**
 * Samples for the sound kit: the notes a sound made as it plays is written in, the synthesiser that turns them into
 * samples, the decoders that read the sound files a player brings, and what a machine's hardware does to a recording
 * on its way out. Every sample here is a signed sixteen-bit number, which is what the game's speakers take.
 *
 * <p>A synthesised sound is a list of {@link Tone}s, each a {@link Waveform}, a frequency, a length and a volume;
 * {@code Audio.tones} sends them and each client makes them into samples with a {@link SynthSource}. Samples are read
 * a stretch at a time from an {@link IPcmSource}, in its {@link PcmFormat}, and opened only when they are about to be
 * heard ({@link IPcmOpener}). A kind of file is read by the {@link IAudioDecoder} registered for its extension in
 * {@link AudioDecoders}; WAV ({@link WavDecoder}) and Ogg Vorbis are there already. On the way out,
 * {@link ResponseFilter} plays a recording as hardware that cannot play it whole would, {@link StereoSelect} gives one
 * speaker of a pair its side, {@link PcmSkip} starts a recording part way through, and {@link PcmTap} keeps the last
 * seconds heard for the bars of an analyser ({@link Spectrum}).
 *
 * <p>How sounds made as they play are used is in {@code core/docs/SOUND.md}. Examples that compile are the test mod's
 * client tests of sound ({@code tests/src/main/java/dev/jstech/tests/clienttest/core/AudioClientTests.java}), which
 * synthesise and decode samples, and the beeps and tunes of J's Computers' programs
 * ({@code computers/src/main/java/dev/jstech/computers/audio/ProgramSounds.java}).
 */
@ApiStatus.Experimental
package dev.jstech.core.audio.pcm;

import org.jetbrains.annotations.ApiStatus;
