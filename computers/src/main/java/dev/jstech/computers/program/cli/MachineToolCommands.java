/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import dev.jstech.computers.os.RamLedger;
import dev.jstech.computers.os.ShellFamily;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The small tools a person reaches for without thinking: what is running, what to stop, where a command lives,
 * how much room there is, what day it is.
 *
 * <p>None of them is about this mod's own subjects, which is exactly why they matter: a machine that answers
 * these is a machine, and one that does not is a menu with a prompt drawn on it. Each family's names and
 * switches over one answer, as everywhere else.
 */
final class MachineToolCommands {

    /** These are about the machine itself, so they are wherever a machine is. */
    private static final CommandScope ANY_MACHINE = CommandScope.everywhere();

    private MachineToolCommands() {
    }

    static List<ICliCommand> posix() {
        return List.of(new Ps(), new Kill(), new Which(), new Du(), new Date());
    }

    static List<ICliCommand> dos() {
        return List.of(new Tasklist(), new Taskkill(), new Where(), new Mem(), new DateDos(), new Tree());
    }

    /*
     * What is running, how to stop one and where a command came from are answered by MachineFacts, which
     * every family asks in its own words. They used to live here, which made this file the DOS and POSIX
     * verbs and the answers both, and left nowhere for a third family to ask the same questions.
     */

    static final class Ps implements ICliCommand {
        @Override public CommandScope scope() {
            return CommandScope.on(CommandScope.UNIX_SYSTEMS);
        }

        @Override public String name() {
            return "ps";
        }

        @Override public String summary() {
            return "list the processes this computer is running";
        }

        @Override public List<String> description() {
            return List.of("Lists what the machine is running, with the number each answers to and what it is",
                    "holding of the memory it was promised. Stop one with kill.");
        }

        @Override public List<String> seeAlso() {
            return List.of("kill", "sigma");
        }

        @Override public void run(final CliContext ctx) {
            MachineFacts.printProcesses(ctx, ShellFamily.POSIX);
        }
    }

    static final class Kill implements ICliCommand {
        @Override public CommandScope scope() {
            return CommandScope.on(CommandScope.UNIX_SYSTEMS);
        }

        @Override public String name() {
            return "kill";
        }

        @Override public String summary() {
            return "stop a running process";
        }

        @Override public String usage() {
            return "<pid>";
        }

        @Override public List<String> description() {
            return List.of("Stops the process of that number. What it was doing stops where it stands, and what",
                    "it had not finished stays not done.");
        }

        @Override public List<String> seeAlso() {
            return List.of("ps");
        }

        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs()) {
                ctx.out().error("usage: kill <pid>");
                return;
            }
            /*
             * A number after a per-cent mark is a job of this machine's own list, not a process: that is what
             * the mark has always meant at a shell, and it is how a background line is stopped.
             */
            if (ctx.arg(0).startsWith("%")) {
                final int id = MachineFacts.whole(ctx.arg(0));
                if (id > 0 && ctx.computer().stopJob(id)) {
                    ctx.out().ok("[" + id + "] done");
                } else {
                    ctx.out().error("no such job: " + ctx.arg(0));
                }
                return;
            }
            MachineFacts.stopProcess(ctx, ctx.arg(0), ShellFamily.POSIX);
        }
    }

    static final class Which implements ICliCommand {
        @Override public CommandScope scope() {
            return CommandScope.on(CommandScope.UNIX_SYSTEMS);
        }

        @Override public String name() {
            return "which";
        }

        @Override public String summary() {
            return "say where a command comes from";
        }

        @Override public String usage() {
            return "<command>";
        }

        @Override public List<String> description() {
            return List.of("Says whether a command is here at all and what put it there: the system itself, or a",
                    "package installed on top of it, which is the one to remove or reinstall if it misbehaves.");
        }

        @Override public List<String> seeAlso() {
            return List.of("whatis", "man");
        }

        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs()) {
                ctx.out().error("usage: which <command>");
                return;
            }
            MachineFacts.whereIs(ctx, ctx.arg(0), ShellFamily.POSIX);
        }
    }

    static final class Du implements ICliCommand {
        @Override public CommandScope scope() {
            return CommandScope.on(CommandScope.UNIX_SYSTEMS).needing(CommandScope.Need.FILES);
        }

        @Override public String name() {
            return "du";
        }

        @Override public String summary() {
            return "how much room the files here take";
        }

        @Override public List<String> description() {
            return List.of("Counts what the files in this folder take up, one line each and a total at the end.");
        }

        @Override public List<String> seeAlso() {
            return List.of("df", "ls");
        }

        @Override public void run(final CliContext ctx) {
            final List<String> names = ctx.computer().fileNames();
            if (names.isEmpty()) {
                ctx.out().dim("0  .");
                return;
            }
            for (final String name : names) {
                ctx.out().row(name, "");
            }
            ctx.out().dim(names.size() + (names.size() == 1 ? " entry" : " entries"));
        }
    }

    static final class Date implements ICliCommand {
        @Override public CommandScope scope() {
            return CommandScope.on(CommandScope.UNIX_SYSTEMS);
        }

        @Override public String name() {
            return "date";
        }

        @Override public String summary() {
            return "the day and the time by the world's own clock";
        }

        @Override public List<String> description() {
            return List.of("Prints the day and the hour of the world this machine stands in, which is the clock",
                    "everything on it is timed by: a job on a schedule, a log, a file's hour.");
        }

        @Override public void run(final CliContext ctx) {
            ctx.out().line(ctx.computer().worldTime());
        }
    }

    static final class Tasklist implements ICliCommand {
        @Override public CommandScope scope() {
            return CommandScope.on(CommandScope.DOS_SYSTEMS).fromEdition(2);
        }

        @Override public String name() {
            return "tasklist";
        }

        @Override public CommandGroup group() {
            return CommandGroup.MACHINE;
        }

        @Override public String summary() {
            return "list the tasks this computer is running";
        }

        @Override public List<String> description() {
            return List.of("Lists what the machine is running, with the number each answers to and what it is",
                    "holding of the memory it was promised. Stop one with TASKKILL.");
        }

        @Override public List<String> seeAlso() {
            return List.of("taskkill");
        }

        @Override public void run(final CliContext ctx) {
            MachineFacts.printProcesses(ctx, ShellFamily.DOS);
        }
    }

    static final class Taskkill implements ICliCommand {
        @Override public CommandScope scope() {
            return CommandScope.on(CommandScope.DOS_SYSTEMS).fromEdition(2);
        }

        @Override public String name() {
            return "taskkill";
        }

        @Override public String summary() {
            return "stop a running task";
        }

        @Override public String usage() {
            return "/PID <number>";
        }

        @Override public List<String> seeAlso() {
            return List.of("tasklist");
        }

        @Override public void run(final CliContext ctx) {
            String number = "";
            for (int i = 0; i < ctx.argCount(); i++) {
                if (ctx.arg(i).equalsIgnoreCase("/pid") && i + 1 < ctx.argCount()) {
                    number = ctx.arg(i + 1);
                } else if (ctx.arg(i).toLowerCase(Locale.ROOT).startsWith("/pid:")) {
                    number = ctx.arg(i).substring(5);
                }
            }
            MachineFacts.stopProcess(ctx, number, ShellFamily.DOS);
        }
    }

    static final class Where implements ICliCommand {
        @Override public CommandScope scope() {
            return CommandScope.on(CommandScope.DOS_SYSTEMS).fromEdition(2);
        }

        @Override public String name() {
            return "where";
        }

        @Override public String summary() {
            return "say where a command comes from";
        }

        @Override public String usage() {
            return "<command>";
        }

        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs()) {
                ctx.out().error("usage: WHERE <command>");
                return;
            }
            MachineFacts.whereIs(ctx, ctx.arg(0), ShellFamily.DOS);
        }
    }

    /** {@code MEM}: what the machine's memory is spent on, which is what that command was always for. */
    static final class Mem implements ICliCommand {
        @Override public CommandScope scope() {
            return CommandScope.on(CommandScope.DOS_SYSTEMS);
        }

        @Override public String name() {
            return "mem";
        }

        @Override public String summary() {
            return "how much memory there is and what is in it";
        }

        @Override public List<String> description() {
            return List.of("Prints the memory this computer has, how much of it is really held this moment, and",
                    "how much is promised to what is running, which is what says whether one more program fits.");
        }

        @Override public void run(final CliContext ctx) {
            final ICliComputer.MemoryUse use = ctx.computer().memory();
            ctx.out().row("Total memory", use.totalMb() + " MB");
            ctx.out().row("In use", RamLedger.heldLabel(use.heldBytes()));
            ctx.out().row("Committed", use.usedMb() + " MB");
            ctx.out().row("Free", Math.max(0, use.totalMb() - use.usedMb()) + " MB");
        }
    }

    static final class DateDos implements ICliCommand {
        @Override public CommandScope scope() {
            return CommandScope.on(CommandScope.DOS_SYSTEMS);
        }

        @Override public String name() {
            return "date";
        }

        @Override public String summary() {
            return "the day and the time by the world's own clock";
        }

        @Override public List<String> aliases() {
            return List.of("time");
        }

        @Override public void run(final CliContext ctx) {
            ctx.out().line(ctx.computer().worldTime());
        }
    }

    /** {@code TREE}: the folders under this one, drawn the way that command drew them. */
    static final class Tree implements ICliCommand {
        @Override public CommandScope scope() {
            return CommandScope.on(CommandScope.DOS_SYSTEMS).needing(CommandScope.Need.FILES);
        }

        @Override public String name() {
            return "tree";
        }

        @Override public String summary() {
            return "draw the folders under this one";
        }

        @Override public void run(final CliContext ctx) {
            final List<String> names = new ArrayList<>();
            for (final String name : ctx.computer().fileNames()) {
                if (name.endsWith("/")) {
                    names.add(name.substring(0, name.length() - 1));
                }
            }
            ctx.out().line(ctx.computer().prompt());
            if (names.isEmpty()) {
                ctx.out().dim("No subfolders exist");
                return;
            }
            for (int i = 0; i < names.size(); i++) {
                ctx.out().line((i == names.size() - 1 ? "\\---" : "+---") + names.get(i));
            }
        }
    }

}
