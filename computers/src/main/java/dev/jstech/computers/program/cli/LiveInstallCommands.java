/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import dev.jstech.computers.program.install.LiveInstallState;
import dev.jstech.computers.program.install.LiveTurn;

import java.util.ArrayList;
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
     * The verbs come from the sequence itself, which is the thing that answers them. The shell turns an
     * unlisted word away before the state machine ever sees it, so a verb listed in one place and not the
     * other is a verb nobody can type; reading the one list means there is no other place to forget.
     */
    public static List<ICliCommand> all() {
        final List<ICliCommand> out = new ArrayList<>();
        for (final String verb : LiveInstallState.VERBS) {
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
            final LiveTurn turn = ctx.computer().liveRun(line);
            /*
             * Every line as the tool wrote it, the blank ones included: the gaps in a real tool's output are
             * part of how it reads, and a refusal arrives already in the colour a refusal is.
             */
            for (final CliLine said : turn.lines()) {
                ctx.out().line(said);
            }
            // A step that takes time holds the terminal from here until it is over.
            if (turn.tool() != null) {
                ctx.out().start(turn.tool());
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
