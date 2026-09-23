/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.os.OsDisks;
import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.item.DiskItem;
import dev.jstech.computers.os.FilesystemKind;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.KernelDef;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.FsPaths;
import dev.jstech.computers.os.fs.InstallerLayout;
import dev.jstech.computers.os.fs.ProgramFilesProjection;
import dev.jstech.computers.os.media.FormattedMediaItem;
import dev.jstech.computers.os.media.InstallerProjection;
import dev.jstech.computers.os.media.MediaReaderBlockEntity;
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.computers.storage.DriveVolumes;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
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

    /** The drives that block has as they are now; none when it is not a computer. */
    public static DriveTable of(final BlockEntity block, final ServerLevel level) {
        if (!(block instanceof IOsHost computer)) {
            return NONE;
        }
        final List<Drive> table = new ArrayList<>();
        final ItemStack system = computer.systemDisk();
        char letter = 'C';
        if (!system.isEmpty()) {
            final OsDef os = computer.installedOs();
            final KernelDef kernel = os != null ? OsRegistry.getKernel(os.kernelId()) : null;
            final FilesystemKind kind = kernel != null ? kernel.filesystem() : FilesystemKind.NONE;
            table.add(new Drive('C', system, kind, computer::setChanged));
            letter = 'D';
        }
        // Data disks: every disk slot holding a real disk other than the boot disk.
        for (int i = 0; i < computer.diskSlots() && letter <= 'Z'; i++) {
            final ItemStack disk = computer.diskInSlot(i);
            if (disk.isEmpty() || disk == system || !(disk.getItem() instanceof DiskItem)) {
                continue;
            }
            table.add(new Drive(letter, disk, FilesystemKind.HIERARCHICAL, computer::setChanged));
            letter++;
        }
        // Linked media readers, in ascending packed-position order for a stable letter assignment.
        final List<Long> readers = new ArrayList<>(computer.linkedEndpoints());
        Collections.sort(readers);
        for (final long pos : readers) {
            if (letter > 'Z') {
                break;
            }
            if (!(level.getBlockEntity(BlockPos.of(pos)) instanceof MediaReaderBlockEntity reader)) {
                continue;
            }
            final ItemStack media = reader.mediaSlot().getStackInSlot(0);
            table.add(new Drive(letter, media, FilesystemKind.HIERARCHICAL, () -> syncReader(reader)));
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

    /** Free space in mB-equivalents on a disk or medium: its capacity less the items, files and system it holds. */
    public static long freeWeightOf(final ItemStack stack) {
        final long capacityItems;
        if (stack.getItem() instanceof DiskItem diskItem) {
            capacityItems = diskItem.spec().capacityItems();
        } else if (stack.getItem() instanceof FormattedMediaItem mediaItem) {
            capacityItems = mediaItem.format().capacityItems();
        } else {
            return 0L;
        }
        final long capacity = capacityItems * StorageKey.MB_EQ_PER_ITEM;
        final long storageUsed = DriveVolumes.usedWeight(stack);
        final long fsUsed = DiskFilesystem.filesWeight(stack);
        final ResourceLocation osId = OsDisks.systemOn(stack);
        final OsDef os = osId != null ? OsRegistry.getOs(osId) : null;
        final long osReserved = os != null
                ? os.footprintItemsOn(DiskFilesystem.eraOf(stack)) * StorageKey.MB_EQ_PER_ITEM : 0L;
        return Math.max(0L, capacity - storageUsed - fsUsed - osReserved);
    }

    /** Pushes a block update so players see a medium whose filesystem was just changed. */
    private static void syncReader(final MediaReaderBlockEntity reader) {
        reader.setChanged();
        if (reader.getLevel() != null) {
            reader.getLevel().sendBlockUpdated(reader.getBlockPos(), reader.getBlockState(),
                    reader.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }
}
