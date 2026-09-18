/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.files;

import dev.jstech.computers.client.os.CodeFileReplies;
import dev.jstech.computers.client.os.FilesApps;
import dev.jstech.computers.item.DiskItem;
import dev.jstech.computers.operation.payload.ClientPayloadHandlers;
import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.computers.operation.payload.DiskFilesPayload;
import dev.jstech.computers.operation.payload.FileContentPayload;
import dev.jstech.computers.operation.payload.FolderContentPayload;
import dev.jstech.computers.operation.payload.RequestDiskFilesPayload;
import dev.jstech.computers.operation.payload.RequestFileContentPayload;
import dev.jstech.computers.operation.payload.RequestFolderContentPayload;
import dev.jstech.computers.os.FilesystemKind;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.VolumeLabel;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.FileType;
import dev.jstech.computers.os.fs.InstallerLayout;
import dev.jstech.computers.os.fs.ProgramFilesProjection;
import dev.jstech.computers.os.media.InstallerProjection;
import dev.jstech.computers.os.media.MediaReaderBlockEntity;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.computers.storage.DriveVolumes;
import dev.jstech.computers.storage.StorageKey;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import static dev.jstech.computers.operation.payload.files.FileAccess.NET_ROOT;
import static dev.jstech.computers.operation.payload.files.FileAccess.filesystemKindOf;
import static dev.jstech.computers.operation.payload.files.FileAccess.mediaStackFor;
import static dev.jstech.computers.operation.payload.files.FileAccess.mediaSubPath;
import static dev.jstech.computers.operation.payload.files.FileAccess.netDos;
import static dev.jstech.computers.operation.payload.files.FileAccess.netShell;
import static dev.jstech.computers.operation.payload.files.FileAccess.resolveDatKey;

/**
 * The payloads that list a computer's drives and folders and read the contents of a file.
 */
public final class FilePayloads {

    private FilePayloads() {
    }

    /** Registers the payloads this class handles. */
    public static void register(final PayloadRegistrar registrar) {
        ComputerAccess.accept(registrar, RequestDiskFilesPayload.TYPE, RequestDiskFilesPayload.STREAM_CODEC,
                ComputerAccess.machine(RequestDiskFilesPayload::hostPos), FilePayloads::handleRequestDiskFiles);
        registrar.playToClient(DiskFilesPayload.TYPE, DiskFilesPayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread(FilePayloads::handleDiskFiles));
        ComputerAccess.accept(registrar, RequestFileContentPayload.TYPE, RequestFileContentPayload.STREAM_CODEC,
                ComputerAccess.machine(RequestFileContentPayload::hostPos), FilePayloads::handleRequestFileContent);
        ComputerAccess.accept(registrar, RequestFolderContentPayload.TYPE, RequestFolderContentPayload.STREAM_CODEC,
                ComputerAccess.machine(RequestFolderContentPayload::hostPos),
                FilePayloads::handleRequestFolderContent);
        registrar.playToClient(FolderContentPayload.TYPE, FolderContentPayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread(FilePayloads::handleFolderContent));
        registrar.playToClient(FileContentPayload.TYPE, FileContentPayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread(FilePayloads::handleFileContent));
    }

    private static void handleRequestDiskFiles(final RequestDiskFilesPayload payload, final ServerPlayer player,
                                               final ServerLevel level) {
        final List<DiskFilesPayload.WireFile> wire = new ArrayList<>();
        final List<DiskFilesPayload.WireVolume> volumes = new ArrayList<>();
        if (level.getBlockEntity(payload.hostPos()) instanceof IOsHost computer) {
            // The mountable volumes (drive tree): the system disk, then each linked drive with a medium.
            if (!computer.systemDisk().isEmpty()
                    && filesystemKindOf(computer)
                            != FilesystemKind.NONE) {
                volumes.add(new DiskFilesPayload.WireVolume("",
                        VolumeLabel.of(
                                computer.systemDisk(), "Local Disk")));
            }
            for (final long endpoint : computer.linkedEndpoints()) {
                if (level.getBlockEntity(BlockPos.of(endpoint))
                        instanceof MediaReaderBlockEntity reader
                        && !reader.mediaSlot().getStackInSlot(0).isEmpty()) {
                    /*
                     * An installer's drive is named for what it installs ("Frames 11 Setup"), so the
                     * tree says what is in the drive before it is opened.
                     */
                    final ItemStack medium = reader.mediaSlot().getStackInSlot(0);
                    final String fallback = InstallerProjection.facts(medium)
                            .map(f -> f.name() + " Setup").orElse("Removable Drive");
                    volumes.add(new DiskFilesPayload.WireVolume("media:" + endpoint,
                            VolumeLabel.of(medium, fallback)));
                }
            }
            // The other machines' shared folders, reached through this machine's own shell.
            if (netShell(level, computer) != null) {
                volumes.add(new DiskFilesPayload.WireVolume(NET_ROOT, "Network"));
            }
            final String reqDir = payload.dir();
            if (reqDir.startsWith(NET_ROOT)) {
                listNetworkInto(wire, level, computer, reqDir);
            } else if (reqDir.startsWith("media:")) {
                // Browsing a removable medium in a linked drive.
                listMediaInto(wire, level, computer, reqDir);
            } else {
                final ItemStack disk = computer.systemDisk();
                final FilesystemKind kind =
                        filesystemKindOf(computer);
                if (!disk.isEmpty()
                        && kind != FilesystemKind.NONE) {
                    // Subdirectories first (folders before files, Windows-style).
                    for (final String d
                            : DiskFilesystem.listDirs(
                                    disk, reqDir, kind)) {
                        wire.add(new DiskFilesPayload.WireFile(d, "", 0L, false, true));
                    }
                    for (final DiskFilesystem.FileEntry e
                            : DiskFilesystem.list(
                                    disk, reqDir, kind)) {
                        wire.add(wireFile(disk, e, ""));
                    }
                    /*
                     * The system's own files and the installed programs' folders are generated, not
                     * stored, and take their place among the real entries; a real one with the same
                     * name (a folder the player made) wins.
                     */
                    final Set<String> seen = new HashSet<>();
                    for (final DiskFilesPayload.WireFile f : wire) {
                        seen.add(f.path());
                    }
                    for (final InstallerLayout.Entry e
                            : ProgramFilesProjection.list(computer, reqDir)) {
                        if (seen.add(e.path())) {
                            wire.add(new DiskFilesPayload.WireFile(e.path(),
                                    e.directory() ? "" : e.type().extension(), 0L, true, e.directory()));
                        }
                    }
                }
                /*
                 * A disc in a drive is a volume of its own, listed beside the disk under This PC and
                 * in the explorer's tree; it is not a folder inside the disk, so the disk's root does
                 * not list it.
                 */
            }
        }
        PacketDistributor.sendToPlayer(player, new DiskFilesPayload(payload.dir(), wire, volumes));
    }

    /** Lists what a network path holds into {@code wire}: hosts, a host's shares, or a shared folder. */
    private static void listNetworkInto(final List<DiskFilesPayload.WireFile> wire,
            final ServerLevel level, final IOsHost computer, final String reqDir) {
        final ServerCliComputer shell = netShell(level, computer);
        if (shell == null) {
            return;
        }
        final ICliComputer.FsResult listing = shell.listDisk(netDos(reqDir));
        if (!listing.ok() || listing.entries() == null) {
            return;
        }
        final String prefix = reqDir.equals(NET_ROOT) ? NET_ROOT : reqDir + "/";
        for (final ICliComputer.FsEntry entry : listing.entries()) {
            wire.add(new DiskFilesPayload.WireFile(prefix + entry.name(), entry.ext(), entry.weightMbEq(),
                    entry.readOnly(), entry.isDir()));
        }
    }

    /** Lists a removable medium's files into {@code wire}, paths prefixed {@code media:<readerPos>/}. */
    private static void listMediaInto(final List<DiskFilesPayload.WireFile> wire,
            final ServerLevel level,
            final IOsHost computer,
            final String reqDir) {
        final ItemStack media = mediaStackFor(level, computer, reqDir);
        if (media.isEmpty()) {
            return;
        }
        final String rest = reqDir.substring("media:".length());
        final int slash = rest.indexOf('/');
        final long readerPos = Long.parseLong(slash < 0 ? rest : rest.substring(0, slash));
        final String subDir = slash < 0 ? "" : rest.substring(slash + 1);
        final FilesystemKind kind =
                FilesystemKind.HIERARCHICAL;
        final String prefix = "media:" + readerPos + "/";
        /*
         * An installer shows the disc of its era: setup, readme, manifest and payload, generated from
         * the medium's stamp the way a disk's .dat files are generated from its storage. It carries no
         * stored files of its own, so the projection is the whole listing; a data medium lists what it
         * really holds.
         */
        for (final InstallerLayout.Entry e
                : InstallerProjection.list(media, subDir)) {
            wire.add(new DiskFilesPayload.WireFile(prefix + e.path(),
                    e.directory() ? "" : e.type().extension(), 0L, true, e.directory()));
        }
        for (final String d : DiskFilesystem.listDirs(
                media, subDir, kind)) {
            wire.add(new DiskFilesPayload.WireFile(prefix + d, "", 0L, false, true));
        }
        for (final DiskFilesystem.FileEntry e
                : DiskFilesystem.list(media, subDir, kind)) {
            wire.add(wireFile(media, e, prefix));
        }
    }

    /**
     * A listed file on the wire. A {@code .dat} row also carries the item it projects and how many are
     * stored, so the explorer shows the item and its count rather than a file name a player has to decode.
     */
    private static DiskFilesPayload.WireFile wireFile(final ItemStack volume,
            final DiskFilesystem.FileEntry e, final String prefix) {
        String itemId = "";
        long count = 0L;
        if (e.type() == FileType.DAT
                && volume.getItem() instanceof DiskItem) {
            final StorageKey key = resolveDatKey(volume, e.path());
            if (key != null && key.item() != null) {
                itemId = BuiltInRegistries.ITEM.getKey(key.item()).toString();
                count = DriveVolumes.contents(volume)
                        .items().getOrDefault(key, 0L);
            }
        }
        return new DiskFilesPayload.WireFile(prefix + e.path(), e.type().extension(), e.weight(), e.readOnly(),
                false, itemId, count);
    }

    private static void handleDiskFiles(final DiskFilesPayload payload, final Player player) {
        if (!CodeFileReplies.listing(payload)) {
            FilesApps.accept(payload);
        }
    }

    /**
     * Reads a whole folder of one kind of file at once.
     *
     * <p>An editor that reports on a program's neighbours has to read them all, and twenty files one at
     * a time is twenty round trips for what is really one question. Only the machine's own disk is read,
     * because a removable medium is browsed rather than compiled against.
     */
    private static void handleRequestFolderContent(final RequestFolderContentPayload payload, final ServerPlayer player,
                                                   final ServerLevel level) {
        final List<FolderContentPayload.WireFile> files = new ArrayList<>();
        if (level.getBlockEntity(payload.hostPos()) instanceof IOsHost computer) {
            final ItemStack disk = computer.systemDisk();
            final FilesystemKind kind = filesystemKindOf(computer);
            if (!disk.isEmpty() && kind != FilesystemKind.NONE) {
                final String suffix = payload.extension().toLowerCase(Locale.ROOT);
                for (final DiskFilesystem.FileEntry entry
                        : DiskFilesystem.list(disk, payload.dir(), kind)) {
                    if (files.size() >= FolderContentPayload.MAX_FILES) {
                        break;
                    }
                    if (!entry.path().toLowerCase(Locale.ROOT).endsWith(suffix)) {
                        continue;
                    }
                    final String text = DiskFilesystem
                            .read(disk, entry.path()).orElse("");
                    /*
                     * A file too long for one reply is left out rather than cut: half a program
                     * would compile to complaints that are the payload's fault, not the player's.
                     */
                    if (text.length() <= FolderContentPayload.MAX_TEXT) {
                        files.add(new FolderContentPayload.WireFile(entry.path(), text));
                    }
                }
            }
        }
        PacketDistributor.sendToPlayer(player, new FolderContentPayload(payload.dir(), files));
    }

    private static void handleFolderContent(final FolderContentPayload payload, final Player player) {
        CodeFileReplies.folder(payload);
    }

    private static void handleRequestFileContent(final RequestFileContentPayload payload, final ServerPlayer player,
                                                 final ServerLevel level) {
        String content = "";
        boolean exists = false;
        if (level.getBlockEntity(payload.hostPos()) instanceof IOsHost computer) {
            final Optional<String> read = readDiskFile(level, computer, payload.path());
            if (read.isPresent()) {
                content = read.get();
                exists = true;
            }
        }
        PacketDistributor.sendToPlayer(player, new FileContentPayload(payload.path(), content, exists));
    }

    /**
     * Reads a file the file explorer named, which is a path from the root of a disk rather than one
     * relative to wherever a shell happens to be.
     */
    public static Optional<String> readDiskFile(
            final ServerLevel level, final IOsHost computer, final String path) {
        if (LiveSessionFiles.names(path)) {
            // A file of a by-hand install, which is on no disk until the install is finished.
            return LiveSessionFiles.read(computer, path);
        }
        if (path.startsWith(NET_ROOT)) {
            final ServerCliComputer shell = netShell(level, computer);
            if (shell == null) {
                return Optional.empty();
            }
            final ICliComputer.FsResult read = shell.readFile(netDos(path));
            return read.ok() ? Optional.of(read.message()) : Optional.empty();
        }
        final boolean media = path.startsWith("media:");
        final ItemStack vol =
                media ? mediaStackFor(level, computer, path) : computer.systemDisk();
        if (vol.isEmpty()) {
            return Optional.empty();
        }
        /*
         * A projected file on an installer (its readme, manifest or autorun) has no stored bytes to
         * read: its text is generated from the medium's stamp.
         */
        final Optional<String> projected = media
                ? InstallerProjection.text(vol, mediaSubPath(path))
                : ProgramFilesProjection.text(computer, path);
        return projected.isPresent() ? projected
                : DiskFilesystem.read(vol, media ? mediaSubPath(path) : path);
    }

    private static void handleFileContent(final FileContentPayload payload, final Player player) {
        /*
         * Every window that opens a file says it is waiting for that file, by name, before it asks.
         * An answer nobody is waiting for belongs to a window that has since closed, and is dropped.
         */
        CodeFileReplies.content(
                payload.path(), payload.content(), payload.exists());
    }
}
