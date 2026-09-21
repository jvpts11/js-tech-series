/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import dev.jstech.computers.terminal.IComputerTerminalHost;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * What a terminal does with a line while an ssh session is open: which machine the line goes to, and what the
 * prompt says about it.
 *
 * <p>There are two terminals a player can type at, the whole screen of a monitor and a shell window on a
 * desktop, and both follow this. Held in one place because it was once written in only one of them, and a
 * session opened at the other ran every line on the machine standing in front of the player instead.
 */
public final class SshTerminal {

    private SshTerminal() {
    }

    /**
     * The machine a line goes to, or null for the one the player is sitting at.
     *
     * <p>ssh itself and the words that end a session stay at home whatever is open, or there would be no way
     * back; and a session whose far end has stopped is closed rather than followed.
     */
    @Nullable
    public static IComputerTerminalHost targetOf(final IComputerTerminalHost host, final ServerLevel level,
                                                 final String line) {
        final var console = host.console();
        if (console == null || console.sshTarget() == null) {
            return null;
        }
        final String verb = line.trim().split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
        if (verb.equals("ssh") || verb.equals("exit") || verb.equals("logout")) {
            return null;
        }
        if (level.getBlockEntity(BlockPos.of(console.sshTarget())) instanceof IComputerTerminalHost remote
                && remote.computerRunning()) {
            return remote;
        }
        console.setSshTarget(null);
        return null;
    }

    /**
     * The prompt a terminal shows: the shell's own, and ahead of it the machine the lines are going to while a
     * session is open.
     *
     * <p>Without that the DOS families give nothing away, since their prompt is only the drive and the folder:
     * connected or not it reads the same, and the player has no way to tell the line has left the computer in
     * front of them.
     *
     * @param local   the machine the player is at, which is the one that holds the session
     * @param running the shell the line was run on, which is the far machine while a session is open
     */
    public static String prompt(final ICliComputer local, final ICliComputer running) {
        return promptLine(local, running).text();
    }

    /** The same prompt a run at a time, in the colours the shell it belongs to gives it. */
    public static CliLine promptLine(final ICliComputer local, final ICliComputer running) {
        final String connected = local.sshSession();
        return connected.isEmpty() ? running.promptLine()
                : CliLine.build().add("[" + connected + "] ", CliStyle.ACCENT).add(running.promptLine()).done();
    }
}
