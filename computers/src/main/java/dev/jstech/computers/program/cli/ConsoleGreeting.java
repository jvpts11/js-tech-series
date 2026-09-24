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
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

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
@TextHolder
public final class ConsoleGreeting {

    /** Who is logged in at a console that is no installer's. */
    private static final String PLAYER = "player";

    private static final TextKey CONSOLE_LOGIN = TextKey.of("jsc.cli.greeting.console_login", "%s Console Login:");
    /* The help line of System V, split round the word "help", which is drawn in a colour of its own. */
    private static final TextKey HELP_BEFORE = TextKey.of("jsc.cli.greeting.help_before", "Type ");
    private static final TextKey HELP_AFTER =
            TextKey.of("jsc.cli.greeting.help_after", " for the UNIX system on-line help.");
    private static final TextKey LIVE_BANNER =
            TextKey.of("jsc.cli.greeting.live_banner", "%s installation medium (tty1)");
    private static final TextKey AUTO_LOGIN =
            TextKey.of("jsc.cli.greeting.auto_login", "%s login: root (automatic login)");
    private static final TextKey LIVE_HELP =
            TextKey.of("jsc.cli.greeting.live_help", "Type 'help' for the installation walkthrough.");
    private static final TextKey LOGIN_AS = TextKey.of("jsc.cli.greeting.login_as", "%s login: %s");
    private static final TextKey LOGIN = TextKey.of("jsc.cli.greeting.login", "login:");
    private static final TextKey PASSWORD = TextKey.of("jsc.cli.greeting.password", "Password:");
    private static final TextKey WELCOME = TextKey.of("jsc.cli.greeting.welcome", "Welcome to %s (%s)");
    private static final TextKey WELCOME_PLAYER =
            TextKey.of("jsc.cli.greeting.welcome_player", "Welcome to %s, %s.");

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
                CliLine.of(new CliSpan(CONSOLE_LOGIN.with(console.hostname()), CliStyle.PLAIN),
                        new CliSpan(" ", CliStyle.PLAIN), new CliSpan(PLAYER, CliStyle.BRIGHT)),
                CliLine.of(new CliSpan(HELP_BEFORE.text(), CliStyle.PLAIN), new CliSpan("help", CliStyle.CYAN),
                        new CliSpan(HELP_AFTER.text(), CliStyle.PLAIN)),
                CliLine.plain(""));
    }

    /** An installer medium: its own banner, and root already logged in, as such a medium comes up. */
    private static List<CliLine> live(final ConsoleIdentity console) {
        return List.of(
                new CliLine(LIVE_BANNER.with(console.osLabel()), CliStyle.ACCENT),
                CliLine.plain(""),
                CliLine.plain(AUTO_LOGIN.with(console.hostname())),
                new CliLine(LIVE_HELP.text(), CliStyle.DIM),
                CliLine.plain(""));
    }

    private static List<CliLine> linux(final ConsoleIdentity console) {
        return List.of(
                new CliLine(console.osLabel() + " " + console.hostname() + " "
                        + KernelNames.terminal(Platform.LINUX), CliStyle.ACCENT),
                CliLine.plain(""),
                CliLine.plain(LOGIN_AS.with(console.hostname(), PLAYER)),
                CliLine.plain(PASSWORD.text()),
                new CliLine(WELCOME.with(console.osLabel(), KernelNames.kernel(Platform.LINUX, console.bits())),
                        CliStyle.DIM),
                CliLine.plain(""));
    }

    private static List<CliLine> freeBsd(final ConsoleIdentity console) {
        return List.of(
                CliLine.plain("FreeBSD/" + KernelNames.architecture(Platform.FREEBSD, console.bits())
                        + " (" + console.hostname() + ") (" + KernelNames.terminal(Platform.FREEBSD) + ")"),
                CliLine.plain(""),
                CliLine.of(new CliSpan(LOGIN.text(), CliStyle.PLAIN), new CliSpan(" ", CliStyle.PLAIN),
                        new CliSpan(PLAYER, CliStyle.BRIGHT)),
                CliLine.plain("FreeBSD " + KernelNames.FREEBSD_RELEASE + " (GENERIC)"),
                CliLine.plain(""),
                new CliLine(WELCOME_PLAYER.with("FreeBSD", PLAYER), CliStyle.BRIGHT),
                CliLine.plain(""));
    }
}
