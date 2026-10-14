/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import dev.jstech.computers.machine.MachinePrograms;
import dev.jstech.computers.operation.payload.program.TerminalTools;
import dev.jstech.computers.program.ComputerConsoleState;
import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.computers.program.tty.ITtyProcess;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * One terminal a {@link TerminalFeed} keeps moving, which is everything the feed needs to know about the
 * machine behind it.
 *
 * <p>A computer has one terminal. A rack has one for every server mounted in it, and only the one its switch
 * is turned to has anybody looking at it, but the tool in front of any of them has to keep running.
 */
interface IFedTerminal {

    /** The console the terminal belongs to, or null when the machine has none just now. */
    @Nullable
    ComputerConsoleState console();

    /** The programs the machine is running, one of which may be in front of the terminal; null when it runs none. */
    @Nullable
    MachinePrograms programs();

    /** Whoever has this terminal on screen; the machine's own list, to be read and not kept. */
    List<ServerPlayer> watching(ServerLevel level);

    /** The prompt its shell would show, a run at a time, for giving it back when a tool ends. */
    CliLine prompt();

    /** Makes again the tool that line started, for one that was running when the world was saved. */
    @Nullable
    ITtyProcess remake(ServerLevel level, String line);

    /** Says the machine's state changed and wants saving. */
    void changed();

    /** The terminal of a computer, which has the one. */
    static IFedTerminal of(final AbstractComputerBlockEntity machine) {
        return new IFedTerminal() {
            @Override
            public ComputerConsoleState console() {
                return machine.console();
            }

            @Override
            public MachinePrograms programs() {
                return machine.programs();
            }

            @Override
            public List<ServerPlayer> watching(final ServerLevel level) {
                return machine.consoleViewers(level);
            }

            @Override
            public CliLine prompt() {
                return machine.shellPromptLine();
            }

            @Override
            public ITtyProcess remake(final ServerLevel level, final String line) {
                return machine instanceof IComputerTerminalHost host ? TerminalTools.remake(host, level, line) : null;
            }

            @Override
            public void changed() {
                machine.setChanged();
            }
        };
    }
}
