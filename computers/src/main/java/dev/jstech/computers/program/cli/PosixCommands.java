/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import dev.jstech.computers.os.PackageManagerKind;
import dev.jstech.computers.program.cli.man.ManCommands;
import java.util.ArrayList;
import java.util.List;

/**
 * The POSIX command set: what a shell on a {@code ShellFamily.POSIX} kernel (Linux) speaks. Every file verb
 * converts its path arguments through {@link PosixPath#toDos} and then calls the same filesystem facade the
 * DOS shell uses, so the two families share one storage model and differ only in syntax and presentation.
 * The network and program verbs are shared with the DOS set (see {@link BuiltinCommands#shared()}).
 */
public final class PosixCommands {

    private PosixCommands() {
    }

    /** The POSIX-only commands (the shared verbs are added by {@link CliCommands}). */
    public static List<ICliCommand> all() {
        final List<ICliCommand> out = new ArrayList<>(files());
        // The tools that work on lines, which are what a pipe is for.
        out.addAll(PipeCommands.posix());
        // And the ways of finding out what a machine can do at all.
        out.addAll(ManCommands.posix());
        return List.copyOf(out);
    }

    private static List<ICliCommand> files() {
        return List.of(
                new PosixFileCommands.Ls(),
                new PosixFileCommands.Pwd(),
                new PosixFileCommands.Cd(),
                new PosixFileCommands.Cat(),
                new PosixFileCommands.Rm(),
                new PosixFileCommands.Mkdir(),
                new PosixFileCommands.Rmdir(),
                new PosixFileCommands.Cp(),
                new PosixFileCommands.Mv(),
                new PosixFileCommands.Touch(),
                new PosixFileCommands.Write(),
                new PosixFileCommands.Run(),
                new PosixFileCommands.Clear(),
                new PosixFileCommands.Man(),
                new PosixSystemCommands.Uname(),
                new PosixSystemCommands.Hostname(),
                new PosixFileCommands.Df(),
                new PosixFileCommands.Mkfs(),
                new PosixSystemCommands.Screenfetch(),
                // One package manager per distribution family; each is only available on the OS that ships it.
                new PackageCommands.PackageManagerCommand(PackageManagerKind.APT),
                new PackageCommands.PackageManagerCommand(PackageManagerKind.DNF),
                new PackageCommands.PackageManagerCommand(PackageManagerKind.PACMAN),
                new PackageCommands.PackageManagerCommand(PackageManagerKind.EMERGE),
                new PackageCommands.PackageManagerCommand(PackageManagerKind.PKG));
    }

}
