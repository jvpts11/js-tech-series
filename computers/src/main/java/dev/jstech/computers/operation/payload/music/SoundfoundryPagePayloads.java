/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.music;

import dev.jstech.computers.audio.MusicDownloads;
import dev.jstech.computers.audio.MusicImports;
import dev.jstech.computers.audio.SongFiles;
import dev.jstech.computers.audio.SongSearch;
import dev.jstech.computers.audio.SongSources;
import dev.jstech.computers.audio.SoundfoundryPlaylists;
import dev.jstech.computers.audio.SoundfoundryServers;
import dev.jstech.computers.audio.SoundfoundryShare;
import dev.jstech.computers.audio.SoundfoundryTexts;
import dev.jstech.computers.audio.catalog.CatalogAlbum;
import dev.jstech.computers.audio.catalog.CatalogTrack;
import dev.jstech.computers.audio.catalog.SoundfoundryCatalog;
import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.blockentity.ServerServices;
import dev.jstech.computers.client.audio.SoundfoundryPages;
import dev.jstech.computers.operation.payload.ClientPayloadHandlers;
import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.computers.operation.payload.SoundfoundryActionPayload;
import dev.jstech.computers.operation.payload.SoundfoundryBrowsePayload;
import dev.jstech.computers.operation.payload.SoundfoundryPagePayload;
import dev.jstech.computers.operation.payload.SoundfoundryPagePayload.Album;
import dev.jstech.computers.operation.payload.SoundfoundryPagePayload.Row;
import dev.jstech.computers.os.fs.RecordingFile;
import dev.jstech.computers.program.Programs;
import dev.jstech.computers.program.SongDownload;
import dev.jstech.computers.program.SongRefs;
import dev.jstech.computers.program.SoundfoundryState;
import dev.jstech.core.audio.media.MediaId;
import dev.jstech.core.text.Text;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.jetbrains.annotations.Nullable;

/**
 * The pages of the Standard Soundfoundry, as the machine answers them, and what a player does on them: playing a
 * page's songs, downloading, sending a song to the network's library, picking a Soundfoundry Server, and keeping
 * playlists.
 *
 * <p>The catalogue and the network's library reach a Standard Soundfoundry only through a Soundfoundry Server on its
 * network; with none, it has its own files and nothing else.
 */
public final class SoundfoundryPagePayloads {

    /** How many albums, and how many songs of each kind, the home page shows. */
    private static final int HOME_ALBUMS = 8;
    private static final int HOME_ROWS = 8;
    /** The most songs a search finds. */
    private static final int MAX_FOUND = 200;

    private SoundfoundryPagePayloads() {
    }

    /** Registers the payloads this class handles. */
    public static void register(final PayloadRegistrar registrar) {
        ComputerAccess.accept(registrar, SoundfoundryBrowsePayload.TYPE, SoundfoundryBrowsePayload.STREAM_CODEC,
                ComputerAccess.machine(SoundfoundryBrowsePayload::hostPos), SoundfoundryPagePayloads::handleBrowse);
        registrar.playToClient(SoundfoundryPagePayload.TYPE, SoundfoundryPagePayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread((page, player) -> SoundfoundryPages.accept(page)));
    }

    /** The page as that machine answers it. */
    public static SoundfoundryPagePayload pageOf(final ServerLevel level, final AbstractComputerBlockEntity computer,
                                                 final int page, final String arg) {
        final Look look = new Look(level, computer);
        final List<Album> albums = new ArrayList<>();
        if (look.catalog) {
            final List<CatalogAlbum> all = SoundfoundryCatalog.current().albums();
            if (page == SoundfoundryPagePayload.HOME || page == SoundfoundryPagePayload.CATALOG) {
                final int shown = page == SoundfoundryPagePayload.HOME ? Math.min(HOME_ALBUMS, all.size())
                        : all.size();
                for (int i = 0; i < shown; i++) {
                    albums.add(look.album(all.get(i)));
                }
            } else if (page == SoundfoundryPagePayload.ALBUM) {
                final CatalogAlbum album = albumOf(arg);
                if (album != null) {
                    albums.add(look.album(album));
                }
            }
        }
        return new SoundfoundryPagePayload(computer.getBlockPos(), page, arg, look.sidebar(), albums,
                rowsOf(look, page, arg));
    }

    /**
     * Carries out an action of the Standard's pages.
     *
     * @return what the player is told, or empty
     */
    public static Text act(final ServerLevel level, final AbstractComputerBlockEntity computer,
                           final SoundfoundryActionPayload payload) {
        final String first = payload.paths().isEmpty() ? "" : payload.paths().getFirst();
        final MusicDownloads downloads = computer.musicDownloads();
        return switch (payload.action()) {
            case SoundfoundryActionPayload.PLAY_PAGE -> playPage(level, computer, payload, first);
            case SoundfoundryActionPayload.DOWNLOAD -> download(level, computer, first);
            case SoundfoundryActionPayload.DOWNLOAD_ALBUM -> downloadAlbum(level, computer, first);
            case SoundfoundryActionPayload.UPLOAD -> downloads.upload(level, first);
            case SoundfoundryActionPayload.PICK_SERVER -> {
                if (computer.musicPlayer().streaming(level)) {
                    computer.musicPlayer().stop(level);
                }
                computer.console().soundfoundry().setServer(first);
                yield Text.EMPTY;
            }
            case SoundfoundryActionPayload.LIKE -> like(level, computer, first);
            case SoundfoundryActionPayload.PLAYLIST_ADD -> payload.paths().size() < 2 ? Text.EMPTY
                    : addTo(level, computer, first, payload.paths().get(1));
            case SoundfoundryActionPayload.PLAYLIST_REMOVE -> removeFrom(level, computer, first, payload.index());
            case SoundfoundryActionPayload.PLAYLIST_NEW -> newPlaylist(level, computer, first.strip());
            case SoundfoundryActionPayload.PLAYLIST_DELETE -> SoundfoundryPlaylists.delete(level, computer, first)
                    ? Text.EMPTY : SoundfoundryTexts.PLAYLIST_NOT_SAVED.with(first);
            default -> Text.EMPTY;
        };
    }

    private static void handleBrowse(final SoundfoundryBrowsePayload payload, final ServerPlayer player,
                                     final ServerLevel level) {
        if (!(level.getBlockEntity(payload.hostPos()) instanceof AbstractComputerBlockEntity computer)
                || !computer.console().isInstalled(Programs.SOUNDFOUNDRY.toString())) {
            return;
        }
        PacketDistributor.sendToPlayer(player, pageOf(level, computer, payload.page(), payload.arg()));
    }

    /* Every song the page lists, each in its section. */
    private static List<Row> rowsOf(final Look look, final int page, final String arg) {
        final List<Row> rows = new ArrayList<>();
        switch (page) {
            case SoundfoundryPagePayload.HOME -> {
                if (look.server != null) {
                    final List<String> library = SoundfoundryServers.library(look.level, look.server);
                    for (final String path : library.subList(0, Math.min(HOME_ROWS, library.size()))) {
                        look.network(path, SoundfoundryPagePayload.NETWORK, rows);
                    }
                }
                final List<String> local = look.localSongs();
                for (final String path : local.subList(0, Math.min(HOME_ROWS, local.size()))) {
                    look.own(path, SoundfoundryPagePayload.ON_DISK, SoundfoundryPagePayload.DOWNLOADED, rows);
                }
            }
            case SoundfoundryPagePayload.LIBRARY -> {
                for (final String path : look.localSongs()) {
                    look.own(path, look.sending(path), SoundfoundryPagePayload.TRACKS, rows);
                }
            }
            case SoundfoundryPagePayload.ALBUM -> {
                final CatalogAlbum album = look.catalog ? albumOf(arg) : null;
                if (album != null) {
                    for (final CatalogTrack track : album.tracks()) {
                        rows.add(look.track(album, track, SoundfoundryPagePayload.TRACKS));
                    }
                }
            }
            case SoundfoundryPagePayload.PLAYLIST -> {
                for (final String ref : SoundfoundryPlaylists.read(look.level, look.computer, arg)) {
                    rows.add(look.listed(ref));
                }
            }
            case SoundfoundryPagePayload.SEARCH -> search(look, new SongSearch(arg), rows);
            default -> {
                // The catalogue's page lists albums only.
            }
        }
        return rows;
    }

    /* The catalogue's songs first, then the network's library, then the machine's own. */
    private static void search(final Look look, final SongSearch search, final List<Row> rows) {
        if (search.everything()) {
            return;
        }
        if (look.catalog) {
            for (final CatalogAlbum album : SoundfoundryCatalog.current().albums()) {
                for (final CatalogTrack track : album.tracks()) {
                    if (rows.size() >= MAX_FOUND) {
                        return;
                    }
                    if (search.matches(track.title(), track.artist(), album.title(), album.artist())) {
                        rows.add(look.track(album, track, SoundfoundryPagePayload.TRACKS));
                    }
                }
            }
        }
        if (look.server != null) {
            for (final String path : SoundfoundryServers.library(look.level, look.server)) {
                if (rows.size() >= MAX_FOUND) {
                    return;
                }
                final RecordingFile song = SoundfoundryServers.song(look.level, look.server, path);
                if (song != null && search.matches(SongSources.titleOf(song, path), song.info().tags().artist(),
                        song.info().tags().album())) {
                    look.network(path, SoundfoundryPagePayload.TRACKS, rows);
                }
            }
        }
        for (final String path : look.localSongs()) {
            if (rows.size() >= MAX_FOUND) {
                return;
            }
            final RecordingFile song = SongFiles.read(look.level, look.computer, path);
            if (song != null && search.matches(SongSources.titleOf(song, path), song.info().tags().artist(),
                    song.info().tags().album())) {
                look.own(path, SoundfoundryPagePayload.LOCAL, SoundfoundryPagePayload.TRACKS, rows);
            }
        }
    }

    /* The page's songs of that section take the list, and the one picked plays. */
    private static Text playPage(final ServerLevel level, final AbstractComputerBlockEntity computer,
                                 final SoundfoundryActionPayload payload, final String arg) {
        final int section = payload.indexes().isEmpty() ? SoundfoundryPagePayload.TRACKS
                : payload.indexes().getFirst();
        final List<String> refs = new ArrayList<>();
        for (final Row row : rowsOf(new Look(level, computer), payload.index(), arg)) {
            if (row.section() == section) {
                refs.add(row.ref());
            }
        }
        final int at = (int) payload.value();
        if (at < 0 || at >= refs.size()) {
            return Text.EMPTY;
        }
        computer.musicPlayer().stop(level);
        computer.console().soundfoundry().replace(refs, at);
        computer.musicPlayer().play(level, at);
        return Text.EMPTY;
    }

    private static Text download(final ServerLevel level, final AbstractComputerBlockEntity computer,
                                 final String ref) {
        if (SongRefs.fromCatalog(ref)) {
            return computer.musicDownloads().start(level, SongDownload.FROM_CATALOG, SongRefs.pathOf(ref), false);
        }
        return SongRefs.fromNetwork(ref)
                ? computer.musicDownloads().startFromServer(level, SongRefs.pathOf(ref), false) : Text.EMPTY;
    }

    /* Every song of the album the machine has neither on its disk nor on its way; the last trouble is told. */
    private static Text downloadAlbum(final ServerLevel level, final AbstractComputerBlockEntity computer,
                                      final String id) {
        final CatalogAlbum album = albumOf(id);
        if (album == null) {
            return Text.EMPTY;
        }
        final Map<MediaId, String> local = SongSources.local(level, computer);
        final SoundfoundryState state = computer.console().soundfoundry();
        Text told = Text.EMPTY;
        for (final CatalogTrack track : album.tracks()) {
            if (!local.containsKey(track.media()) && !state.downloading(track.media())) {
                final Text why = computer.musicDownloads().start(level, SongDownload.FROM_CATALOG,
                        album.id() + "/" + track.file(), false);
                if (!why.isEmpty()) {
                    told = why;
                }
            }
        }
        return told;
    }

    private static Text like(final ServerLevel level, final AbstractComputerBlockEntity computer, final String ref) {
        final List<String> liked = new ArrayList<>(SoundfoundryPlaylists.read(level, computer,
                SoundfoundryPlaylists.LIKED));
        if (!liked.remove(ref)) {
            liked.add(ref);
        }
        return saved(level, computer, SoundfoundryPlaylists.LIKED, liked);
    }

    private static Text addTo(final ServerLevel level, final AbstractComputerBlockEntity computer,
                              final String name, final String ref) {
        final List<String> songs = new ArrayList<>(SoundfoundryPlaylists.read(level, computer, name));
        if (songs.size() >= SoundfoundryState.MAX_SONGS) {
            return SoundfoundryTexts.LIST_FULL.with(SoundfoundryState.MAX_SONGS);
        }
        songs.add(ref);
        return saved(level, computer, name, songs);
    }

    private static Text removeFrom(final ServerLevel level, final AbstractComputerBlockEntity computer,
                                   final String name, final int index) {
        final List<String> songs = new ArrayList<>(SoundfoundryPlaylists.read(level, computer, name));
        if (index < 0 || index >= songs.size()) {
            return Text.EMPTY;
        }
        songs.remove(index);
        return saved(level, computer, name, songs);
    }

    private static Text newPlaylist(final ServerLevel level, final AbstractComputerBlockEntity computer,
                                    final String name) {
        if (!SoundfoundryPlaylists.validName(name)) {
            return SoundfoundryTexts.PLAYLIST_NAME.with(name);
        }
        for (final String taken : SoundfoundryPlaylists.names(computer)) {
            if (taken.equalsIgnoreCase(name)) {
                return SoundfoundryTexts.PLAYLIST_TAKEN.with(taken);
            }
        }
        return saved(level, computer, name, List.of());
    }

    private static Text saved(final ServerLevel level, final AbstractComputerBlockEntity computer,
                              final String name, final List<String> songs) {
        return SoundfoundryPlaylists.write(level, computer, name, songs) ? Text.EMPTY
                : SoundfoundryTexts.PLAYLIST_NOT_SAVED.with(name);
    }

    @Nullable
    private static CatalogAlbum albumOf(final String id) {
        for (final CatalogAlbum album : SoundfoundryCatalog.current().albums()) {
            if (album.id().equals(id)) {
                return album;
            }
        }
        return null;
    }

    /*
     * One look at the machine for one page: what it reaches and what it keeps, each read once however many songs
     * of the page ask about it.
     */
    private static final class Look {

        final ServerLevel level;
        final AbstractComputerBlockEntity computer;
        @Nullable
        final ServerServices.Host server;
        final boolean catalog;
        @Nullable
        private Map<MediaId, String> local;
        @Nullable
        private List<String> localSongs;
        @Nullable
        private Set<String> liked;

        Look(final ServerLevel level, final AbstractComputerBlockEntity computer) {
            this.level = level;
            this.computer = computer;
            this.server = SoundfoundryServers.chosen(level, computer);
            this.catalog = server != null && SoundfoundryShare.storeOpen(computer);
        }

        SoundfoundryPagePayload.Sidebar sidebar() {
            final List<SoundfoundryPagePayload.Server> servers = new ArrayList<>();
            for (final ServerServices.Host host : SoundfoundryServers.of(level, computer)) {
                servers.add(new SoundfoundryPagePayload.Server(host.id(), host.hostname(),
                        SoundfoundryServers.library(level, host).size(), SoundfoundryServers.listeners(level, host)));
            }
            return new SoundfoundryPagePayload.Sidebar(servers, server == null ? "" : server.id(),
                    SoundfoundryPlaylists.names(computer), catalog);
        }

        Album album(final CatalogAlbum album) {
            int downloaded = 0;
            for (final CatalogTrack track : album.tracks()) {
                if (local().containsKey(track.media())) {
                    downloaded++;
                }
            }
            return new Album(album.id(), album.title(), album.artist(), album.year(), album.tracks().size(),
                    album.millis(), downloaded);
        }

        Row track(final CatalogAlbum album, final CatalogTrack track, final int section) {
            final String ref = SongRefs.catalog(album.id(), track.file());
            return new Row(ref, track.title(), track.artist().isEmpty() ? album.artist() : track.artist(),
                    album.title(), track.info().millis(), "", stateOf(track.media()), section, liked(ref));
        }

        void network(final String path, final int section, final List<Row> rows) {
            final RecordingFile song = server == null ? null : SoundfoundryServers.song(level, server, path);
            if (song != null) {
                final String ref = SongRefs.network(path);
                rows.add(new Row(ref, SongSources.titleOf(song, path), song.info().tags().artist(),
                        song.info().tags().album(), song.info().millis(), SoundfoundryServers.fromOf(server, path),
                        stateOf(song.media()), section, liked(ref)));
            }
        }

        void own(final String path, final int state, final int section, final List<Row> rows) {
            final RecordingFile song = SongFiles.read(level, computer, path);
            if (song != null) {
                rows.add(new Row(path, SongSources.titleOf(song, path), song.info().tags().artist(),
                        song.info().tags().album(), song.info().millis(), "", state, section, liked(path)));
            }
        }

        /* A song a playlist names, which can be any of the three kinds, or no longer anything at all. */
        Row listed(final String ref) {
            final SongSources.Described song = SongSources.describe(level, computer, ref);
            if (song == null) {
                return new Row(ref, SongFiles.nameOf(SongRefs.pathOf(ref)), "", "", 0L, "",
                        SoundfoundryPagePayload.LOCAL, SoundfoundryPagePayload.TRACKS, liked(ref));
            }
            final int state = SongRefs.streamed(ref) ? stateOf(song.media()) : SoundfoundryPagePayload.LOCAL;
            return new Row(ref, song.title(), song.artist(), song.info().tags().album(), song.info().millis(),
                    "", state, SoundfoundryPagePayload.TRACKS, liked(ref));
        }

        private boolean liked(final String ref) {
            if (liked == null) {
                liked = new HashSet<>(SoundfoundryPlaylists.read(level, computer, SoundfoundryPlaylists.LIKED));
            }
            return liked.contains(ref);
        }

        /* Whether one of the machine's own songs is on its way to a server. */
        int sending(final String path) {
            for (final SongDownload download : computer.console().soundfoundry().downloads()) {
                if (download.upload() && download.active() && download.path().equals(path)) {
                    return SoundfoundryPagePayload.SENDING;
                }
            }
            return SoundfoundryPagePayload.LOCAL;
        }

        List<String> localSongs() {
            if (localSongs == null) {
                localSongs = SongFiles.under(level, computer, MusicImports.musicFolderOf(computer));
            }
            return localSongs;
        }

        private int stateOf(final MediaId media) {
            if (local().containsKey(media)) {
                return SoundfoundryPagePayload.ON_DISK;
            }
            return computer.console().soundfoundry().downloading(media) ? SoundfoundryPagePayload.COMING
                    : SoundfoundryPagePayload.STREAM;
        }

        private Map<MediaId, String> local() {
            if (local == null) {
                local = SongSources.local(level, computer);
            }
            return local;
        }
    }
}
