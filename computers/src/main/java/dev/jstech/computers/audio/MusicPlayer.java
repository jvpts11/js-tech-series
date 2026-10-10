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
import dev.jstech.computers.blockentity.ServerServices;
import dev.jstech.computers.os.fs.RecordingFile;
import dev.jstech.computers.program.SoundfoundryState;
import dev.jstech.core.audio.media.MediaInfo;
import dev.jstech.core.audio.media.MediaPlace;
import dev.jstech.core.audio.media.MediaSessions;
import dev.jstech.core.audio.media.MediaStore;
import dev.jstech.core.text.Text;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.random.RandomGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

/**
 * Soundfoundry playing on one computer: the song it is on, out of the computer's own monitors and speakers, as its
 * sound hardware plays it, at the player's volume and balance on top of the system's own volume.
 *
 * <p>It is part of the computer rather than of the window, so the music plays on with the screen closed, the next
 * song follows when one ends, and it stops when the machine goes off. What it plays and how is the machine's
 * {@link SoundfoundryState}.
 */
public final class MusicPlayer {

    private final AbstractComputerBlockEntity computer;
    /* Where the song was last sent to be heard from and how loud, so a change to either is noticed. */
    private List<MediaPlace> heardFrom = List.of();
    private float heardAt = -1.0F;
    /* Why the last song could not play, for the screen; empty when it could. */
    private Text trouble = Text.EMPTY;
    /* What the song playing is, kept from when it started so a screen asking every second reads nothing. */
    @Nullable
    private SongSources.Source on;
    /* The Soundfoundry Server the song streams from, or null for a song played from the machine's own disk. */
    @Nullable
    private String streamingFrom;
    private String streamingName = "";

    private static final String KEY = JsComputers.MODID + ":soundfoundry/";
    private static final long TICK_MILLIS = 50L;
    /** How often the machine is looked at while it plays, in ticks: its power, its outputs, its volume. */
    private static final int LOOK_EVERY = 10;
    private static final RandomGenerator SHUFFLING = new Random();

    public MusicPlayer(final AbstractComputerBlockEntity computer) {
        this.computer = computer;
    }

    /** What this computer's music plays under, among every recording playing in its world. */
    public String key() {
        return keyOf(computer.getBlockPos());
    }

    /** What the music of the computer at {@code host} plays under, which is how a screen finds it to show it. */
    public static String keyOf(final BlockPos host) {
        return KEY + host.asLong();
    }

    /** Why the last song could not play, or empty when it could. */
    public Text trouble() {
        return trouble;
    }

    /** Whether a song is playing, not paused. */
    public boolean playing(final ServerLevel level) {
        return MediaSessions.has(level, key()) && !MediaSessions.paused(level, key());
    }

    /** Whether a song is paused. */
    public boolean paused(final ServerLevel level) {
        return MediaSessions.paused(level, key());
    }

    /** How far into its song it has got, in milliseconds; 0 when nothing plays. */
    public long position(final ServerLevel level) {
        return Math.max(0L, MediaSessions.position(level, key()));
    }

    /** What the song it is on is, or null when it is on none or the song cannot be read. */
    @Nullable
    public MediaInfo info(final ServerLevel level) {
        final SoundfoundryState state = state();
        if (state.current() < 0 || state.current() >= state.size()) {
            return null;
        }
        if (on != null && MediaSessions.has(level, key())) {
            return on.info();
        }
        final SongSources.Described song = SongSources.describe(level, computer, state.song(state.current()));
        return song == null ? null : song.info();
    }

    /** Whether the song playing streams from a Soundfoundry Server rather than playing from the machine's disk. */
    public boolean streaming(final ServerLevel level) {
        return streamingFrom != null && MediaSessions.has(level, key());
    }

    /**
     * Plays the song at that place in the playlist from its start. Given -1, it takes a paused song up again, or
     * plays the song it is on, or the first.
     */
    public void play(final ServerLevel level, final int index) {
        if (index >= 0) {
            start(level, index, 0L);
            return;
        }
        if (MediaSessions.paused(level, key())) {
            resume(level);
        } else if (!MediaSessions.has(level, key())) {
            start(level, Math.max(0, state().current()), 0L);
        }
    }

    /** Pauses the song, or takes a paused one up again; a paused song holds no voice of the sound card. */
    public void pause(final ServerLevel level) {
        if (MediaSessions.paused(level, key())) {
            resume(level);
        } else if (MediaSessions.has(level, key())) {
            MediaSessions.pause(level, key());
            computer.voices().release(key());
        }
    }

    /** Stops the song; it stays the one it is on, to play from the start. */
    public void stop(final ServerLevel level) {
        MediaSessions.stop(level, key());
        computer.voices().release(key());
        leaveServer(level);
    }

    /** Goes to the next song: playing it if one was playing, else only choosing it. */
    public void next(final ServerLevel level) {
        go(level, state().next(true, SHUFFLING));
    }

    /** Goes to the song before, the same way. */
    public void previous(final ServerLevel level) {
        go(level, state().previous(SHUFFLING));
    }

    /** Jumps to that point of the song, keeping it paused if it was. */
    public void seek(final ServerLevel level, final long millis) {
        if (!MediaSessions.has(level, key())) {
            return;
        }
        final boolean wasPaused = MediaSessions.paused(level, key());
        start(level, state().current(), Math.max(0L, millis));
        if (wasPaused) {
            MediaSessions.pause(level, key());
            // start took voices for the rest of the song; a paused song holds none.
            computer.voices().release(key());
        }
    }

    /** Its volume or balance changed: the song goes on from where it is, the new way. */
    public void refresh(final ServerLevel level) {
        if (MediaSessions.has(level, key())) {
            heardFrom = places();
            heardAt = volume();
            MediaSessions.replace(level, key(), heardFrom, heardAt);
        }
    }

    /**
     * Looks at the machine while a song plays: one that has gone off stops it, and new outputs or a new volume are
     * heard from where the song has got to.
     */
    public void tick(final ServerLevel level) {
        if (level.getGameTime() % LOOK_EVERY != 0 || !MediaSessions.has(level, key())) {
            return;
        }
        if (!computer.isRunning()) {
            stop(level);
            return;
        }
        final Text why = MediaSessions.paused(level, key()) ? Text.EMPTY : streamTrouble(level);
        if (!why.isEmpty()) {
            stop(level);
            trouble = why;
            return;
        }
        final List<MediaPlace> places = places();
        final float volume = volume();
        if (!places.equals(heardFrom) || volume != heardAt) {
            heardFrom = places;
            heardAt = volume;
            MediaSessions.replace(level, key(), places, volume);
        }
    }

    /** The machine is gone from the world, broken or unloaded: its music stops. */
    public void removed(final ServerLevel level) {
        MediaSessions.stop(level, key());
        computer.voices().release(key());
        leaveServer(level);
    }

    /*
     * A paused song taken up again takes its voices again, for what is left of it, and a streamed one asks its server
     * to stream to it again, which a server with no memory left refuses.
     */
    private void resume(final ServerLevel level) {
        final Text why = streamTrouble(level);
        if (!why.isEmpty()) {
            stop(level);
            trouble = why;
            return;
        }
        MediaSessions.resume(level, key());
        final MediaInfo info = info(level);
        if (info != null) {
            takeVoices(level, info, MediaSessions.position(level, key()));
        }
    }

    /*
     * Tells the server the song streams from that this computer still listens, and answers why it will not stream
     * any more, or empty when it will or the song plays from the disk.
     */
    private Text streamTrouble(final ServerLevel level) {
        if (streamingFrom == null) {
            return Text.EMPTY;
        }
        final ServerServices.Host host = SoundfoundryServers.byId(level, computer, streamingFrom);
        if (host == null) {
            return SoundfoundryTexts.SERVER_GONE.with(streamingName);
        }
        return SoundfoundryServers.listen(level, host, computer) ? Text.EMPTY
                : SoundfoundryTexts.SERVER_FULL.with(streamingName);
    }

    /* The song no longer streams: the server it streamed from lets its stream go. */
    private void leaveServer(final ServerLevel level) {
        if (streamingFrom != null) {
            final ServerServices.Host host = SoundfoundryServers.byId(level, computer, streamingFrom);
            if (host != null) {
                SoundfoundryServers.leave(host, computer);
            }
        }
        streamingFrom = null;
        streamingName = "";
        on = null;
    }

    /*
     * The song takes voices of the sound card, two for a stereo song on a card that plays both sides and one
     * otherwise, until its end; another sound taking them stops it, and says so on the screen.
     */
    private void takeVoices(final ServerLevel level, final MediaInfo info, final long from) {
        final boolean stereo = computer.audioHost().audioDevice().stereo() && info.channels() >= 2;
        final long left = Math.max(1L, (info.millis() - Math.max(0L, from)) / TICK_MILLIS);
        computer.voices().take(level, false, key(), stereo ? 2 : 1, level.getGameTime() + left, stopped -> {
            MediaSessions.stop(stopped, key());
            trouble = SoundfoundryTexts.VOICES_TAKEN.text();
        });
    }

    private void go(final ServerLevel level, final int index) {
        if (index < 0) {
            stop(level);
            return;
        }
        if (MediaSessions.has(level, key())) {
            start(level, index, 0L);
        } else {
            state().select(index);
        }
    }

    private void start(final ServerLevel level, final int index, final long from) {
        final SoundfoundryState state = state();
        if (index < 0 || index >= state.size()) {
            stop(level);
            return;
        }
        state.select(index);
        computer.setChanged();
        trouble = Text.EMPTY;
        if (!computer.playsRecordings()) {
            trouble = SoundfoundryTexts.NO_DEVICE.text();
            stop(level);
            return;
        }
        final SongSources.Found found = SongSources.find(level, computer, state.song(index));
        final SongSources.Source song = found.source();
        // Whatever the last song streamed from is let go of first, so a server is not asked to hold two streams.
        leaveServer(level);
        if (song == null) {
            trouble = found.trouble();
            stop(level);
            return;
        }
        if (song.server() != null) {
            if (!SoundfoundryServers.listen(level, song.server(), computer)) {
                trouble = SoundfoundryTexts.SERVER_FULL.with(song.server().hostname());
                stop(level);
                return;
            }
            streamingFrom = song.server().id();
            streamingName = song.server().hostname();
        }
        on = song;
        heardFrom = places();
        heardAt = volume();
        final BlockPos at = computer.getBlockPos();
        MediaSessions.play(level, key(), ComputingSounds.MUSIC, song.media(), heardFrom, heardAt, from,
                () -> ended(level, at));
        takeVoices(level, song.info(), from);
    }

    /* A song played to its end: the next one, as the list's order and repeat say, or silence after the last. */
    private static void ended(final ServerLevel level, final BlockPos at) {
        if (!(level.getBlockEntity(at) instanceof AbstractComputerBlockEntity computer) || !computer.isRunning()) {
            return;
        }
        final SoundfoundryState state = computer.console().soundfoundry();
        final int next = state.next(state.repeat(), SHUFFLING);
        if (next >= 0) {
            computer.musicPlayer().start(level, next, 0L);
        }
    }

    /* The computer's outputs as its sound hardware plays them, each turned by the balance for its side. */
    private List<MediaPlace> places() {
        final SoundfoundryState state = state();
        final List<MediaPlace> places = new ArrayList<>();
        for (final MediaPlace place : MediaSessions.placesOf(computer.audioHost())) {
            places.add(place.withGain(state.gainFor(place.side())));
        }
        return places;
    }

    private float volume() {
        return state().volume() / (float) SoundfoundryState.MAX_VOLUME * computer.audioHost().audioVolume();
    }

    private SoundfoundryState state() {
        return computer.console().soundfoundry();
    }

    /* How long the recording a song names runs, read from the server's store, or 0 when it cannot be read. */
    static long millisOf(final RecordingFile song) {
        if (song.info().millis() > 0) {
            return song.info().millis();
        }
        return MediaStore.current().map(store -> {
            try {
                return store.info(song.media()).millis();
            } catch (final IOException unreadable) {
                return 0L;
            }
        }).orElse(0L);
    }
}
