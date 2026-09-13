/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.desktop;

import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.operation.payload.ClientPayloadHandlers;
import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.computers.operation.payload.DesktopFilesPayload;
import dev.jstech.computers.operation.payload.DesktopWindowsPayload;
import dev.jstech.computers.operation.payload.DiskFilesPayload;
import dev.jstech.computers.operation.payload.EndProcessPayload;
import dev.jstech.computers.operation.payload.RequestDesktopFilesPayload;
import dev.jstech.computers.operation.payload.RequestSettingsPayload;
import dev.jstech.computers.operation.payload.SetDesktopPrefsPayload;
import dev.jstech.computers.operation.payload.SetIconPositionPayload;
import dev.jstech.computers.operation.payload.SetSettingPayload;
import dev.jstech.computers.operation.payload.SettingsSnapshotPayload;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.program.Programs;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.List;

import static dev.jstech.computers.operation.payload.files.FileAccess.filesystemKindOf;

/**
 * The desktop's payloads: its files and icons, its preferences and settings, and the windows it leaves open.
 */
public final class DesktopPayloads {

    private DesktopPayloads() {
    }

    /** Registers the payloads this class handles. */
    public static void register(final PayloadRegistrar registrar) {
        registrar.playBidirectional(DesktopWindowsPayload.TYPE, DesktopWindowsPayload.STREAM_CODEC,
                new net.neoforged.neoforge.network.handling.DirectionalPayloadHandler<>(
                        ClientPayloadHandlers.onMainThread(DesktopPayloads::handleDesktopWindowsOnClient),
                        ComputerAccess.guarded(DesktopWindowsPayload.TYPE,
                                ComputerAccess.machineOrClosingDesktop(DesktopWindowsPayload::host),
                                DesktopPayloads::handleDesktopWindowsOnServer)));
        ComputerAccess.accept(registrar, RequestDesktopFilesPayload.TYPE, RequestDesktopFilesPayload.STREAM_CODEC,
                ComputerAccess.machine(RequestDesktopFilesPayload::hostPos), DesktopPayloads::handleRequestDesktopFiles);
        registrar.playToClient(DesktopFilesPayload.TYPE, DesktopFilesPayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread(DesktopPayloads::handleDesktopFiles));
        ComputerAccess.accept(registrar, SetDesktopPrefsPayload.TYPE, SetDesktopPrefsPayload.STREAM_CODEC,
                ComputerAccess.machine(SetDesktopPrefsPayload::hostPos), DesktopPayloads::handleSetDesktopPrefs);
        ComputerAccess.accept(registrar, RequestSettingsPayload.TYPE, RequestSettingsPayload.STREAM_CODEC,
                ComputerAccess.machine(RequestSettingsPayload::hostPos), DesktopPayloads::handleRequestSettings);
        ComputerAccess.accept(registrar, SetSettingPayload.TYPE, SetSettingPayload.STREAM_CODEC,
                ComputerAccess.machine(SetSettingPayload::hostPos), DesktopPayloads::handleSetSetting);
        ComputerAccess.accept(registrar, EndProcessPayload.TYPE, EndProcessPayload.STREAM_CODEC,
                ComputerAccess.machine(EndProcessPayload::hostPos), DesktopPayloads::handleEndProcess);
        registrar.playToClient(SettingsSnapshotPayload.TYPE, SettingsSnapshotPayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread(DesktopPayloads::handleSettingsSnapshot));
        ComputerAccess.accept(registrar, SetIconPositionPayload.TYPE, SetIconPositionPayload.STREAM_CODEC,
                ComputerAccess.machine(SetIconPositionPayload::hostPos), DesktopPayloads::handleSetIconPosition);
    }

    private static void handleRequestDesktopFiles(final RequestDesktopFilesPayload payload, final ServerPlayer player,
                                                  final ServerLevel level) {
        final java.util.List<DiskFilesPayload.WireFile> wire = new java.util.ArrayList<>();
        final String[] prefs = {"", ""};
        // accent override (0=none), brightness, clock12h (0/1), taskbar centered (1) vs left (0), dark (0/1), scale (%)
        final int[] deskPrefs = {0, 100, 0, 1, 0, 0};
        final java.util.List<String> programs = new java.util.ArrayList<>();
        final java.util.List<DesktopFilesPayload.WireIconCell> iconCells = new java.util.ArrayList<>();
        final java.util.List<String> pinned = new java.util.ArrayList<>();
        if (level.getBlockEntity(payload.hostPos()) instanceof dev.jstech.computers.os.IOsHost computer) {
            final net.minecraft.world.item.ItemStack disk = computer.systemDisk();
            final dev.jstech.computers.os.FilesystemKind kind =
                    filesystemKindOf(computer);
            prefs[0] = computer.console().wallpaper();
            prefs[1] = computer.console().computerName();
            pinned.addAll(computer.console().settings().pinned());
            deskPrefs[0] = computer.console().settings().accent();
            deskPrefs[1] = computer.console().settings().brightness();
            deskPrefs[2] = computer.console().settings().clock12h() ? 1 : 0;
            deskPrefs[3] = computer.console().settings().taskbarCentered() ? 1 : 0;
            deskPrefs[4] = computer.console().settings().darkMode() ? 1 : 0;
            deskPrefs[5] = computer.console().settings().guiScale();
            /*
             * Installed programs that open as their own desktop window (vs. the always-present built-in
             * apps). Each is gated by the installed OS, hardware and host scope; the built-in apps are
             * added on the client, so only installable desktop apps flow through this list.
             */
            for (final dev.jstech.computers.os.ProgramSpec spec
                    : dev.jstech.computers.os.OsRegistry.programs()) {
                final dev.jstech.computers.os.OsDef hostOs = computer.installedOs();
                if (spec.installable()
                        && spec.kind() == dev.jstech.computers.os.ProgramKind.APP
                        && hostOs != null && spec.platforms().contains(hostOs.platform())
                        && installedAndAllowed(computer, spec.id())) {
                    programs.add(spec.id().getPath());
                }
            }
            /*
             * The desktop folder only exists on a hierarchical (desktop OS) disk; a POSIX kernel keeps it
             * under the home directory, the DOS family under Users/Public.
             */
            if (!disk.isEmpty()
                    && kind == dev.jstech.computers.os.FilesystemKind.HIERARCHICAL) {
                final dev.jstech.computers.os.OsDef osDef = computer.installedOs();
                final String desktopDir = dev.jstech.computers.os.fs.SystemLayout.desktopDirFor(
                        osDef == null ? null : dev.jstech.computers.os.OsRegistry.getKernel(
                                osDef.kernelId()));
                for (final String d
                        : dev.jstech.computers.os.fs.DiskFilesystem.listDirs(
                                disk, desktopDir, kind)) {
                    wire.add(new DiskFilesPayload.WireFile(d, "", 0L, false, true));
                }
                for (final dev.jstech.computers.os.fs.DiskFilesystem.FileEntry e
                        : dev.jstech.computers.os.fs.DiskFilesystem.list(
                                disk, desktopDir, kind)) {
                    wire.add(new DiskFilesPayload.WireFile(
                            e.path(), e.type().extension(), e.weight(), e.readOnly(), false));
                }
            }
            /*
             * Pinned icon cells. A "file:" pin whose desktop file no longer exists is dropped here and
             * forgotten from the console state too, so a stale position never haunts a later file that
             * happens to take the same name (self-healing). "app:" launcher pins are always kept.
             */
            final java.util.Set<String> desktopNames = new java.util.HashSet<>();
            for (final DiskFilesPayload.WireFile f : wire) {
                desktopNames.add(baseNameOf(f.path()));
            }
            boolean prunedAnyPin = false;
            for (final java.util.Map.Entry<String, Integer> e
                    : new java.util.ArrayList<>(computer.console().iconCells().entrySet())) {
                final String key = e.getKey();
                if (key.startsWith("file:") && !desktopNames.contains(key.substring("file:".length()))) {
                    computer.console().clearIconCell(key);
                    prunedAnyPin = true;
                    continue;
                }
                iconCells.add(new DesktopFilesPayload.WireIconCell(key, e.getValue()));
            }
            if (prunedAnyPin) {
                computer.setChanged();
            }
        }
        /*
         * The machine's open windows travel with the desktop listing, so the desktop that is opening
         * restores them from the machine and not from a cache in this client.
         */
        PacketDistributor.sendToPlayer(player, DesktopWindowsPayload.of(payload.hostPos(),
                level.getBlockEntity(payload.hostPos()) instanceof IOsHost machine
                        ? machine.openWindows() : java.util.List.of()));
        /*
         * Programs the player installed from the Mirror get a launcher of their own, so the icon on
         * the desktop is not only for what came with the machines.
         */
        final java.util.List<DesktopFilesPayload.WireCommunity> community = new java.util.ArrayList<>();
        if (level.getBlockEntity(payload.hostPos())
                instanceof dev.jstech.computers.terminal.IComputerTerminalHost terminal) {
            final var console = terminal.console();
            if (console != null) {
                for (final var one : console.community()) {
                    community.add(new DesktopFilesPayload.WireCommunity(
                            one.name(), one.icon(), one.entry()));
                }
            }
        }
        PacketDistributor.sendToPlayer(player, new DesktopFilesPayload(wire, prefs[0], prefs[1], programs, iconCells,
                new DesktopFilesPayload.Prefs(deskPrefs[0], deskPrefs[1], deskPrefs[2] != 0,
                        deskPrefs[3] != 0, deskPrefs[4] != 0, deskPrefs[5]), community, pinned));
    }

    private static void handleSetDesktopPrefs(final SetDesktopPrefsPayload payload, final ServerPlayer player,
                                              final ServerLevel level) {
        if (level.getBlockEntity(payload.hostPos()) instanceof dev.jstech.computers.os.IOsHost computer) {
            computer.console().setWallpaper(payload.wallpaper());
            computer.console().setComputerName(payload.computerName());
            computer.setChanged();
        }
    }

    private static void handleRequestSettings(final RequestSettingsPayload payload, final ServerPlayer player,
                                              final ServerLevel level) {
        if (level.getBlockEntity(payload.hostPos()) instanceof dev.jstech.computers.os.IOsHost computer) {
            PacketDistributor.sendToPlayer(player, buildSettingsSnapshot(computer, payload.hostPos()));
        }
    }

    private static void handleEndProcess(final EndProcessPayload payload, final ServerPlayer player,
                                         final ServerLevel level) {
        if (level.getBlockEntity(payload.hostPos())
                instanceof dev.jstech.computers.blockentity.AbstractComputerBlockEntity computer
                && computer.cannon().stop(payload.id())) {
            computer.setChanged();
            PacketDistributor.sendToPlayer(player, buildSettingsSnapshot(
                    (dev.jstech.computers.os.IOsHost) computer, payload.hostPos()));
        }
    }

    private static void handleSetSetting(final SetSettingPayload payload, final ServerPlayer player,
                                         final ServerLevel level) {
        if (level.getBlockEntity(payload.hostPos()) instanceof dev.jstech.computers.os.IOsHost computer
                && computer instanceof dev.jstech.computers.terminal.IComputerTerminalHost host) {
            /*
             * Route through the same setConfig the MC-DOS 'config' command uses, so both front-ends
             * clamp and persist identically.
             */
            new dev.jstech.computers.program.ServerCliComputer(host, level)
                    .setConfig(payload.key(), payload.value());
            PacketDistributor.sendToPlayer(player, buildSettingsSnapshot(computer, payload.hostPos()));
        }
    }

    private static void handleSettingsSnapshot(final SettingsSnapshotPayload payload, final Player player) {
        dev.jstech.computers.client.os.SettingsApp.accept(payload);
        dev.jstech.computers.client.os.SystemMonitorApp.accept(payload);
        dev.jstech.computers.client.os.TaskManagerApp.accept(payload);
    }

    /** Reads the full Settings snapshot (editable knobs + read-only specs, disks and programs) from a computer. */
    private static SettingsSnapshotPayload buildSettingsSnapshot(
            final dev.jstech.computers.os.IOsHost computer,
            final BlockPos pos) {
        final dev.jstech.computers.program.ComputerConsoleState console = computer.console();
        final dev.jstech.computers.program.ComputerSettings st = console.settings();
        final ItemStack sysDisk = computer.systemDisk();
        final int netshare = dev.jstech.computers.item.DiskItem.publicPermille(sysDisk);
        final int cpuCount = computer.installedCpus();
        final String cpuLabel = cpuCount + (cpuCount == 1 ? " CPU" : " CPUs");
        final net.minecraft.resources.ResourceLocation osId = computer.installedOsId();
        final String osLabel = osId == null ? "none" : osId.getPath();
        final dev.jstech.computers.os.OsDef os = computer.installedOs();
        final String platform = os == null ? "-" : os.platform().label();
        final List<String> installed = new ArrayList<>(console.installed());
        final List<SettingsSnapshotPayload.DiskUse> disks = new ArrayList<>();
        final long mbEq = dev.jstech.computers.storage.StorageKey.MB_EQ_PER_ITEM;
        for (final ItemStack stack : computer.diskStacks()) {
            if (!(stack.getItem() instanceof dev.jstech.computers.item.DiskItem diskItem)) {
                continue;
            }
            // Megabytes follow the disk's own era: what an item costs there is what its usage is worth.
            final long mbPerItem = diskItem.spec().era().mbPerItem();
            final long capMb = diskItem.spec().capacityMb();
            final long storageUsed = dev.jstech.computers.storage.DriveVolumes.usedWeight(stack);
            final long fsUsed = dev.jstech.computers.os.fs.DiskFilesystem.filesWeight(stack);
            final net.minecraft.resources.ResourceLocation dOsId =
                    stack.get(dev.jstech.computers.ComputingModule.SYSTEM_OS.get());
            final dev.jstech.computers.os.OsDef dOs =
                    dOsId != null ? dev.jstech.computers.os.OsRegistry.getOs(dOsId) : null;
            final long osReserved = dOs != null ? dOs.footprintItemsOn(diskItem.spec().era()) * mbEq : 0L;
            final long usedMb = (storageUsed + fsUsed + osReserved) * mbPerItem / mbEq;
            disks.add(new SettingsSnapshotPayload.DiskUse(
                    stack.getHoverName().getString(), capMb, usedMb, stack == sysDisk));
        }
        // The memory ledger: what the system, its desktop, its services and its windows hold right now.
        final dev.jstech.computers.os.RamLedger ledger = computer.ramLedger();
        final List<SettingsSnapshotPayload.RamUse> ramUses = new ArrayList<>();
        for (final dev.jstech.computers.os.RamLedger.Entry entry : ledger.entries()) {
            ramUses.add(new SettingsSnapshotPayload.RamUse(
                    entry.name(), entry.mb(), entry.kind().name(), entry.id()));
        }
        final List<SettingsSnapshotPayload.ShareRow> shares = new ArrayList<>();
        for (final dev.jstech.computers.program.ComputerSettings.Share share : st.shares()) {
            shares.add(new SettingsSnapshotPayload.ShareRow(share.name(), share.path(), share.writable()));
        }
        return new SettingsSnapshotPayload(pos, console.wallpaper(), console.computerName(),
                st.accent(), st.clock12h(), st.guiScale(), st.brightness(),
                String.valueOf(st.defaultSaveDrive()), st.removableAutoOpen(), st.themePreset(),
                st.taskbarCentered(), st.darkMode(),
                netshare, cpuLabel, computer.maxCpuMhz(),
                computer.ramTotalMb(), computer.totalVramMb(),
                osLabel, platform, installed, disks, ledger.usedMb(), ramUses, shares, st.remoteAllowed());
    }

    /** The last path segment (after the final {@code /}), or the whole path when it has no slash. */
    private static String baseNameOf(final String path) {
        final int slash = path.lastIndexOf('/');
        return slash >= 0 && slash < path.length() - 1 ? path.substring(slash + 1) : path;
    }

    private static void handleSetIconPosition(final SetIconPositionPayload payload, final ServerPlayer player,
                                              final ServerLevel level) {
        if (level.getBlockEntity(payload.hostPos()) instanceof dev.jstech.computers.os.IOsHost computer) {
            computer.console().setIconCell(payload.iconKey(), payload.cell());
            computer.setChanged();
        }
    }

    private static void handleDesktopFiles(final DesktopFilesPayload payload, final Player player) {
        dev.jstech.computers.client.os.DesktopScreen.acceptDesktop(payload);
    }

    /** Whether {@code progId} is installed on the computer AND runnable on its current OS (version + specs). */
    private static boolean installedAndAllowed(
            final dev.jstech.computers.os.IOsHost computer,
            final net.minecraft.resources.ResourceLocation progId) {
        return computer.console() != null
                && computer.console().isInstalled(progId.toString())
                && hostScopeAllows(dev.jstech.computers.os.OsRegistry.getProgram(progId),
                        computer)
                && dev.jstech.computers.os.OsRegistry.canRunProgram(
                        computer.installedOsId(), progId, computer.maxCpuMhz(), computer.totalVramMb());
    }

    /** Whether a program's host scope permits it on this computer (a null spec places no restriction). */
    private static boolean hostScopeAllows(
            final dev.jstech.computers.os.ProgramSpec spec,
            final dev.jstech.computers.os.IOsHost computer) {
        if (spec == null) {
            return true;
        }
        return switch (spec.hostScope()) {
            case ANY -> true;
            case MAINFRAME -> computer
                    instanceof dev.jstech.computers.blockentity.MainframeBlockEntity;
            case CRAFTING_COMPUTER -> computer
                    instanceof CraftingComputerBlockEntity;
            /*
             * A rack answers as the machine it is showing, so scoping to SERVER means "this session
             * is a rack server", which is exactly where the headless server services belong.
             */
            case SERVER -> computer
                    instanceof dev.jstech.computers.blockentity.ServerRackBlockEntity;
            case CLUSTER_MANAGEMENT_COMPUTER -> computer
                    instanceof dev.jstech.computers.blockentity.ClusterManagementComputerBlockEntity;
        };
    }

    /** A player left the monitor: the layout they left behind becomes the machine's. */
    private static void handleDesktopWindowsOnServer(final DesktopWindowsPayload payload, final ServerPlayer player,
                                                     final ServerLevel level) {
        if (level.getBlockEntity(payload.host()) instanceof IOsHost computer
                && computer.isRunning()) {
            /*
             * A machine that has since been switched off or restarted keeps its empty desktop: the
             * layout in flight belongs to a session that no longer exists. The machine keeps only the
             * windows its RAM holds: a client that claims more than fits is trimmed to what does.
             */
            if (!computer.needsPost()) {
                computer.setOpenWindows(computer.windowsWithinBudget(payload.toOpenWindows()));
            }
        }
    }

    /** The desktop is opening: hand it the windows the machine has. */
    private static void handleDesktopWindowsOnClient(final DesktopWindowsPayload payload, final Player player) {
        dev.jstech.computers.client.os.DesktopScreen.applyWindows(payload);
    }
}
