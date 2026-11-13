/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.os.OsDisks;
import dev.jstech.computers.hardware.DiskSpec;
import dev.jstech.computers.os.FileSystemType;
import dev.jstech.computers.os.FilesystemKind;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.KernelDef;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.disk.DiskPartitions;
import dev.jstech.computers.os.disk.DiskSnapshots;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.FsPaths;
import dev.jstech.computers.os.fs.InstallerLayout;
import dev.jstech.computers.os.fs.ProgramFilesProjection;
import dev.jstech.computers.os.media.FormattedMediaItem;
import dev.jstech.computers.os.media.IMediaDrive;
import dev.jstech.computers.os.media.InstallerProjection;
import dev.jstech.computers.os.media.MachineMedia;
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.computers.registry.HardwareRegistry;
import dev.jstech.computers.storage.DriveVolumes;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * The drives a machine's shell and programs see, lettered the way the machine letters them: {@code C:} is the system
 * disk, then {@code D:}, {@code E:} and on are the other disks in slot order, then the media readers linked to the
 * machine in position order, so a letter stays with its drive. A reader with no medium in it keeps its letter as a
 * drive that is not ready.
 *
 * <p>A table is made for one operation and let go of: a disk can be pulled or a medium swapped between any two, and
 * making one costs a short list and a look at each linked reader.
 */
@TextHolder
public final class DriveTable {

    /**
     * One drive.
     *
     * @param drive  its letter
     * @param disk   the disk or medium in it, empty for a reader with nothing in it
     * @param kind   the filesystem it keeps
     * @param commit how a change to what it holds is kept, which for a reader includes showing it to players
     */
    public record Drive(char drive, ItemStack disk, FilesystemKind kind, Runnable commit) {
    }

    private static final DriveTable NONE = new DriveTable(List.of());

    private static final TextKey DRIVE_MISSING =
            TextKey.of("jsc.service.drive.missing", "%s:\\ The system cannot find the drive specified.");
    private static final TextKey NOT_READY =
            TextKey.of("jsc.service.drive.not_ready", "%s:\\ The device is not ready.");

    private final List<Drive> drives;

    private DriveTable(final List<Drive> drives) {
        this.drives = drives;
    }

    /**
     * The drives that block has as they are now; none when it is not a computer. Every partition is a volume of its own
     * (computers/docs/design/storage-devices.md): the system's is C:, then come the other partitions of its disk and of
     * the other disks, each in a file system the running system reads; one it cannot read gets no letter, as a Linux
     * partition gets none from Frames.
     */
    public static DriveTable of(final BlockEntity block, final ServerLevel level) {
        if (!(block instanceof IOsHost computer)) {
            return NONE;
        }
        final List<Drive> table = new ArrayList<>();
        final ItemStack system = computer.systemDisk();
        final OsDef os = computer.installedOs();
        // C: is the system's, with a system disk or without: a data disk is never read as the system.
        char letter = 'D';
        if (!system.isEmpty()) {
            final KernelDef kernel = os != null ? OsRegistry.kernelOf(os) : null;
            final FilesystemKind kind = kernel != null ? kernel.filesystem() : FilesystemKind.NONE;
            table.add(new Drive('C', system, kind, computer::setChanged));
        }
        // Then every other volume the running system reads: the system disk's other partitions, then the other disks'.
        for (int i = 0; i < computer.diskSlots() && letter <= 'Z'; i++) {
            final ItemStack disk = computer.diskInSlot(i);
            if (disk.isEmpty() || !DriveVolumes.isDisk(disk)) {
                continue;
            }
            for (final DiskPartitions.Volume volume : DiskPartitions.volumes(disk)) {
                if (letter > 'Z' || volume.stack() == system || !reads(os, computer.installedEra(), volume)) {
                    continue;
                }
                table.add(new Drive(letter, volume.stack(), FilesystemKind.HIERARCHICAL, () -> {
                    volume.commit();
                    computer.setChanged();
                }));
                letter++;
            }
        }
        // Then the removable drives, in the order the machine keeps them, so a letter stays with its drive.
        for (final IMediaDrive drive : MachineMedia.of(computer, level).drives()) {
            if (letter > 'Z') {
                break;
            }
            table.add(new Drive(letter, drive.medium(), FilesystemKind.HIERARCHICAL, drive::commit));
            letter++;
        }
        return new DriveTable(table);
    }

    /** Every drive, in letter order. */
    public List<Drive> all() {
        return this.drives;
    }

    /** The drive with that letter, whatever its case, or null when the machine has none. */
    @Nullable
    public Drive find(final char letter) {
        final char upper = Character.toUpperCase(letter);
        for (final Drive drive : this.drives) {
            if (drive.drive() == upper) {
                return drive;
            }
        }
        return null;
    }

    /**
     * Whether a path on that drive is its root or a folder: one the disk keeps, one an install disc shows, or, on the
     * system disk, the system's own folder or an installed program's.
     *
     * @param machine the machine whose system and programs the system disk shows, or null for none
     */
    public static boolean dirExists(@Nullable final IOsHost machine, final Drive drive, final String storagePath) {
        if (storagePath.isEmpty()) {
            return true;
        }
        if (drive.kind() != FilesystemKind.HIERARCHICAL) {
            return false;
        }
        final String parent = FsPaths.parentDir(storagePath);
        if (DiskFilesystem.listDirs(drive.disk(), parent, drive.kind()).contains(storagePath)) {
            return true;
        }
        // A folder on an install disc is projected, not stored, and can still be entered.
        for (final InstallerLayout.Entry e : InstallerProjection.list(drive.disk(), parent)) {
            if (e.directory() && e.path().equals(storagePath)) {
                return true;
            }
        }
        // And so is the system's own folder, and an installed program's.
        return machine != null && drive.drive() == 'C' && ProgramFilesProjection.isDir(machine, storagePath);
    }

    /** What a drive letter the machine does not have answers: the no-system message for C:, the DOS one otherwise. */
    public static ICliComputer.FsResult missing(final char drive) {
        if (Character.toUpperCase(drive) == 'C') {
            return ICliComputer.FsResult.noOs();
        }
        return ICliComputer.FsResult.fail(DRIVE_MISSING.with(String.valueOf(Character.toUpperCase(drive))));
    }

    /** What a drive with nothing in it answers: a media reader with no medium inserted. */
    public static ICliComputer.FsResult notReady(final char drive) {
        return ICliComputer.FsResult.fail(NOT_READY.with(String.valueOf(Character.toUpperCase(drive))));
    }

    /**
     * Free space in mB-equivalents on a volume or medium: its capacity (a partition's, or a whole disk's) less the
     * items, files and system it holds.
     */
    public static long freeWeightOf(final ItemStack stack) {
        final long capacity;
        if (HardwareRegistry.disk(stack) instanceof DiskSpec) {
            // Bad sectors take their share of the room, and a dead disk has none.
            capacity = DriveVolumes.usableCapacity(stack, DiskPartitions.capacityItems(stack))
                    * StorageKey.MB_EQ_PER_ITEM;
        } else if (stack.getItem() instanceof FormattedMediaItem mediaItem) {
            capacity = mediaItem.capacityWeight();
        } else {
            return 0L;
        }
        final long storageUsed = DriveVolumes.usedWeight(stack);
        final long fsUsed = DiskFilesystem.filesWeight(stack) + DiskSnapshots.filesWeight(stack);
        final ResourceLocation osId = OsDisks.systemOn(stack);
        final OsDef os = osId != null ? OsRegistry.getOs(osId) : null;
        final long osReserved = os != null
                ? (OsDisks.footprintItemsOn(stack, os, DiskFilesystem.eraOf(stack))
                + OsDisks.pageItemsOn(stack, osId, DiskFilesystem.eraOf(stack))) * StorageKey.MB_EQ_PER_ITEM : 0L;
        return Math.max(0L, capacity - storageUsed - fsUsed - osReserved);
    }

    /* Whether the running system reads that volume's file system; a machine with no system letters them all. */
    private static boolean reads(@Nullable final OsDef os, @Nullable final Era era,
                                 final DiskPartitions.Volume volume) {
        final FileSystemType fs = volume.partition() == null ? null : volume.partition().fs();
        return os == null || fs != null && fs.readBy(os.platform(), era);
    }
}
