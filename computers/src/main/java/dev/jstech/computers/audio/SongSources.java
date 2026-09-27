/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.audio;

import dev.jstech.computers.audio.catalog.CatalogAlbum;
import dev.jstech.computers.audio.catalog.CatalogTrack;
import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.blockentity.ServerServices;
import dev.jstech.computers.os.fs.RecordingFile;
import dev.jstech.computers.program.SongRefs;
import dev.jstech.core.audio.media.MediaId;
import dev.jstech.core.audio.media.MediaInfo;
import dev.jstech.core.audio.media.MediaStore;
import dev.jstech.core.text.Text;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

/**
 * Where a song on Soundfoundry's list plays from: a file on the machine's disks from the disk; a song of the
 * catalogue or of the network's library from the machine's own disk when it keeps the same recording, which needs no
 * server, and otherwise streamed from the Soundfoundry Server it picked.
 */
public final class SongSources {

    private SongSources() {
    }

    /**
     * Where a song plays from and what it is.
     *
     * @param server the server it streams from, or null for a song on the machine's own disk
     */
    public record Source(MediaId media, MediaInfo info, String title, String artist,
                         @Nullable ServerServices.Host server) {

        public boolean streamed() {
            return server != null;
        }
    }

    /**
     * A song looked for: where it plays from, or why it cannot play.
     *
     * @param source where it plays from, or null when it cannot
     */
    public record Found(@Nullable Source source, Text trouble) {

        static Found of(final Source source) {
            return new Found(source, Text.EMPTY);
        }

        static Found not(final Text why) {
            return new Found(null, why);
        }
    }

    /** What a song is, as a screen lists it: the recording, what it is called and the album it is on. */
    public record Described(MediaId media, String title, String artist, String album, MediaInfo info) {
    }

    /**
     * What the song the list names so is, read without asking where it would play from, which is what a screen
     * showing the list needs every second; null when it cannot be read.
     */
    @Nullable
    public static Described describe(final ServerLevel level, final AbstractComputerBlockEntity computer,
                                     final String ref) {
        final String path = SongRefs.pathOf(ref);
        if (SongRefs.fromCatalog(ref)) {
            final CatalogTrack track = SoundfoundryShare.catalogTrack(path);
            final CatalogAlbum album = SoundfoundryShare.catalogAlbum(path);
            return track == null ? null : new Described(track.media(), track.title(), track.artist(),
                    album == null ? track.info().tags().album() : album.title(), track.info());
        }
        final RecordingFile song;
        if (SongRefs.fromNetwork(ref)) {
            final ServerServices.Host host = SoundfoundryServers.chosen(level, computer);
            song = host == null ? null : SoundfoundryServers.song(level, host, path);
        } else {
            song = SongFiles.read(level, computer, path);
        }
        return song == null ? null : new Described(song.media(), titleOf(song, path), song.info().tags().artist(),
                song.info().tags().album(), song.info());
    }

    /** Where the song the list names so plays from on that computer. */
    public static Found find(final ServerLevel level, final AbstractComputerBlockEntity computer, final String ref) {
        final String path = SongRefs.pathOf(ref);
        if (SongRefs.fromCatalog(ref)) {
            return catalog(level, computer, path);
        }
        if (SongRefs.fromNetwork(ref)) {
            return network(level, computer, path);
        }
        final RecordingFile song = SongFiles.read(level, computer, path);
        if (song == null) {
            return Found.not(SoundfoundryTexts.MISSING.with(SongFiles.nameOf(path)));
        }
        if (!kept(song.media())) {
            return Found.not(SoundfoundryTexts.NOT_KEPT.with(SongFiles.nameOf(path)));
        }
        return Found.of(new Source(song.media(), song.info(), titleOf(song, path), song.info().tags().artist(),
                null));
    }

    /** The songs of the machine's music folder, by the recording each names: what it has downloaded or brought. */
    public static Map<MediaId, String> local(final ServerLevel level, final AbstractComputerBlockEntity computer) {
        final Map<MediaId, String> found = new HashMap<>();
        for (final String path : SongFiles.under(level, computer, MusicImports.musicFolderOf(computer))) {
            final RecordingFile song = SongFiles.read(level, computer, path);
            if (song != null) {
                found.putIfAbsent(song.media(), path);
            }
        }
        return found;
    }

    /** A song's title: its tag, or its file's name without the kind. */
    public static String titleOf(final RecordingFile song, final String path) {
        final String title = song.info().tags().title();
        final String file = SongFiles.nameOf(path);
        return title.isEmpty() ? RecordingFile.stemOf(file, file) : title;
    }

    private static Found catalog(final ServerLevel level, final AbstractComputerBlockEntity computer,
                                 final String path) {
        final CatalogTrack track = SoundfoundryShare.catalogTrack(path);
        if (track == null) {
            return Found.not(SoundfoundryTexts.NOT_IN_CATALOG.with(SongFiles.nameOf(path)));
        }
        return streamedOrLocal(level, computer, track.media(), track.info(), track.title(), track.artist());
    }

    private static Found network(final ServerLevel level, final AbstractComputerBlockEntity computer,
                                 final String path) {
        final ServerServices.Host host = SoundfoundryServers.chosen(level, computer);
        if (host == null) {
            return Found.not(SoundfoundryTexts.NO_SERVER.with(SongFiles.nameOf(path)));
        }
        final RecordingFile song = SoundfoundryServers.song(level, host, path);
        if (song == null) {
            return Found.not(SoundfoundryTexts.NOT_IN_LIBRARY.with(SongFiles.nameOf(path)));
        }
        return streamedOrLocal(level, computer, song.media(), song.info(), titleOf(song, path),
                song.info().tags().artist());
    }

    /* The machine's own copy of the recording when it has one, else the stream of the server it picked. */
    private static Found streamedOrLocal(final ServerLevel level, final AbstractComputerBlockEntity computer,
                                         final MediaId media, final MediaInfo info, final String title,
                                         final String artist) {
        if (!kept(media)) {
            return Found.not(SoundfoundryTexts.NOT_KEPT.with(title));
        }
        if (local(level, computer).containsKey(media)) {
            return Found.of(new Source(media, info, title, artist, null));
        }
        final ServerServices.Host host = SoundfoundryServers.chosen(level, computer);
        return host == null ? Found.not(SoundfoundryTexts.NO_SERVER.with(title))
                : Found.of(new Source(media, info, title, artist, host));
    }

    private static boolean kept(final MediaId media) {
        return MediaStore.current().map(store -> store.has(media)).orElse(false);
    }
}
