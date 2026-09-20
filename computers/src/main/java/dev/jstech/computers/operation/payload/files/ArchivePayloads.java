/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.files;

import dev.jstech.computers.client.os.PixWallpaper;
import dev.jstech.computers.operation.payload.ArchiveFilesPayload;
import dev.jstech.computers.operation.payload.ClientPayloadHandlers;
import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.computers.operation.payload.ExtractArchivePayload;
import dev.jstech.computers.operation.payload.FileContentPayload;
import dev.jstech.computers.operation.payload.FileSavedPayload;
import dev.jstech.computers.operation.payload.RequestWallpaperImagePayload;
import dev.jstech.computers.operation.payload.WallpaperImagePayload;
import dev.jstech.computers.os.FilesystemKind;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.fs.Archive;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.FileType;
import dev.jstech.computers.os.fs.PixImage;
import dev.jstech.computers.os.fs.StoredFile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The payloads that pack files into an archive and take them back out again.
 *
 * <p>Both happen on the server. The saving an archive gives has to be real, and only the server can say
 * what a disk has room for, so a client that asked for the impossible is told no rather than shown a
 * number that was never true.
 */
public final class ArchivePayloads {

    private ArchivePayloads() {
    }

    /** Registers the payloads this class handles. */
    public static void register(final PayloadRegistrar registrar) {
        ComputerAccess.accept(registrar, ArchiveFilesPayload.TYPE, ArchiveFilesPayload.STREAM_CODEC,
                ComputerAccess.machine(ArchiveFilesPayload::hostPos), ArchivePayloads::handleArchive);
        ComputerAccess.accept(registrar, ExtractArchivePayload.TYPE, ExtractArchivePayload.STREAM_CODEC,
                ComputerAccess.machine(ExtractArchivePayload::hostPos), ArchivePayloads::handleExtract);
        ComputerAccess.accept(registrar, RequestWallpaperImagePayload.TYPE,
                RequestWallpaperImagePayload.STREAM_CODEC,
                ComputerAccess.machine(RequestWallpaperImagePayload::hostPos),
                ArchivePayloads::handleWallpaper);
        registrar.playToClient(WallpaperImagePayload.TYPE, WallpaperImagePayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread(ArchivePayloads::handleWallpaperImage));
    }

    /**
     * Sends back the picture a desktop has been told to hang.
     *
     * <p>Refused politely rather than loudly when it is missing or is not a picture: an empty answer puts
     * the plain wallpaper back, which is the right thing for a drawing somebody deleted.
     */
    private static void handleWallpaper(final RequestWallpaperImagePayload payload, final ServerPlayer player,
                                        final ServerLevel level) {
        String content = "";
        if (level.getBlockEntity(payload.hostPos()) instanceof IOsHost computer) {
            final ItemStack disk = computer.systemDisk();
            final String read = disk.isEmpty() ? null
                    : DiskFilesystem.read(disk, payload.path()).orElse(null);
            if (read != null && read.startsWith(PixImage.MAGIC)
                    && read.length() <= WallpaperImagePayload.MAX_CONTENT) {
                content = read;
            }
        }
        PacketDistributor.sendToPlayer(player, new WallpaperImagePayload(payload.path(), content));
    }

    private static void handleWallpaperImage(final WallpaperImagePayload payload, final Player player) {
        PixWallpaper.accept(payload.path(), payload.content());
    }

    private static void handleArchive(final ArchiveFilesPayload payload, final ServerPlayer player,
                                      final ServerLevel level) {
        String message = "No computer";
        boolean ok = false;
        if (level.getBlockEntity(payload.hostPos()) instanceof IOsHost computer) {
            final ItemStack disk = computer.systemDisk();
            final FilesystemKind kind = FileAccess.filesystemKindOf(computer);
            if (disk.isEmpty() || kind == FilesystemKind.NONE) {
                message = "No system disk";
            } else if (payload.paths().isEmpty()) {
                message = "Nothing to archive";
            } else {
                message = pack(computer, disk, kind, payload, level);
                ok = message.startsWith("Packed");
            }
        }
        PacketDistributor.sendToPlayer(player, new FileSavedPayload(ok, message));
    }

    private static String pack(final IOsHost computer, final ItemStack disk, final FilesystemKind kind,
                               final ArchiveFilesPayload payload, final ServerLevel level) {
        final List<StoredFile> files = new ArrayList<>(payload.paths().size());
        for (final String path : payload.paths()) {
            /*
             * The archive must not be one of the files going into it. Written over a file that is then
             * deleted as an original, everything that went in would be gone: the archive overwrites it,
             * and the delete takes the archive away with it.
             */
            if (path.equals(payload.archivePath())) {
                return "An archive cannot hold itself";
            }
            final String content = DiskFilesystem.read(disk, path).orElse(null);
            if (content == null) {
                return "Missing " + Archive.leaf(path);
            }
            files.add(new StoredFile(path, typeOf(path), content));
        }
        final String packed;
        try {
            packed = Archive.pack(files);
        } catch (final IllegalArgumentException refused) {
            return refused.getMessage();
        }
        /*
         * An archive nobody could ever open again is not worth writing. A file is handed to a screen
         * through a packet with a cap on it, so one past that cap would be on the disk and unreadable.
         */
        if (packed.length() > FileContentPayload.MAX_CONTENT) {
            return "Too much to pack into one archive";
        }
        /*
         * The room needed is what the archive weighs, and the originals only pay for themselves once they
         * are actually gone. Counting them as free before they are deleted would let a full disk accept an
         * archive it has no room for and then fail halfway.
         */
        final long free = computer.systemDiskFreeWeight();
        final DiskFilesystem.WriteResult result = DiskFilesystem.write(
                disk, payload.archivePath(), FileType.ARK, packed, free, kind, level.getGameTime());
        switch (result) {
            case OK -> { }
            case DISK_FULL -> {
                return "Not enough free space";
            }
            case INVALID_PATH -> {
                return "Invalid archive name";
            }
            case READ_ONLY -> {
                return "Read-only";
            }
        }
        int removed = 0;
        if (payload.removeOriginals()) {
            for (final String path : payload.paths()) {
                if (DiskFilesystem.delete(disk, path)) {
                    removed++;
                }
            }
        }
        computer.setChanged();
        final int before = Archive.originalBytes(packed);
        final int after = packed.getBytes(StandardCharsets.UTF_8).length;
        return "Packed " + files.size() + " into " + Archive.leaf(payload.archivePath())
                + ", " + before + " bytes became " + after
                + (removed > 0 ? ", " + removed + " removed" : "");
    }

    private static void handleExtract(final ExtractArchivePayload payload, final ServerPlayer player,
                                      final ServerLevel level) {
        String message = "No computer";
        boolean ok = false;
        if (level.getBlockEntity(payload.hostPos()) instanceof IOsHost computer) {
            final ItemStack disk = computer.systemDisk();
            final FilesystemKind kind = FileAccess.filesystemKindOf(computer);
            final String content = disk.isEmpty() ? null
                    : DiskFilesystem.read(disk, payload.archivePath()).orElse(null);
            if (disk.isEmpty() || kind == FilesystemKind.NONE) {
                message = "No system disk";
            } else if (content == null) {
                message = "No such archive";
            } else if (!Archive.isArchive(content)) {
                message = Archive.leaf(payload.archivePath()) + " is not an archive";
            } else {
                message = extract(computer, disk, kind, payload, content, level);
                ok = message.startsWith("Took");
            }
        }
        PacketDistributor.sendToPlayer(player, new FileSavedPayload(ok, message));
    }

    private static String extract(final IOsHost computer, final ItemStack disk, final FilesystemKind kind,
                                  final ExtractArchivePayload payload, final String content,
                                  final ServerLevel level) {
        final List<StoredFile> inside = Archive.unpack(content);
        if (inside.isEmpty()) {
            return "The archive is damaged";
        }
        final List<StoredFile> wanted = new ArrayList<>();
        for (final StoredFile file : inside) {
            if (payload.entry().isEmpty() || file.path().equals(payload.entry())) {
                wanted.add(file);
            }
        }
        if (wanted.isEmpty()) {
            return "No " + payload.entry() + " in the archive";
        }
        /*
         * Everything is weighed before anything is written, so a disk that cannot hold the lot refuses the
         * lot instead of unpacking half of it and leaving the player to work out which half.
         */
        long needed = 0L;
        for (final StoredFile file : wanted) {
            needed += file.weight(DiskFilesystem.eraOf(disk));
        }
        if (needed > computer.systemDiskFreeWeight()) {
            return "Not enough free space for " + wanted.size()
                    + (wanted.size() == 1 ? " file" : " files");
        }
        int written = 0;
        for (final StoredFile file : wanted) {
            final String target = into(payload.intoDir(), file.path(), kind);
            final DiskFilesystem.WriteResult result = DiskFilesystem.write(disk, target, file.type(),
                    file.content(), computer.systemDiskFreeWeight(), kind, level.getGameTime());
            if (result == DiskFilesystem.WriteResult.OK) {
                written++;
            }
        }
        computer.setChanged();
        if (written == 0) {
            return "Nothing could be written";
        }
        return "Took out " + written + (written == 1 ? " file" : " files");
    }

    /** Where a file inside the archive lands, which is its own name under the folder asked for. */
    private static String into(final String dir, final String name, final FilesystemKind kind) {
        if (kind == FilesystemKind.FLAT || dir == null || dir.isEmpty()) {
            return name;
        }
        return dir.endsWith("/") ? dir + name : dir + "/" + name;
    }

    private static FileType typeOf(final String path) {
        final int dot = path.lastIndexOf('.');
        final String ext = dot >= 0 && dot < path.length() - 1
                ? path.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
        return FileType.of(ext);
    }
}
