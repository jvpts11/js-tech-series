/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import dev.jstech.computers.program.cli.interac.InteracCommand;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The shell verbs that ship with the mod. Each is a small, self-contained {@link ICliCommand}; add-ons add their own the same way. They talk only to the {@link ICliComputer} facade, so the whole set is exercised in unit tests against a fake computer.
 */
public final class BuiltinCommands {

    private BuiltinCommands() {
    }

    /** Every built-in command, in the order they appear in {@code help}. */
    /** The DOS-only verbs: everything else in {@link #all()} is shared with the POSIX shell. */
    private static final Set<String> DOS_ONLY = Set.of(
            "cls", "dir", "cd", "type", "del", "write", "run", "mkdir", "rmdir", "copy", "move", "ren",
            // The POSIX shell formats with mkfs and removes packages through its package manager.
            "format", "uninstall",
            /*
             * pckmgr is the Frames package manager: a Linux distribution keeps apt/dnf/pacman/emerge,
             * and offering both on the same shell would be two doors to one room.
             */
            "pckmgr");

    /** The verbs both shell families share (network, programs, config, maintenance); no DOS file verbs. */
    public static List<ICliCommand> shared() {
        final List<ICliCommand> out = new ArrayList<>();
        for (final ICliCommand command : all()) {
            if (!DOS_ONLY.contains(command.name())) {
                out.add(command);
            }
        }
        return out;
    }

    public static List<ICliCommand> all() {
        final List<ICliCommand> out = new ArrayList<>(base());
        /*
         * The toolchain's verbs come from the toolchain, so adding one there is enough to have it. Two
         * lists of the same commands is two lists that eventually disagree, and the one that loses is
         * always the one a player types into.
         */
        out.addAll(SigmaCommands.all());
        // The DOS family's own tools for working on lines, which its pipes feed the same way.
        out.addAll(PipeCommands.dos());
        return List.copyOf(out);
    }

    private static List<ICliCommand> base() {
        return List.of(
                new ShellCommands.Help(),
                new ShellCommands.Clear(),
                new ShellCommands.Echo(),
                new ShellCommands.Version(),
                new ShellCommands.Whoami(),
                new MachineCommands.Status(),
                new NetworkCommands.Net(),
                /*
                 * What the network holds and what it is doing are words of interac now, not seven loose verbs
                 * of their own: one program for the network, the way one program is what a player opens for it
                 * on a desktop. IQL stays beside it for whoever wants to write a statement out.
                 */
                new NetworkCommands.Operation(),
                new InteracCommand(),
                new MachineCommands.Devices(),
                new NetworkCommands.Ssh(),
                new ShellCommands.Exit(),
                new SoftwareCommands.Pckmgr(),
                new SoftwareCommands.ProgramsList(),
                new SoftwareCommands.Install(),
                new SoftwareCommands.Store(),
                new SoftwareCommands.IqlEngineCommand(),
                new SoftwareCommands.Services(),
                new SoftwareCommands.Maint("analyze", "analyze"),
                new SoftwareCommands.Maint("reindex", "reindex"),
                new SoftwareCommands.Maint("vacuum", "vacuum"),
                new MachineCommands.Config(),
                new MachineCommands.Reboot(),
                new ClusterCommand(),
                new GatewayCommand(),
                new SoftwareCommands.MirrorCommand(),
                new SoftwareCommands.Uninstall(),
                new DosFileCommands.Format(),
                new DosFileCommands.Dir(),
                new DosFileCommands.Cd(),
                new DosFileCommands.Type(),
                new DosFileCommands.Del(),
                new DosFileCommands.Write(),
                new DosFileCommands.Run(),
                new DosFileCommands.Mkdir(),
                new DosFileCommands.Rmdir(),
                new DosFileCommands.Copy(),
                new DosFileCommands.Move(),
                new DosFileCommands.Ren(),
                /*
                 * The editors that take the terminal rather than opening a window. They are verbs like
                 * any other, so they are listed by help and gated by whether the machine has them,
                 * which is what lets a headless server be programmed at all.
                 */
                new TtyEditorCommand("vim", "edit a file in the terminal", "vim"),
                new TtyEditorCommand("emacs", "edit a file in the terminal", "emacs"));
    }

    // meta

    // this computer

    // storage

    // operations

    // filesystem

}
