/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.audio;

import dev.jstech.computers.audio.catalog.CatalogTrack;
import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.config.ComputersServerConfig;
import dev.jstech.computers.os.fs.RecordingFile;
import dev.jstech.computers.program.SongDownload;
import dev.jstech.computers.program.SoundfoundryState;
import dev.jstech.core.audio.media.MediaId;
import dev.jstech.core.audio.media.MediaInfo;
import dev.jstech.core.audio.media.MediaReceipt;
import dev.jstech.core.audio.media.MediaStore;
import dev.jstech.core.network.DataTier;
import dev.jstech.core.text.Text;
import java.io.IOException;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

/**
 * The songs Soundfoundry is fetching over one computer's network, brought in while the computer runs.
 *
 * <p>A song comes at the speed of the slowest cable on its way, one from the catalogue at the speed of the cable the
 * computer itself is plugged into, and the songs coming in at once share that speed between them. Once a second each
 * one is looked at again: a way that has got faster or slower is taken up, a computer that went off or left the
 * network leaves its song waiting until it is back, and a song taken out of the shared folder or the catalogue is given
 * up on. The last byte in, the song is kept in the music folder like one brought from the player's own computer.
 *
 * <p>It is part of the computer, like its {@link MusicPlayer}; what it fetches is in the computer's
 * {@link SoundfoundryState}, so a download goes on after the world is loaded again.
 */
public final class MusicDownloads {

    private final AbstractComputerBlockEntity computer;

    /** How often every download is looked at again, in ticks. */
    private static final int LOOK_EVERY = 20;
    private static final long TICK_MILLIS = 50L;

    public MusicDownloads(final AbstractComputerBlockEntity computer) {
        this.computer = computer;
    }

    /**
     * Starts fetching a song a search found.
     *
     * @param source   where the computer sharing it stands, or {@link SongDownload#FROM_CATALOG}
     * @param path     where the song is on that computer, or its album and file in the catalogue
     * @param playlist whether the song goes on the playlist once it is kept
     * @return why it cannot be fetched, or empty when it is on its way
     */
    public Text start(final ServerLevel level, final long source, final String path, final boolean playlist) {
        if (computer.networkUuid() == null) {
            return SoundfoundryTexts.NO_NETWORK.text();
        }
        final String name;
        final MediaId media;
        final String from;
        final DataTier link;
        if (source == SongDownload.FROM_CATALOG) {
            final CatalogTrack track = SoundfoundryShare.storeOpen(computer) ? SoundfoundryShare.catalogTrack(path)
                    : null;
            if (track == null) {
                return SoundfoundryTexts.NOT_THERE.with(SongFiles.nameOf(path));
            }
            name = SoundfoundryShare.keptName(track);
            media = track.media();
            from = "";
            link = SoundfoundryShare.ownLink(level, computer);
        } else {
            final SoundfoundryShare.Peer peer = SoundfoundryShare.peerAt(level, computer, source);
            final RecordingFile song = peer == null ? null : SongFiles.read(level, peer.machine(), path);
            if (song == null || !SoundfoundryShare.stillShared(level, peer.machine(), path, song.media())) {
                return SoundfoundryTexts.NOT_THERE.with(SongFiles.nameOf(path));
            }
            name = SongFiles.nameOf(path);
            media = song.media();
            from = peer.hostname();
            link = peer.link();
        }
        final SoundfoundryState state = computer.console().soundfoundry();
        if (state.downloading(media)) {
            return SoundfoundryTexts.ALREADY.with(name);
        }
        final Text why = MusicImports.whyNot(level, computer.getBlockPos(), name, media);
        if (why != null) {
            return why;
        }
        final SongDownload download = new SongDownload(name, media, source, path, from, playlist);
        download.reached(link);
        if (!state.addDownload(download)) {
            return SoundfoundryTexts.TOO_MANY.with(SoundfoundryState.MAX_DOWNLOADS);
        }
        computer.setChanged();
        return Text.EMPTY;
    }

    /** Brings the songs on, once a tick while the computer runs. */
    public void tick(final ServerLevel level) {
        final List<SongDownload> downloads = computer.console().soundfoundry().downloads();
        if (downloads.isEmpty() || !computer.isRunning()) {
            return;
        }
        final boolean look = level.getGameTime() % LOOK_EVERY == 0;
        if (look) {
            look(level, downloads);
        }
        final int running = running(downloads);
        boolean changed = look;
        for (final SongDownload download : downloads) {
            if (download.status() != SongDownload.Status.RUNNING) {
                continue;
            }
            download.advance(speedOf(download.link()) / running, TICK_MILLIS);
            if (download.complete()) {
                finish(level, download);
                changed = true;
            }
        }
        if (changed) {
            computer.setChanged();
        }
    }

    /** How long what is left of a download takes as things stand, in milliseconds; -1 while it is not coming in. */
    public long millisLeft(final SongDownload download) {
        return download.millisLeft(speedOf(download.link()) / running(computer.console().soundfoundry().downloads()));
    }

    private void look(final ServerLevel level, final List<SongDownload> downloads) {
        DataTier own = null;
        boolean ownKnown = false;
        for (final SongDownload download : downloads) {
            if (!download.active()) {
                continue;
            }
            if (computer.networkUuid() == null) {
                download.unreachable();
            } else if (download.fromCatalog()) {
                if (!ownKnown) {
                    own = SoundfoundryShare.ownLink(level, computer);
                    ownKnown = true;
                }
                lookAtCatalog(download, own);
            } else {
                lookAtPeer(level, download);
            }
        }
    }

    private void lookAtCatalog(final SongDownload download, @Nullable final DataTier own) {
        final CatalogTrack track = SoundfoundryShare.storeOpen(computer)
                ? SoundfoundryShare.catalogTrack(download.path()) : null;
        if (track == null || !track.media().equals(download.media())) {
            download.failed(SoundfoundryTexts.NOT_IN_CATALOG.with(download.name()));
        } else {
            download.reached(own);
        }
    }

    private void lookAtPeer(final ServerLevel level, final SongDownload download) {
        final SoundfoundryShare.Peer peer = SoundfoundryShare.peerAt(level, computer, download.source());
        if (peer == null) {
            download.unreachable();
        } else if (!SoundfoundryShare.stillShared(level, peer.machine(), download.path(), download.media())) {
            download.failed(SoundfoundryTexts.NOT_SHARED.with(download.name(), peer.hostname()));
        } else {
            download.reached(peer.link());
        }
    }

    /* Every byte is in: the song is kept in the music folder, or the download says why it could not be. */
    private void finish(final ServerLevel level, final SongDownload download) {
        final MediaStore store = MediaStore.current().orElse(null);
        final MediaInfo info;
        try {
            info = store == null ? null : store.info(download.media());
        } catch (final IOException unreadable) {
            download.failed(SoundfoundryTexts.NOT_KEPT.with(download.name()));
            return;
        }
        if (info == null) {
            download.failed(SoundfoundryTexts.NOT_KEPT.with(download.name()));
            return;
        }
        final MediaReceipt told = MusicImports.keep(level, computer.getBlockPos(), "", download.playlist(),
                download.name(), download.media(), info);
        if (told.accepted()) {
            download.kept();
        } else {
            download.failed(told.message());
        }
    }

    private static int running(final List<SongDownload> downloads) {
        int running = 0;
        for (final SongDownload download : downloads) {
            if (download.status() == SongDownload.Status.RUNNING) {
                running++;
            }
        }
        return Math.max(1, running);
    }

    /* A way whose cable is unknown is taken at the slowest cable's speed. */
    private static long speedOf(@Nullable final DataTier link) {
        return ComputersServerConfig.songBytesPerSecond(link == null ? DataTier.T1_ETHERNET : link);
    }
}
