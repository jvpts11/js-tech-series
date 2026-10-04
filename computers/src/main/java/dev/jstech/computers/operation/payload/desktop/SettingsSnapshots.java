/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.desktop;

import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.blockentity.MonitorBlockEntity;
import dev.jstech.computers.blockentity.SpeakerBlockEntity;
import dev.jstech.computers.hardware.ComputerBuild;
import dev.jstech.computers.hardware.CpuSpec;
import dev.jstech.computers.hardware.GpuSpec;
import dev.jstech.computers.item.DiskItem;
import dev.jstech.computers.item.GpuItem;
import dev.jstech.computers.item.HardwareTooltip;
import dev.jstech.computers.monitor.VideoMemory;
import dev.jstech.computers.operation.payload.SettingsSnapshotPayload;
import dev.jstech.computers.operation.payload.machine.MachineLabels;
import dev.jstech.computers.os.DesktopEffects;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsDisks;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.os.RamLedger;
import dev.jstech.computers.os.VramLedger;
import dev.jstech.computers.os.WindowKeys;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.program.ComputerConsoleState;
import dev.jstech.computers.program.ComputerSettings;
import dev.jstech.computers.storage.DriveVolumes;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.util.Loaded;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
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
        final Text cpuLabel = (cpuCount == 1 ? SettingsSnapshotPayload.ONE_CPU : SettingsSnapshotPayload.CPUS)
                .with(cpuCount);
        final String osLabel = MachineLabels.osLabelOf(computer.installedOsId());
        final OsDef os = computer.installedOs();
        final String platform = os == null ? "-" : os.platform().label();
        final List<String> installed = new ArrayList<>(console.installed());
        final List<SettingsSnapshotPayload.DiskUse> disks = new ArrayList<>();
        for (final ItemStack stack : computer.diskStacks()) {
            if (stack.getItem() instanceof DiskItem diskItem) {
                disks.add(new SettingsSnapshotPayload.DiskUse(GameText.of(stack.getHoverName()),
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
                netshare, cpuLabel, computer.maxCpuMhz(), isaOf(computer),
                computer.ramTotalMb(), computer.totalVramMb(),
                osLabel, platform, installed, disks, ledger.usedMb(), ramUses, shares, st.remoteAllowed(),
                soundOf(computer, st), gpuOf(computer),
                new DesktopEffects(List.copyOf(st.effectsOff()), st.effectSpeed()));
    }

    /**
     * The machine's graphics: its first card (or the graphics on its processor's die), and what its video memory goes
     * to. How busy the card is is a reading of what it drives: half of it by how full its memory is, a fifth more for
     * each graphics window open on it.
     */
    static SettingsSnapshotPayload.Gpu gpuOf(final IOsHost computer) {
        if (!(computer instanceof AbstractComputerBlockEntity machine)
                || !(machine.getLevel() instanceof ServerLevel level) || machine.currentBuild() == null) {
            return SettingsSnapshotPayload.Gpu.NONE;
        }
        final ComputerBuild build = machine.currentBuild();
        final VideoMemory.State state = VideoMemory.of(level, computer);
        final VramLedger ledger = state.ledger();
        Text card = Text.EMPTY;
        int cores = 0;
        int mhz = 0;
        String slot = "";
        if (!build.gpus().isEmpty()) {
            final GpuSpec gpu = build.gpus().getFirst();
            cores = gpu.cores();
            mhz = gpu.clockMhz();
            slot = build.motherboard().pcieGeneration().slotName().english();
            for (final ItemStack stack : computer.hardwareStacks()) {
                if (stack.getItem() instanceof GpuItem) {
                    card = GameText.of(stack.getHoverName());
                    break;
                }
            }
        } else if (state.shared()) {
            for (final CpuSpec cpu : build.cpus()) {
                if (cpu.hasIntegratedGraphics()) {
                    card = Text.literal(cpu.design().graphics().model());
                    cores = cpu.design().graphics().units();
                    mhz = cpu.design().graphics().mhz();
                    break;
                }
            }
        }
        final List<SettingsSnapshotPayload.VramRow> usedBy = new ArrayList<>();
        int windows = 0;
        for (final VramLedger.Entry entry : ledger.entries()) {
            final boolean monitor = entry.kind() == VramLedger.Kind.MONITOR;
            if (!monitor) {
                windows++;
            }
            usedBy.add(new SettingsSnapshotPayload.VramRow(monitor ? monitorName(level, entry.name())
                    : windowName(entry.name()), entry.kb(), monitor));
        }
        final double full = ledger.totalKb() <= 0 ? 0.0 : (double) ledger.usedKb() / ledger.totalKb();
        final int load = ledger.usedKb() <= 0 ? 0 : (int) Math.min(100L, Math.round(full * 50.0 + windows * 20.0));
        return new SettingsSnapshotPayload.Gpu(card, cores, mhz, slot, ledger.totalKb(),
                ledger.usedKb(VramLedger.Kind.MONITOR), ledger.usedKb(VramLedger.Kind.WINDOW), state.shared(), load,
                usedBy);
    }

    /* A monitor holding video memory, by its name, and its size when it is a big screen. */
    private static Text monitorName(final ServerLevel level, final String entry) {
        final BlockPos at = BlockPos.of(Long.parseLong(entry.substring(entry.indexOf(':') + 1)));
        final Text name = GameText.of(level.getBlockState(at).getBlock().getName());
        if (Loaded.blockEntity(level, at) instanceof MonitorBlockEntity monitor && monitor.panel() != null) {
            return SettingsSnapshotsTexts.BIG_SCREEN.with(name, monitor.panel().width(), monitor.panel().height());
        }
        return name;
    }

    /* A graphics window holding video memory, by the name of its program. */
    private static Text windowName(final String key) {
        final ProgramSpec spec = WindowKeys.program(key);
        return spec == null ? Text.literal(key) : spec.name().text();
    }

    /** The system's sound: its settings, and what plays it on a machine that has sound hardware of its own. */
    static SettingsSnapshotPayload.Sound soundOf(final IOsHost computer, final ComputerSettings st) {
        if (!(computer instanceof AbstractComputerBlockEntity machine)) {
            return new SettingsSnapshotPayload.Sound(st.volume(), st.muted(), st.soundOutput().id(), Text.EMPTY,
                    false, List.of());
        }
        final List<SettingsSnapshotPayload.SpeakerRow> speakers = new ArrayList<>();
        for (final SpeakerBlockEntity speaker : machine.linkedSpeakers()) {
            speakers.add(new SettingsSnapshotPayload.SpeakerRow(speaker.name(),
                    machine.speakerSide(speaker.getBlockPos())));
        }
        return new SettingsSnapshotPayload.Sound(st.volume(), st.muted(), st.soundOutput().id(),
                machine.soundHardwareLabel(), machine.playsRecordings(), speakers);
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

    /** How the machine's instruction set reads on a screen, or empty when it has no processor to read it from. */
    private static Text isaOf(final IOsHost computer) {
        if (!(computer instanceof AbstractComputerBlockEntity machine)) {
            return Text.EMPTY;
        }
        final ComputerBuild build = machine.currentBuild();
        return build == null || build.cpus().isEmpty() ? Text.EMPTY : HardwareTooltip.isa(build.cpus().getFirst());
    }
}
