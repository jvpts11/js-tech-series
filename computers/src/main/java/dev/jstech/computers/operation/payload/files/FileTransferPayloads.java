/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.files;

import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.computers.operation.payload.CopyFilePayload;
import dev.jstech.computers.operation.payload.MediumTransferPayload;
import dev.jstech.computers.operation.payload.MoveFilePayload;
import dev.jstech.computers.operation.payload.RenameVolumePayload;
import dev.jstech.computers.os.FilesystemKind;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.VolumeLabel;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.FileType;
import dev.jstech.computers.os.media.MediaItem;
import dev.jstech.computers.os.media.MediaKind;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.storage.StorageKey;
import java.util.Locale;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import static dev.jstech.computers.operation.payload.files.FileAccess.NET_ROOT;
import static dev.jstech.computers.operation.payload.files.FileAccess.commitMedia;
import static dev.jstech.computers.operation.payload.files.FileAccess.filesystemKindOf;
import static dev.jstech.computers.operation.payload.files.FileAccess.localDos;
import static dev.jstech.computers.operation.payload.files.FileAccess.mediaFreeWeight;
import static dev.jstech.computers.operation.payload.files.FileAccess.mediaStackFor;
import static dev.jstech.computers.operation.payload.files.FileAccess.mediaSubPath;
import static dev.jstech.computers.operation.payload.files.FileAccess.netDos;
import static dev.jstech.computers.operation.payload.files.FileAccess.netShell;
import static dev.jstech.computers.operation.payload.files.FileAccess.resolveDatKey;
import static dev.jstech.computers.operation.payload.files.FileAccess.transferDatToMedium;
import static dev.jstech.computers.operation.payload.files.FileAccess.volumeKey;
import static dev.jstech.computers.operation.payload.interactor.NetworkInteractorPayloads.sendNetworkInteractor;
import static dev.jstech.computers.operation.payload.terminal.TerminalHosts.niHost;

/**
 * The payloads that copy and move files between drives, move a data file onto a medium and rename a volume.
 */
public final class FileTransferPayloads {

    private FileTransferPayloads() {
    }

    /** Registers the payloads this class handles. */
    public static void register(final PayloadRegistrar registrar) {
        ComputerAccess.accept(registrar, CopyFilePayload.TYPE, CopyFilePayload.STREAM_CODEC,
                ComputerAccess.machine(CopyFilePayload::hostPos), FileTransferPayloads::handleCopyFile);
        ComputerAccess.accept(registrar, MoveFilePayload.TYPE, MoveFilePayload.STREAM_CODEC,
                ComputerAccess.machine(MoveFilePayload::hostPos), FileTransferPayloads::handleMoveFile);
        ComputerAccess.accept(registrar, MediumTransferPayload.TYPE, MediumTransferPayload.STREAM_CODEC,
                ComputerAccess.machine(MediumTransferPayload::hostPos), FileTransferPayloads::handleMediumTransfer);
        ComputerAccess.accept(registrar, RenameVolumePayload.TYPE, RenameVolumePayload.STREAM_CODEC,
                ComputerAccess.machine(RenameVolumePayload::host), FileTransferPayloads::handleRenameVolume);
    }

    /**
     * Copies a file within a volume or across to another one; the source stays. A projected file has no
     * bytes and is refused by the read; a name already taken gets a numbered copy rather than overwriting.
     */
    private static void handleCopyFile(final CopyFilePayload payload, final ServerPlayer player,
                                       final ServerLevel level) {
        if (!(level.getBlockEntity(payload.hostPos()) instanceof IOsHost computer)) {
            return;
        }
        final String src = payload.src();
        final String destDir = payload.destDir();
        if (src.startsWith(NET_ROOT) || destDir.startsWith(NET_ROOT)) {
            /*
             * A copy to or from another machine's share goes through the shell, which reads where
             * the file is and writes where it goes; a medium is not on that road.
             */
            if (src.startsWith("media:") || destDir.startsWith("media:")) {
                return;
            }
            final ServerCliComputer shell = netShell(level, computer);
            if (shell != null && shell.copyPath(src.startsWith(NET_ROOT) ? netDos(src) : localDos(src),
                    destDir.startsWith(NET_ROOT) ? netDos(destDir) : localDos(destDir)).ok()) {
                computer.setChanged();
            }
            return;
        }
        final boolean srcMedia = src.startsWith("media:");
        final boolean dstMedia = destDir.startsWith("media:");
        final ItemStack srcVol =
                srcMedia ? mediaStackFor(level, computer, src) : computer.systemDisk();
        final ItemStack dstVol =
                dstMedia ? mediaStackFor(level, computer, destDir) : computer.systemDisk();
        if (srcVol.isEmpty() || dstVol.isEmpty()) {
            return;
        }
        final String realSrc = srcMedia ? mediaSubPath(src) : src;
        final String realDstDir = dstMedia ? mediaSubPath(destDir) : destDir;
        final var read = DiskFilesystem.read(srcVol, realSrc);
        if (read.isEmpty()) {
            return;
        }
        final String name = realSrc.contains("/") ? realSrc.substring(realSrc.lastIndexOf('/') + 1) : realSrc;
        final int dot = name.lastIndexOf('.');
        final String stem = dot > 0 ? name.substring(0, dot) : name;
        final String ext = dot >= 0 && dot < name.length() - 1
                ? name.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
        final FileType type = FileType.of(ext);
        final FilesystemKind dstKind = dstMedia
                ? FilesystemKind.HIERARCHICAL
                : filesystemKindOf(computer);
        // "name - Copy.ext", then "name - Copy (2).ext", the way a desktop names a duplicate.
        String candidate = name;
        final String suffix = ext.isEmpty() ? "" : "." + ext;
        for (int n = 1; n < 100; n++) {
            final String path = realDstDir.isEmpty() ? candidate : realDstDir + "/" + candidate;
            if (!DiskFilesystem.exists(dstVol, path)) {
                break;
            }
            candidate = stem + (n == 1 ? " - Copy" : " - Copy (" + n + ")") + suffix;
        }
        final String destPath = realDstDir.isEmpty() ? candidate : realDstDir + "/" + candidate;
        final long free = dstMedia ? mediaFreeWeight(dstVol) : computer.systemDiskFreeWeight();
        if (DiskFilesystem.write(
                dstVol, destPath, type, read.get(), free, dstKind, level.getGameTime())
                == DiskFilesystem.WriteResult.OK) {
            if (dstMedia) {
                commitMedia(level, computer, destDir);
            } else {
                computer.setChanged();
            }
        }
    }

    private static void handleMoveFile(final MoveFilePayload payload, final ServerPlayer player,
                                       final ServerLevel level) {
        if (!(level.getBlockEntity(payload.hostPos()) instanceof IOsHost computer)) {
            return;
        }
        final String src = payload.srcPath();
        final String destDir = payload.destDir();
        if (src.startsWith(NET_ROOT) || destDir.startsWith(NET_ROOT)) {
            // A file on another machine is copied, not moved: the copy is what the explorer offers.
            return;
        }
        final boolean srcMedia = src.startsWith("media:");
        final boolean dstMedia = destDir.startsWith("media:");
        final ItemStack srcVol =
                srcMedia ? mediaStackFor(level, computer, src) : computer.systemDisk();
        final ItemStack dstVol =
                dstMedia ? mediaStackFor(level, computer, destDir) : computer.systemDisk();
        if (srcVol.isEmpty() || dstVol.isEmpty()) {
            return;
        }
        final String realSrc = srcMedia ? mediaSubPath(src) : src;
        final String realDstDir = dstMedia ? mediaSubPath(destDir) : destDir;
        final FilesystemKind srcKind = srcMedia
                ? FilesystemKind.HIERARCHICAL
                : filesystemKindOf(computer);
        if (volumeKey(src).equals(volumeKey(destDir))) {
            // Same volume, an in-place move.
            if (DiskFilesystem.move(
                    srcVol, realSrc, realDstDir, srcKind)) {
                if (srcMedia) {
                    commitMedia(level, computer, src);
                } else {
                    computer.setChanged();
                }
            }
            return;
        }
        /*
         * Cross-volume (disk <-> media): copy the file then delete the source. Directories are
         * not copied across volumes here.
         */
        final var read = DiskFilesystem.read(srcVol, realSrc);
        if (read.isEmpty()) {
            return;
        }
        final String name = realSrc.contains("/")
                ? realSrc.substring(realSrc.lastIndexOf('/') + 1) : realSrc;
        final int dot = name.lastIndexOf('.');
        final String ext = dot >= 0 && dot < name.length() - 1
                ? name.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
        final FileType type = FileType.of(ext);
        final FilesystemKind dstKind = dstMedia
                ? FilesystemKind.HIERARCHICAL
                : filesystemKindOf(computer);
        final String destPath = realDstDir.isEmpty() ? name : realDstDir + "/" + name;
        final long free = dstMedia ? mediaFreeWeight(dstVol) : computer.systemDiskFreeWeight();
        if (DiskFilesystem.write(
                dstVol, destPath, type, read.get(), free, dstKind, level.getGameTime())
                == DiskFilesystem.WriteResult.OK) {
            DiskFilesystem.delete(srcVol, realSrc);
            if (srcMedia) {
                commitMedia(level, computer, src);
            } else {
                computer.setChanged();
            }
            if (dstMedia) {
                commitMedia(level, computer, destDir);
            } else {
                computer.setChanged();
            }
        }
    }

    /**
     * Sanctioned {@code .dat}-onto-medium item transfer (the one manual {@code .dat} operation that is allowed).
     *
     * <p>A {@code .dat} is a read-only projection of an item kept in the computer's disks. Dragging it onto a
     * removable medium does not copy a file; it moves the stored item. The flow is conservative end to end so
     * an item is never lost or duplicated:
     * <ol>
     *   <li>Resolve {@code datPath} back to its {@link StorageKey} by re-projecting the system disk (the
     *       projection is deterministic, so the same path maps back to the same key).</li>
     *   <li>Extract the full stored quantity of that key from the computer's local storage.</li>
     *   <li>Insert as much as the medium's free capacity allows into its {@code MEDIA_DATA} snapshot; whatever
     *       does not fit is returned to the computer's storage.</li>
     * </ol>
     * The source {@code .dat} vanishes on its own once the key leaves the disk's volume, and the item then
     * shows up under the medium's projection, with no second item-movement path, no byte copy.
     */
    private static void handleMediumTransfer(final MediumTransferPayload payload, final ServerPlayer player,
                                             final ServerLevel level) {
        final var host = niHost(player, level, payload.hostPos(), payload.monitorPos());
        if (host == null
                || !(host instanceof IOsHost computer)) {
            return;
        }
        // The destination must be a DATA medium in a linked drive; anything else cannot hold a snapshot.
        final String mediaKey = payload.mediaVolumeKey();
        if (!mediaKey.startsWith("media:")) {
            return;
        }
        final ItemStack media = mediaStackFor(level, computer, mediaKey);
        if (media.isEmpty()
                || MediaItem.kind(media)
                        != MediaKind.DATA) {
            return;
        }
        // Resolve the .dat path back to the StorageKey it projects from the system disk.
        final StorageKey key = resolveDatKey(computer.systemDisk(), payload.datPath());
        if (key == null) {
            return;
        }
        if (transferDatToMedium(host, key, media) > 0L) {
            commitMedia(level, computer, mediaKey);
            computer.setChanged();
            sendNetworkInteractor(player, level, computer);
        }
    }

    private static void handleRenameVolume(final RenameVolumePayload payload, final ServerPlayer player,
                                           final ServerLevel level) {
        if (!(level.getBlockEntity(payload.host()) instanceof IOsHost computer)) {
            return;
        }
        final String key = payload.volumeKey();
        final boolean media = key.startsWith("media:");
        final ItemStack vol;
        if (media) {
            vol = mediaStackFor(level, computer, key);
        } else if (key.startsWith("disk:")) {
            // Rename a specific installed disk by its slot (not just the system disk).
            int slot = -1;
            try {
                slot = Integer.parseInt(key.substring("disk:".length()));
            } catch (final NumberFormatException ignored) {
                // leaves slot = -1, which diskInSlot rejects
            }
            vol = computer.diskInSlot(slot);
        } else {
            vol = computer.systemDisk();
        }
        if (vol.isEmpty()) {
            return;
        }
        VolumeLabel.set(vol, payload.label());
        if (media) {
            commitMedia(level, computer, key);
        } else {
            computer.setChanged();
        }
    }
}
