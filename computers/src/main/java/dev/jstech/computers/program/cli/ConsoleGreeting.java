/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import dev.jstech.computers.os.ConsoleIdentity;
import dev.jstech.computers.os.KernelNames;
import dev.jstech.computers.os.Platform;

import java.util.List;

/**
 * What a Unix console says when somebody sits down at it: the line the terminal names itself with, the login,
 * and the system's word of welcome.
 *
 * <p>Each family says it in a shape of its own, and that shape is most of how one is told from another before a
 * single command is typed: a Linux names the system, the machine and the terminal and welcomes with its kernel
 * in brackets, where FreeBSD opens with its name over its architecture, puts the machine in brackets, and gives
 * its release on a line of its own after the login, and System V says the least of the three: the machine, the
 * login, and where its help is.
 */
public final class ConsoleGreeting {

    private ConsoleGreeting() {
    }

    /** The lines of that console's greeting, top to bottom, or none for a console that is no Unix prompt. */
    public static List<CliLine> of(final ConsoleIdentity console) {
        if (!console.posix()) {
            return List.of();
        }
        if (console.live()) {
            return live(console);
        }
        if (console.platform() == Platform.UNIX) {
            return systemV(console);
        }
        return console.platform() == Platform.FREEBSD ? freeBsd(console) : linux(console);
    }

    /** System V: the machine's name before the login on one line, and where its own help is on the next. */
    private static List<CliLine> systemV(final ConsoleIdentity console) {
        return List.of(
                CliLine.of(new CliSpan(console.hostname() + " Console Login: ", CliStyle.PLAIN),
                        new CliSpan("player", CliStyle.BRIGHT)),
                CliLine.of(new CliSpan("Type ", CliStyle.PLAIN), new CliSpan("help", CliStyle.CYAN),
                        new CliSpan(" for the UNIX system on-line help.", CliStyle.PLAIN)),
                CliLine.plain(""));
    }

    /** An installer medium: its own banner, and root already logged in, as such a medium comes up. */
    private static List<CliLine> live(final ConsoleIdentity console) {
        return List.of(
                new CliLine(console.osLabel() + " installation medium (tty1)", CliStyle.ACCENT),
                CliLine.plain(""),
                CliLine.plain(console.hostname() + " login: root (automatic login)"),
                new CliLine("Type 'help' for the installation walkthrough.", CliStyle.DIM),
                CliLine.plain(""));
    }

    private static List<CliLine> linux(final ConsoleIdentity console) {
        return List.of(
                new CliLine(console.osLabel() + " " + console.hostname() + " "
                        + KernelNames.terminal(Platform.LINUX), CliStyle.ACCENT),
                CliLine.plain(""),
                CliLine.plain(console.hostname() + " login: player"),
                CliLine.plain("Password:"),
                new CliLine("Welcome to " + console.osLabel() + " ("
                        + KernelNames.kernel(Platform.LINUX, console.bits()) + ")", CliStyle.DIM),
                CliLine.plain(""));
    }

    private static List<CliLine> freeBsd(final ConsoleIdentity console) {
        return List.of(
                CliLine.plain("FreeBSD/" + KernelNames.architecture(Platform.FREEBSD, console.bits())
                        + " (" + console.hostname() + ") (" + KernelNames.terminal(Platform.FREEBSD) + ")"),
                CliLine.plain(""),
                CliLine.of(new CliSpan("login: ", CliStyle.PLAIN), new CliSpan("player", CliStyle.BRIGHT)),
                CliLine.plain("FreeBSD " + KernelNames.FREEBSD_RELEASE + " (GENERIC)"),
                CliLine.plain(""),
                new CliLine("Welcome to FreeBSD, player.", CliStyle.BRIGHT),
                CliLine.plain(""));
    }
}
