/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.desktop;

import dev.jstech.computers.blockentity.ClusterManagementComputerBlockEntity;
import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.item.DiskItem;
import dev.jstech.computers.operation.payload.DesktopFilesPayload;
import dev.jstech.computers.operation.payload.DiskFilesPayload;
import dev.jstech.computers.operation.payload.files.TrashPayloads;
import dev.jstech.computers.os.DesktopEffects;
import dev.jstech.computers.os.FilesystemKind;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.ProgramKind;
import dev.jstech.computers.os.ProgramLoading;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.DiskTrash;
import dev.jstech.computers.os.fs.SystemLayout;
import dev.jstech.computers.os.fs.TrashFolder;
import dev.jstech.computers.program.ComputerConsoleState;
import dev.jstech.computers.program.ComputerSettings;
import dev.jstech.computers.program.DesktopLayout;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import static dev.jstech.computers.operation.payload.files.FileAccess.filesystemKindOf;

/**
 * What a desktop is handed when it opens: the files on it, where its icons were put, the programs that get a
 * launcher of their own, and the preferences its chrome applies.
 */
final class DesktopListings {

    /** A pin for a file on the desktop, as opposed to a program's launcher, which is kept whatever happens. */
    private static final String FILE_PIN = "file:";

    private DesktopListings() {
    }

    /**
     * The listing for that machine.
     *
     * @param computer the machine as a system host, or null when it is not one, which gets an empty desktop
     * @param terminal the machine as a terminal host, whose console lists the programs players published
     */
    static DesktopFilesPayload of(@Nullable final IOsHost computer, @Nullable final IComputerTerminalHost terminal) {
        final List<DesktopFilesPayload.WireCommunity> community = community(terminal);
        if (computer == null) {
            return new DesktopFilesPayload(List.of(), "", "", "", List.of(), List.of(), List.of(),
                    new DesktopFilesPayload.Prefs(0, 100, false, true, false, 0, DesktopEffects.ALL_ON), community,
                    List.of(), List.of(), Map.of(),
                    false, Map.of(), (float) ProgramLoading.REFERENCE_MB_PER_SECOND);
        }
        final ComputerConsoleState console = computer.console();
        final ComputerSettings settings = console.settings();
        final ItemStack disk = computer.systemDisk();
        final TrashFolder trash = TrashPayloads.trashOf(computer);
        final boolean trashFull = trash != null && !disk.isEmpty() && DiskTrash.holdsAnything(disk, trash);
        final List<DiskFilesPayload.WireFile> files = desktopFiles(computer, disk);
        return new DesktopFilesPayload(files, console.desktop().wallpaper(), console.desktop().cdeStyle().encoded(),
                console.computerName(), installedApps(computer), sourceBuilt(console), iconCells(computer, files),
                new DesktopFilesPayload.Prefs(settings.accent(), settings.brightness(), settings.clock12h(),
                        settings.taskbarCentered(), settings.darkMode(), settings.guiScale(),
                        MachineEffects.of(computer, settings)),
                community, settings.pinned(), settings.startTiles().encoded(), settings.defaultApps(), trashFull,
                versions(console),
                (float) loadRate(computer, disk));
    }

    /* How fast the machine loads a program: the disk it starts from, at its processor's pace. */
    private static double loadRate(final IOsHost computer, final ItemStack disk) {
        final int tier = disk.getItem() instanceof DiskItem item ? item.spec().tier().speedMultiplier() : 1;
        return ProgramLoading.loadRate(tier, computer.cpuCores(), computer.maxCpuMhz());
    }

    /** The version each installed package is at, by id path; a package with no version on record is left out. */
    private static Map<String, String> versions(final ComputerConsoleState console) {
        final Map<String, String> versions = new LinkedHashMap<>();
        for (final String id : console.installed()) {
            final String version = console.installedVersion(id);
            final ResourceLocation program = ResourceLocation.tryParse(id);
            if (!version.isBlank() && program != null && versions.size() < DesktopFilesPayload.MAX_PROGRAMS) {
                versions.put(program.getPath(), version);
            }
        }
        return versions;
    }

    /**
     * What is in the desktop folder. It only exists on a hierarchical disk: a POSIX kernel keeps it under the home
     * directory, the DOS family under Users/Public.
     */
    private static List<DiskFilesPayload.WireFile> desktopFiles(final IOsHost computer, final ItemStack disk) {
        final List<DiskFilesPayload.WireFile> files = new ArrayList<>();
        final FilesystemKind kind = filesystemKindOf(computer);
        if (disk.isEmpty() || kind != FilesystemKind.HIERARCHICAL) {
            return files;
        }
        final OsDef os = computer.installedOs();
        final String desktopDir = SystemLayout.desktopDirFor(os,
                os == null ? null : OsRegistry.getKernel(os.kernelId()));
        // The packet's list codec refuses more than MAX_FILES entries, so a crowded folder is clipped here
        // (folders first) instead of making the whole desktop packet fail to encode.
        for (final String dir : DiskFilesystem.listDirs(disk, desktopDir, kind)) {
            if (files.size() >= DesktopFilesPayload.MAX_FILES) {
                return files;
            }
            files.add(new DiskFilesPayload.WireFile(dir, "", 0L, false, true));
        }
        for (final DiskFilesystem.FileEntry entry : DiskFilesystem.list(disk, desktopDir, kind)) {
            if (files.size() >= DesktopFilesPayload.MAX_FILES) {
                break;
            }
            files.add(new DiskFilesPayload.WireFile(entry.path(), entry.type().extension(), entry.weight(),
                    entry.readOnly(), false));
        }
        return files;
    }

    /**
     * Where each icon was put. A pin for a desktop file that no longer exists is dropped here and forgotten by the
     * machine too, so a stale position never haunts a later file that happens to take the same name. A program's
     * launcher keeps its pin always.
     */
    private static List<DesktopFilesPayload.WireIconCell> iconCells(final IOsHost computer,
                                                                    final List<DiskFilesPayload.WireFile> files) {
        final Set<String> names = new HashSet<>();
        for (final DiskFilesPayload.WireFile file : files) {
            names.add(baseNameOf(file.path()));
        }
        // Pins belong to the machine while desktop files belong to the boot disk, so an unreadable listing (no disk,
        // or a filesystem without folders) or a clipped one proves nothing about which files were deleted.
        final boolean complete = !computer.systemDisk().isEmpty()
                && filesystemKindOf(computer) == FilesystemKind.HIERARCHICAL
                && files.size() < DesktopFilesPayload.MAX_FILES;
        final List<DesktopFilesPayload.WireIconCell> cells = new ArrayList<>();
        boolean pruned = false;
        final DesktopLayout layout = computer.console().desktop();
        for (final Map.Entry<String, Integer> pin : layout.iconCells().entrySet()) {
            final String key = pin.getKey();
            if (complete && key.startsWith(FILE_PIN) && !names.contains(key.substring(FILE_PIN.length()))) {
                layout.clearIconCell(key);
                pruned = true;
                continue;
            }
            cells.add(new DesktopFilesPayload.WireIconCell(key, pin.getValue()));
        }
        if (pruned) {
            computer.setChanged();
        }
        return cells;
    }

    /**
     * The installed programs that open as a window of their own, each allowed by the system, the hardware and the
     * kind of machine. The built-in apps are added on the client, so only installable ones are listed.
     */
    private static List<String> installedApps(final IOsHost computer) {
        final List<String> apps = new ArrayList<>();
        final OsDef os = computer.installedOs();
        for (final ProgramSpec spec : OsRegistry.programs()) {
            if (spec.installable() && spec.kind() == ProgramKind.APP && os != null
                    && spec.platforms().contains(os.platform()) && installedAndAllowed(computer, spec)) {
                apps.add(spec.id().getPath());
            }
        }
        return apps;
    }

    /** The installed programs the machine built from source, by id path, whose windows hold a little less. */
    private static List<String> sourceBuilt(final ComputerConsoleState console) {
        final List<String> built = new ArrayList<>();
        for (final String id : console.builtFromSource()) {
            final ResourceLocation program = ResourceLocation.tryParse(id);
            if (program != null) {
                built.add(program.getPath());
            }
        }
        return built;
    }

    /** The programs players published on the Mirror and installed here, each of which gets a launcher. */
    private static List<DesktopFilesPayload.WireCommunity> community(@Nullable final IComputerTerminalHost terminal) {
        final List<DesktopFilesPayload.WireCommunity> community = new ArrayList<>();
        final ComputerConsoleState console = terminal == null ? null : terminal.console();
        if (console != null) {
            for (final ComputerConsoleState.Community one : console.community()) {
                community.add(new DesktopFilesPayload.WireCommunity(one.name(), one.icon(), one.entry()));
            }
        }
        return community;
    }

    /** Whether that program is installed on the computer AND runnable on its current system and hardware. */
    private static boolean installedAndAllowed(final IOsHost computer, final ProgramSpec spec) {
        final String id = spec.id().toString();
        return computer.console().isInstalled(id) && hostScopeAllows(spec, computer)
                && OsRegistry.canRunProgram(computer.installedOsId(), spec.id(), computer.maxCpuMhz(),
                        computer.totalVramMb(), computer.console().builtFromSource(id));
    }

    /** Whether a program's host scope permits it on this computer. */
    private static boolean hostScopeAllows(final ProgramSpec spec, final IOsHost computer) {
        return switch (spec.hostScope()) {
            case ANY -> true;
            case MAINFRAME -> computer instanceof MainframeBlockEntity;
            case CRAFTING_COMPUTER -> computer instanceof CraftingComputerBlockEntity;
            /*
             * A rack answers as the machine it is showing, so scoping to SERVER means "this session is a rack
             * server", which is exactly where the headless server services belong.
             */
            case SERVER -> computer instanceof ServerRackBlockEntity;
            case CLUSTER_MANAGEMENT_COMPUTER -> computer instanceof ClusterManagementComputerBlockEntity;
            case PERSONAL_COMPUTER -> computer instanceof PersonalComputerBlockEntity;
        };
    }

    /** The last path segment (after the final {@code /}), or the whole path when it has no slash. */
    private static String baseNameOf(final String path) {
        final int slash = path.lastIndexOf('/');
        return slash >= 0 && slash < path.length() - 1 ? path.substring(slash + 1) : path;
    }
}
