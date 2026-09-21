/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.files;

import dev.jstech.computers.client.os.CodeFileReplies;
import dev.jstech.computers.operation.payload.ClientPayloadHandlers;
import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.computers.operation.payload.DeleteFilePayload;
import dev.jstech.computers.operation.payload.FileSavedPayload;
import dev.jstech.computers.operation.payload.MkdirPayload;
import dev.jstech.computers.operation.payload.RenameFilePayload;
import dev.jstech.computers.operation.payload.SaveFilePayload;
import dev.jstech.computers.os.FilesystemKind;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.FileType;
import dev.jstech.computers.os.fs.FsPaths;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.ICliComputer;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import static dev.jstech.computers.operation.payload.files.FileAccess.NET_ROOT;
import static dev.jstech.computers.operation.payload.files.FileAccess.commitMedia;
import static dev.jstech.computers.operation.payload.files.FileAccess.filesystemKindOf;
import static dev.jstech.computers.operation.payload.files.FileAccess.mediaFreeWeight;
import static dev.jstech.computers.operation.payload.files.FileAccess.mediaStackFor;
import static dev.jstech.computers.operation.payload.files.FileAccess.mediaSubPath;
import static dev.jstech.computers.operation.payload.files.FileAccess.netDos;
import static dev.jstech.computers.operation.payload.files.FileAccess.netShell;

/**
 * The payloads that save, delete and rename files and create folders.
 */
public final class FileEditPayloads {

    private FileEditPayloads() {
    }

    /** Registers the payloads this class handles. */
    public static void register(final PayloadRegistrar registrar) {
        ComputerAccess.accept(registrar, SaveFilePayload.TYPE, SaveFilePayload.STREAM_CODEC,
                ComputerAccess.machine(SaveFilePayload::hostPos), FileEditPayloads::handleSaveFile);
        registrar.playToClient(FileSavedPayload.TYPE, FileSavedPayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread(FileEditPayloads::handleFileSaved));
        ComputerAccess.accept(registrar, DeleteFilePayload.TYPE, DeleteFilePayload.STREAM_CODEC,
                ComputerAccess.machine(DeleteFilePayload::hostPos), FileEditPayloads::handleDeleteFile);
        ComputerAccess.accept(registrar, RenameFilePayload.TYPE, RenameFilePayload.STREAM_CODEC,
                ComputerAccess.machine(RenameFilePayload::hostPos), FileEditPayloads::handleRenameFile);
        ComputerAccess.accept(registrar, MkdirPayload.TYPE, MkdirPayload.STREAM_CODEC,
                ComputerAccess.machine(MkdirPayload::hostPos), FileEditPayloads::handleMkdir);
    }

    private static void handleSaveFile(final SaveFilePayload payload, final ServerPlayer player,
                                       final ServerLevel level) {
        boolean ok = false;
        String msg = "No computer";
        if (level.getBlockEntity(payload.hostPos()) instanceof IOsHost computer) {
            final String path = payload.path();
            if (LiveSessionFiles.names(path)) {
                // A file of a by-hand install: what is written is what the steps after it read.
                final boolean written = LiveSessionFiles.write(computer, path, payload.content());
                PacketDistributor.sendToPlayer(player,
                        new FileSavedPayload(written, written ? "Saved " + path : "No live medium is booted"));
                return;
            }
            if (path.startsWith(NET_ROOT)) {
                // Another machine's share: its own shell says whether the write may happen.
                final ServerCliComputer shell = netShell(level, computer);
                final ICliComputer.FsResult written =
                        shell == null ? null : shell.writeFile(netDos(path), payload.content());
                PacketDistributor.sendToPlayer(player, new FileSavedPayload(written != null && written.ok(),
                        written == null ? "No shell" : written.ok() ? "Saved " + path : written.message()));
                return;
            }
            final boolean media = path.startsWith("media:");
            final ItemStack vol =
                    media ? mediaStackFor(level, computer, path) : computer.systemDisk();
            final String real = media ? mediaSubPath(path) : path;
            final FilesystemKind kind = media
                    ? FilesystemKind.HIERARCHICAL
                    : filesystemKindOf(computer);
            final int dot = real.lastIndexOf('.');
            final String ext = dot >= 0 && dot < real.length() - 1
                    ? real.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
            final FileType type = FileType.of(ext);
            if (vol.isEmpty()
                    || kind == FilesystemKind.NONE) {
                msg = media ? "No medium" : "No system disk";
            } else if (!type.userEditable()) {
                msg = "." + type.extension() + " is read-only";
            } else {
                final long oldWeight = DiskFilesystem
                        .read(vol, real)
                        .map(c -> FsPaths.sizeMbEq(
                                c.getBytes(StandardCharsets.UTF_8).length,
                                DiskFilesystem.eraOf(vol)))
                        .orElse(0L);
                final long free = media ? mediaFreeWeight(vol) + oldWeight
                        : computer.systemDiskFreeWeight() + oldWeight;
                final var result = DiskFilesystem.write(
                        vol, real, type, payload.content(), free, kind, level.getGameTime());
                switch (result) {
                    case OK -> {
                        if (media) {
                            commitMedia(level, computer, path);
                        } else {
                            computer.setChanged();
                        }
                        ok = true;
                        msg = "Saved " + path;
                    }
                    case INVALID_PATH -> msg = "Invalid file name";
                    case DISK_FULL -> msg = "Not enough free space";
                    case READ_ONLY -> msg = "Read-only";
                }
            }
        }
        PacketDistributor.sendToPlayer(player, new FileSavedPayload(ok, msg));
    }

    private static void handleFileSaved(final FileSavedPayload payload, final Player player) {
        // Whoever asked for the save said so first; a result nobody is waiting for has no window left.
        CodeFileReplies.saved(payload.ok(), payload.message());
    }

    private static void handleDeleteFile(final DeleteFilePayload payload, final ServerPlayer player,
                                         final ServerLevel level) {
        if (!(level.getBlockEntity(payload.hostPos()) instanceof IOsHost computer)) {
            return;
        }
        final String path = payload.path();
        if (path.startsWith(NET_ROOT)) {
            final ServerCliComputer shell = netShell(level, computer);
            if (shell != null && shell.deleteFile(netDos(path)).ok()) {
                computer.setChanged();
            }
            return;
        }
        final boolean media = path.startsWith("media:");
        final ItemStack vol =
                media ? mediaStackFor(level, computer, path) : computer.systemDisk();
        if (vol.isEmpty()) {
            return;
        }
        final String real = media ? mediaSubPath(path) : path;
        final FilesystemKind kind = media
                ? FilesystemKind.HIERARCHICAL
                : filesystemKindOf(computer);
        // Try removing a real file first; if the path is a folder, remove it recursively.
        if (DiskFilesystem.delete(vol, real)
                || DiskFilesystem.rmdir(vol, real, kind)) {
            if (media) {
                commitMedia(level, computer, path);
            } else {
                computer.setChanged();
            }
        }
    }

    private static void handleMkdir(final MkdirPayload payload, final ServerPlayer player, final ServerLevel level) {
        if (!(level.getBlockEntity(payload.hostPos()) instanceof IOsHost computer)) {
            return;
        }
        final String path = payload.path();
        if (path.startsWith(NET_ROOT)) {
            final ServerCliComputer shell = netShell(level, computer);
            if (shell != null && shell.makeDir(netDos(path)).ok()) {
                computer.setChanged();
            }
            return;
        }
        final boolean media = path.startsWith("media:");
        final ItemStack vol =
                media ? mediaStackFor(level, computer, path) : computer.systemDisk();
        if (vol.isEmpty()) {
            return;
        }
        final String real = media ? mediaSubPath(path) : path;
        final FilesystemKind kind = media
                ? FilesystemKind.HIERARCHICAL
                : filesystemKindOf(computer);
        if (DiskFilesystem.mkdir(vol, real, kind)) {
            if (media) {
                commitMedia(level, computer, path);
            } else {
                computer.setChanged();
            }
        }
    }

    private static void handleRenameFile(final RenameFilePayload payload, final ServerPlayer player,
                                         final ServerLevel level) {
        if (!(level.getBlockEntity(payload.hostPos()) instanceof IOsHost computer)) {
            return;
        }
        final String oldPath = payload.oldPath();
        final String newPath = payload.newPath();
        if (oldPath.startsWith(NET_ROOT)) {
            // Another machine's files keep the names their owner gave them.
            return;
        }
        final boolean media = oldPath.startsWith("media:");
        final ItemStack vol =
                media ? mediaStackFor(level, computer, oldPath) : computer.systemDisk();
        if (vol.isEmpty()) {
            return;
        }
        final FilesystemKind kind = media
                ? FilesystemKind.HIERARCHICAL
                : filesystemKindOf(computer);
        // rename() re-keys a real file or directory in place (rejecting .dat projections).
        if (DiskFilesystem.rename(vol,
                media ? mediaSubPath(oldPath) : oldPath,
                media ? mediaSubPath(newPath) : newPath, kind)) {
            if (media) {
                commitMedia(level, computer, oldPath);
            } else {
                computer.setChanged();
            }
        }
    }
}
