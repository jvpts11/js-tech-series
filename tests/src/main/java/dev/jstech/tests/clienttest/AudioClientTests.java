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
import dev.jstech.core.audio.ToneSoundPayload;
import dev.jstech.core.audio.pcm.AudioDecoders;
import dev.jstech.core.audio.pcm.IPcmSource;
import dev.jstech.core.audio.pcm.PcmFormat;
import dev.jstech.core.audio.pcm.SynthSource;
import dev.jstech.core.audio.pcm.Tone;
import dev.jstech.core.client.audio.AudioEngine;
import dev.jstech.core.client.audio.AudioMixer;
import dev.jstech.core.client.audio.AudioPrefsStore;
import dev.jstech.core.client.audio.CapturingAudioSink;
import dev.jstech.core.client.audio.PcmAudioStream;
import dev.jstech.core.client.audio.SoundDirector;
import dev.jstech.tests.TestSounds;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The sound system on a client: a sound of the screen reaches the speakers through the mixer, at the volume the player
 * gave its channel, and not at all once the player turned it off; a sound of the game itself is turned off the same
 * way. The director keeps the world's running sounds within its budget and makes rooms of many. A sound made as it
 * plays opens into the samples the speakers take.
 */
public final class AudioClientTests {

    private static final String INTERFACE = AudioChannels.INTERFACE.id().toString();
    private static final String CLICK = TestSounds.CLICK.id().toString();
    private static final String BELL = SoundEvents.NOTE_BLOCK_BELL.value().getLocation().toString();
    private static final String DEVICES = AudioChannels.DEVICES.id().toString();
    private static final String BEEP = TestSounds.BEEP.id().toString();

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

    /**
     * A sound made as it plays goes the way every other does, through the mixer and its channel, and opens into the
     * samples the speakers are handed: notes from the server synthesised, a recording in the game's own Ogg decoded,
     * and a stereo one made mono when it is heard from a place in the world.
     */
    @ClientTest(timeoutTicks = 400)
    public static void made_playsNotesAndRecordingsAsTheSpeakersWouldTakeThem(final ClientTestContext ctx) {
        final CapturingAudioSink sink = new CapturingAudioSink();
        final AudioPrefs prefs = AudioPrefsStore.prefs();
        final List<Tone> notes = List.of(Tone.beep(880, 100), Tone.rest(50));
        final int noteSamples = (int) new SynthSource(notes).totalSamples();
        ctx.then(0, () -> {
                    AudioEngine.useSink(sink);
                    final Vec3 at = ctx.player().position();
                    AudioEngine.playTones(new ToneSoundPayload(TestSounds.BEEP.id(), false, at.x, at.y, at.z, notes));
                })
                .thenAssert(1, () -> {
                    final SoundInstance beep = sink.played().getLast();
                    final ByteBuffer first = firstSamples(beep);
                    return sink.played().size() == 1 && beep.getLocation().equals(TestSounds.BEEP.id())
                            && beep.getSound().shouldStream() && beep.getSound().getAttenuationDistance() == 12
                            && first != null && first.remaining() == noteSamples * 2;
                }, "the server's notes are played as the beep, streamed, and open into every sample they make")
                .thenAssert(0, () -> {
                    prefs.setVolume(DEVICES, 0.25F);
                    return Math.abs(sink.played().getLast().getVolume() - 0.25F) < 0.001F;
                }, "at the volume the player gave the devices")
                .then(0, () -> {
                    prefs.setMuted(BEEP, true);
                    final Vec3 at = ctx.player().position();
                    AudioEngine.playTones(new ToneSoundPayload(TestSounds.BEEP.id(), false, at.x, at.y, at.z, notes));
                })
                .thenAssert(1, () -> sink.played().size() == 1, "and not at all once the player turned it off")
                .then(0, () -> AudioEngine.playMadeOnScreen(TestSounds.TUNE,
                        () -> AudioDecoders.open("click.ogg", Minecraft.getInstance().getResourceManager()
                                .open(ResourceLocation.withDefaultNamespace("sounds/random/click.ogg"))), 1.0F))
                .thenAssert(1, () -> {
                    final ByteBuffer first = firstSamples(sink.played().getLast());
                    return sink.played().size() == 2 && first != null && first.remaining() > 0;
                }, "a recording in the game's own Ogg is decoded into samples")
                .thenAssert(0, () -> {
                    final PcmAudioStream mono = new PcmAudioStream(new Stereo(new short[] {100, 300, -50, 50}), true);
                    final PcmAudioStream kept = new PcmAudioStream(new Stereo(new short[] {100, 300}), false);
                    try {
                        final ShortBuffer mixed = mono.read(1024).asShortBuffer();
                        return mono.getFormat().getChannels() == 1 && mixed.remaining() == 2 && mixed.get(0) == 200
                                && mixed.get(1) == 0 && mono.read(1024) == null
                                && kept.getFormat().getChannels() == 2 && kept.read(1024).remaining() == 4;
                    } catch (final IOException unexpected) {
                        return false;
                    }
                }, "a stereo recording heard from a place is made mono, and one on the screen is kept stereo")
                .then(0, () -> {
                    prefs.setMuted(BEEP, false);
                    prefs.setVolume(DEVICES, 1.0F);
                    AudioEngine.restoreSink();
                });
    }

    /* Opens the sound's stream as the game would, and reads what it would queue first: a second of samples. */
    @Nullable
    private static ByteBuffer firstSamples(final SoundInstance sound) {
        try (AudioStream stream = sound.getStream(null, sound.getSound(), false).join()) {
            return stream.read(Short.BYTES * stream.getFormat().getChannels()
                    * (int) stream.getFormat().getSampleRate());
        } catch (final IOException unreadable) {
            return null;
        }
    }

    /** A few stereo samples, left and right in turn. */
    private static final class Stereo implements IPcmSource {

        private final short[] samples;
        private int read;

        Stereo(final short[] samples) {
            this.samples = samples;
        }

        @Override
        public PcmFormat format() {
            return new PcmFormat(44_100, 2);
        }

        @Override
        public int read(final short[] into, final int offset, final int length) {
            if (read == samples.length) {
                return -1;
            }
            final int count = Math.min(length, samples.length - read);
            System.arraycopy(samples, read, into, offset, count);
            read += count;
            return count;
        }
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
