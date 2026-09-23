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
 * @param shellId  the shell a system met at a Unix prompt runs, {@code live} on an installer medium, and empty
 *                 for every other family
 * @param hostname the name the machine answers to, empty where the family has no such thing
 * @param osLabel  what the system is called, empty when there is none
 * @param platform the family the system belongs to, or null when there is no system
 * @param bits     how wide the processor's word is, which is what the family names its architecture after
 */
public record ConsoleIdentity(String shellId, String hostname, String osLabel, @Nullable Platform platform,
                              int bits) {

    /** A console with nothing to say about itself. */
    public static final ConsoleIdentity NONE = new ConsoleIdentity("", "", "", null, 0);

    /** The shell id an installer medium's console goes by. */
    public static final String LIVE = "live";

    /** Who a FreeBSD prompt says is logged in, before the machine's name. */
    private static final String ROOT = "root@";

    private static final StableNames<Platform> PLATFORMS = StableNames.of(Platform.class);

    public ConsoleIdentity {
        shellId = shellId == null ? "" : shellId;
        hostname = hostname == null ? "" : hostname;
        osLabel = osLabel == null ? "" : osLabel;
        bits = Math.max(0, bits);
    }

    /** The same console as it comes off the wire, where the family travels by name and none is an empty one. */
    public static ConsoleIdentity ofWire(final String shellId, final String hostname, final String osLabel,
                                         final String platformName, final int bits) {
        return new ConsoleIdentity(shellId, hostname, osLabel, PLATFORMS.find(platformName), bits);
    }

    /**
     * The prompt that shell stands at in that directory, for whoever is logged in on that machine.
     *
     * <p>Each shell has a shape of its own: zsh ends in a percent sign with spaces about the path, bash runs the
     * path up against its dollar sign, and System V's sh leaves a space before the dollar. FreeBSD is met as
     * root, as a machine fresh from its installer is: its sh stands at root's hash. Said once here because the
     * machine writes it on every line and the screen writes it once before the machine has answered, and the two
     * must not differ.
     */
    public static String promptOf(@Nullable final Platform platform, final String shellId, final String hostname,
                                  final String cwd) {
        if (platform == Platform.FREEBSD) {
            return ROOT + hostname + ":" + cwd + " #";
        }
        return switch (shellId) {
            case "zsh" -> "player@" + hostname + " " + cwd + " %";
            case "sh" -> "player@" + hostname + ":" + cwd + " $";
            default -> "player@" + hostname + ":" + cwd + "$";
        };
    }

    /**
     * The same prompt as the glass shows it. FreeBSD's has root and the machine's name in red and the rest in a
     * prompt's ink; every other one is in the one colour a terminal gives a prompt.
     */
    public static CliLine promptLineOf(@Nullable final Platform platform, final String shellId,
                                       final String hostname, final String cwd) {
        if (platform == Platform.FREEBSD) {
            return CliLine.build().add(ROOT + hostname, CliStyle.RED).add(":" + cwd + " #", CliStyle.PROMPT).done();
        }
        return new CliLine(promptOf(platform, shellId, hostname, cwd), CliStyle.ACCENT);
    }

    /** The family's name for the wire, empty when there is no system. */
    public String platformName() {
        return this.platform == null ? "" : this.platform.serializedName();
    }

    /** Whether this is a Unix prompt, which wears a login banner and a prompt made of a name and a path. */
    public boolean posix() {
        return !this.shellId.isEmpty();
    }

    /** Whether this is the console of a booted installer medium rather than of an installed system. */
    public boolean live() {
        return LIVE.equals(this.shellId);
    }
}
