/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.item.DiskItem;
import dev.jstech.computers.os.boot.SystemIntegrity;
import dev.jstech.computers.os.boot.SystemWelcome;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.FileType;
import dev.jstech.computers.os.fs.FilesystemContents;
import dev.jstech.computers.os.fs.SystemLayout;
import dev.jstech.computers.program.ComputerConsoleState;
import dev.jstech.computers.storage.DriveVolumes;
import dev.jstech.computers.storage.StorageKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.IntFunction;
import java.util.function.ObjIntConsumer;
import org.jetbrains.annotations.Nullable;

/**
 * The disk-set logic every OS-hosting machine shares: which disk boots, where an install lands,
 * whether a footprint fits, and what a format erases. Extracted so a desk computer (disks in its
 * hardware inventory) and a rack server (drives in the rack's front-panel bays) run the exact same
 * rules over different physical slots: the callers supply slot accessors, this class supplies the
 * behavior.
 */
public final class OsDisks {

    /** Answered by the slot search when a disk already carries the system and nothing has to be written. */
    private static final int ALREADY_THERE = -2;

    private OsDisks() {
    }

    /** The systems that disk carries; a disk nobody has installed anything onto carries none. */
    public static DiskSystems systemsOn(final ItemStack disk) {
        return disk.getOrDefault(ComputingModule.DISK_SYSTEMS.get(), DiskSystems.NONE);
    }

    /**
     * The system that disk boots, or nothing when it carries none the machines know.
     *
     * <p>What almost every caller wants: a disk may carry several now, and the one it boots is the one that
     * answers for it everywhere a single system used to.
     */
    @Nullable
    public static ResourceLocation systemOn(final ItemStack disk) {
        final ResourceLocation osId = systemsOn(disk).boots();
        return osId != null && OsRegistry.getOs(osId) != null ? osId : null;
    }

    /** Whether the stack is a disk carrying at least one registered operating system. */
    public static boolean hasSystem(final ItemStack disk) {
        for (final ResourceLocation osId : systemsOn(disk).ids()) {
            if (OsRegistry.getOs(osId) != null) {
                return true;
            }
        }
        return false;
    }

    /** What the system that disk boots remembers about being greeted; a disk with no mark has met nobody. */
    public static SystemWelcome welcomeOn(final ItemStack disk) {
        return welcomeOn(disk, systemsOn(disk).boots());
    }

    /** What that system on that disk remembers about being greeted, since each of them remembers its own. */
    public static SystemWelcome welcomeOn(final ItemStack disk, @Nullable final ResourceLocation osId) {
        return systemsOn(disk).welcomeOf(osId);
    }

    /** The same disk with that system's mark written back onto it. */
    public static ItemStack remembering(final ItemStack disk, @Nullable final ResourceLocation osId,
                                        final SystemWelcome welcome) {
        final ItemStack updated = disk.copy();
        updated.set(ComputingModule.DISK_SYSTEMS.get(), systemsOn(disk).remembering(osId, welcome));
        return updated;
    }

    /**
     * The slot the machine boots from, or {@code -1} when nothing on it carries a system.
     *
     * <p>The same search {@link #systemDisk} makes, answering where rather than what, for the callers that have
     * to write something back onto that disk.
     */
    public static int systemDiskSlot(final int diskCount, final IntFunction<ItemStack> diskInSlot,
                                     final int preferredSlot) {
        if (preferredSlot >= 0 && preferredSlot < diskCount) {
            final ItemStack preferred = diskInSlot.apply(preferredSlot);
            if (preferred.getItem() instanceof DiskItem && hasSystem(preferred)) {
                return preferredSlot;
            }
        }
        for (int i = 0; i < diskCount; i++) {
            final ItemStack stack = diskInSlot.apply(i);
            if (stack.getItem() instanceof DiskItem && hasSystem(stack)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * The disk that boots: the preferred slot when it holds a system, else the first disk with a
     * system, else EMPTY, so a machine with two installed OSes dual-boots by choice.
     */
    public static ItemStack systemDisk(final int diskCount, final IntFunction<ItemStack> diskInSlot,
                                       final int preferredSlot) {
        if (preferredSlot >= 0 && preferredSlot < diskCount) {
            final ItemStack preferred = diskInSlot.apply(preferredSlot);
            if (preferred.getItem() instanceof DiskItem && hasSystem(preferred)) {
                return preferred;
            }
        }
        for (int i = 0; i < diskCount; i++) {
            final ItemStack stack = diskInSlot.apply(i);
            if (stack.getItem() instanceof DiskItem && hasSystem(stack)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * The slot the firmware installs onto by default: the first disk without a system (a second OS
     * lands beside the first for dual boot), else the first disk; {@code -1} with no disk at all.
     */
    public static int defaultInstallSlot(final int diskCount, final IntFunction<ItemStack> diskInSlot) {
        int firstDisk = -1;
        for (int i = 0; i < diskCount; i++) {
            final ItemStack stack = diskInSlot.apply(i);
            if (!(stack.getItem() instanceof DiskItem)) {
                continue;
            }
            if (!hasSystem(stack)) {
                return i;
            }
            if (firstDisk == -1) {
                firstDisk = i;
            }
        }
        return firstDisk;
    }

    /**
     * Installs {@code osId} onto {@code preferredSlot}, or ({@code -1}) onto the default target: a
     * slot already carrying this OS (an idempotent re-install), else the first disk without a
     * system, else the first disk. The OS footprint must fit the chosen disk's free weight
     * (capacity − stored items − files). On success the stamped disk is written back through
     * {@code setDiskInSlot} so the owner's change hooks fire.
     */
    /**
     * Whether that system can go onto that disk at all: the same question {@link #installOs} answers on its way
     * to writing, asked on its own so a machine can refuse before it spends a minute copying rather than after.
     *
     * <p>A disk already carrying the system answers yes, since putting it there again is nothing to do.
     */
    public static boolean roomFor(final int diskCount, final IntFunction<ItemStack> diskInSlot,
                                  final ResourceLocation osId, final int preferredSlot) {
        final OsDef def = OsRegistry.getOs(osId);
        if (def == null) {
            return false;
        }
        final int targetSlot = targetFor(diskCount, diskInSlot, osId, preferredSlot);
        if (targetSlot == ALREADY_THERE) {
            return true;
        }
        if (targetSlot < 0) {
            return false;
        }
        final ItemStack disk = diskInSlot.apply(targetSlot);
        if (systemsOn(disk).has(osId)) {
            return true;
        }
        final DiskItem target = (DiskItem) disk.getItem();
        final long freeWeight = target.spec().capacityItems() * StorageKey.MB_EQ_PER_ITEM
                - DriveVolumes.usedWeight(disk) - DiskFilesystem.filesWeight(disk);
        return def.footprintItemsOn(target.spec().era()) * StorageKey.MB_EQ_PER_ITEM <= freeWeight;
    }

    /**
     * The slot that system would go onto: the one asked for when it holds a disk, else a disk already carrying
     * it ({@link #ALREADY_THERE}), else the default target, and {@code -1} when there is no disk at all.
     */
    private static int targetFor(final int diskCount, final IntFunction<ItemStack> diskInSlot,
                                 final ResourceLocation osId, final int preferredSlot) {
        if (preferredSlot >= 0 && preferredSlot < diskCount
                && diskInSlot.apply(preferredSlot).getItem() instanceof DiskItem) {
            return preferredSlot;
        }
        for (int i = 0; i < diskCount; i++) {
            final ItemStack stack = diskInSlot.apply(i);
            if (stack.getItem() instanceof DiskItem && systemsOn(stack).has(osId)) {
                return ALREADY_THERE;
            }
        }
        return defaultInstallSlot(diskCount, diskInSlot);
    }

    public static boolean installOs(final int diskCount, final IntFunction<ItemStack> diskInSlot,
                                    final ObjIntConsumer<ItemStack> setDiskInSlot,
                                    final ResourceLocation osId, final int preferredSlot) {
        final OsDef def = OsRegistry.getOs(osId);
        if (def == null) {
            return false;
        }
        if (!roomFor(diskCount, diskInSlot, osId, preferredSlot)) {
            return false;
        }
        final int targetSlot = targetFor(diskCount, diskInSlot, osId, preferredSlot);
        if (targetSlot == ALREADY_THERE) {
            /*
             * A disk already carries it, so there is nothing to add; what there may be is something to put
             * back. Installing over a system whose files have been deleted is how a wrecked machine is
             * repaired, and a repair that did nothing because the disk still remembered the system would be
             * no repair at all.
             */
            for (int slot = 0; slot < diskCount; slot++) {
                final ItemStack carrying = diskInSlot.apply(slot);
                if (systemsOn(carrying).has(osId)) {
                    final ItemStack repaired = carrying.copy();
                    writeLoader(repaired, def);
                    setDiskInSlot.accept(repaired, slot);
                    break;
                }
            }
            return true;
        }
        final ItemStack disk = diskInSlot.apply(targetSlot);
        /*
         * Installed beside whatever the disk already carries rather than over it, and booting by default, which
         * is what a machine does the moment you finish installing something on it. A disk that carried a system
         * used to simply lose it here, with nothing anywhere saying so.
         */
        final ItemStack updated = disk.copy();
        updated.set(ComputingModule.DISK_SYSTEMS.get(), systemsOn(disk).with(osId));
        /*
         * A graphical desktop OS lays down the Windows-like system folder skeleton on first install
         * (Program Files, Windows, Users\Public\Desktop, ...). Terminal/network OSes get nothing.
         */
        final List<String> systemDirs = SystemLayout.directoriesFor(def, OsRegistry.getKernel(def.kernelId()));
        if (!systemDirs.isEmpty()) {
            FilesystemContents fs = updated.getOrDefault(
                    ComputingModule.FILESYSTEM.get(), FilesystemContents.EMPTY);
            for (final String d : systemDirs) {
                fs = fs.withDir(d);
            }
            updated.set(ComputingModule.FILESYSTEM.get(), fs);
        }
        writeLoader(updated, def);
        setDiskInSlot.accept(updated, targetSlot);
        return true;
    }

    /**
     * Writes the one file that starts the system, and the folder it sits in.
     *
     * <p>A system is a real thing on a real disk here, which is what makes deleting it mean something: a
     * machine whose loader is gone finds a system and will not start it, and one whose folder is gone finds
     * nothing at all. Installing again writes this back, which is how such a machine is repaired.
     */
    private static void writeLoader(final ItemStack disk, final OsDef def) {
        final String loader = SystemIntegrity.loaderOf(def);
        final String folder = SystemIntegrity.folderOf(def);
        if (loader.isEmpty()) {
            return;
        }
        FilesystemContents fs = disk.getOrDefault(ComputingModule.FILESYSTEM.get(), FilesystemContents.EMPTY);
        if (!folder.isEmpty()) {
            fs = fs.withDir(folder);
        }
        disk.set(ComputingModule.FILESYSTEM.get(), fs);
        /*
         * Written in the kind of filesystem the system's own kernel gives it, not in the one every system
         * used to have. A flat disk keeps its loader at the root because it has no folder to keep it in.
         */
        final KernelDef kernel = OsRegistry.getKernel(def.kernelId());
        DiskFilesystem.write(disk, loader, FileType.SYS, def.displayName() + " loader",
                Long.MAX_VALUE,
                kernel != null ? kernel.filesystem() : FilesystemKind.HIERARCHICAL);
    }

    /**
     * Formats the disk in {@code slot}: erases the installed system, every file, the item storage
     * and the privacy split, leaving a blank disk written back through {@code setDiskInSlot}.
     * Returns whether the erased disk carried a system (so the caller can wipe its software state).
     */
    public static FormatResult formatDisk(final int diskCount, final IntFunction<ItemStack> diskInSlot,
                                          final ObjIntConsumer<ItemStack> setDiskInSlot, final int slot) {
        if (slot < 0 || slot >= diskCount) {
            return FormatResult.NOTHING;
        }
        final ItemStack disk = diskInSlot.apply(slot);
        if (!(disk.getItem() instanceof DiskItem)) {
            return FormatResult.NOTHING;
        }
        final boolean hadSystem = hasSystem(disk);
        final ItemStack updated = disk.copy();
        /*
         * Every system on it, and with them every greeting each of them remembered: installing again on this
         * disk is a first meeting again, which is what a format means.
         */
        updated.remove(ComputingModule.DISK_SYSTEMS.get());
        updated.remove(ComputingModule.FILESYSTEM.get());
        DriveVolumes.erase(updated);
        updated.remove(ComputingModule.DISK_PUBLIC_PERMILLE.get());
        setDiskInSlot.accept(updated, slot);
        return hadSystem ? FormatResult.ERASED_SYSTEM : FormatResult.ERASED_DATA;
    }

    /**
     * The desktop environment a machine boots into: the OS's bundled one (the Frames editions), else
     * the first desktop-environment package installed on it (a Linux distribution after its desktop
     * package went in), else null (a TTY-only or network OS).
     */
    @Nullable
    public static ResourceLocation installedDesktopId(
            @Nullable final OsDef os,
            @Nullable
            final ComputerConsoleState console) {
        if (os == null) {
            return null;
        }
        if (os.bundledDesktop().isPresent()) {
            return os.bundledDesktop().get();
        }
        if (console == null) {
            return null;
        }
        for (final String id : console.installed()) {
            final ResourceLocation rl = ResourceLocation.tryParse(id);
            final ProgramSpec spec = rl == null ? null : OsRegistry.getProgram(rl);
            if (spec != null && spec.kind() == ProgramKind.DESKTOP_ENVIRONMENT
                    && OsRegistry.getDesktop(rl) != null) {
                return rl;
            }
        }
        return null;
    }

    /**
     * The operating space a network machine draws with, or null when it has none and is a bare prompt.
     *
     * <p>What a desktop environment is to a Linux, for the system that has no desktop: the space is a package,
     * so the machine draws whatever is installed on it and falls back to its prompt when nothing is. That is
     * why what a monitor opens is asked of this and not of {@link OsCapability}: a network system is capable of
     * a screen, and whether it has one to show is a thing about the machine rather than about the system.
     */
    @Nullable
    public static ResourceLocation installedSpaceId(
            @Nullable final OsDef os,
            @Nullable final ComputerConsoleState console) {
        if (os == null || os.capability() != OsCapability.NETWORK_GUI || console == null) {
            return null;
        }
        for (final String id : console.installed()) {
            final ResourceLocation rl = ResourceLocation.tryParse(id);
            final ProgramSpec spec = rl == null ? null : OsRegistry.getProgram(rl);
            if (spec != null && spec.kind() == ProgramKind.OPERATING_SPACE
                    && OsRegistry.getSpace(rl) != null) {
                return rl;
            }
        }
        return null;
    }

    /**
     * Puts the space a network system ships with onto the machine, which is what its install pays for.
     *
     * <p>Every other kind of system either bundles its interface with itself (the Frames editions) or leaves
     * the player to fetch one (the distributions). A network system does neither: it arrives with a space on
     * and the space can be taken off afterwards, so the install writes it and nothing else does. A machine
     * that already has one keeps it, so installing the system again over a space somebody chose does not
     * quietly swap it for ours.
     *
     * <p>Which space is the system's own is read off the register rather than named here, so a network system
     * an addon brings arrives with the addon's space by the same rule.
     *
     * @return whether a space was put on, so a caller only writes the machine back when something changed
     */
    public static boolean installBundledSpace(@Nullable final OsDef os,
                                              @Nullable final ComputerConsoleState console) {
        if (os == null || os.capability() != OsCapability.NETWORK_GUI || console == null
                || installedSpaceId(os, console) != null) {
            return false;
        }
        for (final ProgramSpec spec : OsRegistry.programs()) {
            if (spec.kind() == ProgramKind.OPERATING_SPACE
                    && spec.platforms().contains(os.platform())
                    && OsRegistry.getSpace(spec.id()) != null) {
                console.install(spec.id().toString());
                console.setInstalledVersion(spec.id().toString(), ProgramVersions.of(spec.id().toString()));
                return true;
            }
        }
        return false;
    }

    /**
     * Free weight in mB-equivalents available on a system disk for user files: the disk capacity
     * minus the stored items, the existing files, and the installed OS footprint.
     */
    public static long systemDiskFreeWeight(final ItemStack disk) {
        if (!(disk.getItem() instanceof DiskItem diskItem)) {
            return 0L;
        }
        final long capacity = diskItem.spec().capacityItems() * StorageKey.MB_EQ_PER_ITEM;
        final long storageUsed = DriveVolumes.usedWeight(disk);
        final long fsUsed = DiskFilesystem.filesWeight(disk);
        /*
         * Every system on the disk takes its own room. A disk carrying two used to be charged for one of them,
         * so it read as having space it did not have and a third install could be accepted onto nothing.
         */
        long osReserved = 0L;
        for (final ResourceLocation osId : systemsOn(disk).ids()) {
            final OsDef os = OsRegistry.getOs(osId);
            if (os != null) {
                osReserved += os.footprintItemsOn(diskItem.spec().era()) * StorageKey.MB_EQ_PER_ITEM;
            }
        }
        return Math.max(0L, capacity - storageUsed - fsUsed - osReserved);
    }

    /** What a format actually erased. */
    public enum FormatResult {
        /** No disk in the slot; nothing changed. */
        NOTHING,
        /** A data disk was blanked. */
        ERASED_DATA,
        /** The blanked disk carried a system, so the machine's software state died with it. */
        ERASED_SYSTEM;

        public boolean formatted() {
            return this != NOTHING;
        }
    }
}
