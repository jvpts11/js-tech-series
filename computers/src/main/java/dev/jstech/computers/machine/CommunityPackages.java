/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.os.player.PlayerDesktop;
import dev.jstech.computers.program.ComputerConsoleState;
import dev.jstech.computers.program.cli.CliTexts;
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.computers.program.cli.ICliPackages;
import dev.jstech.computers.sigma.pack.Manifest;
import dev.jstech.computers.sigma.pack.Packed;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * The packages players publish on the Mirror, as a machine installs and removes them.
 *
 * <p>Each is unpacked into a folder of its own, named after the package, so two of them cannot quietly overwrite
 * each other's files and removing one takes exactly its own files with it. A desktop is a package like any other: its
 * programs, its pictures and its wallpaper go into its folder, and the machine draws it from there
 * (computers/docs/design/player-systems.md).
 */
@TextHolder
final class CommunityPackages {

    private final IComputerTerminalHost terminal;
    private final FileService files;
    private final MirrorService mirror;
    /** Whether the Σ# runtime is on this machine, which is what a player's package needs to run. */
    private final BooleanSupplier hasRuntime;

    /* What a path from the disk's root starts with. */
    private static final String ROOT = "/";
    /* The system disk's drive, which every machine and the desktop read packages from. */
    private static final String SYSTEM_DRIVE = "C:";

    private static final TextKey UNREADABLE =
            TextKey.of("jsc.service.community.unreadable", "the Mirror's copy of this package is not readable");
    private static final TextKey A_SYSTEM = TextKey.of("jsc.service.community.a_system",
            "this is an operating system: boot its installer from the firmware, through the network");
    private static final TextKey NEEDS_SIGMA =
            TextKey.of("jsc.service.community.needs_sigma", "%s is a Σ# program; install sigma first");
    private static final TextKey NO_SYSTEM_DISK =
            TextKey.of("jsc.service.community.no_system_disk", "no system disk to install onto");
    private static final TextKey NO_FOLDERS =
            TextKey.of("jsc.service.community.no_folders", "this system has no folders to install into");
    private static final TextKey INSTALLED = TextKey.of("jsc.service.community.installed", "installed %s into %s");
    private static final TextKey REMOVED = TextKey.of("jsc.service.community.removed", "removed %s");

    CommunityPackages(final IComputerTerminalHost terminal, final FileService files, final MirrorService mirror,
                      final BooleanSupplier hasRuntime) {
        this.terminal = terminal;
        this.files = files;
        this.mirror = mirror;
        this.hasRuntime = hasRuntime;
    }

    /** Installs the package a player published under that name, or null when the name belongs to something else. */
    @Nullable
    ICliComputer.OpResult install(final String wanted) {
        final MainframeBlockEntity serving = this.mirror.serving();
        final String held = serving == null ? null : serving.shelvedPackage(wanted);
        if (held == null) {
            return null;
        }
        final Packed packed = Packed.read(held);
        // A player's system is installed by the firmware, which boots its installer from the Mirror.
        if (packed != null && Manifest.SYSTEM.equals(packed.manifest().kind())) {
            return ICliComputer.OpResult.fail(CliTexts.SAID_BY.with(wanted, A_SYSTEM));
        }
        if (packed == null || !packed.problems().isEmpty()) {
            return ICliComputer.OpResult.fail(CliTexts.SAID_BY.with(wanted, UNREADABLE));
        }
        if (!this.hasRuntime.getAsBoolean()) {
            return ICliComputer.OpResult.fail(NEEDS_SIGMA.with(wanted));
        }
        return this.unpack(wanted, packed.manifest(), packed.files());
    }

    /**
     * Lays a player's desktop on this machine as installing its package does, for a system of theirs that starts it:
     * the system brings the desktop it starts. One already installed is left as it is.
     */
    ICliComputer.OpResult lay(final PlayerDesktop desktop) {
        final ComputerConsoleState console = this.terminal.console();
        if (console != null && console.communityProgram(desktop.facts().name()) != null) {
            return ICliComputer.OpResult.ok(INSTALLED.with(desktop.facts().label(), desktop.installedFolder()));
        }
        return this.unpack(desktop.facts().name(), desktop.manifest(), desktop.files());
    }

    /**
     * The packages of players installed here that the Mirror now holds at another version, each against the one
     * installed: publishing a package again under its name is how a player puts out its next version. A copy the
     * machine could not install, unreadable or a system, is no update.
     */
    List<ICliPackages.PackageUpdate> updates() {
        final ComputerConsoleState console = this.terminal.console();
        final MainframeBlockEntity serving = this.mirror.serving();
        if (console == null || serving == null) {
            return List.of();
        }
        final List<ICliPackages.PackageUpdate> out = new ArrayList<>();
        for (final ComputerConsoleState.Community installed : console.community()) {
            final String held = serving.shelvedPackage(installed.name());
            final Packed packed = held == null ? null : Packed.read(held);
            if (packed != null && packed.problems().isEmpty() && !Manifest.SYSTEM.equals(packed.manifest().kind())
                    && !packed.manifest().version().equals(installed.version())) {
                out.add(new ICliPackages.PackageUpdate(installed.name(), installed.name(), installed.version(),
                        packed.manifest().version()));
            }
        }
        return out;
    }

    /** Takes a player's package off this machine, or null when no package of theirs goes by that name here. */
    @Nullable
    ICliComputer.OpResult remove(final String wanted) {
        final ComputerConsoleState console = this.terminal.console();
        if (console == null || console.communityProgram(wanted) == null) {
            return null;
        }
        final String folder = onSystem(folderOf(wanted));
        // Its own files and nothing else: what was written when it was installed.
        for (final ICliComputer.FsEntry file : this.files.listDisk(folder).entries()) {
            // The listing's name is the whole last segment, extension and all.
            this.files.deleteFile(folder + "/" + file.name());
        }
        // And the folder it was installed into, which was its own: left behind, it stood in a reinstall's way.
        this.files.removeDir(folder);
        console.removeCommunity(wanted);
        ((BlockEntity) this.terminal).setChanged();
        return ICliComputer.OpResult.ok(REMOVED.with(wanted));
    }

    /*
     * Writes a package's files into its own folder and records it on the machine. A program is recorded by the listing
     * it runs; a desktop runs nothing, so it is recorded by its folder, which is where the machine draws it from.
     */
    private ICliComputer.OpResult unpack(final String wanted, final Manifest manifest,
                                         final Map<String, String> packedFiles) {
        final ComputerConsoleState console = this.terminal.console();
        if (console == null) {
            return ICliComputer.OpResult.fail(NO_SYSTEM_DISK);
        }
        // Its own folder, made before anything is written into it.
        final String folder = folderOf(wanted);
        final String onDisk = onSystem(folder);
        this.files.makeDir(onSystem(ROOT + Packed.INSTALL_DIR));
        // A folder already there is as good as one made now: it is where the files go either way.
        if (!this.files.makeDir(onDisk).ok() && !this.files.folderExists(onDisk)) {
            return ICliComputer.OpResult.fail(CliTexts.SAID_BY.with(wanted, NO_FOLDERS));
        }
        for (final Map.Entry<String, String> file : packedFiles.entrySet()) {
            // Checked again here, where the files are written, so no name can reach outside the package's folder.
            if (!Packed.isSafeFileName(file.getKey())) {
                return ICliComputer.OpResult.fail(CliTexts.SAID_BY.with(wanted, UNREADABLE));
            }
            final ICliComputer.FsResult written = this.files.installFile(onDisk + "/" + file.getKey(),
                    file.getValue());
            if (!written.ok()) {
                return ICliComputer.OpResult.fail(CliTexts.SAID_BY.with(wanted, written.message()));
            }
        }
        final String entry = Manifest.DESKTOP.equals(manifest.kind()) ? folder : folder + "/" + manifest.entry();
        console.addCommunity(new ComputerConsoleState.Community(wanted, manifest.version(), manifest.house(),
                manifest.icon(), entry));
        ((BlockEntity) this.terminal).setChanged();
        return ICliComputer.OpResult.ok(INSTALLED.with(manifest.label(), folder));
    }

    /*
     * The folder of the package of that name, from the disk's root: where the prompt stands decides nothing, so a
     * POSIX prompt in its home installs where every other machine and the desktop look for it.
     */
    private static String folderOf(final String name) {
        return ROOT + Packed.folderOf(name);
    }

    /*
     * A path from the disk's root, on the system drive whatever drive the prompt stands on: a root path alone keeps
     * the prompt's drive, and a package installed from D: would land where nothing looks for it.
     */
    private static String onSystem(final String fromRoot) {
        return SYSTEM_DRIVE + fromRoot;
    }
}
