/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import dev.jstech.computers.os.RamLedger;
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

    /** The processes a machine is running, with what each is holding. */
    private static void printProcesses(final CliContext ctx, final boolean dosStyle) {
        final List<ICliComputer.SigmaProcess> running = ctx.computer().sigmaProcesses();
        if (running.isEmpty()) {
            ctx.out().dim(dosStyle ? "No tasks are running." : "no processes");
            return;
        }
        /*
         * The file as well as the name: a program that gave itself no name is listed under the runtime that
         * runs it, and then the file is the only thing telling two of them apart.
         */
        ctx.out().header(CliText.pad(dosStyle ? "PID" : "  PID", 7) + CliText.pad("NAME", 14)
                + CliText.pad("FILE", 24) + CliText.pad("STATE", 10) + CliText.padLeft("MEM", 10));
        for (final ICliComputer.SigmaProcess one : running) {
            ctx.out().line(CliText.pad(String.valueOf(one.id()), 7) + CliText.pad(one.name(), 14)
                    + CliText.pad(one.file(), 24) + CliText.pad(one.state(), 10)
                    + CliText.padLeft(RamLedger.heldLabel(one.heldBytes()), 10));
        }
    }

    /** Stops the process of that number, in whichever family's words. */
    private static void stopProcess(final CliContext ctx, final String number, final boolean dosStyle) {
        final int id = whole(number);
        if (id <= 0) {
            ctx.out().error(dosStyle ? "taskkill: /PID takes the number of a task" : "kill: not a process id");
            return;
        }
        final ICliComputer.OpResult result = ctx.computer().stopSigma(id);
        ctx.out().styled(result.message(), result.ok() ? CliStyle.OK : CliStyle.ERROR);
    }

    private static int whole(final String text) {
        try {
            return Integer.parseInt(text.trim().replace("%", ""));
        } catch (final NumberFormatException notANumber) {
            return -1;
        }
    }

    /** Where a command lives, which for a machine like this is which package put it there. */
    private static void whereIs(final CliContext ctx, final String name, final boolean dosStyle) {
        final ICliCommand found = ctx.shell().find(name);
        if (found == null || !found.available(ctx.computer())) {
            ctx.out().error(dosStyle ? "INFO: Could not find \"" + name + "\"." : name + " not found");
            return;
        }
        final String path = found.scope().fromAPackage() ? found.scope().packageId() : "the system";
        ctx.out().row(found.name(), path);
    }

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
            printProcesses(ctx, false);
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
            stopProcess(ctx, ctx.arg(0), false);
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
            whereIs(ctx, ctx.arg(0), false);
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
            printProcesses(ctx, true);
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
            stopProcess(ctx, number, true);
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
            whereIs(ctx, ctx.arg(0), true);
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
