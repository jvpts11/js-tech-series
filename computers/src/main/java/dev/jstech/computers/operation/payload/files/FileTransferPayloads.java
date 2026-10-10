/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.files;

import dev.jstech.computers.client.os.DesktopCopies;
import dev.jstech.computers.machine.FileCopyJobs;
import dev.jstech.computers.operation.payload.CancelCopyPayload;
import dev.jstech.computers.operation.payload.ClientPayloadHandlers;
import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.computers.operation.payload.CopyFilePayload;
import dev.jstech.computers.operation.payload.CopyProgressPayload;
import dev.jstech.computers.operation.payload.MediumTransferPayload;
import dev.jstech.computers.operation.payload.MoveFilePayload;
import dev.jstech.computers.operation.payload.PauseCopyPayload;
import dev.jstech.computers.operation.payload.RenameVolumePayload;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.VolumeLabel;
import dev.jstech.computers.os.fs.CopyTiming;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.FileType;
import dev.jstech.computers.os.media.MediaItem;
import dev.jstech.computers.os.media.MediaKind;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.storage.StorageKey;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.jetbrains.annotations.Nullable;

import static dev.jstech.computers.operation.payload.files.FileAccess.NET_ROOT;
import static dev.jstech.computers.operation.payload.files.FileAccess.commitMedia;
import static dev.jstech.computers.operation.payload.files.FileAccess.kindOf;
import static dev.jstech.computers.operation.payload.files.FileAccess.localDos;
import static dev.jstech.computers.operation.payload.files.FileAccess.mediaFreeWeight;
import static dev.jstech.computers.operation.payload.files.FileAccess.mediaStackFor;
import static dev.jstech.computers.operation.payload.files.FileAccess.mediaSubPath;
import static dev.jstech.computers.operation.payload.files.FileAccess.netDos;
import static dev.jstech.computers.operation.payload.files.FileAccess.netShell;
import static dev.jstech.computers.operation.payload.files.FileAccess.resolveDatKey;
import static dev.jstech.computers.operation.payload.files.FileAccess.transferDatToMedium;
import static dev.jstech.computers.operation.payload.files.FileAccess.volumeKey;
import static dev.jstech.computers.operation.payload.files.FileAccess.volumeOf;
import static dev.jstech.computers.operation.payload.interactor.NetworkInteractorPayloads.sendNetworkInteractor;
import static dev.jstech.computers.operation.payload.terminal.TerminalHosts.niHost;

/**
 * The payloads that copy and move files between drives, move a data file onto a medium and rename a volume.
 */
public final class FileTransferPayloads {

    /** How many names a copy tries ("name", "name - Copy", "name - Copy (2)" and on) before it gives up. */
    private static final int MAX_COPY_NAMES = 100;

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
        ComputerAccess.accept(registrar, CancelCopyPayload.TYPE, CancelCopyPayload.STREAM_CODEC,
                ComputerAccess.machine(CancelCopyPayload::hostPos),
                (payload, player, level) -> FileCopyJobs.cancel(level, payload.hostPos(), payload.job()));
        ComputerAccess.accept(registrar, PauseCopyPayload.TYPE, PauseCopyPayload.STREAM_CODEC,
                ComputerAccess.machine(PauseCopyPayload::hostPos),
                (payload, player, level) -> FileCopyJobs.pause(level, payload.hostPos(), payload.pause()));
        registrar.playToClient(CopyProgressPayload.TYPE, CopyProgressPayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread((payload, player) -> DesktopCopies.accept(payload)));
    }

    /**
     * Copies a file within a volume or across to another one; the source stays. The copy takes the time its size
     * and the slower of the two volumes say, or the slowest cable on the way to another machine, and the file arrives
     * when that time is up.
     */
    private static void handleCopyFile(final CopyFilePayload payload, final ServerPlayer player,
                                       final ServerLevel level) {
        copy(level, player, payload.hostPos(), payload.src(), payload.destDir());
    }

    /**
     * Starts copying the file at {@code src} into {@code destDir} on the machine at {@code host} for {@code player},
     * which is what pasting a copy in the explorer does: the file arrives when the copy's time is up.
     */
    public static void copy(final ServerLevel level, final ServerPlayer player, final BlockPos host,
                            final String src, final String destDir) {
        if (!(level.getBlockEntity(host) instanceof IOsHost computer)) {
            return;
        }
        final FileCopyJobs.Copy copy = CopyPlans.plan(level, computer, CopyProgressPayload.COPY, src, destDir);
        FileCopyJobs.start(level, host, player, copy, CopyTiming.ticks(copy.sizeMb(), copy.mbPerSecond()),
                () -> copyNow(level, host, src, destDir));
    }

    /**
     * The copy carried out: a projected file has no bytes and is refused by the read; a name already taken gets a
     * numbered copy rather than overwriting.
     */
    private static void copyNow(final ServerLevel level, final BlockPos host, final String src,
                                final String destDir) {
        if (!(level.getBlockEntity(host) instanceof IOsHost computer)) {
            return;
        }
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
        transfer(level, computer, src, destDir, true);
    }

    /**
     * Moves a file. Within one volume it is only renamed, at once; across to another it is copied there and taken
     * away here, which takes the time a copy between those two volumes does.
     */
    private static void handleMoveFile(final MoveFilePayload payload, final ServerPlayer player,
                                       final ServerLevel level) {
        move(level, player, payload.hostPos(), payload.srcPath(), payload.destDir());
    }

    /** Moves a file on the computer at {@code host}, for {@code player}, as a cut and paste in its window does. */
    public static void move(final ServerLevel level, final ServerPlayer player, final BlockPos host,
                            final String src, final String destDir) {
        if (!(level.getBlockEntity(host) instanceof IOsHost computer)) {
            return;
        }
        if (volumeKey(src).equals(volumeKey(destDir)) || src.startsWith(NET_ROOT) || destDir.startsWith(NET_ROOT)) {
            moveNow(level, host, src, destDir);
            return;
        }
        final FileCopyJobs.Copy move = CopyPlans.plan(level, computer, CopyProgressPayload.MOVE, src, destDir);
        FileCopyJobs.start(level, host, player, move, CopyTiming.ticks(move.sizeMb(), move.mbPerSecond()),
                () -> moveNow(level, host, src, destDir));
    }

    private static void moveNow(final ServerLevel level, final BlockPos host, final String src,
                                final String destDir) {
        if (!(level.getBlockEntity(host) instanceof IOsHost computer)) {
            return;
        }
        if (src.startsWith(NET_ROOT) || destDir.startsWith(NET_ROOT)) {
            // A file on another machine is copied, not moved: the copy is what the explorer offers.
            return;
        }
        if (volumeKey(src).equals(volumeKey(destDir))) {
            // Same volume, an in-place move.
            final ItemStack srcVol = volumeOf(level, computer, src);
            if (srcVol.isEmpty()) {
                return;
            }
            final boolean srcMedia = src.startsWith("media:");
            if (DiskFilesystem.move(srcVol, srcMedia ? mediaSubPath(src) : src,
                    destDir.startsWith("media:") ? mediaSubPath(destDir) : destDir, kindOf(computer, src))) {
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
         * not copied across volumes here. A name already taken at the destination is not moved over, because the
         * source is deleted after the write and the file written over would be lost.
         */
        if (transfer(level, computer, src, destDir, false) == null) {
            return;
        }
        final boolean srcMedia = src.startsWith("media:");
        DiskFilesystem.delete(volumeOf(level, computer, src), srcMedia ? mediaSubPath(src) : src);
        if (srcMedia) {
            commitMedia(level, computer, src);
        } else {
            computer.setChanged();
        }
    }

    /**
     * Writes the file at {@code src} into the folder {@code destDir}, which may be on another volume, and saves the
     * volume it was written to.
     *
     * @param uniqueName whether a name already taken gets a numbered copy ("name - Copy.ext", then
     *                   "name - Copy (2).ext") rather than being refused
     * @return the path written within its volume, or null when nothing was written: the file could not be read, the
     *         volume is not reachable, the name was taken, or the disk was full
     */
    @Nullable
    private static String transfer(final ServerLevel level, final IOsHost computer, final String src,
                                   final String destDir, final boolean uniqueName) {
        final ItemStack srcVol = volumeOf(level, computer, src);
        final ItemStack dstVol = volumeOf(level, computer, destDir);
        if (srcVol.isEmpty() || dstVol.isEmpty()) {
            return null;
        }
        final boolean srcMedia = src.startsWith("media:");
        final boolean dstMedia = destDir.startsWith("media:");
        final String realSrc = srcMedia ? mediaSubPath(src) : src;
        final String realDstDir = dstMedia ? mediaSubPath(destDir) : destDir;
        final Optional<String> read = DiskFilesystem.read(srcVol, realSrc);
        if (read.isEmpty()) {
            return null;
        }
        final String name = realSrc.substring(realSrc.lastIndexOf('/') + 1);
        final String destPath = uniqueName ? freeCopyPath(dstVol, realDstDir, name) : joined(realDstDir, name);
        if (destPath == null || !uniqueName && DiskFilesystem.exists(dstVol, destPath)) {
            return null;
        }
        final long free = dstMedia ? mediaFreeWeight(dstVol) : computer.systemDiskFreeWeight();
        if (DiskFilesystem.write(dstVol, destPath, FileType.ofPath(name), read.get(), free,
                kindOf(computer, destDir), level.getGameTime()) != DiskFilesystem.WriteResult.OK) {
            return null;
        }
        if (dstMedia) {
            commitMedia(level, computer, destDir);
        } else {
            computer.setChanged();
        }
        return destPath;
    }

    /**
     * The first path free in {@code dir}: the name itself, then "name - Copy.ext", then "name - Copy (2).ext" and on,
     * the way a desktop names a duplicate. Every candidate is tested before it is used, and null comes back when
     * none of them is free, so a copy never writes over a file.
     */
    @Nullable
    private static String freeCopyPath(final ItemStack volume, final String dir, final String name) {
        final int dot = name.lastIndexOf('.');
        final String stem = dot > 0 ? name.substring(0, dot) : name;
        final String suffix = dot > 0 ? name.substring(dot) : "";
        String candidate = name;
        for (int n = 0; n < MAX_COPY_NAMES; n++) {
            final String path = joined(dir, candidate);
            if (!DiskFilesystem.exists(volume, path)) {
                return path;
            }
            candidate = stem + (n == 0 ? " - Copy" : " - Copy (" + (n + 1) + ")") + suffix;
        }
        return null;
    }

    private static String joined(final String dir, final String name) {
        return dir.isEmpty() ? name : dir + "/" + name;
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
