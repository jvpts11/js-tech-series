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
import dev.jstech.core.audio.IAudible;
import dev.jstech.core.audio.LoopRequest;
import dev.jstech.core.client.audio.AudioEngine;
import dev.jstech.core.client.audio.AudioMixer;
import dev.jstech.core.client.audio.AudioPrefsStore;
import dev.jstech.core.client.audio.CapturingAudioSink;
import dev.jstech.core.client.audio.SoundDirector;
import dev.jstech.tests.TestSounds;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The sound system on a client: a sound of the screen reaches the speakers through the mixer, at the volume the player
 * gave its channel, and not at all once the player turned it off; a sound of the game itself is turned off the same
 * way. The director keeps the world's running sounds within its budget and makes rooms of many.
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

    /**
     * The director keeps the running sounds sources ask for inside its budget, hears many sources of one field close
     * together as the field's one room, and lets every sound go once its source is gone.
     */
    @ClientTest(timeoutTicks = 400)
    public static void director_keepsTheBudgetMakesARoomOfManyAndLetsGo(final ClientTestContext ctx) {
        final CapturingAudioSink sink = new CapturingAudioSink();
        final List<Whirring> sources = new ArrayList<>();
        ctx.then(0, () -> {
                    AudioEngine.useSink(sink);
                    final Vec3 at = ctx.player().position();
                    for (int i = 0; i < 30; i++) {
                        sources.add(new Whirring(at.add(i % 6 - 3, 1, i / 6 - 2), null));
                    }
                    sources.forEach(SoundDirector::track);
                    SoundDirector.updateNow();
                })
                .thenAssert(1, () -> SoundDirector.playing().size() == 24,
                        "thirty whirring things near the player are held to the director's budget of short sounds")
                .then(0, () -> {
                    sources.forEach(SoundDirector::untrack);
                    SoundDirector.updateNow();
                })
                .thenAssert(1, () -> SoundDirector.playing().isEmpty(), "and every one goes when its source goes")
                .then(0, () -> {
                    sources.clear();
                    final Vec3 at = ctx.player().position();
                    for (int i = 0; i < 6; i++) {
                        sources.add(new Whirring(at.add(i % 3, 1, i / 3), TestSounds.ROOM_FIELD.id()));
                    }
                    sources.forEach(SoundDirector::track);
                    SoundDirector.updateNow();
                })
                .thenAssert(1, () -> SoundDirector.playing().size() == 1
                        && SoundDirector.playing().iterator().next().startsWith("bed|"),
                        "six of a room's things close together are heard as the room, once")
                .then(0, () -> {
                    sources.forEach(SoundDirector::untrack);
                    SoundDirector.updateNow();
                    AudioEngine.restoreSink();
                })
                .thenAssert(1, () -> SoundDirector.playing().isEmpty(), "and the room goes with them");
    }

    /** A thing that whirs where it stands, alone or as part of a room. */
    private record Whirring(Vec3 at, @Nullable ResourceLocation field) implements IAudible {

        @Override
        public double audioX() {
            return at.x;
        }

        @Override
        public double audioY() {
            return at.y;
        }

        @Override
        public double audioZ() {
            return at.z;
        }

        @Override
        public List<LoopRequest> loops() {
            return List.of(new LoopRequest(TestSounds.WHIR, 1.0F, 1.0F, field));
        }
    }
}
