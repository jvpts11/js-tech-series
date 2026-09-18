/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.item.DiskItem;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.OpenWindow;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsDisks;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.boot.SystemWelcome;
import dev.jstech.computers.os.install.InstallerFlow;
import dev.jstech.computers.os.install.Installers;
import dev.jstech.computers.os.install.OsInstallJob;
import dev.jstech.computers.os.media.MediaKind;
import dev.jstech.computers.os.media.MediaReaderBlockEntity;
import dev.jstech.computers.program.ComputerConsoleState;
import dev.jstech.computers.program.install.LiveInstallState;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.tier.HardwareEra;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The system a computer is running and the session it is running it in: which disk it boots, which
 * system is on that disk, which desktop this session came up in, and which windows are open on it.
 *
 * <p>The session lives on the machine rather than in any client, so whoever opens the monitor next
 * finds what the last person left, and it survives the game being closed.
 */
final class OsSession {

    private final AbstractComputerBlockEntity machine;
    /*
     * The windows open on this machine's desktop. Kept here, not in the client, so they belong to the
     * machine: whoever opens the monitor next sees them, and they survive the game being closed.
     */
    private final List<OpenWindow> openWindows = new ArrayList<>();
    /*
     * A guided installer that finished writing the system but has not rebooted yet. Persisted: the
     * machine is still in the installer after a reload, the same way it keeps its booted desktop.
     */
    private int pendingInstall = IOsHost.NO_PENDING_INSTALL;
    /*
     * A system being copied onto a disk right now. Persisted: the copy belongs to the machine, so it carries on
     * where it was after a reload, and closing the monitor is not what decides whether it happened.
     */
    @Nullable
    private OsInstallJob installing;
    /*
     * The installer the machine is in: which page it is on and what has been answered. Persisted with the copy
     * it belongs to, so leaving the monitor and coming back finds the same page with the same answers.
     */
    @Nullable
    private InstallerFlow installer;
    /*
     * The answers read back from the save, waiting for a level to build the installer from. A machine is loaded
     * before it has one, and the list of disks and of desktops has to be read off the world.
     */
    @Nullable
    private CompoundTag installerMemo;
    /*
     * The desktop this session booted into. Held apart from what is on disk so that installing or
     * removing a desktop package takes effect on the next boot, not the next time the monitor is opened.
     */
    @Nullable
    private ResourceLocation bootedDesktop;
    /* The firmware's preferred boot disk slot (-1 = the first disk with a system). Persisted, so dual boot sticks. */
    private int bootDisk = -1;
    /*
     * A disk picked from the one-time boot menu, for this boot and no other. Deliberately not persisted and
     * dropped with the session: "this boot only" means exactly that, and the order saved in setup is untouched.
     */
    private int bootOnce = -1;
    /*
     * And which system on that disk, for a disk carrying several. "This boot only" means this system this time;
     * the one the disk boots by default is written on the disk and is not touched by choosing here.
     */
    @Nullable
    private ResourceLocation bootOnceOs;

    OsSession(final AbstractComputerBlockEntity machine) {
        this.machine = machine;
    }

    int pendingInstallSlot() {
        return this.pendingInstall;
    }

    void setPendingInstallSlot(final int slot) {
        this.pendingInstall = slot;
        this.machine.setChanged();
    }

    /** The system being copied onto a disk right now, or nothing. */
    @Nullable
    OsInstallJob installing() {
        return this.installing;
    }

    /** Starts, replaces or ends the copy this machine is doing. */
    void setInstalling(@Nullable final OsInstallJob job) {
        this.installing = job;
        this.machine.setChanged();
    }

    /**
     * The installer this machine is in, or nothing.
     *
     * <p>Built back from the answers the first time it is asked for after a reload, since the disks and the
     * desktops it offers are read off the world and the machine is loaded before it can reach one.
     */
    @Nullable
    InstallerFlow installer() {
        if (this.installer == null && this.installerMemo != null
                && this.machine.getLevel() instanceof ServerLevel level) {
            final CompoundTag memo = this.installerMemo;
            this.installerMemo = null;
            final OsDef system = OsRegistry.getOs(ResourceLocation.tryParse(memo.getString("Os")));
            if (system != null) {
                this.installer = Installers.restored(this.machine, level, system, memo.getInt("Copy"),
                        memo.getInt("Stage"), memo.getInt("Slot"), memo.getString("Name"),
                        memo.getString("Desktop"), memo.getInt("Erase"));
            }
        }
        return this.installer;
    }

    /** Puts the machine in an installer, or takes it out of one. */
    void setInstaller(@Nullable final InstallerFlow flow) {
        this.installer = flow;
        this.installerMemo = null;
        this.machine.setChanged();
    }

    @Nullable
    ResourceLocation bootedDesktopId() {
        return this.bootedDesktop;
    }

    void setBootedDesktopId(@Nullable final ResourceLocation id) {
        this.bootedDesktop = id;
        this.machine.setChanged();
    }

    List<OpenWindow> openWindows() {
        return List.copyOf(this.openWindows);
    }

    void setOpenWindows(final List<OpenWindow> windows) {
        this.openWindows.clear();
        for (final OpenWindow window : windows) {
            if (this.openWindows.size() >= OpenWindow.MAX) {
                break;
            }
            this.openWindows.add(window);
        }
        this.machine.setChanged();
    }

    /** What a cold start and a power cut leave of the session: no desktop, and no installer waiting. */
    void drop() {
        this.openWindows.clear();
        this.pendingInstall = IOsHost.NO_PENDING_INSTALL;
        // A copy dies with the power, as it would on any machine, and nothing of it reaches the disk.
        this.installing = null;
        this.installer = null;
        this.installerMemo = null;
        // "This boot only" ends with the boot it was for.
        this.bootOnce = -1;
        this.bootOnceOs = null;
    }

    /**
     * The disk this machine boots from: the one chosen in the firmware's boot order when it holds a system,
     * otherwise the first disk that has one, so a computer with two installed systems dual-boots by choice.
     */
    ItemStack systemDisk() {
        final int from = this.bootOnce >= 0 ? this.bootOnce : this.bootDisk;
        return OsDisks.systemDisk(this.machine.layout().diskCount(), this::diskInSlot, from);
    }

    /** The slot the machine boots from, or -1 when nothing in it carries a system. */
    private int systemDiskSlot() {
        final int from = this.bootOnce >= 0 ? this.bootOnce : this.bootDisk;
        return OsDisks.systemDiskSlot(this.machine.layout().diskCount(), this::diskInSlot, from);
    }

    /** What the system this machine boots remembers about being greeted. */
    SystemWelcome welcome() {
        final int slot = this.systemDiskSlot();
        return slot < 0 ? SystemWelcome.UNSEEN
                : OsDisks.welcomeOn(this.diskInSlot(slot));
    }

    /**
     * Writes that back onto the disk, against the system it belongs to.
     *
     * <p>Against that system and not against the disk: a disk carrying two systems remembers having met each of
     * them on its own, so installing a second one beside the first does not have it arrive already greeted.
     */
    void setWelcome(final SystemWelcome welcome) {
        final int slot = this.systemDiskSlot();
        if (slot < 0) {
            return;
        }
        final ItemStack disk = this.diskInSlot(slot);
        this.putDisk(OsDisks.remembering(disk, this.installedOsId(), welcome), slot);
    }

    /** Boots that system on that disk for this boot only, leaving what the disk boots by default alone. */
    void setBootOnce(final int slot, @Nullable final ResourceLocation osId) {
        this.bootOnce = slot;
        this.bootOnceOs = osId;
    }

    /** The disk slot the firmware boots first, or -1 for "the first disk with a system". */
    int bootDiskSlot() {
        return this.bootDisk;
    }

    void setBootDiskSlot(final int slot) {
        this.bootDisk = slot;
        this.machine.setChanged();
        this.machine.markBuildDirty();
    }

    /**
     * The desktop environment this computer boots into: the system's bundled one (the Frames editions), else
     * the first desktop-environment package installed on it (a Linux distribution after {@code apt install
     * gnome}), else null for a terminal-only or network system.
     */
    ResourceLocation installedDesktopId() {
        return OsDisks.installedDesktopId(installedOs(), this.machine.console());
    }

    boolean hasOs() {
        return !systemDisk().isEmpty();
    }

    /**
     * The stacks in this computer's disk slots, in slot order. Entries may be empty or hold non-disk items;
     * callers filter as needed (the "This PC" disk listing reads it).
     */
    List<ItemStack> diskStacks() {
        final int count = this.machine.layout().diskCount();
        final List<ItemStack> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            out.add(diskInSlot(i));
        }
        return out;
    }

    /** The disk stack in that 0-based disk slot, or EMPTY when the slot is out of range. */
    ItemStack diskInSlot(final int slot) {
        if (slot < 0 || slot >= this.machine.layout().diskCount()) {
            return ItemStack.EMPTY;
        }
        return this.machine.getHardware().getStackInSlot(this.machine.layout().diskStart() + slot);
    }

    /**
     * The system this machine boots, or null when no bootable disk is installed.
     *
     * <p>What the boot manager was told this time, when it was told anything, and otherwise what the disk boots
     * by default. A disk carries several systems now, so which of them is running is a question with an answer
     * that is not simply "the disk".
     */
    @Nullable
    ResourceLocation installedOsId() {
        final ItemStack disk = systemDisk();
        if (this.bootOnceOs != null && OsDisks.systemsOn(disk).has(this.bootOnceOs)) {
            return this.bootOnceOs;
        }
        return OsDisks.systemOn(disk);
    }

    /** The system on the boot disk, or null when there is no bootable disk or the registry has no entry. */
    @Nullable
    OsDef installedOs() {
        final ResourceLocation osId = installedOsId();
        return osId != null ? OsRegistry.getOs(osId) : null;
    }

    /**
     * What the installed system takes on its disk, in item-equivalents, or zero when there is none. It is
     * subtracted from the usable capacity, so the system competes for space with stored data.
     */
    long reservedByOs() {
        // One lookup of the system disk serves both the system and the era the system sits on.
        final ItemStack disk = systemDisk();
        /*
         * Every system on the disk, not only the one running: they are all really on it, and each takes its own
         * room away from what the disk can store.
         */
        long reserved = 0L;
        for (final ResourceLocation osId : OsDisks.systemsOn(disk).ids()) {
            final OsDef os = OsRegistry.getOs(osId);
            if (os != null) {
                reserved += os.footprintItemsOn(diskEra(disk));
            }
        }
        return reserved;
    }

    /**
     * Free weight in mB-equivalents on the system disk for a player's files: the disk's capacity less the
     * items stored, the files already there and the system's own footprint. Zero when no system disk is in.
     */
    long systemDiskFreeWeight() {
        return OsDisks.systemDiskFreeWeight(systemDisk());
    }

    /**
     * Free space on the system disk in real MB, for the program-install footprint gate: the free
     * mB-equivalent weight at what an item costs on that disk's era.
     */
    long systemDiskFreeMb() {
        final ItemStack disk = systemDisk();
        return OsDisks.systemDiskFreeWeight(disk) * diskEra(disk).mbPerItem() / StorageKey.MB_EQ_PER_ITEM;
    }

    /**
     * Installs {@code osId} onto disk slot {@code preferredSlot}, or (-1) onto the default target: a slot
     * already carrying this system (so a re-install is idempotent), else the first disk without one, else the
     * first disk. False when the system is unknown, no disk is present, or the footprint does not fit.
     */
    /** Whether that system could go on that disk, asked before a copy starts rather than after it ends. */
    boolean canTakeOs(final ResourceLocation osId, final int preferredSlot) {
        return OsDisks.roomFor(this.machine.layout().diskCount(), this::diskInSlot, osId, preferredSlot);
    }

    boolean installOs(final ResourceLocation osId, final int preferredSlot) {
        /*
         * Writing back through setStackInSlot makes onContentsChanged fire (setChanged + build
         * invalidation); the block update then pushes the new disk state to watching clients.
         */
        final boolean installed = OsDisks.installOs(
                this.machine.layout().diskCount(), this::diskInSlot, this::putDisk, osId, preferredSlot);
        if (installed) {
            tellClients();
        }
        return installed;
    }

    /**
     * Whether the session on screen still stands: an installer running from live medium needs that medium to
     * still be in a linked drive, and anything else needs a system on the boot disk.
     */
    boolean validateOsSession() {
        final ComputerConsoleState console = this.machine.console();
        final LiveInstallState live = console == null ? null : console.liveInstall();
        if (live != null) {
            if (hasLiveMediumFor(live.distro())) {
                return true;
            }
            console.clearLiveInstall();
            this.machine.setChanged();
        }
        return installedOsId() != null;
    }

    /**
     * Formats that disk slot: erases the installed system, every file, the item storage and the privacy split
     * on it, leaving a blank disk. The boot-order pointer is cleared when it pointed there. Whether a disk was
     * actually formatted comes back.
     */
    boolean formatDisk(final int slot) {
        // Write back through the handler so onContentsChanged fires (setChanged + build invalidation).
        final OsDisks.FormatResult result = OsDisks.formatDisk(
                this.machine.layout().diskCount(), this::diskInSlot, this::putDisk, slot);
        if (!result.formatted()) {
            return false;
        }
        if (this.bootDisk == slot) {
            this.bootDisk = -1;
        }
        if (result == OsDisks.FormatResult.ERASED_SYSTEM) {
            this.machine.onSystemErased();
        }
        this.machine.setChanged();
        return true;
    }

    /**
     * The disk slot the firmware installs onto by default: the first disk without a system, so a second
     * system lands beside the first for dual boot, else the first disk; -1 when no disk is installed.
     */
    int defaultInstallSlot() {
        return OsDisks.defaultInstallSlot(this.machine.layout().diskCount(), this::diskInSlot);
    }

    /** Takes the system off the boot disk. Nothing at all when no bootable disk is installed. */
    void uninstallOs() {
        for (int i = 0; i < this.machine.layout().diskCount(); i++) {
            final ItemStack stack = diskInSlot(i);
            if (!(stack.getItem() instanceof DiskItem) || OsDisks.systemOn(stack) == null) {
                continue;
            }
            /*
             * Clear the component whether or not the system's id is still registered: if an addon system was
             * installed and the addon later removed, the id is unknown but the player must still be able to
             * uninstall it, rather than pull the disk physically and risk losing the files on it.
             */
            final ItemStack updated = stack.copy();
            updated.remove(ComputingModule.DISK_SYSTEMS.get());
            putDisk(updated, i);
            tellClients();
            return;
        }
    }

    void save(final CompoundTag tag) {
        if (this.bootDisk >= 0) {
            tag.putInt("BootDisk", this.bootDisk);
        }
        /*
         * The running session survives a reload, exactly like the POST flag: a machine that was left up
         * with a desktop on screen must come back to that desktop, not fall to a shell.
         */
        if (this.bootedDesktop != null) {
            tag.putString("BootedDesktop", this.bootedDesktop.toString());
        }
        if (!this.openWindows.isEmpty()) {
            tag.put("OpenWindows", OpenWindow.saveAll(this.openWindows));
        }
        if (this.pendingInstall != IOsHost.NO_PENDING_INSTALL) {
            tag.putInt("PendingInstall", this.pendingInstall);
        }
        if (this.installing != null) {
            final CompoundTag copying = new CompoundTag();
            copying.putString("Os", this.installing.osId());
            copying.putInt("Slot", this.installing.targetSlot());
            copying.putLong("Reader", this.installing.readerPos());
            copying.putInt("Total", this.installing.ticksTotal());
            copying.putInt("Left", this.installing.ticksLeft());
            tag.put("Installing", copying);
        }
        if (this.installer != null) {
            final CompoundTag pages = new CompoundTag();
            pages.putString("Os", this.installer.systemId());
            pages.putInt("Copy", this.installer.copyTicks());
            pages.putInt("Stage", this.installer.stageIndex());
            pages.putInt("Slot", this.installer.targetSlot());
            pages.putString("Name", this.installer.computerName());
            pages.putString("Desktop", this.installer.desktopId());
            pages.putInt("Erase", this.installer.eraseSlot());
            tag.put("Installer", pages);
        } else if (this.installerMemo != null) {
            // Saved again without ever being asked for: a machine unloaded before anybody looked at it.
            tag.put("Installer", this.installerMemo);
        }
    }

    void load(final CompoundTag tag) {
        this.bootDisk = tag.contains("BootDisk") ? tag.getInt("BootDisk") : -1;
        this.bootedDesktop = tag.contains("BootedDesktop")
                ? ResourceLocation.tryParse(tag.getString("BootedDesktop")) : null;
        this.openWindows.clear();
        this.openWindows.addAll(OpenWindow.loadAll(tag.getList("OpenWindows", Tag.TAG_COMPOUND)));
        this.pendingInstall = tag.contains("PendingInstall")
                ? tag.getInt("PendingInstall") : IOsHost.NO_PENDING_INSTALL;
        if (tag.contains("Installing")) {
            final CompoundTag copying = tag.getCompound("Installing");
            this.installing = new OsInstallJob(copying.getString("Os"), copying.getInt("Slot"),
                    copying.getLong("Reader"), copying.getInt("Total"), copying.getInt("Left"));
        } else {
            this.installing = null;
        }
        this.installer = null;
        this.installerMemo = tag.contains("Installer") ? tag.getCompound("Installer") : null;
    }

    /** Puts a disk stack back in its slot through the handler, so the machine hears the change. */
    private void putDisk(final ItemStack stack, final int slot) {
        this.machine.getHardware().setStackInSlot(this.machine.layout().diskStart() + slot, stack);
    }

    /** The disks changed under the players watching this block, who are drawing what is in them. */
    private void tellClients() {
        final Level level = this.machine.getLevel();
        if (level == null) {
            return;
        }
        final BlockPos pos = this.machine.getBlockPos();
        level.sendBlockUpdated(pos, this.machine.getBlockState(), this.machine.getBlockState(),
                Block.UPDATE_CLIENTS);
    }

    /** Whether a linked drive still holds the live installer medium for that distribution. */
    private boolean hasLiveMediumFor(final LiveInstallState.Distro distro) {
        final Level level = this.machine.getLevel();
        if (level == null) {
            return true; // not resolvable right now; do not kill the session over a missing level
        }
        final String wanted = distro == LiveInstallState.Distro.ARCH ? "arch" : "gentoo";
        for (final long endpoint : this.machine.linkedEndpoints()) {
            if (level.getBlockEntity(BlockPos.of(endpoint)) instanceof MediaReaderBlockEntity reader
                    && reader.insertedKind() == MediaKind.OS_INSTALL
                    && reader.insertedPayload() != null
                    && wanted.equals(reader.insertedPayload().getPath())) {
                return true;
            }
        }
        return false;
    }

    /** The era a disk was made for, which sets what an item and a system image cost on it; standard for none. */
    private static HardwareEra diskEra(final ItemStack disk) {
        return disk.getItem() instanceof DiskItem item ? item.spec().era() : HardwareEra.STANDARD;
    }
}
