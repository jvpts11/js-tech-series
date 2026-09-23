/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os;

import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.computers.program.cli.CliStyle;
import dev.jstech.core.id.StableNames;
import org.jetbrains.annotations.Nullable;

/**
 * Who a console says it is when somebody sits down at it: the shell, the machine's name, the system and what
 * the system runs on.
 *
 * <p>Worked out on the machine and handed to the screen in one piece, so the screen can greet and draw its
 * first prompt before the machine has answered anything, and so it never has to guess at the family from the
 * name a system happens to go by.
 *
 * @param shell    the shell a system met at a Unix prompt runs, or null for every other family and for an
 *                 installer medium, whose prompt is the medium's own
 * @param live     whether this is the console of a booted installer medium rather than of an installed system
 * @param hostname the name the machine answers to, empty where the family has no such thing
 * @param osLabel  what the system is called, empty when there is none
 * @param platform the family the system belongs to, or null when there is no system
 * @param bits     how wide the processor's word is, which is what the family names its architecture after
 */
public record ConsoleIdentity(@Nullable ShellKind shell, boolean live, String hostname, String osLabel,
                              @Nullable Platform platform, int bits) {

    /** A console with nothing to say about itself. */
    public static final ConsoleIdentity NONE = new ConsoleIdentity(null, false, "", "", null, 0);

    /** Who a FreeBSD prompt says is logged in, before the machine's name. */
    private static final String ROOT = "root@";

    private static final StableNames<Platform> PLATFORMS = StableNames.of(Platform.class);
    private static final StableNames<ShellKind> SHELLS = StableNames.of(ShellKind.class);

    public ConsoleIdentity {
        hostname = hostname == null ? "" : hostname;
        osLabel = osLabel == null ? "" : osLabel;
        bits = Math.max(0, bits);
    }

    /** The same console as it comes off the wire, where the shell and the family travel by name, none as empty. */
    public static ConsoleIdentity ofWire(final String shellName, final boolean live, final String hostname,
                                         final String osLabel, final String platformName, final int bits) {
        return new ConsoleIdentity(SHELLS.find(shellName), live, hostname, osLabel, PLATFORMS.find(platformName),
                bits);
    }

    /**
     * The prompt that shell stands at in that directory, for whoever is logged in on that machine.
     *
     * <p>Each shell draws its own ({@link ShellKind#prompt}); a console that names none gets bash's, the plainest.
     * FreeBSD is met as root, as a machine fresh from its installer is: its sh stands at root's hash. Said once
     * here because the machine writes it on every line and the screen writes it once before the machine has
     * answered, and the two must not differ.
     */
    public static String promptOf(@Nullable final Platform platform, @Nullable final ShellKind shell,
                                  final String hostname, final String cwd) {
        if (platform == Platform.FREEBSD) {
            return ROOT + hostname + ":" + cwd + " #";
        }
        return (shell == null ? ShellKind.BASH : shell).prompt(hostname, cwd);
    }

    /**
     * The same prompt as the glass shows it. FreeBSD's has root and the machine's name in red and the rest in a
     * prompt's ink; every other one is in the one colour a terminal gives a prompt.
     */
    public static CliLine promptLineOf(@Nullable final Platform platform, @Nullable final ShellKind shell,
                                       final String hostname, final String cwd) {
        if (platform == Platform.FREEBSD) {
            return CliLine.build().add(ROOT + hostname, CliStyle.RED).add(":" + cwd + " #", CliStyle.PROMPT).done();
        }
        return new CliLine(promptOf(platform, shell, hostname, cwd), CliStyle.ACCENT);
    }

    /** The shell's name for the wire, empty when there is none. */
    public String shellName() {
        return this.shell == null ? "" : this.shell.serializedName();
    }

    /** The family's name for the wire, empty when there is no system. */
    public String platformName() {
        return this.platform == null ? "" : this.platform.serializedName();
    }

    /** Whether this is a Unix prompt, which wears a login banner and a prompt made of a name and a path. */
    public boolean posix() {
        return this.live || this.shell != null;
    }
}
