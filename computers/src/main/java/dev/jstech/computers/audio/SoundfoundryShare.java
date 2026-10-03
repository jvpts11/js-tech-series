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
import dev.jstech.computers.audio.catalog.SoundfoundryCatalog;
import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.config.ComputersServerConfig;
import dev.jstech.computers.machine.RemoteComputerService;
import dev.jstech.computers.operation.payload.files.FileAccess;
import dev.jstech.computers.os.FilesystemKind;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.Platform;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.FsPaths;
import dev.jstech.computers.os.fs.RecordingFile;
import dev.jstech.computers.program.Programs;
import dev.jstech.computers.program.SongDownload;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.core.audio.media.MediaId;
import dev.jstech.core.network.DataLink;
import dev.jstech.core.network.NetworkSystem;
import dev.jstech.core.util.Loaded;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * What Soundfoundry's sharing window reaches over a computer's network: the other computers of the network running
 * Soundfoundry, each sharing the songs in its music folder's {@value #SHARED_FOLDER} folder, and the server's
 * catalogue, which the players of the time knew as an online music store.
 *
 * <p>Nothing is shared but what a player put in that folder. A computer on no network reaches nothing at all, the
 * catalogue included: it is on the network that the store is reached.
 */
public final class SoundfoundryShare {

    /** The folder, in a computer's music folder, whose songs it shares with the rest of its network. */
    public static final String SHARED_FOLDER = "Shared";
    /** The most songs one search finds. */
    public static final int MAX_FOUND = 200;

    private SoundfoundryShare() {
    }

    /**
     * Another computer of the network running Soundfoundry.
     *
     * @param machine  the computer
     * @param hostname what it is called on the network
     * @param link     the slowest cable on the way to it
     */
    public record Peer(AbstractComputerBlockEntity machine, String hostname, DataLink link) {
    }

    /**
     * A song a search found.
     *
     * @param title  what it is listed as: a catalogue song's title, or a shared song's file name
     * @param artist who a catalogue song is by, or empty
     * @param media  the recording
     * @param from   what the computer sharing it is called, or {@code ""} for the catalogue
     * @param source where that computer stands, or {@link SongDownload#FROM_CATALOG}
     * @param path   where it is on that computer, or its album and file in the catalogue
     * @param link   the slowest cable on the way to that computer, or null for the catalogue
     */
    public record Found(String title, String artist, MediaId media, String from, long source, String path,
                        @Nullable DataLink link) {
    }

    /** Where a computer keeps the songs it shares. */
    public static String sharedFolderOf(final IOsHost computer) {
        return FsPaths.join(MusicImports.musicFolderOf(computer), SHARED_FOLDER);
    }

    /** The same, written the way its system writes a path: from a drive on Frames, from the root elsewhere. */
    public static String shownFolderOf(final IOsHost computer) {
        final String folder = sharedFolderOf(computer);
        final OsDef os = computer.installedOs();
        return os != null && os.platform() == Platform.FRAMES ? "C:\\" + folder.replace('/', '\\') : "/" + folder;
    }

    /**
     * Lays the shared folder down on a computer that has none yet, the way the programs of the time made theirs the
     * first time they ran, so a player finds where to put the songs they want to share.
     */
    public static void layDownSharedFolder(final AbstractComputerBlockEntity computer) {
        final ItemStack disk = computer.systemDisk();
        if (disk.isEmpty() || FileAccess.filesystemKindOf(computer) != FilesystemKind.HIERARCHICAL) {
            return;
        }
        final String folder = sharedFolderOf(computer);
        final String music = FsPaths.parentDir(folder);
        if (!DiskFilesystem.listDirs(disk, music, FilesystemKind.HIERARCHICAL).contains(folder)
                && DiskFilesystem.mkdirs(disk, folder, FilesystemKind.HIERARCHICAL)) {
            computer.setChanged();
        }
    }

    /** The songs a computer shares, by path. */
    public static List<String> sharedSongs(final ServerLevel level, final IOsHost computer) {
        return SongFiles.under(level, computer, sharedFolderOf(computer));
    }

    /** Whether the server's catalogue reaches that computer: offered, with songs, to a computer on a network. */
    public static boolean storeOpen(final AbstractComputerBlockEntity computer) {
        return computer.networkUuid() != null && ComputersServerConfig.soundfoundryCatalog()
                && SoundfoundryCatalog.current().songs() > 0;
    }

    /** How many other computers of the network run Soundfoundry, which is how many a search looks through. */
    public static int peerCount(final ServerLevel level, final AbstractComputerBlockEntity computer) {
        int count = 0;
        for (final BlockEntity machine : machinesOf(level, computer).values()) {
            if (sharing(machine)) {
                count++;
            }
        }
        return count;
    }

    /** The other computers of the network running Soundfoundry, each with the slowest cable on the way to it. */
    public static List<Peer> peers(final ServerLevel level, final AbstractComputerBlockEntity computer) {
        final List<Peer> peers = new ArrayList<>();
        for (final Map.Entry<String, BlockEntity> entry : machinesOf(level, computer).entrySet()) {
            if (sharing(entry.getValue()) && entry.getValue() instanceof AbstractComputerBlockEntity peer) {
                computer.slowestCableTo(level, peer)
                        .ifPresent(link -> peers.add(new Peer(peer, entry.getKey(), link)));
            }
        }
        return peers;
    }

    /**
     * The other computer of the network at that position running Soundfoundry, or null when there is none there, it
     * is on another network or it cannot be reached.
     */
    @Nullable
    public static Peer peerAt(final ServerLevel level, final AbstractComputerBlockEntity computer, final long pos) {
        if (computer.networkUuid() == null
                || !(Loaded.blockEntity(level, BlockPos.of(pos)) instanceof AbstractComputerBlockEntity peer)
                || peer == computer || !sharing(peer) || !computer.networkUuid().equals(peer.networkUuid())
                || !(peer instanceof IComputerTerminalHost host)) {
            return null;
        }
        return computer.slowestCableTo(level, peer).map(link -> new Peer(peer, host.hostname(), link)).orElse(null);
    }

    /** The fastest cable a computer is plugged into, which is how fast the catalogue reaches it; null for none. */
    @Nullable
    public static DataLink ownLink(final ServerLevel level, final AbstractComputerBlockEntity computer) {
        DataLink fastest = null;
        for (final long cable : computer.networkCables(level)) {
            final DataLink link = NetworkSystem.get(level).connectivity().linkOf(cable).orElse(null);
            if (link != null && (fastest == null || link.throughput() > fastest.throughput())) {
                fastest = link;
            }
        }
        return fastest;
    }

    /**
     * Every song a search finds: the catalogue's first, album by album, then those the other computers share, one
     * computer after another, {@value #MAX_FOUND} at most.
     */
    public static List<Found> search(final ServerLevel level, final AbstractComputerBlockEntity computer,
                                     final String query) {
        final SongSearch search = new SongSearch(query);
        final List<Found> found = new ArrayList<>();
        if (storeOpen(computer)) {
            for (final CatalogAlbum album : SoundfoundryCatalog.current().albums()) {
                for (final CatalogTrack track : album.tracks()) {
                    if (found.size() >= MAX_FOUND) {
                        return found;
                    }
                    if (search.matches(track.title(), track.artist(), album.title(), album.artist(), track.file())) {
                        found.add(new Found(track.title(), track.artist(), track.media(), "",
                                SongDownload.FROM_CATALOG, catalogPath(album, track), null));
                    }
                }
            }
        }
        for (final Peer peer : peers(level, computer)) {
            for (final String path : sharedSongs(level, peer.machine())) {
                if (found.size() >= MAX_FOUND) {
                    return found;
                }
                final RecordingFile song = SongFiles.read(level, peer.machine(), path);
                final String file = FsPaths.fileName(path);
                if (song != null && search.matches(file, song.info().tags().title(), song.info().tags().artist(),
                        song.info().tags().album())) {
                    found.add(new Found(file, "", song.media(), peer.hostname(),
                            peer.machine().getBlockPos().asLong(), path, peer.link()));
                }
            }
        }
        return found;
    }

    /** The catalogue song at that path, as {@link Found#path()} names it, or null when the catalogue has none. */
    @Nullable
    public static CatalogTrack catalogTrack(final String path) {
        final int slash = path.lastIndexOf('/');
        if (slash <= 0) {
            return null;
        }
        final String albumId = path.substring(0, slash);
        final String file = path.substring(slash + 1);
        for (final CatalogAlbum album : SoundfoundryCatalog.current().albums()) {
            if (album.id().equals(albumId)) {
                for (final CatalogTrack track : album.tracks()) {
                    if (track.file().equals(file)) {
                        return track;
                    }
                }
            }
        }
        return null;
    }

    /** The album of the catalogue song at that path, or null when the catalogue has none. */
    @Nullable
    public static CatalogAlbum catalogAlbum(final String path) {
        final int slash = path.lastIndexOf('/');
        if (slash <= 0) {
            return null;
        }
        final String albumId = path.substring(0, slash);
        for (final CatalogAlbum album : SoundfoundryCatalog.current().albums()) {
            if (album.id().equals(albumId)) {
                return album;
            }
        }
        return null;
    }

    /** What a catalogue song's file is called once it is kept: its artist and title, and its kind. */
    public static String keptName(final CatalogTrack track) {
        final String listed = track.artist().isEmpty() ? track.title() : track.artist() + " - " + track.title();
        return listed + "." + track.media().format();
    }

    /** Whether a shared song at that path of that computer is still that recording. */
    public static boolean stillShared(final ServerLevel level, final AbstractComputerBlockEntity peer,
                                      final String path, final MediaId media) {
        if (!FsPaths.isUnder(sharedFolderOf(peer), path)) {
            return false;
        }
        final RecordingFile song = SongFiles.read(level, peer, path);
        return song != null && Objects.equals(song.media(), media);
    }

    private static String catalogPath(final CatalogAlbum album, final CatalogTrack track) {
        return album.id() + "/" + track.file();
    }

    /* Whether a machine of the network is one whose songs a search looks through. */
    private static boolean sharing(final BlockEntity machine) {
        return machine instanceof AbstractComputerBlockEntity peer && peer.isRunning()
                && peer.console().isInstalled(Programs.SOUNDFOUNDRY.toString());
    }

    private static Map<String, BlockEntity> machinesOf(final ServerLevel level,
                                                       final AbstractComputerBlockEntity computer) {
        if (!(computer instanceof IComputerTerminalHost terminal) || computer.networkUuid() == null) {
            return Map.of();
        }
        return new RemoteComputerService(terminal, level).machines();
    }
}
