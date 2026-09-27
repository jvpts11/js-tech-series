/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.music;

import dev.jstech.computers.audio.MusicPlayer;
import dev.jstech.computers.audio.SongFiles;
import dev.jstech.computers.audio.SoundfoundryTexts;
import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.client.audio.SoundfoundryStates;
import dev.jstech.computers.operation.payload.ClientPayloadHandlers;
import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.computers.operation.payload.SoundfoundryActionPayload;
import dev.jstech.computers.operation.payload.SoundfoundryStatePayload;
import dev.jstech.computers.operation.payload.files.FileAccess;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.FileType;
import dev.jstech.computers.os.fs.FsPaths;
import dev.jstech.computers.os.fs.RecordingFile;
import dev.jstech.computers.program.Programs;
import dev.jstech.computers.program.SoundfoundryState;
import dev.jstech.core.audio.media.MediaInfo;
import dev.jstech.core.text.Text;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Soundfoundry's payloads: what a player does in its window, carried out by the machine it runs on, and how the
 * machine then stands, sent back.
 *
 * <p>The window asks again every second while it is open, so a song that ends and gives way to the next is shown
 * within a second even though nobody touched anything; the playlist travels only when it has changed.
 */
public final class SoundfoundryPayloads {

    /* A saved playlist's first line, which is how the players of the time recognised one. */
    private static final String M3U_HEADER = "#EXTM3U";
    private static final String M3U_ENTRY = "#EXTINF:";

    private SoundfoundryPayloads() {
    }

    /** Registers the payloads this class handles. */
    public static void register(final PayloadRegistrar registrar) {
        ComputerAccess.accept(registrar, SoundfoundryActionPayload.TYPE, SoundfoundryActionPayload.STREAM_CODEC,
                ComputerAccess.machine(SoundfoundryActionPayload::hostPos), SoundfoundryPayloads::handleAction);
        registrar.playToClient(SoundfoundryStatePayload.TYPE, SoundfoundryStatePayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread((state, player) -> SoundfoundryStates.accept(state)));
    }

    /** How Soundfoundry stands on that machine, with the playlist when it is not the revision given. */
    public static SoundfoundryStatePayload stateOf(final ServerLevel level, final AbstractComputerBlockEntity computer,
                                                   final int knownRevision) {
        final SoundfoundryState state = computer.console().soundfoundry();
        final MusicPlayer music = computer.musicPlayer();
        final Optional<List<SoundfoundryStatePayload.Song>> songs = knownRevision == state.revision()
                ? Optional.empty() : Optional.of(songsOf(level, computer, state));
        final int status = music.playing(level) ? SoundfoundryStatePayload.PLAYING
                : music.paused(level) ? SoundfoundryStatePayload.PAUSED : SoundfoundryStatePayload.STOPPED;
        final MediaInfo info = music.info(level);
        final SoundfoundryStatePayload.Playing playing = info == null ? SoundfoundryStatePayload.Playing.NONE
                : new SoundfoundryStatePayload.Playing(info.millis(), info.kbps(), info.sampleRate(),
                info.channels());
        final boolean monitors = computer.monitorsPlay();
        final boolean speakers = computer.speakersPlay() && computer.speakerCount() > 0;
        final int output = !computer.playsRecordings() ? SoundfoundryStatePayload.OUT_NONE
                : monitors && speakers ? SoundfoundryStatePayload.OUT_BOTH
                : speakers ? SoundfoundryStatePayload.OUT_SPEAKERS
                : monitors ? SoundfoundryStatePayload.OUT_MONITOR : SoundfoundryStatePayload.OUT_NONE;
        return new SoundfoundryStatePayload(computer.getBlockPos(), state.revision(), songs, state.current(), status,
                music.position(level), playing, state.shuffle(), state.repeat(), state.volume(), state.balance(),
                computer.playsRecordings(), output, music.trouble());
    }

    private static void handleAction(final SoundfoundryActionPayload payload, final ServerPlayer player,
                                     final ServerLevel level) {
        if (!(level.getBlockEntity(payload.hostPos()) instanceof AbstractComputerBlockEntity computer)
                || !computer.console().isInstalled(Programs.SOUNDFOUNDRY.toString())) {
            return;
        }
        final int before = computer.console().soundfoundry().revision();
        final Text note = act(level, computer, payload);
        /*
         * The playlist goes back when the window's copy is stale: a look names the revision it holds, and an
         * action that changed the list makes any copy stale. Every other action leaves the window's copy as it is.
         */
        final int known = payload.action() == SoundfoundryActionPayload.LOOK ? payload.index()
                : computer.console().soundfoundry().revision() == before ? before : -1;
        final SoundfoundryStatePayload state = stateOf(level, computer, known);
        PacketDistributor.sendToPlayer(player, note.isEmpty() ? state : withTrouble(state, note));
    }

    /* Carries the action out, and answers what the player is told about it, or empty. */
    private static Text act(final ServerLevel level, final AbstractComputerBlockEntity computer,
                            final SoundfoundryActionPayload payload) {
        final MusicPlayer music = computer.musicPlayer();
        final SoundfoundryState state = computer.console().soundfoundry();
        Text note = Text.EMPTY;
        switch (payload.action()) {
            case SoundfoundryActionPayload.PLAY -> music.play(level, payload.index());
            case SoundfoundryActionPayload.PAUSE -> music.pause(level);
            case SoundfoundryActionPayload.STOP -> music.stop(level);
            case SoundfoundryActionPayload.NEXT -> music.next(level);
            case SoundfoundryActionPayload.PREVIOUS -> music.previous(level);
            case SoundfoundryActionPayload.SEEK -> music.seek(level, payload.value());
            case SoundfoundryActionPayload.VOLUME -> {
                state.setVolume(payload.index());
                music.refresh(level);
            }
            case SoundfoundryActionPayload.BALANCE -> {
                state.setBalance(payload.index());
                music.refresh(level);
            }
            case SoundfoundryActionPayload.SHUFFLE -> state.setShuffle(payload.index() != 0);
            case SoundfoundryActionPayload.REPEAT -> state.setRepeat(payload.index() != 0);
            case SoundfoundryActionPayload.ADD -> {
                final int before = state.size();
                note = add(state, songsAmong(level, computer, payload.paths()));
                // A song opened rather than added is played as well, the first of them if there are several.
                if (payload.index() == SoundfoundryActionPayload.AND_PLAY && state.size() > before) {
                    music.play(level, before);
                }
            }
            case SoundfoundryActionPayload.ADD_FOLDER -> note = addFolders(level, computer, state, payload.paths());
            case SoundfoundryActionPayload.REMOVE -> {
                final Set<Integer> gone = new HashSet<>(payload.indexes());
                if (gone.contains(state.current())) {
                    music.stop(level);
                }
                state.remove(gone);
            }
            case SoundfoundryActionPayload.CROP -> {
                final Set<Integer> kept = new HashSet<>(payload.indexes());
                if (!kept.contains(state.current())) {
                    music.stop(level);
                }
                state.keepOnly(kept);
            }
            case SoundfoundryActionPayload.CLEAR -> {
                music.stop(level);
                state.clear();
            }
            case SoundfoundryActionPayload.SORT_TITLE -> state.sort(byTitle(level, computer, state));
            case SoundfoundryActionPayload.SORT_FILE -> state.sort(Comparator.comparing(
                    (String path) -> FsPaths.fileName(path).toLowerCase(Locale.ROOT)));
            case SoundfoundryActionPayload.REVERSE -> state.reverse();
            case SoundfoundryActionPayload.OPEN_LIST -> note = openList(level, computer, state, payload.paths());
            case SoundfoundryActionPayload.SAVE_LIST -> note = saveList(level, computer, state, payload.paths());
            default -> {
                return Text.EMPTY;
            }
        }
        computer.setChanged();
        return note;
    }

    private static Text add(final SoundfoundryState state, final List<String> songs) {
        final int added = state.add(songs);
        return added < songs.size() ? SoundfoundryTexts.LIST_FULL.with(SoundfoundryState.MAX_SONGS) : Text.EMPTY;
    }

    private static Text addFolders(final ServerLevel level, final AbstractComputerBlockEntity computer,
                                   final SoundfoundryState state, final List<String> folders) {
        final List<String> songs = new ArrayList<>();
        for (final String folder : folders) {
            songs.addAll(SongFiles.under(level, computer, folder));
        }
        if (songs.isEmpty()) {
            return SoundfoundryTexts.NO_SONGS.with(folders.isEmpty() ? "" : FsPaths.fileName(folders.getFirst()));
        }
        return add(state, songs);
    }

    /* Only the paths that name a song this machine can reach: anything else is not put on a playlist. */
    private static List<String> songsAmong(final ServerLevel level, final AbstractComputerBlockEntity computer,
                                           final List<String> paths) {
        final List<String> songs = new ArrayList<>();
        for (final String path : paths) {
            if (SongFiles.read(level, computer, path) != null) {
                songs.add(path);
            }
        }
        return songs;
    }

    private static Comparator<String> byTitle(final ServerLevel level, final AbstractComputerBlockEntity computer,
                                              final SoundfoundryState state) {
        final Map<String, String> titles = new HashMap<>();
        for (final String path : state.songs()) {
            titles.computeIfAbsent(path, p -> shownAs(describe(level, computer, p)).toLowerCase(Locale.ROOT));
        }
        return Comparator.comparing(titles::get);
    }

    /* A saved playlist: its songs by path, one to a line, each after the line naming it the way the old players did. */
    private static Text saveList(final ServerLevel level, final AbstractComputerBlockEntity computer,
                                 final SoundfoundryState state, final List<String> paths) {
        if (paths.isEmpty()) {
            return Text.EMPTY;
        }
        final String target = paths.getFirst();
        final StringBuilder list = new StringBuilder(M3U_HEADER).append('\n');
        for (final String path : state.songs()) {
            final SoundfoundryStatePayload.Song song = describe(level, computer, path);
            list.append(M3U_ENTRY).append(song.millis() / 1000L).append(',').append(shownAs(song)).append('\n')
                    .append(path).append('\n');
        }
        final String content = list.toString();
        final ItemStack disk = computer.systemDisk();
        final DiskFilesystem.WriteResult result = DiskFilesystem.write(disk, target,
                FileType.of(extensionOf(target)), content,
                computer.systemDiskFreeWeight() + DiskFilesystem.weightOf(disk, target),
                FileAccess.filesystemKindOf(computer), level.getGameTime());
        return result == DiskFilesystem.WriteResult.OK
                ? SoundfoundryTexts.LIST_SAVED.with(FsPaths.fileName(target))
                : SoundfoundryTexts.LIST_NOT_SAVED.with(FsPaths.fileName(target));
    }

    /* A saved playlist in place of the one it had; a line that names no file there is kept, and shown as missing. */
    private static Text openList(final ServerLevel level, final AbstractComputerBlockEntity computer,
                                 final SoundfoundryState state, final List<String> paths) {
        if (paths.isEmpty()) {
            return Text.EMPTY;
        }
        final String source = paths.getFirst();
        final String content = SongFiles.content(level, computer, source).orElse(null);
        if (content == null || !content.startsWith(M3U_HEADER)) {
            return SoundfoundryTexts.NOT_A_LIST.with(FsPaths.fileName(source));
        }
        final String folder = FsPaths.parentDir(source);
        final List<String> songs = new ArrayList<>();
        for (final String line : content.split("\n")) {
            final String entry = line.strip();
            if (entry.isEmpty() || entry.startsWith("#")) {
                continue;
            }
            // A line written by another machine may name its song beside the list rather than from the root.
            songs.add(SongFiles.read(level, computer, entry) == null
                    && SongFiles.read(level, computer, FsPaths.join(folder, entry)) != null
                    ? FsPaths.join(folder, entry) : entry);
        }
        computer.musicPlayer().stop(level);
        state.clear();
        return add(state, songs);
    }

    private static List<SoundfoundryStatePayload.Song> songsOf(final ServerLevel level,
                                                               final AbstractComputerBlockEntity computer,
                                                               final SoundfoundryState state) {
        final List<SoundfoundryStatePayload.Song> songs = new ArrayList<>(state.size());
        for (final String path : state.songs()) {
            songs.add(describe(level, computer, path));
        }
        return songs;
    }

    private static SoundfoundryStatePayload.Song describe(final ServerLevel level,
                                                          final AbstractComputerBlockEntity computer,
                                                          final String path) {
        final RecordingFile song = SongFiles.read(level, computer, path);
        final String fileName = FsPaths.fileName(path);
        if (song == null) {
            return new SoundfoundryStatePayload.Song(path, RecordingFile.stemOf(fileName, fileName), "", 0L, false);
        }
        final String title = song.info().tags().title();
        return new SoundfoundryStatePayload.Song(path, title.isEmpty() ? RecordingFile.stemOf(fileName, fileName)
                : title, song.info().tags().artist(), song.info().millis(), true);
    }

    /* How a song is listed: its artist and its title, or its title alone. */
    private static String shownAs(final SoundfoundryStatePayload.Song song) {
        return song.artist().isEmpty() ? song.title() : song.artist() + " - " + song.title();
    }

    private static String extensionOf(final String path) {
        final String name = FsPaths.fileName(path);
        final int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(dot + 1) : "";
    }

    private static SoundfoundryStatePayload withTrouble(final SoundfoundryStatePayload state, final Text note) {
        return new SoundfoundryStatePayload(state.hostPos(), state.revision(), state.songs(), state.current(),
                state.status(), state.position(), state.playing(), state.shuffle(), state.repeat(), state.volume(),
                state.balance(), state.device(), state.output(), note);
    }
}
