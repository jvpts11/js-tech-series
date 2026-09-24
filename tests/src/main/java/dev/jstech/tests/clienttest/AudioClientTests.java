/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import com.mojang.blaze3d.audio.ListenerTransform;
import dev.jstech.core.audio.AlertSignBoard;
import dev.jstech.core.audio.AudioChannels;
import dev.jstech.core.audio.AudioPrefs;
import dev.jstech.core.audio.CueSoundPayload;
import dev.jstech.core.audio.IAudible;
import dev.jstech.core.audio.LoopRequest;
import dev.jstech.core.audio.SoundContext;
import dev.jstech.core.audio.SoundSet;
import dev.jstech.core.audio.SoundSetJson;
import dev.jstech.core.audio.ToneSoundPayload;
import dev.jstech.core.audio.pcm.AudioDecoders;
import dev.jstech.core.audio.pcm.IPcmSource;
import dev.jstech.core.audio.pcm.PcmFormat;
import dev.jstech.core.audio.pcm.SynthSource;
import dev.jstech.core.audio.pcm.Tone;
import dev.jstech.core.client.audio.AlertSigns;
import dev.jstech.core.client.audio.AudioDebugLines;
import dev.jstech.core.client.audio.AudioEngine;
import dev.jstech.core.client.audio.AudioKeys;
import dev.jstech.core.client.audio.AudioMixer;
import dev.jstech.core.client.audio.AudioPrefsStore;
import dev.jstech.core.client.audio.CapturingAudioSink;
import dev.jstech.core.client.audio.PcmAudioStream;
import dev.jstech.core.client.audio.SoundCueBindings;
import dev.jstech.core.client.audio.SoundDirector;
import dev.jstech.tests.TestSounds;
import java.io.IOException;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The sound system on a client: a sound of the screen reaches the speakers through the mixer, at the volume the player
 * gave its channel, and not at all once the player turned it off; a sound of the game itself is turned off the same
 * way. The director keeps the world's running sounds within its budget and makes rooms of many. A sound made as it
 * plays opens into the samples the speakers take, and a cue plays what its file binds it to for its context. An
 * alert shows on screen to a player who asked, and the debug screen says what the sound system keeps.
 */
public final class AudioClientTests {

    private static final String INTERFACE = AudioChannels.INTERFACE.id().toString();
    private static final String CLICK = TestSounds.CLICK.id().toString();
    private static final String BELL = SoundEvents.NOTE_BLOCK_BELL.value().getLocation().toString();
    private static final String DEVICES = AudioChannels.DEVICES.id().toString();
    private static final String BEEP = TestSounds.BEEP.id().toString();
    private static final String ALERTS = AudioChannels.ALERTS.id().toString();

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

    /**
     * The key turns off the last sound heard around the player, never their own footsteps or the screen's clicks,
     * says which on the action bar, and brings it back when pressed again at once; everything heard is kept as recent.
     */
    @ClientTest(timeoutTicks = 400)
    public static void key_turnsOffTheLastSoundAroundThePlayerAndBringsItBack(final ClientTestContext ctx) {
        final CapturingAudioSink sink = new CapturingAudioSink();
        final AudioPrefs prefs = AudioPrefsStore.prefs();
        final String hum = TestSounds.HUM.id().toString();
        final String step = SoundEvents.STONE_STEP.getLocation().toString();
        final Component[] said = new Component[2];
        ctx.then(0, () -> {
                    AudioEngine.useSink(sink);
                    final Vec3 at = ctx.player().position();
                    sink.play(new SimpleSoundInstance(TestSounds.HUM.event().get(), SoundSource.BLOCKS, 1.0F, 1.0F,
                            SoundInstance.createUnseededRandom(), at.x + 2, at.y, at.z));
                    sink.play(new SimpleSoundInstance(SoundEvents.STONE_STEP, SoundSource.PLAYERS, 1.0F, 1.0F,
                            SoundInstance.createUnseededRandom(), at.x, at.y, at.z));
                    AudioEngine.playOnScreen(TestSounds.CLICK);
                    said[0] = AudioKeys.turnOffLastSound();
                })
                .thenAssert(0, () -> {
                    final List<String> recent = AudioMixer.recent(60_000L);
                    return recent.indexOf(CLICK) >= 0 && recent.indexOf(CLICK) < recent.indexOf(step)
                            && recent.indexOf(step) < recent.indexOf(hum);
                }, "everything heard is recent, newest first, the player's own step and the screen's click too")
                .thenAssert(0, () -> prefs.isMuted(hum) && !prefs.isMuted(step) && !prefs.isMuted(CLICK)
                                && said[0].getString().startsWith("Turned off: A test hum."),
                        "but the key turns off the hum, the last sound around the player, and says so by its subtitle")
                .then(0, () -> said[1] = AudioKeys.turnOffLastSound())
                .thenAssert(0, () -> !prefs.isMuted(hum) && said[1].getString().equals("Turned back on: A test hum."),
                        "and pressed again at once, brings it back")
                .then(0, () -> {
                    prefs.setMuted(hum, false);
                    AudioPrefsStore.save();
                    AudioEngine.restoreSink();
                });
    }

    /**
     * A cue's rules come from its file in the resources, which a pack replaces; its sound is picked by the context,
     * one the series did not declare playing in the cue's channel; and a cue bound to nothing is silent.
     */
    @ClientTest(timeoutTicks = 400)
    public static void cue_picksItsSoundByContextFromItsFile(final ClientTestContext ctx) {
        final CapturingAudioSink sink = new CapturingAudioSink();
        final AudioPrefs prefs = AudioPrefsStore.prefs();
        final SoundContext buzzer = SoundContext.EMPTY.with(SoundContext.DEVICE, TestSounds.BUZZER.id());
        final ResourceLocation bass = ResourceLocation.withDefaultNamespace("block.note_block.bass");
        ctx.then(0, () -> AudioEngine.useSink(sink))
                .thenAssert(0, () -> {
                    final Optional<Resource> file = Minecraft.getInstance().getResourceManager()
                            .getResource(TestSounds.ALARM.file());
                    try (Reader reader = file.orElseThrow().openAsReader()) {
                        return SoundSetJson.read(GsonHelper.parse(reader)).equals(TestSounds.ALARM.defaults())
                                && SoundCueBindings.of(TestSounds.ALARM).equals(TestSounds.ALARM.defaults());
                    } catch (final IOException | RuntimeException unreadable) {
                        return false;
                    }
                }, "the alarm's rules are in its generated file, and that is what this client has")
                .then(0, () -> {
                    prefs.setVolume(ALERTS, 0.5F);
                    final Vec3 at = ctx.player().position();
                    AudioEngine.playCue(new CueSoundPayload(TestSounds.ALARM.id(), false, at.x, at.y, at.z, buzzer,
                            1.0F, 1.0F));
                    AudioEngine.playCue(TestSounds.ALARM, SoundContext.EMPTY, at.x, at.y, at.z, 1.0F, 1.0F);
                })
                .thenAssert(1, () -> sink.played().size() == 2 && sink.played().get(0).getLocation().equals(bass)
                                && Math.abs(sink.played().get(0).getVolume() - 0.5F) < 0.001F
                                && sink.played().get(1).getLocation().equals(TestSounds.HUM.id()),
                        "through the buzzer the alarm is the game's bass, in the alerts channel, and the hum otherwise")
                .then(0, () -> {
                    SoundCueBindings.bind(TestSounds.ALARM, SoundSet.SILENT);
                    AudioEngine.playCue(TestSounds.ALARM, buzzer, 0, 0, 0, 1.0F, 1.0F);
                })
                .thenAssert(1, () -> sink.played().size() == 2, "bound to nothing, as a pack may, it is silent")
                .then(0, () -> {
                    SoundCueBindings.unbind(TestSounds.ALARM);
                    prefs.setVolume(ALERTS, 1.0F);
                    AudioEngine.restoreSink();
                });
    }

    /**
     * An alert comes up at the top of the screen for a player who asked for alerts on screen, and only for them,
     * saying what the alert is and pointing to the side it comes from.
     */
    @ClientTest(timeoutTicks = 400)
    public static void signs_showAnAlertOnlyToWhoAskedAndPointWhereItIs(final ClientTestContext ctx) {
        final CapturingAudioSink sink = new CapturingAudioSink();
        final AudioPrefs prefs = AudioPrefsStore.prefs();
        final SoundContext buzzer = SoundContext.EMPTY.with(SoundContext.DEVICE, TestSounds.BUZZER.id());
        final ResourceLocation bass = ResourceLocation.withDefaultNamespace("block.note_block.bass");
        ctx.then(0, () -> {
                    ctx.mc().setScreen(null);
                    AudioEngine.useSink(sink);
                    prefs.setVisualCues(false);
                    final Vec3 at = toTheRight(ctx);
                    AudioEngine.playCue(TestSounds.ALARM, buzzer, at.x, at.y, at.z, 1.0F, 1.0F);
                })
                .thenAssert(0, () -> AlertSigns.showing().isEmpty(), "no sign for a player who did not ask for them")
                .then(0, () -> {
                    prefs.setVisualCues(true);
                    final Vec3 at = toTheRight(ctx);
                    AudioEngine.playCue(TestSounds.ALARM, buzzer, at.x, at.y, at.z, 1.0F, 1.0F);
                })
                .thenAssert(0, () -> {
                    final List<AlertSignBoard.Sign> up = AlertSigns.showing();
                    final Component subtitle = ctx.mc().getSoundManager().getSoundEvent(bass).getSubtitle();
                    final ListenerTransform listener = ctx.mc().getSoundManager().getListenerTransform();
                    final Vec3 towards = new Vec3(up.getFirst().x(), up.getFirst().y(), up.getFirst().z())
                            .subtract(listener.position()).normalize();
                    return up.size() == 1 && subtitle != null && up.getFirst().label().equals(subtitle.getString())
                            && AlertSignBoard.direction(listener.forward().dot(towards),
                            listener.right().dot(towards)) == 1;
                }, "one sign, saying the alert's subtitle, pointing right where the alert is")
                .thenScreenshot(30, "alert-sign")
                .then(0, () -> {
                    prefs.setVisualCues(false);
                    AudioEngine.restoreSink();
                });
    }

    /** The game's debug screen shows the sound system's lines under the Sound Mixer's name. */
    @ClientTest(timeoutTicks = 400)
    public static void debug_linesSayWhatTheSoundSystemKeeps(final ClientTestContext ctx) {
        ctx.then(0, () -> {
                    ctx.mc().setScreen(null);
                    ctx.mc().getDebugOverlay().toggleOverlay();
                })
                .thenAssert(1, () -> {
                    final List<String> lines = AudioDebugLines.lines();
                    return lines.size() == 6 && lines.get(0).startsWith("Running: ")
                            && lines.get(0).endsWith("/24 short, 0/2 long") && lines.get(3).startsWith("Lowered")
                            && lines.get(5).startsWith("Turned off: ");
                }, "six lines: running out of the budget, rooms, muffled, lowering, last heard, turned off")
                .thenScreenshot(2, "debug")
                .then(0, () -> ctx.mc().getDebugOverlay().toggleOverlay());
    }

    /* A point a few blocks to the listener's right. */
    private static Vec3 toTheRight(final ClientTestContext ctx) {
        final ListenerTransform listener = ctx.mc().getSoundManager().getListenerTransform();
        return listener.position().add(listener.right().scale(6));
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
