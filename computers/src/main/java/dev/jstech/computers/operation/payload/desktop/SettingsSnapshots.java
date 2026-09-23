/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.desktop;

import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.hardware.ComputerBuild;
import dev.jstech.computers.item.DiskItem;
import dev.jstech.computers.item.HardwareTooltip;
import dev.jstech.computers.operation.payload.SettingsSnapshotPayload;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsDisks;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.RamLedger;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.program.ComputerConsoleState;
import dev.jstech.computers.program.ComputerSettings;
import dev.jstech.computers.storage.DriveVolumes;
import dev.jstech.computers.storage.StorageKey;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Everything the Settings, System Monitor and Task Manager windows read off a machine at once: the knobs a player
 * can turn, and what the machine is built of and holding right now.
 */
final class SettingsSnapshots {

    private SettingsSnapshots() {
    }

    /** The whole snapshot of that machine. */
    static SettingsSnapshotPayload of(final IOsHost computer, final BlockPos pos) {
        final ComputerConsoleState console = computer.console();
        final ComputerSettings st = console.settings();
        final ItemStack sysDisk = computer.systemDisk();
        final int netshare = DiskItem.publicPermille(sysDisk);
        final int cpuCount = computer.installedCpus();
        final String cpuLabel = cpuCount + (cpuCount == 1 ? " CPU" : " CPUs");
        final ResourceLocation osId = computer.installedOsId();
        final String osLabel = osId == null ? "none" : osId.getPath();
        final OsDef os = computer.installedOs();
        final String platform = os == null ? "-" : os.platform().label();
        final List<String> installed = new ArrayList<>(console.installed());
        final List<SettingsSnapshotPayload.DiskUse> disks = new ArrayList<>();
        for (final ItemStack stack : computer.diskStacks()) {
            if (stack.getItem() instanceof DiskItem diskItem) {
                disks.add(new SettingsSnapshotPayload.DiskUse(stack.getHoverName().getString(),
                        diskItem.spec().capacityMb(), usedMb(stack), stack == sysDisk));
            }
        }
        // The memory ledger: what the system, its desktop, its services and its windows hold right now.
        final RamLedger ledger = computer.ramLedger();
        final List<SettingsSnapshotPayload.RamUse> ramUses = new ArrayList<>();
        for (final RamLedger.Entry entry : ledger.entries()) {
            ramUses.add(new SettingsSnapshotPayload.RamUse(
                    entry.name(), entry.mb(), entry.heldBytes(), entry.kind().serializedName(), entry.id()));
        }
        final List<SettingsSnapshotPayload.ShareRow> shares = new ArrayList<>();
        for (final ComputerSettings.Share share : st.shares()) {
            shares.add(new SettingsSnapshotPayload.ShareRow(share.name(), share.path(), share.writable()));
        }
        return new SettingsSnapshotPayload(pos, console.desktop().wallpaper(), console.computerName(),
                st.accent(), st.clock12h(), st.guiScale(), st.brightness(),
                String.valueOf(st.defaultSaveDrive()), st.removableAutoOpen(), st.themePreset(),
                st.taskbarCentered(), st.darkMode(),
                netshare, cpuLabel, computer.maxCpuMhz(), architectureOf(computer),
                computer.ramTotalMb(), computer.totalVramMb(),
                osLabel, platform, installed, disks, ledger.usedMb(), ramUses, shares, st.remoteAllowed());
    }

    /**
     * How many megabytes of a disk are taken: what is stored on it, its files, and the room a system on it keeps
     * for itself. Megabytes follow the disk's own era, since what an item costs there is what its usage is worth.
     */
    static long usedMb(final ItemStack stack) {
        if (!(stack.getItem() instanceof DiskItem diskItem)) {
            return 0L;
        }
        final long mbEq = StorageKey.MB_EQ_PER_ITEM;
        final long mbPerItem = diskItem.spec().era().mbPerItem();
        final ResourceLocation systemId = OsDisks.systemOn(stack);
        final OsDef system = systemId != null ? OsRegistry.getOs(systemId) : null;
        final long reserved = system != null ? system.footprintItemsOn(diskItem.spec().era()) * mbEq : 0L;
        return (DriveVolumes.usedWeight(stack) + DiskFilesystem.filesWeight(stack) + reserved) * mbPerItem / mbEq;
    }

    /** How the machine's architecture reads on a screen, or empty when it has no processor to read it from. */
    private static String architectureOf(final IOsHost computer) {
        if (!(computer instanceof AbstractComputerBlockEntity machine)) {
            return "";
        }
        final ComputerBuild build = machine.currentBuild();
        return build == null || build.cpus().isEmpty() ? ""
                : HardwareTooltip.architecture(build.cpus().getFirst());
    }
}
