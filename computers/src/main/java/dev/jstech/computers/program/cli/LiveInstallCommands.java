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
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

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
            out.add(verb.equals(LiveEditor.VERB) ? new LiveEditor() : new LiveVerb(verb));
        }
        out.add(new Clear());
        return out;
    }

    /**
     * The medium's editor, which gives the terminal away on a file of the session.
     *
     * <p>It is run like any other verb, so that a name it cannot open is refused in words, and then the file
     * is named to the terminal with the mark that says it is the session's and not the disk's.
     */
    static final class LiveEditor extends LiveVerb implements CliShell.IHandOver {

        static final String VERB = "nano";

        LiveEditor() {
            super(VERB);
        }

        /** An editor, and editors stand with the rest of what a program is written with. */
        @Override public CommandGroup group() {
            return CommandGroup.PROGRAMMING;
        }

        @Override
        public String fileOf(final ICliComputer computer, final List<String> args) {
            final LiveInstallState live = computer.liveInstall();
            final String file = live == null ? null : live.editable(args);
            return file == null ? null : LiveInstallState.FILE_SCHEME + file;
        }
    }

    /** One install verb; the state machine decides what it does at this point of the sequence. */
    @TextHolder
    static class LiveVerb implements ICliCommand {

        private final String verb;

        /* The verb goes in as it is typed: it is the tool's own name, the same in every language. */
        private static final TextKey SUMMARY = TextKey.of("jsc.cli.live.verb.summary", "live installer: %s");

        LiveVerb(final String verb) {
            this.verb = verb;
        }

        /** The live installer is a shell of its own, swapped in whole, so its words exist wherever it is booted. */
        @Override public CommandScope scope() {
            return CommandScope.everywhere();
        }

        @Override public String name() { return verb; }

        /** A step of putting a system on the machine. */
        @Override public CommandGroup group() {
            return CommandGroup.MACHINE;
        }

        @Override public Text summary() { return SUMMARY.with(verb); }

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

    @TextHolder
    static final class Clear implements ICliCommand, CliShell.IClearMarker {

        private static final TextKey SUMMARY = TextKey.of("jsc.cli.live.clear.summary", "clear the terminal");

        @Override public CommandScope scope() {
            return CommandScope.everywhere();
        }

        @Override public String name() { return "clear"; }

        @Override public CommandGroup group() { return CommandGroup.MACHINE; }

        @Override public Text summary() { return SUMMARY.text(); }

        @Override public void run(final CliContext ctx) {
        }
    }
}
