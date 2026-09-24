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
import dev.jstech.core.text.Text;
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
        Text message = FileSavedPayload.NO_COMPUTER.text();
        boolean ok = false;
        if (level.getBlockEntity(payload.hostPos()) instanceof IOsHost computer) {
            final ItemStack disk = computer.systemDisk();
            final FilesystemKind kind = FileAccess.filesystemKindOf(computer);
            if (disk.isEmpty() || kind == FilesystemKind.NONE) {
                message = FileSavedPayload.NO_SYSTEM_DISK.text();
            } else if (payload.paths().isEmpty()) {
                message = ArchiveTexts.NOTHING_TO_ARCHIVE.text();
            } else {
                final Outcome packed = pack(computer, disk, kind, payload, level);
                message = packed.message();
                ok = packed.ok();
            }
        }
        PacketDistributor.sendToPlayer(player, new FileSavedPayload(ok, message));
    }

    private static Outcome pack(final IOsHost computer, final ItemStack disk, final FilesystemKind kind,
                                final ArchiveFilesPayload payload, final ServerLevel level) {
        /*
         * A folder stands for everything under it, so compressing one from the right button packs what is
         * in it rather than failing on a path that names no file. Gathered here because only the machine
         * knows what a folder holds.
         */
        final List<String> wanted = new ArrayList<>();
        for (final String path : payload.paths()) {
            if (DiskFilesystem.read(disk, path).isPresent()) {
                wanted.add(path);
                continue;
            }
            final int before = wanted.size();
            gather(disk, path, kind, wanted);
            if (wanted.size() == before) {
                return Outcome.refused(ArchiveTexts.NOTHING_IN.with(Archive.leaf(path)));
            }
        }
        final List<StoredFile> files = new ArrayList<>(wanted.size());
        for (final String path : wanted) {
            /*
             * The archive must not be one of the files going into it. Written over a file that is then
             * deleted as an original, everything that went in would be gone: the archive overwrites it,
             * and the delete takes the archive away with it.
             */
            if (path.equals(payload.archivePath())) {
                return Outcome.refused(ArchiveTexts.HOLDS_ITSELF.text());
            }
            final String content = DiskFilesystem.read(disk, path).orElse(null);
            if (content == null) {
                return Outcome.refused(ArchiveTexts.MISSING.with(Archive.leaf(path)));
            }
            files.add(new StoredFile(path, typeOf(path), content));
        }
        final String packed;
        try {
            packed = Archive.pack(files);
        } catch (final Archive.Refused refused) {
            return Outcome.refused(refused.text());
        }
        /*
         * An archive nobody could ever open again is not worth writing. A file is handed to a screen
         * through a packet with a cap on it, so one past that cap would be on the disk and unreadable.
         */
        if (packed.length() > FileContentPayload.MAX_CONTENT) {
            return Outcome.refused(ArchiveTexts.TOO_MUCH.text());
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
                return Outcome.refused(FileSavedPayload.NO_ROOM.text());
            }
            case INVALID_PATH -> {
                return Outcome.refused(ArchiveTexts.INVALID_NAME.text());
            }
            case READ_ONLY -> {
                return Outcome.refused(FileSavedPayload.READ_ONLY.text());
            }
        }
        int removed = 0;
        if (payload.removeOriginals()) {
            // What actually went in, so a folder that stood for its files takes those files away.
            for (final String path : wanted) {
                if (DiskFilesystem.delete(disk, path)) {
                    removed++;
                }
            }
        }
        computer.setChanged();
        final int before = Archive.originalBytes(packed);
        final int after = packed.getBytes(StandardCharsets.UTF_8).length;
        final String into = Archive.leaf(payload.archivePath());
        return Outcome.done(removed > 0
                ? ArchiveTexts.PACKED_REMOVED.with(files.size(), into, before, after, removed)
                : ArchiveTexts.PACKED.with(files.size(), into, before, after));
    }

    /**
     * Every real file at or under {@code dir}, however deep, added to {@code out}.
     *
     * <p>The projected view of what a drive is holding is left out: it is a window onto the network's
     * storage rather than a file, and packing it would archive a picture of something that is still there.
     */
    private static void gather(final ItemStack disk, final String dir, final FilesystemKind kind,
                               final List<String> out) {
        /*
         * A flat disk has no folders, so a path that named no file names nothing at all. Without this the
         * listing, which ignores the folder it is given on such a disk, would answer with every file there.
         */
        if (kind != FilesystemKind.HIERARCHICAL) {
            return;
        }
        for (final DiskFilesystem.FileEntry entry : DiskFilesystem.list(disk, dir, kind)) {
            if (!entry.type().virtualProjection() && !out.contains(entry.path())) {
                out.add(entry.path());
            }
        }
        for (final String sub : DiskFilesystem.listDirs(disk, dir, kind)) {
            // Only downwards, so a listing that ever named its own folder could not send this round for ever.
            if (sub.length() > dir.length() && sub.startsWith(dir)) {
                gather(disk, sub, kind, out);
            }
        }
    }

    private static void handleExtract(final ExtractArchivePayload payload, final ServerPlayer player,
                                      final ServerLevel level) {
        Text message = FileSavedPayload.NO_COMPUTER.text();
        boolean ok = false;
        if (level.getBlockEntity(payload.hostPos()) instanceof IOsHost computer) {
            final ItemStack disk = computer.systemDisk();
            final FilesystemKind kind = FileAccess.filesystemKindOf(computer);
            final String content = disk.isEmpty() ? null
                    : DiskFilesystem.read(disk, payload.archivePath()).orElse(null);
            if (disk.isEmpty() || kind == FilesystemKind.NONE) {
                message = FileSavedPayload.NO_SYSTEM_DISK.text();
            } else if (content == null) {
                message = ArchiveTexts.NO_SUCH_ARCHIVE.text();
            } else if (!Archive.isArchive(content)) {
                message = ArchiveTexts.NOT_AN_ARCHIVE.with(Archive.leaf(payload.archivePath()));
            } else {
                final Outcome taken = extract(computer, disk, kind, payload, content, level);
                message = taken.message();
                ok = taken.ok();
            }
        }
        PacketDistributor.sendToPlayer(player, new FileSavedPayload(ok, message));
    }

    private static Outcome extract(final IOsHost computer, final ItemStack disk, final FilesystemKind kind,
                                   final ExtractArchivePayload payload, final String content,
                                   final ServerLevel level) {
        final List<StoredFile> inside = Archive.unpack(content);
        if (inside.isEmpty()) {
            return Outcome.refused(ArchiveTexts.DAMAGED.text());
        }
        final List<StoredFile> wanted = new ArrayList<>();
        for (final StoredFile file : inside) {
            if (payload.entry().isEmpty() || file.path().equals(payload.entry())) {
                wanted.add(file);
            }
        }
        if (wanted.isEmpty()) {
            return Outcome.refused(ArchiveTexts.NOT_INSIDE.with(payload.entry()));
        }
        /*
         * A file already sitting where one is going is left exactly as it is. Writing over it would be an
         * archive quietly destroying whatever somebody had done to their copy since, with nothing to undo
         * it with, and taking things out of an archive must never be able to lose anything.
         */
        int inTheWay = 0;
        final List<StoredFile> going = new ArrayList<>(wanted.size());
        for (final StoredFile file : wanted) {
            if (DiskFilesystem.exists(disk, into(payload.intoDir(), file.path(), kind))) {
                inTheWay++;
            } else {
                going.add(file);
            }
        }
        if (going.isEmpty()) {
            return Outcome.refused(inTheWay == 1 ? ArchiveTexts.ONE_ALREADY_THERE.text()
                    : ArchiveTexts.ALL_ALREADY_THERE.with(inTheWay));
        }
        /*
         * Everything is weighed before anything is written, so a disk that cannot hold the lot refuses the
         * lot instead of unpacking half of it and leaving the player to work out which half.
         */
        long needed = 0L;
        for (final StoredFile file : going) {
            needed += file.weight(DiskFilesystem.eraOf(disk));
        }
        if (needed > computer.systemDiskFreeWeight()) {
            return Outcome.refused((going.size() == 1 ? ArchiveTexts.NO_ROOM_FOR_ONE : ArchiveTexts.NO_ROOM_FOR)
                    .with(going.size()));
        }
        int written = 0;
        for (final StoredFile file : going) {
            final String target = into(payload.intoDir(), file.path(), kind);
            final DiskFilesystem.WriteResult result = DiskFilesystem.write(disk, target, file.type(),
                    file.content(), computer.systemDiskFreeWeight(), kind, level.getGameTime());
            if (result == DiskFilesystem.WriteResult.OK) {
                written++;
            }
        }
        computer.setChanged();
        if (written > 0 && inTheWay > 0) {
            return Outcome.done((written == 1 ? ArchiveTexts.TOOK_OUT_ONE_SKIPPED : ArchiveTexts.TOOK_OUT_SKIPPED)
                    .with(written, inTheWay));
        }
        if (written == 0) {
            return Outcome.refused(ArchiveTexts.NOTHING_WRITTEN.text());
        }
        return Outcome.done((written == 1 ? ArchiveTexts.TOOK_OUT_ONE : ArchiveTexts.TOOK_OUT).with(written));
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

    /** What packing or taking out came to: whether it was done, and what to tell the player either way. */
    private record Outcome(boolean ok, Text message) {

        static Outcome done(final Text message) {
            return new Outcome(true, message);
        }

        static Outcome refused(final Text message) {
            return new Outcome(false, message);
        }
    }
}
