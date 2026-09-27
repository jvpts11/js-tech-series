/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.audio;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.blockentity.SpeakerBlockEntity;
import dev.jstech.computers.os.fs.RecordingFile;
import dev.jstech.core.audio.Audio;
import dev.jstech.core.audio.AudioDevice;
import dev.jstech.core.audio.IAudioHost;
import dev.jstech.core.audio.StereoSide;
import dev.jstech.core.audio.media.MediaPlace;
import dev.jstech.core.audio.media.MediaSessions;
import dev.jstech.core.audio.media.MediaStore;
import dev.jstech.core.audio.pcm.Tone;
import dev.jstech.core.audio.pcm.Waveform;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * What the programs running on a computer play: beeps out of the speaker inside its case, tunes through its sound card
 * in the card's own voice, and recordings from its disks out of its monitors and speakers or out of one speaker named.
 *
 * <p>Everything takes voices of the machine's sound hardware ({@link MachineVoices}), so a program playing a chord on
 * an FM card with nine voices and a song besides can take the song's; a machine with no card beeps and plays its
 * tunes from the case, one note at a time. Everything stops when the machine goes off.
 */
public final class ProgramSounds {

    private final AbstractComputerBlockEntity computer;
    /** The recordings the machine's programs are playing, by the key each plays under. */
    private final Set<String> recordings = new HashSet<>();

    private static final String KEY = JsComputers.MODID + ":program/";
    private static final long TICK_MILLIS = 50L;
    /** How often the machine is looked at while its programs play, in ticks. */
    private static final int LOOK_EVERY = 10;

    public ProgramSounds(final AbstractComputerBlockEntity computer) {
        this.computer = computer;
    }

    /**
     * A beep out of the speaker inside the case, which cuts off the one before it.
     *
     * @param frequency its pitch in hertz, kept to what a speaker makes
     * @param millis    how long it lasts, kept to a minute
     */
    public void beep(final ServerLevel level, final int frequency, final int millis) {
        final float volume = volume();
        final int length = Math.clamp(millis, 1, 60_000);
        if (volume <= 0.0F) {
            return;
        }
        final Tone beep = Tone.beep(Math.clamp(frequency, 20, 20_000), length);
        final MachineVoices voices = computer.voices();
        final String voice = voices.name("beep");
        if (voices.take(level, true, voice, 1, level.getGameTime() + ticks(length), stopTones(voice))) {
            Audio.tones(level, computer.getBlockPos(), ComputingSounds.PC_SPEAKER,
                    List.of(new Tone(beep.wave(), beep.frequency(), beep.millis(), beep.volume() * volume)), voice);
        }
    }

    /**
     * Plays a tune written the way {@link ToneNotation} reads it: through the sound card in its voice, each note of a
     * chord taking a voice of its own, or from the case as square notes on a machine with no card, the first note of
     * each chord only.
     *
     * @return whether it plays; a machine whose sound is off, or that makes no sound, plays nothing
     * @throws IllegalArgumentException when the tune has an item that is no note, naming it
     */
    public boolean tones(final ServerLevel level, final String tune) {
        final IAudioHost host = computer.audioHost();
        final AudioDevice device = host.audioDevice();
        final MachineVoices voices = computer.voices();
        final boolean inCase = voices.inCase();
        final List<List<Tone>> read = ToneNotation.parts(tune, inCase ? Waveform.SQUARE : device.timbre());
        // The speaker in the case has one voice, so of each chord it plays the first note: the tune's own line.
        final List<List<Tone>> parts = inCase && read.size() > 1 ? read.subList(0, 1) : read;
        final float volume = volume();
        if (parts.isEmpty() || !device.audible() || volume <= 0.0F) {
            return false;
        }
        final long endsAt = level.getGameTime() + ticks(ToneNotation.millisOf(parts));
        for (final List<Tone> part : parts) {
            final String voice = voices.name("tones");
            if (!voices.take(level, inCase, voice, 1, endsAt, stopTones(voice))) {
                return false;
            }
            if (inCase) {
                Audio.tones(level, computer.getBlockPos(), ComputingSounds.PC_SPEAKER, scaled(part, volume), voice);
            } else {
                Audio.tones(level, host, ComputingSounds.PROGRAM_TONES, part, voice);
            }
        }
        return true;
    }

    /**
     * Plays a recording from the machine's disks out of its monitors and speakers, in place of the one its programs
     * were playing.
     *
     * @return whether it plays: not when the file is no song, the server does not keep it, or the machine's sound
     *         plays no recordings
     */
    public boolean play(final ServerLevel level, final String path) {
        final RecordingFile song = playable(level, path);
        if (song == null) {
            return false;
        }
        final String key = KEY + computer.getBlockPos().asLong();
        MediaSessions.play(level, key, computer.audioHost(), ComputingSounds.MUSIC, song.media(), 1.0F, 0L,
                () -> ended(key));
        return started(level, key, song, computer.audioHost().audioDevice().stereo());
    }

    /**
     * Plays a recording out of one of the machine's speakers alone, in place of the one its programs were playing
     * there, whichever output the system chose.
     *
     * @return whether it plays, as {@link #play}
     */
    public boolean playOn(final ServerLevel level, final SpeakerBlockEntity speaker, final String path) {
        final RecordingFile song = playable(level, path);
        if (song == null) {
            return false;
        }
        final AudioDevice device = computer.audioHost().audioDevice();
        final Vec3 at = Vec3.atCenterOf(speaker.getBlockPos());
        final StereoSide side = device.stereo() ? computer.speakerSide(speaker.getBlockPos()) : StereoSide.BOTH;
        final MediaPlace place = new MediaPlace(at.x, at.y, at.z, side, device.response().through(speaker.response()));
        final String key = KEY + computer.getBlockPos().asLong() + "/" + speaker.getBlockPos().asLong();
        MediaSessions.play(level, key, ComputingSounds.MUSIC, song.media(), List.of(place),
                computer.audioHost().audioVolume(), 0L, () -> ended(key));
        return started(level, key, song, false);
    }

    /** Stops every recording the machine's programs are playing. */
    public void stop(final ServerLevel level) {
        for (final String key : List.copyOf(recordings)) {
            MediaSessions.stop(level, key);
            computer.voices().release(key);
        }
        recordings.clear();
    }

    /** Whether a recording the machine's programs started is still playing. */
    public boolean playing(final ServerLevel level) {
        return recordings.stream().anyMatch(key -> MediaSessions.has(level, key));
    }

    /** The speaker of that name linked to the machine, or null when none is called so. */
    @Nullable
    public SpeakerBlockEntity speaker(final String name) {
        for (final SpeakerBlockEntity speaker : computer.linkedSpeakers()) {
            if (speaker.name().equals(name)) {
                return speaker;
            }
        }
        return null;
    }

    /** Looks at the machine now and then: one that has gone off stops what its programs play. */
    public void tick(final ServerLevel level) {
        if (recordings.isEmpty() || level.getGameTime() % LOOK_EVERY != 0) {
            return;
        }
        if (!computer.isRunning()) {
            stop(level);
            return;
        }
        recordings.removeIf(key -> !MediaSessions.has(level, key));
    }

    /* The song at that path, when the machine can play it at all. */
    @Nullable
    private RecordingFile playable(final ServerLevel level, final String path) {
        if (!computer.playsRecordings() || volume() <= 0.0F) {
            return null;
        }
        final RecordingFile song = SongFiles.read(level, computer, path);
        return song != null && MediaStore.current().map(store -> store.has(song.media())).orElse(false) ? song : null;
    }

    /* A recording is under way: it takes its voices, two for stereo on a stereo device, until it ends. */
    private boolean started(final ServerLevel level, final String key, final RecordingFile song,
                            final boolean stereoPlaces) {
        recordings.add(key);
        final int voices = stereoPlaces && song.info().channels() >= 2 ? 2 : 1;
        computer.voices().take(level, false, key, voices, level.getGameTime() + ticks(song.info().millis()),
                stopped -> {
                    MediaSessions.stop(stopped, key);
                    recordings.remove(key);
                });
        return true;
    }

    private void ended(final String key) {
        recordings.remove(key);
        computer.voices().release(key);
    }

    /* How loud the machine's sound plays, nothing while it is muted. */
    private float volume() {
        return Math.min(1.0F, computer.audioHost().audioVolume());
    }

    private static Consumer<ServerLevel> stopTones(final String voice) {
        return level -> Audio.stopVoice(level, voice);
    }

    private static List<Tone> scaled(final List<Tone> part, final float volume) {
        final List<Tone> out = new ArrayList<>(part.size());
        for (final Tone tone : part) {
            out.add(new Tone(tone.wave(), tone.frequency(), tone.millis(), tone.volume() * volume));
        }
        return out;
    }

    private static long ticks(final long millis) {
        return Math.max(1L, (millis + TICK_MILLIS - 1L) / TICK_MILLIS);
    }
}
