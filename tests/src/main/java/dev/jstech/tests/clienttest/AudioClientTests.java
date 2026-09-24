/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.core.audio.AudioChannels;
import dev.jstech.core.audio.AudioPrefs;
import dev.jstech.core.client.audio.AudioEngine;
import dev.jstech.core.client.audio.AudioMixer;
import dev.jstech.core.client.audio.AudioPrefsStore;
import dev.jstech.core.client.audio.CapturingAudioSink;
import dev.jstech.tests.TestSounds;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvents;

/**
 * The sound system on a client: a sound of the screen reaches the speakers through the mixer, at the volume the player
 * gave its channel, and not at all once the player turned it off; a sound of the game itself is turned off the same
 * way, and one the player never touched is left as it was.
 */
public final class AudioClientTests {

    private static final String INTERFACE = AudioChannels.INTERFACE.id().toString();
    private static final String CLICK = TestSounds.CLICK.id().toString();
    private static final String BELL = SoundEvents.NOTE_BLOCK_BELL.value().getLocation().toString();

    private AudioClientTests() {
    }

    @ClientTest(timeoutTicks = 400)
    public static void mixer_playsAtTheChannelsVolumeAndDropsWhatThePlayerTurnedOff(final ClientTestContext ctx) {
        final CapturingAudioSink sink = new CapturingAudioSink();
        final AudioPrefs prefs = AudioPrefsStore.prefs();
        ctx.then(0, () -> AudioEngine.useSink(sink))
                .then(1, () -> AudioEngine.playOnScreen(TestSounds.CLICK))
                .thenAssert(1, () -> sink.played().size() == 1
                        && sink.played().getFirst().getLocation().equals(TestSounds.CLICK.id()),
                        "the click is played once, as itself")
                .then(0, () -> {
                    prefs.setVolume(INTERFACE, 0.5F);
                    AudioEngine.playOnScreen(TestSounds.CLICK);
                })
                .thenAssert(1, () -> sink.played().size() == 2
                        && Math.abs(sink.played().get(1).getVolume() - 0.5F) < 0.001F,
                        "at half the volume once the interface is turned down to half")
                .then(0, () -> {
                    prefs.setMuted(CLICK, true);
                    AudioEngine.playOnScreen(TestSounds.CLICK);
                })
                .thenAssert(1, () -> sink.played().size() == 2, "and not at all once the player turned it off")
                .thenAssert(0, () -> {
                    final SoundInstance bell = SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BELL, 1.0F);
                    final boolean untouched = AudioMixer.mix(bell) == bell;
                    prefs.setMuted(BELL, true);
                    return untouched && AudioMixer.mix(bell) == null;
                }, "a sound of the game is left alone until the player turns it off too")
                .then(0, () -> {
                    prefs.setMuted(CLICK, false);
                    prefs.setMuted(BELL, false);
                    prefs.setVolume(INTERFACE, 1.0F);
                    AudioEngine.restoreSink();
                });
    }
}
