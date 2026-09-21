/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import dev.jstech.computers.program.ComputerConsoleState;
import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.computers.program.tty.ITtyProcess;
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

    /** Whoever has this terminal on screen; the machine's own list, to be read and not kept. */
    List<ServerPlayer> watching(ServerLevel level);

    /** The prompt its shell would show, a run at a time, for giving it back when a tool ends. */
    CliLine prompt();

    /** Makes again the tool that line started, for one that was running when the world was saved. */
    @Nullable
    ITtyProcess remake(ServerLevel level, String line);

    /** Says the machine's state changed and wants saving. */
    void changed();
}
