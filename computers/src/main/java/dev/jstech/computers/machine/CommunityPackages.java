/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.program.ComputerConsoleState;
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.computers.sigma.pack.Packed;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import java.util.function.BooleanSupplier;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * The packages players publish on the Mirror, as a machine installs and removes them.
 *
 * <p>Each is unpacked into a folder of its own, named after the package, so two of them cannot quietly overwrite
 * each other's files and removing one takes exactly its own files with it.
 */
final class CommunityPackages {

    private final IComputerTerminalHost terminal;
    private final FileService files;
    private final MirrorService mirror;
    /** Whether the Σ# runtime is on this machine, which is what a player's package needs to run. */
    private final BooleanSupplier hasRuntime;

    /** The folder the packages are unpacked into, one folder each. */
    private static final String DIR = "PROGRAMS";

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
        if (packed == null || !packed.problems().isEmpty()) {
            return ICliComputer.OpResult.fail(wanted + ": the Mirror's copy of this package is not readable");
        }
        if (!this.hasRuntime.getAsBoolean()) {
            return ICliComputer.OpResult.fail(wanted + " is a Σ# program; install sigma first");
        }
        final ComputerConsoleState console = this.terminal.console();
        if (console == null) {
            return ICliComputer.OpResult.fail("no system disk to install onto");
        }
        // Its own folder, made before anything is written into it.
        final String folder = DIR + "/" + wanted;
        this.files.makeDir(DIR);
        // A folder already there is as good as one made now: it is where the files go either way.
        if (!this.files.makeDir(folder).ok() && !this.files.folderExists(folder)) {
            return ICliComputer.OpResult.fail(wanted + ": this system has no folders to install into");
        }
        for (final var file : packed.files().entrySet()) {
            final ICliComputer.FsResult written = this.files.writeFile(folder + "/" + file.getKey(), file.getValue());
            if (!written.ok()) {
                return ICliComputer.OpResult.fail(wanted + ": " + written.message());
            }
        }
        console.addCommunity(new ComputerConsoleState.Community(
                wanted, packed.manifest().version(), packed.manifest().house(),
                packed.manifest().icon(), folder + "/" + packed.manifest().entry()));
        ((BlockEntity) this.terminal).setChanged();
        return ICliComputer.OpResult.ok("installed " + packed.manifest().label() + " into " + folder);
    }

    /** Takes a player's package off this machine, or null when no package of theirs goes by that name here. */
    @Nullable
    ICliComputer.OpResult remove(final String wanted) {
        final ComputerConsoleState console = this.terminal.console();
        if (console == null || console.communityProgram(wanted) == null) {
            return null;
        }
        // Its own files and nothing else: what was written when it was installed.
        for (final ICliComputer.FsEntry file : this.files.listDisk(DIR + "/" + wanted).entries()) {
            // The listing's name is the whole last segment, extension and all.
            this.files.deleteFile(DIR + "/" + wanted + "/" + file.name());
        }
        // And the folder it was installed into, which was its own: left behind, it stood in a reinstall's way.
        this.files.removeDir(DIR + "/" + wanted);
        console.removeCommunity(wanted);
        ((BlockEntity) this.terminal).setChanged();
        return ICliComputer.OpResult.ok("removed " + wanted);
    }
}
