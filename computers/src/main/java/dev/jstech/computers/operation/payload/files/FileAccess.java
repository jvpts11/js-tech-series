/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.files;

import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/**
 * The drives a computer's files live on (its system disk, a medium in a linked reader, a share on the network) and
 * the paths that name them.
 */
public final class FileAccess {

    /** The explorer's key for the network: the other machines' shares, under their host names. */
    static final String NET_ROOT = "net:";

    private FileAccess() {
    }

    /**
     * The shell of the machine the explorer is on, which is how another machine's shared folder is
     * reached: the path the explorer holds is handed to it in the shell's own spelling. Null on a
     * machine no shell can run on.
     */
    @org.jetbrains.annotations.Nullable
    static dev.jstech.computers.program.ServerCliComputer netShell(
            final ServerLevel level, final dev.jstech.computers.os.IOsHost computer) {
        if (computer instanceof dev.jstech.computers.terminal.IComputerTerminalHost terminal) {
            return new dev.jstech.computers.program.ServerCliComputer(terminal, level);
        }
        return null;
    }

    /** An explorer path on the network ({@code net:host/share/rest}) as the shell writes it ({@code \\host\share\rest}). */
    static String netDos(final String path) {
        return "\\\\" + path.substring(NET_ROOT.length()).replace('/', '\\');
    }

    /** An explorer path on the system disk as the shell writes it. */
    static String localDos(final String path) {
        return "C:\\" + path.replace('/', '\\');
    }

    /**
     * Resolves a {@code media:<readerPos>[/sub]} path to the medium's {@link net.minecraft.world.item.ItemStack}
     * in a linked drive, or {@link net.minecraft.world.item.ItemStack#EMPTY} if not reachable.
     */
    public static net.minecraft.world.item.ItemStack mediaStackFor(final ServerLevel level,
            final dev.jstech.computers.os.IOsHost computer,
            final String mediaPath) {
        final String rest = mediaPath.substring("media:".length());
        final int slash = rest.indexOf('/');
        final long readerPos;
        try {
            readerPos = Long.parseLong(slash < 0 ? rest : rest.substring(0, slash));
        } catch (final NumberFormatException e) {
            return net.minecraft.world.item.ItemStack.EMPTY;
        }
        if (!computer.linkedEndpoints().contains(readerPos)
                || !(level.getBlockEntity(net.minecraft.core.BlockPos.of(readerPos))
                        instanceof dev.jstech.computers.os.media
                                .MediaReaderBlockEntity reader)) {
            return net.minecraft.world.item.ItemStack.EMPTY;
        }
        return reader.mediaSlot().getStackInSlot(0);
    }

    /** Strips the {@code media:<readerPos>/} prefix from a media path, leaving the path within the medium. */
    static String mediaSubPath(final String mediaPath) {
        final String rest = mediaPath.substring("media:".length());
        final int slash = rest.indexOf('/');
        return slash < 0 ? "" : rest.substring(slash + 1);
    }

    /** Re-syncs the reader holding {@code media:<readerPos>} after its medium's filesystem changed. */
    static void commitMedia(final ServerLevel level,
            final dev.jstech.computers.os.IOsHost computer,
            final String mediaPath) {
        final String rest = mediaPath.substring("media:".length());
        final int slash = rest.indexOf('/');
        final long readerPos;
        try {
            readerPos = Long.parseLong(slash < 0 ? rest : rest.substring(0, slash));
        } catch (final NumberFormatException e) {
            return;
        }
        if (level.getBlockEntity(net.minecraft.core.BlockPos.of(readerPos))
                instanceof dev.jstech.computers.os.media.MediaReaderBlockEntity reader) {
            reader.setChanged();
            level.sendBlockUpdated(net.minecraft.core.BlockPos.of(readerPos),
                    reader.getBlockState(), reader.getBlockState(), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
        }
    }

    /** Free space on a medium in mB-equivalents (capacity minus its stored files). */
    static long mediaFreeWeight(final net.minecraft.world.item.ItemStack media) {
        final long cap = media.getItem()
                instanceof dev.jstech.computers.os.media.FormattedMediaItem fm
                ? fm.format().capacityItems() : 64L;
        final long capWeight = cap
                * dev.jstech.computers.storage.StorageKey.MB_EQ_PER_ITEM;
        final long fsUsed = dev.jstech.computers.os.fs.DiskFilesystem.filesWeight(media);
        /*
         * A DATA medium can also hold a stored item/fluid snapshot (MEDIA_DATA); both consume the medium's
         * capacity, so deduct both, mirroring IOsHost.systemDiskFreeWeight (stored items +
         * FILESYSTEM). Ignoring MEDIA_DATA let the player write files past the medium's real capacity.
         */
        final long dataUsed = media.getOrDefault(
                        dev.jstech.computers.ComputingModule.MEDIA_DATA.get(),
                        dev.jstech.computers.storage.ServerStorageContents.EMPTY)
                .usedWeight();
        return Math.max(0L, capWeight - fsUsed - dataUsed);
    }

    /** Resolves the filesystem kind of the computer's installed OS, or NONE when absent. */
    public static dev.jstech.computers.os.FilesystemKind filesystemKindOf(
            final dev.jstech.computers.os.IOsHost computer) {
        final dev.jstech.computers.os.OsDef os = computer.installedOs();
        if (os == null) {
            return dev.jstech.computers.os.FilesystemKind.NONE;
        }
        final dev.jstech.computers.os.KernelDef kernel =
                dev.jstech.computers.os.OsRegistry.getKernel(os.kernelId());
        return kernel != null ? kernel.filesystem()
                : dev.jstech.computers.os.FilesystemKind.NONE;
    }

    /**
     * Moves the stored quantity of {@code key} from a computer's local storage onto a DATA {@code media} stack,
     * bounded by the medium's free capacity. Conservative: it extracts first and inserts only what was
     * extracted, capped by what fits, so the sum across the two stores is invariant, and nothing is created or
     * destroyed. Returns the number of native units actually moved.
     *
     * <p>Exposed so it can be exercised directly by a GameTest with real component stacks, without a
     * player/menu round-trip.
     */
    public static long transferDatToMedium(final IComputerTerminalHost host, final StorageKey key,
                                           final ItemStack media) {
        final long stored = host.localStore().count(key);
        if (stored <= 0L) {
            return 0L;
        }
        // How many native units fit in the medium's remaining capacity (weight budget / per-unit weight).
        final long unitWeight = Math.max(1L, key.weight(1L));
        final long roomUnits = mediaFreeWeight(media) / unitWeight;
        if (roomUnits <= 0L) {
            return 0L;
        }
        final long toMove = Math.min(stored, roomUnits);
        // Extract first; only what was actually extracted is ever inserted, so the two halves stay balanced.
        final long extracted = host.localStore().extract(key, toMove);
        if (extracted <= 0L) {
            return 0L;
        }
        try {
            final java.util.Map<StorageKey, Long> next = new java.util.LinkedHashMap<>(
                    dev.jstech.computers.os.media.MediaItem.data(media).items());
            next.merge(key, extracted, Long::sum);
            dev.jstech.computers.os.media.MediaItem.setData(media,
                    new dev.jstech.computers.storage.ServerStorageContents(next));
        } catch (final RuntimeException e) {
            /*
             * The medium write failed after the items already left local storage; put them back so the
             * exceptional path still conserves items (nothing lost), then rethrow.
             */
            host.localStore().insert(key, extracted);
            throw e;
        }
        return extracted;
    }

    /**
     * Resolves a {@code .dat} path back to the {@link StorageKey} it projects, by re-running the deterministic
     * {@link dev.jstech.computers.os.fs.StorageProjection} over the disk's storage volume
     * and matching the requested path. Returns {@code null} when no projected entry matches (e.g. a stale path).
     */
    @org.jetbrains.annotations.Nullable
    public static StorageKey resolveDatKey(final net.minecraft.world.item.ItemStack disk, final String datPath) {
        if (disk.isEmpty()) {
            return null;
        }
        final dev.jstech.computers.storage.ServerStorageContents storage =
                dev.jstech.computers.storage.DriveVolumes.contents(disk);
        /*
         * The projection emits one entry per key in iteration order, with the same path each time; pair each
         * emitted path with the storage key at the same position to invert the path back to its key.
         */
        final java.util.List<dev.jstech.computers.os.fs.DiskFilesystem.FileEntry> entries =
                dev.jstech.computers.os.fs.StorageProjection.project(storage);
        final java.util.Iterator<StorageKey> keys = storage.items().keySet().iterator();
        for (final var entry : entries) {
            final StorageKey key = keys.hasNext() ? keys.next() : null;
            if (key != null && entry.path().equals(datPath)) {
                return key;
            }
        }
        return null;
    }

    /** The volume identity of a path: {@code ""} for the system disk, or the reader pos for a {@code media:} path. */
    static String volumeKey(final String path) {
        if (!path.startsWith("media:")) {
            return "";
        }
        final String rest = path.substring("media:".length());
        final int slash = rest.indexOf('/');
        return slash < 0 ? rest : rest.substring(0, slash);
    }
}
