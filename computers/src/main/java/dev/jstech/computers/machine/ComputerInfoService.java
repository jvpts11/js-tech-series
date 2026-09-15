/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.hardware.ComputerBuild;
import dev.jstech.computers.hardware.CpuSpec;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.boot.BootController;
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.computers.program.cli.ICliFiles;
import dev.jstech.computers.storage.StorageKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A machine as what runs on it can read it: what it is called, what it is built from, what it runs and what it holds.
 *
 * <p>Everything is a picture taken when it is asked for, not a live view, so whoever asks holds what it was told and
 * asks again when it wants to know again.
 */
public final class ComputerInfoService {

    /** A machine's processors taken as one: every core, the fastest clock among them, and the era of the first. */
    public record Cpu(int mhz, int cores, String era) {
    }

    /** The system a machine has installed, with empty text for each part when it has none. */
    public record Os(String id, String name) {
    }

    /** One of a machine's drives, measured in megabytes. */
    public record Disk(String mount, long usedMb, long capacityMb) {
    }

    /** One of the programs a machine is running, as its Task Manager lists it. */
    public record Running(int id, String name, String state, long heldBytes) {
    }

    private final AbstractComputerBlockEntity machine;
    /** The machine's drives as its shell sees them, which is how they are measured. */
    private final ICliFiles drives;

    ComputerInfoService(final AbstractComputerBlockEntity machine, final ICliFiles drives) {
        this.machine = machine;
        this.drives = drives;
    }

    /** What the machine is called: the name its owner gave it, or what kind of machine it is. */
    public String name() {
        final String given = this.machine.customName();
        return given.isEmpty() ? this.machine.getBlockState().getBlock().getName().getString() : given;
    }

    /** The processors the machine is built with; nothing counted when it has no build. */
    public Cpu cpu() {
        final ComputerBuild build = this.machine.currentBuild();
        int cores = 0;
        int mhz = 0;
        String era = "";
        if (build != null && !build.cpus().isEmpty()) {
            for (final CpuSpec one : build.cpus()) {
                cores += one.cores();
                mhz = Math.max(mhz, one.freqMhz());
            }
            era = build.cpus().getFirst().era().name().toLowerCase(Locale.ROOT);
        }
        return new Cpu(mhz, cores, era);
    }

    /** The system the machine has installed. */
    public Os os() {
        final OsDef installed = this.machine.installedOs();
        return installed == null ? new Os("", "") : new Os(installed.id().toString(), installed.displayName());
    }

    /** How much memory the machine has in all. */
    public int ramMb() {
        return this.machine.ramTotalMb();
    }

    /** How much of the machine's memory nothing holds. */
    public int freeRamMb() {
        return this.machine.ramLedger().freeMb();
    }

    /** Whether the machine is switched on. */
    public boolean online() {
        return this.machine.isRunning();
    }

    /** Whether the machine has somewhere to put a window: a system that boots to a desktop. */
    public boolean hasDesktop() {
        return BootController.targetForComputer(this.machine) == BootController.BootTarget.FULL_DESKTOP;
    }

    /** The machine's drives, each measured the way the shell measures it. */
    public List<Disk> disks() {
        final List<Disk> all = new ArrayList<>();
        for (final ICliComputer.MountInfo mount : this.drives.mounts()) {
            all.add(new Disk(String.valueOf(mount.drive()),
                    (mount.capacityMbEq() - mount.freeMbEq()) / StorageKey.MB_EQ_PER_ITEM,
                    mount.capacityMbEq() / StorageKey.MB_EQ_PER_ITEM));
        }
        return all;
    }

    /** The programs installed on the machine. */
    public List<String> programs() {
        return List.copyOf(this.machine.console().installed());
    }

    /** The programs the machine is running. */
    public List<Running> running() {
        final List<Running> all = new ArrayList<>();
        for (final ProgramView one : this.machine.programs().view()) {
            all.add(new Running(one.id(), one.name(), one.state(), one.heldBytes()));
        }
        return all;
    }
}
