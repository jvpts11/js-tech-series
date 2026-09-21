/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli.man;

import dev.jstech.computers.program.cli.CliContext;
import dev.jstech.computers.program.cli.CommandScope;
import dev.jstech.computers.program.cli.ICliCommand;
import dev.jstech.computers.program.cli.ICliComputer;
import java.util.List;

/**
 * The ways of finding out what a machine can do, each in the words of the family that has it.
 *
 * <p>A player who does not already know a system is the one this whole set is for. Every one of these reads
 * the same pages and the same list of what this machine really offers, so none of them can teach something
 * that is not there.
 */
public final class ManCommands {

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
    static final class Whatis implements ICliCommand {
        @Override public CommandScope scope() {
            return CommandScope.on(CommandScope.UNIX_SYSTEMS);
        }

        @Override public String name() {
            return "whatis";
        }

        @Override public String summary() {
            return "say in one line what a command is for";
        }

        @Override public String usage() {
            return "<command>";
        }

        @Override public List<String> description() {
            return List.of("Prints the one line a manual page opens with, which is enough to tell whether",
                    "the page is worth opening at all.");
        }

        @Override public List<String> seeAlso() {
            return List.of("man", "apropos");
        }

        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs()) {
                ctx.out().error("whatis what?");
                return;
            }
            final ICliCommand found = ctx.shell().find(ctx.arg(0));
            if (found == null || !found.available(ctx.computer())) {
                ctx.out().error(ctx.arg(0) + ": nothing appropriate.");
                return;
            }
            ctx.out().line(ManPage.whatis(found));
        }
    }

    /** {@code apropos}: every page whose one line answers to a word. */
    static final class Apropos implements ICliCommand {
        @Override public CommandScope scope() {
            return CommandScope.on(CommandScope.UNIX_SYSTEMS);
        }

        @Override public String name() {
            return "apropos";
        }

        @Override public String summary() {
            return "search what every command is for";
        }

        @Override public String usage() {
            return "<text>";
        }

        @Override public List<String> description() {
            return List.of("Searches the name and the one-line summary of every page this machine has, and",
                    "prints the ones that answer. It is how to find a command whose name you do not know.");
        }

        @Override public List<ICliCommand.Example> examples() {
            return List.of(new ICliCommand.Example("apropos network", "every command that works the network"));
        }

        @Override public List<String> seeAlso() {
            return List.of("man", "whatis");
        }

        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs()) {
                ctx.out().error("apropos what?");
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
                ctx.out().error(wanted + ": nothing appropriate.");
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
    static final class ListCommands implements ICliCommand {
        @Override public CommandScope scope() {
            return CommandScope.everywhere();
        }

        @Override public String name() {
            return "listcmd";
        }

        @Override public String summary() {
            return "list everything this computer can run";
        }

        @Override public List<String> description() {
            return List.of("Lists every command this computer offers and every program installed on it, in one",
                    "list, whichever system it runs. Nothing here is hidden and nothing is added: it is the",
                    "same answer the prompt itself gives when it decides whether a word is a command.");
        }

        /** Off unless a server turns it on, in which case it is a command like any other. */
        @Override public boolean available(final ICliComputer computer) {
            return computer.listsEverything() && ICliCommand.super.available(computer);
        }

        @Override public void run(final CliContext ctx) {
            ctx.out().header("COMMANDS");
            for (final ICliCommand command : ctx.shell().commands()) {
                if (command.available(ctx.computer())) {
                    ctx.out().row("  " + command.name(), command.summary());
                }
            }
            ctx.out().header("PROGRAMS");
            for (final var program : ctx.computer().programs()) {
                ctx.out().row("  " + program.name(), program.id());
            }
        }
    }

}
