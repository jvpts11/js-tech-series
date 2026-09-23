/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import dev.jstech.computers.os.KernelNames;
import dev.jstech.computers.os.Platform;
import java.util.ArrayList;
import java.util.List;

/**
 * What the Unix systems say about themselves: {@code uname} and {@code hostname}.
 *
 * <p>Moved here from PosixCommands, which keeps the lists that say which system gets which command. screenfetch,
 * which also runs at Frames' Command Prompt, is a command of its own, {@link ScreenfetchCommand}.
 */
final class PosixSystemCommands {

    private PosixSystemCommands() {
    }

    static final class Uname implements ICliCommand {

        /** Every letter the tool takes: all of it, the kernel, the machine's name, the release, the architecture. */
        private static final String LETTERS = "asnrm";

        @Override public CommandScope scope() {
            return CommandScope.on(CommandScope.UNIX_SYSTEMS);
        }

        @Override public String name() { return "uname"; }

        @Override public String summary() { return "print system information"; }

        @Override public String usage() { return "[-a|-s|-r|-m|-sr]"; }

        /*
         * The letters may come together or apart, as they always could: -sr is -s -r. What is printed is in
         * the order the tool has always printed it, whatever order it was asked in.
         */
        @Override public void run(final CliContext ctx) {
            final Platform platform = ctx.computer().platform();
            final int bits = ctx.computer().processorBits();
            final StringBuilder asked = new StringBuilder();
            for (int i = 0; i < ctx.argCount(); i++) {
                if (!ctx.arg(i).startsWith("-") || ctx.arg(i).length() < 2) {
                    ctx.out().error("uname: extra operand '" + ctx.arg(i) + "'");
                    return;
                }
                asked.append(ctx.arg(i).substring(1));
            }
            for (int i = 0; i < asked.length(); i++) {
                if (LETTERS.indexOf(asked.charAt(i)) < 0) {
                    ctx.out().error("uname: invalid option -- '" + asked.charAt(i) + "'");
                    return;
                }
            }
            if (asked.indexOf("a") >= 0) {
                ctx.out().line(KernelNames.everything(platform, ctx.computer().hostname(), bits));
                return;
            }
            // Asked nothing, it names the kernel, which is what -s alone says.
            final String wanted = asked.isEmpty() ? "s" : asked.toString();
            final List<String> said = new ArrayList<>();
            if (wanted.indexOf('s') >= 0) {
                said.add(KernelNames.name(platform));
            }
            if (wanted.indexOf('n') >= 0) {
                said.add(ctx.computer().hostname());
            }
            if (wanted.indexOf('r') >= 0) {
                said.add(KernelNames.release(platform));
            }
            if (wanted.indexOf('m') >= 0) {
                said.add(KernelNames.architecture(platform, bits));
            }
            ctx.out().line(String.join(" ", said));
        }
    }

    static final class Hostname implements ICliCommand {
        @Override public CommandScope scope() {
            return CommandScope.on(CommandScope.UNIX_SYSTEMS);
        }

        @Override public String name() { return "hostname"; }

        @Override public CommandGroup group() { return CommandGroup.MACHINE; }

        @Override public String summary() { return "print this computer's host name"; }

        @Override public void run(final CliContext ctx) {
            ctx.out().line(ctx.computer().hostname());
        }
    }
}
