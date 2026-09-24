/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli.man;

import dev.jstech.computers.program.cli.CliContext;
import dev.jstech.computers.program.cli.CommandGroup;
import dev.jstech.computers.program.cli.CommandScope;
import dev.jstech.computers.program.cli.ICliCommand;
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;

/**
 * The ways of finding out what a machine can do, each in the words of the family that has it.
 *
 * <p>A player who does not already know a system is the one this whole set is for. Every one of these reads
 * the same pages and the same list of what this machine really offers, so none of them can teach something
 * that is not there.
 */
@TextHolder
public final class ManCommands {

    private static final TextKey NOTHING_APPROPRIATE =
            TextKey.of("jsc.cli.man.nothing_appropriate", "%s: nothing appropriate.");

    private ManCommands() {
    }

    /** The Unix family's, which is where these names come from. */
    public static List<ICliCommand> posix() {
        return List.of(new Whatis(), new Apropos(), new ListCommands());
    }

    /** The one the DOS family has, under the name it uses. */
    public static List<ICliCommand> dos() {
        return List.of(new ListCommands());
    }

    /** {@code whatis}: the one line a page opens with. */
    @TextHolder
    static final class Whatis implements ICliCommand {

        private static final TextKey SUMMARY =
                TextKey.of("jsc.cli.man.whatis.summary", "say in one line what a command is for");
        private static final TextKey USAGE = TextKey.of("jsc.cli.man.whatis.usage", "<command>");
        private static final TextKey ABOUT = TextKey.of("jsc.cli.man.whatis.about", "Prints the one line a manual"
                + " page opens with, which is enough to tell whether the page is worth opening at all.");
        private static final TextKey WHAT = TextKey.of("jsc.cli.man.whatis.what", "whatis what?");

        @Override public CommandScope scope() {
            return CommandScope.on(CommandScope.UNIX_SYSTEMS);
        }

        @Override public String name() {
            return "whatis";
        }

        @Override public CommandGroup group() {
            return CommandGroup.HELP;
        }

        @Override public Text summary() {
            return SUMMARY.text();
        }

        @Override public Text usage() {
            return USAGE.text();
        }

        @Override public List<Text> description() {
            return List.of(ABOUT.text());
        }

        @Override public List<String> seeAlso() {
            return List.of("man", "apropos");
        }

        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs()) {
                ctx.out().error(WHAT);
                return;
            }
            final ICliCommand found = ctx.shell().find(ctx.arg(0));
            if (found == null || !found.available(ctx.computer())) {
                ctx.out().error(NOTHING_APPROPRIATE.with(ctx.arg(0)));
                return;
            }
            ctx.out().line(ManPage.whatis(found));
        }
    }

    /** {@code apropos}: every page whose one line answers to a word. */
    @TextHolder
    static final class Apropos implements ICliCommand {

        private static final TextKey SUMMARY =
                TextKey.of("jsc.cli.man.apropos.summary", "search what every command is for");
        private static final TextKey USAGE = TextKey.of("jsc.cli.man.apropos.usage", "<text>");
        private static final TextKey ABOUT = TextKey.of("jsc.cli.man.apropos.about", "Searches the name and the"
                + " one-line summary of every page this machine has, and prints the ones that answer. It is how to"
                + " find a command whose name you do not know.");
        private static final TextKey NETWORK_EXAMPLE =
                TextKey.of("jsc.cli.man.apropos.example.network", "every command that works the network");
        private static final TextKey WHAT = TextKey.of("jsc.cli.man.apropos.what", "apropos what?");

        @Override public CommandScope scope() {
            return CommandScope.on(CommandScope.UNIX_SYSTEMS);
        }

        @Override public String name() {
            return "apropos";
        }

        @Override public CommandGroup group() {
            return CommandGroup.HELP;
        }

        @Override public Text summary() {
            return SUMMARY.text();
        }

        @Override public Text usage() {
            return USAGE.text();
        }

        @Override public List<Text> description() {
            return List.of(ABOUT.text());
        }

        @Override public List<ICliCommand.Example> examples() {
            return List.of(new ICliCommand.Example("apropos network", NETWORK_EXAMPLE));
        }

        @Override public List<String> seeAlso() {
            return List.of("man", "whatis");
        }

        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs()) {
                ctx.out().error(WHAT);
                return;
            }
            final String wanted = ctx.rest(0);
            int found = 0;
            for (final ICliCommand command : ctx.shell().commands()) {
                if (command.available(ctx.computer()) && ManPage.answersTo(command, wanted)) {
                    ctx.out().line(ManPage.whatis(command));
                    found++;
                }
            }
            if (found == 0) {
                ctx.out().error(NOTHING_APPROPRIATE.with(wanted));
            }
        }
    }

    /**
     * {@code listcmd}: absolutely everything this computer can run right now.
     *
     * <p>Every system here teaches what it has in its own way, and each of those ways is a little of the whole.
     * This is the one word that is the same everywhere and hides nothing: the commands, whatever family they
     * belong to, and the programs installed beside them. It is off unless a server turns it on, so that a
     * player who wants each system's own experience keeps it.
     */
    @TextHolder
    static final class ListCommands implements ICliCommand {

        private static final TextKey SUMMARY =
                TextKey.of("jsc.cli.man.listcmd.summary", "list everything this computer can run");
        private static final TextKey ABOUT = TextKey.of("jsc.cli.man.listcmd.about", "Lists every command this"
                + " computer offers and every program installed on it, in one list, whichever system it runs."
                + " Nothing here is hidden and nothing is added: it is the same answer the prompt itself gives"
                + " when it decides whether a word is a command.");
        private static final TextKey COMMANDS = TextKey.of("jsc.cli.man.listcmd.commands", "COMMANDS");
        private static final TextKey PROGRAMS = TextKey.of("jsc.cli.man.listcmd.programs", "PROGRAMS");

        @Override public CommandScope scope() {
            return CommandScope.everywhere();
        }

        @Override public String name() {
            return "listcmd";
        }

        @Override public CommandGroup group() {
            return CommandGroup.HELP;
        }

        @Override public Text summary() {
            return SUMMARY.text();
        }

        @Override public List<Text> description() {
            return List.of(ABOUT.text());
        }

        /** Off unless a server turns it on, in which case it is a command like any other. */
        @Override public boolean available(final ICliComputer computer) {
            return computer.listsEverything() && ICliCommand.super.available(computer);
        }

        @Override public void run(final CliContext ctx) {
            ctx.out().header(COMMANDS);
            for (final ICliCommand command : ctx.shell().commands()) {
                if (command.available(ctx.computer())) {
                    ctx.out().row(Text.literal("  " + command.name()), command.summary());
                }
            }
            ctx.out().header(PROGRAMS);
            for (final var program : ctx.computer().programs()) {
                ctx.out().row("  " + program.name(), program.id());
            }
        }
    }

}
