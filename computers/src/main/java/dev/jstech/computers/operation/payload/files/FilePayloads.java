/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.files;

import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.computers.operation.payload.DiskFilesPayload;
import dev.jstech.computers.operation.payload.FileContentPayload;
import dev.jstech.computers.operation.payload.FolderContentPayload;
import dev.jstech.computers.operation.payload.RequestDiskFilesPayload;
import dev.jstech.computers.operation.payload.RequestFileContentPayload;
import dev.jstech.computers.operation.payload.RequestFolderContentPayload;
import dev.jstech.computers.os.media.MediaReaderBlockEntity;
import dev.jstech.computers.storage.StorageKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
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
                FilePayloads::handleDiskFiles);
        ComputerAccess.accept(registrar, RequestFileContentPayload.TYPE, RequestFileContentPayload.STREAM_CODEC,
                ComputerAccess.machine(RequestFileContentPayload::hostPos), FilePayloads::handleRequestFileContent);
        ComputerAccess.accept(registrar, RequestFolderContentPayload.TYPE, RequestFolderContentPayload.STREAM_CODEC,
                ComputerAccess.machine(RequestFolderContentPayload::hostPos),
                FilePayloads::handleRequestFolderContent);
        registrar.playToClient(FolderContentPayload.TYPE, FolderContentPayload.STREAM_CODEC,
                FilePayloads::handleFolderContent);
        registrar.playToClient(FileContentPayload.TYPE, FileContentPayload.STREAM_CODEC,
                FilePayloads::handleFileContent);
    }

    private static void handleRequestDiskFiles(final RequestDiskFilesPayload payload,
                                               final IPayloadContext context) {
        context.enqueueWork(() -> {
            final java.util.List<DiskFilesPayload.WireFile> wire = new java.util.ArrayList<>();
            final java.util.List<DiskFilesPayload.WireVolume> volumes = new java.util.ArrayList<>();
            if (context.player() instanceof ServerPlayer player
                    && player.level() instanceof ServerLevel level
                    && level.getBlockEntity(payload.hostPos())
                            instanceof dev.jstech.computers.os
                                    .IOsHost computer) {
                // The mountable volumes (drive tree): the system disk, then each linked drive with a medium.
                if (!computer.systemDisk().isEmpty()
                        && filesystemKindOf(computer)
                                != dev.jstech.computers.os.FilesystemKind.NONE) {
                    volumes.add(new DiskFilesPayload.WireVolume("",
                            dev.jstech.computers.os.VolumeLabel.of(
                                    computer.systemDisk(), "Local Disk")));
                }
                for (final long endpoint : computer.linkedEndpoints()) {
                    if (level.getBlockEntity(net.minecraft.core.BlockPos.of(endpoint))
                            instanceof MediaReaderBlockEntity reader
                            && !reader.mediaSlot().getStackInSlot(0).isEmpty()) {
                        /*
                         * An installer's drive is named for what it installs ("Frames 11 Setup"), so the
                         * tree says what is in the drive before it is opened.
                         */
                        final net.minecraft.world.item.ItemStack medium = reader.mediaSlot().getStackInSlot(0);
                        final String fallback = dev.jstech.computers.os.media
                                .InstallerProjection.facts(medium).map(f -> f.name() + " Setup").orElse("Removable Drive");
                        volumes.add(new DiskFilesPayload.WireVolume("media:" + endpoint,
                                dev.jstech.computers.os.VolumeLabel.of(medium, fallback)));
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
                    final net.minecraft.world.item.ItemStack disk = computer.systemDisk();
                    final dev.jstech.computers.os.FilesystemKind kind =
                            filesystemKindOf(computer);
                    if (!disk.isEmpty()
                            && kind != dev.jstech.computers.os.FilesystemKind.NONE) {
                        // Subdirectories first (folders before files, Windows-style).
                        for (final String d
                                : dev.jstech.computers.os.fs.DiskFilesystem.listDirs(
                                        disk, reqDir, kind)) {
                            wire.add(new DiskFilesPayload.WireFile(d, "", 0L, false, true));
                        }
                        for (final dev.jstech.computers.os.fs.DiskFilesystem.FileEntry e
                                : dev.jstech.computers.os.fs.DiskFilesystem.list(
                                        disk, reqDir, kind)) {
                            wire.add(wireFile(disk, e, ""));
                        }
                        /*
                         * The system's own files and the installed programs' folders are generated, not
                         * stored, and take their place among the real entries; a real one with the same
                         * name (a folder the player made) wins.
                         */
                        final java.util.Set<String> seen = new java.util.HashSet<>();
                        for (final DiskFilesPayload.WireFile f : wire) {
                            seen.add(f.path());
                        }
                        for (final dev.jstech.computers.os.fs.InstallerLayout.Entry e
                                : dev.jstech.computers.os.fs.ProgramFilesProjection.list(computer, reqDir)) {
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
            context.reply(new DiskFilesPayload(payload.dir(), wire, volumes));
        });
    }

    /** Lists what a network path holds into {@code wire}: hosts, a host's shares, or a shared folder. */
    private static void listNetworkInto(final java.util.List<DiskFilesPayload.WireFile> wire,
            final ServerLevel level, final dev.jstech.computers.os.IOsHost computer, final String reqDir) {
        final dev.jstech.computers.program.ServerCliComputer shell = netShell(level, computer);
        if (shell == null) {
            return;
        }
        final dev.jstech.computers.program.cli.ICliComputer.FsResult listing = shell.listDisk(netDos(reqDir));
        if (!listing.ok() || listing.entries() == null) {
            return;
        }
        final String prefix = reqDir.equals(NET_ROOT) ? NET_ROOT : reqDir + "/";
        for (final dev.jstech.computers.program.cli.ICliComputer.FsEntry entry : listing.entries()) {
            wire.add(new DiskFilesPayload.WireFile(prefix + entry.name(), entry.ext(), entry.weightMbEq(),
                    entry.readOnly(), entry.isDir()));
        }
    }

    /** Lists a removable medium's files into {@code wire}, paths prefixed {@code media:<readerPos>/}. */
    private static void listMediaInto(final java.util.List<DiskFilesPayload.WireFile> wire,
            final ServerLevel level,
            final dev.jstech.computers.os.IOsHost computer,
            final String reqDir) {
        final net.minecraft.world.item.ItemStack media = mediaStackFor(level, computer, reqDir);
        if (media.isEmpty()) {
            return;
        }
        final String rest = reqDir.substring("media:".length());
        final int slash = rest.indexOf('/');
        final long readerPos = Long.parseLong(slash < 0 ? rest : rest.substring(0, slash));
        final String subDir = slash < 0 ? "" : rest.substring(slash + 1);
        final dev.jstech.computers.os.FilesystemKind kind =
                dev.jstech.computers.os.FilesystemKind.HIERARCHICAL;
        final String prefix = "media:" + readerPos + "/";
        /*
         * An installer shows the disc of its era: setup, readme, manifest and payload, generated from
         * the medium's stamp the way a disk's .dat files are generated from its storage. It carries no
         * stored files of its own, so the projection is the whole listing; a data medium lists what it
         * really holds.
         */
        for (final dev.jstech.computers.os.fs.InstallerLayout.Entry e
                : dev.jstech.computers.os.media.InstallerProjection.list(media, subDir)) {
            wire.add(new DiskFilesPayload.WireFile(prefix + e.path(),
                    e.directory() ? "" : e.type().extension(), 0L, true, e.directory()));
        }
        for (final String d : dev.jstech.computers.os.fs.DiskFilesystem.listDirs(
                media, subDir, kind)) {
            wire.add(new DiskFilesPayload.WireFile(prefix + d, "", 0L, false, true));
        }
        for (final dev.jstech.computers.os.fs.DiskFilesystem.FileEntry e
                : dev.jstech.computers.os.fs.DiskFilesystem.list(media, subDir, kind)) {
            wire.add(wireFile(media, e, prefix));
        }
    }

    /**
     * A listed file on the wire. A {@code .dat} row also carries the item it projects and how many are
     * stored, so the explorer shows the item and its count rather than a file name a player has to decode.
     */
    private static DiskFilesPayload.WireFile wireFile(final net.minecraft.world.item.ItemStack volume,
            final dev.jstech.computers.os.fs.DiskFilesystem.FileEntry e, final String prefix) {
        String itemId = "";
        long count = 0L;
        if (e.type() == dev.jstech.computers.os.fs.FileType.DAT
                && volume.getItem() instanceof dev.jstech.computers.item.DiskItem) {
            final StorageKey key = resolveDatKey(volume, e.path());
            if (key != null && key.item() != null) {
                itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(key.item()).toString();
                count = dev.jstech.computers.storage.DriveVolumes.contents(volume)
                        .items().getOrDefault(key, 0L);
            }
        }
        return new DiskFilesPayload.WireFile(prefix + e.path(), e.type().extension(), e.weight(), e.readOnly(),
                false, itemId, count);
    }

    private static void handleDiskFiles(final DiskFilesPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!dev.jstech.computers.client.os.CodeFileReplies.listing(payload)) {
                dev.jstech.computers.client.os.FilesApps.accept(payload);
            }
        });
    }

    /**
     * Reads a whole folder of one kind of file at once.
     *
     * <p>An editor that reports on a program's neighbours has to read them all, and twenty files one at
     * a time is twenty round trips for what is really one question. Only the machine's own disk is read,
     * because a removable medium is browsed rather than compiled against.
     */
    private static void handleRequestFolderContent(final RequestFolderContentPayload payload,
                                                   final IPayloadContext context) {
        context.enqueueWork(() -> {
            final java.util.List<FolderContentPayload.WireFile> files = new java.util.ArrayList<>();
            if (context.player() instanceof ServerPlayer player
                    && player.level() instanceof ServerLevel level
                    && level.getBlockEntity(payload.hostPos())
                            instanceof dev.jstech.computers.os.IOsHost computer) {
                final net.minecraft.world.item.ItemStack disk = computer.systemDisk();
                final dev.jstech.computers.os.FilesystemKind kind = filesystemKindOf(computer);
                if (!disk.isEmpty() && kind != dev.jstech.computers.os.FilesystemKind.NONE) {
                    final String suffix = payload.extension().toLowerCase(java.util.Locale.ROOT);
                    for (final dev.jstech.computers.os.fs.DiskFilesystem.FileEntry entry
                            : dev.jstech.computers.os.fs.DiskFilesystem.list(disk, payload.dir(), kind)) {
                        if (files.size() >= FolderContentPayload.MAX_FILES) {
                            break;
                        }
                        if (!entry.path().toLowerCase(java.util.Locale.ROOT).endsWith(suffix)) {
                            continue;
                        }
                        final String text = dev.jstech.computers.os.fs.DiskFilesystem
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
            context.reply(new FolderContentPayload(payload.dir(), files));
        });
    }

    private static void handleFolderContent(final FolderContentPayload payload, final IPayloadContext context) {
        context.enqueueWork(() ->
                dev.jstech.computers.client.os.CodeFileReplies.folder(payload));
    }

    private static void handleRequestFileContent(final RequestFileContentPayload payload,
                                                 final IPayloadContext context) {
        context.enqueueWork(() -> {
            String content = "";
            boolean exists = false;
            if (context.player() instanceof ServerPlayer player
                    && player.level() instanceof ServerLevel level
                    && level.getBlockEntity(payload.hostPos())
                            instanceof dev.jstech.computers.os
                                    .IOsHost computer) {
                final java.util.Optional<String> read = readDiskFile(level, computer, payload.path());
                if (read.isPresent()) {
                    content = read.get();
                    exists = true;
                }
            }
            context.reply(new FileContentPayload(payload.path(), content, exists));
        });
    }

    /**
     * Reads a file the file explorer named, which is a path from the root of a disk rather than one
     * relative to wherever a shell happens to be.
     */
    public static java.util.Optional<String> readDiskFile(
            final ServerLevel level, final dev.jstech.computers.os.IOsHost computer, final String path) {
        if (path.startsWith(NET_ROOT)) {
            final dev.jstech.computers.program.ServerCliComputer shell = netShell(level, computer);
            if (shell == null) {
                return java.util.Optional.empty();
            }
            final dev.jstech.computers.program.cli.ICliComputer.FsResult read = shell.readFile(netDos(path));
            return read.ok() ? java.util.Optional.of(read.message()) : java.util.Optional.empty();
        }
        final boolean media = path.startsWith("media:");
        final net.minecraft.world.item.ItemStack vol =
                media ? mediaStackFor(level, computer, path) : computer.systemDisk();
        if (vol.isEmpty()) {
            return java.util.Optional.empty();
        }
        /*
         * A projected file on an installer (its readme, manifest or autorun) has no stored bytes to
         * read: its text is generated from the medium's stamp.
         */
        final java.util.Optional<String> projected = media
                ? dev.jstech.computers.os.media.InstallerProjection.text(vol, mediaSubPath(path))
                : dev.jstech.computers.os.fs.ProgramFilesProjection.text(computer, path);
        return projected.isPresent() ? projected
                : dev.jstech.computers.os.fs.DiskFilesystem.read(vol, media ? mediaSubPath(path) : path);
    }

    private static void handleFileContent(final FileContentPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            /*
             * Every window that opens a file says it is waiting for that file, by name, before it asks.
             * An answer nobody is waiting for belongs to a window that has since closed, and is dropped.
             */
            dev.jstech.computers.client.os.CodeFileReplies.content(
                    payload.path(), payload.content(), payload.exists());
        });
    }
}
