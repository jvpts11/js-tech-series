/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import dev.jstech.computers.program.install.LiveInstallState;

import java.util.List;

/**
 * The command set of a live installation medium (the Arch ISO, the Gentoo live CD): every verb of the manual
 * install sequence, each forwarded as its full line to the computer's {@link LiveInstallState}, which
 * validates the order and answers with the real tool's output. {@code clear} and {@code help} round it out.
 */
public final class LiveInstallCommands {

    private LiveInstallCommands() {
    }

    /*
     * Every verb the sequence answers to. A verb the state machine knows and this list does not is a verb
     * nobody can type, since the shell turns an unlisted word away before the state machine ever sees it:
     * anything added there has to be added here in the same breath.
     */
    private static final String[] VERBS = {
            "ls", "cat", "less", "more", "cd",
            "lsblk", "fdisk", "mkfs.ext4", "mkfs", "mkfs.fat", "mkfs.vfat", "mount",
            "pacstrap", "tar", "genfstab", "arch-chroot", "chroot",
            "emerge-webrsync", "emerge", "genkernel", "hostname", "mkinitcpio",
            "grub-install", "grub-mkconfig", "passwd", "exit", "reboot", "help"};

    public static List<ICliCommand> all() {
        final List<ICliCommand> out = new java.util.ArrayList<>();
        for (final String verb : VERBS) {
            out.add(new LiveVerb(verb));
        }
        out.add(new Clear());
        return out;
    }

    /** One install verb; the state machine decides what it does at this point of the sequence. */
    static final class LiveVerb implements ICliCommand {
        private final String verb;

        LiveVerb(final String verb) {
            this.verb = verb;
        }

        @Override public String name() { return verb; }

        @Override public String summary() { return "live installer: " + verb; }

        @Override public void run(final CliContext ctx) {
            final String line = ctx.hasArgs() ? verb + " " + ctx.rest(0) : verb;
            final ICliComputer.OpResult result = ctx.computer().liveRun(line);
            for (final String out : result.message().split("\n", -1)) {
                if (out.isEmpty()) {
                    continue;
                }
                if (result.ok()) {
                    ctx.out().line(out);
                } else {
                    ctx.out().error(out);
                }
            }
        }
    }

    static final class Clear implements ICliCommand, CliShell.IClearMarker {
        @Override public String name() { return "clear"; }

        @Override public String summary() { return "clear the terminal"; }

        @Override public void run(final CliContext ctx) {
        }
    }
}
