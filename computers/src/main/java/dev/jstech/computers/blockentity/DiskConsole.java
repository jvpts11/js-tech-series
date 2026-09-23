/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import dev.jstech.computers.registry.ComputingComponents;
import dev.jstech.computers.program.ComputerConsoleState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The console a computer keeps: its history, where its shell sits, what is installed on it and what is
 * building. It rides on the system disk rather than on the machine, so pulling the drive takes the
 * software with it and putting it in another computer brings it along.
 */
final class DiskConsole {

    private final AbstractComputerBlockEntity machine;
    private final ComputerConsoleState console = new ComputerConsoleState();
    /*
     * The disk stack the console was read from, or null when nothing has been read yet. Identity, not
     * equality: a different stack object means a different physical drive, while writing to the same drive
     * (installing a system, adding a program) keeps the same object and must NOT discard what is held in
     * memory, because doing that resurrected a cleared live-install session from the older disk copy.
     */
    @Nullable
    private ItemStack disk;

    DiskConsole(final AbstractComputerBlockEntity machine) {
        this.machine = machine;
    }

    /** The console as it stands, read off the current system disk whenever the drive has changed. */
    ComputerConsoleState state() {
        final ItemStack current = this.machine.systemDisk();
        if (this.disk != current) {
            readFrom(current);
        }
        return this.console;
    }

    /**
     * Writes the console back onto the disk it came from.
     *
     * <p>Back onto THAT drive, not onto whatever is the system disk now: if a drive has just been swapped,
     * this console belongs to the old one and must not be copied onto the new.
     */
    void flush() {
        if (this.disk == null || this.disk.isEmpty()) {
            return;
        }
        final CompoundTag tag = new CompoundTag();
        this.console.save(tag);
        this.disk.set(ComputingComponents.DISK_CONSOLE.get(), tag);
    }

    /**
     * A world saved before the software moved onto the disk still carries the old block-level tag; it is
     * adopted once here, and lands on the disk at the next flush.
     */
    void loadLegacy(final CompoundTag tag) {
        if (!tag.contains("Console")) {
            return;
        }
        this.console.load(tag.getCompound("Console"));
        this.disk = this.machine.systemDisk();
    }

    /**
     * Reads the console off that disk, replacing whatever the previous drive left in memory. A disk with
     * nothing on it, fresh or freshly formatted, yields an empty console, which is what a clean install
     * must see.
     */
    private void readFrom(final ItemStack disk) {
        this.disk = disk; // set first: nothing below may recurse back into state()
        this.console.clear();
        final CompoundTag saved = disk.isEmpty() ? null : disk.get(ComputingComponents.DISK_CONSOLE.get());
        if (saved != null) {
            this.console.load(saved);
        }
    }
}
