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
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
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
@TextHolder
final class MachineToolCommands {

    /** What {@code which} and {@code WHERE} are both for, in the same words. */
    private static final TextKey SAYS_WHERE =
            TextKey.of("jsc.cli.tool.which.summary", "say where a command comes from");

    /** What both families' {@code date} is for. */
    private static final TextKey WORLD_CLOCK =
            TextKey.of("jsc.cli.tool.date.summary", "the day and the time by the world's own clock");

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

    @TextHolder
    static final class Ps implements ICliCommand {

        private static final TextKey SUMMARY =
                TextKey.of("jsc.cli.tool.ps.summary", "list the processes this computer is running");
        private static final TextKey ABOUT = TextKey.of("jsc.cli.tool.ps.about", "Lists what the machine is"
                + " running, with the number each answers to and what it is holding of the memory it was promised."
                + " Stop one with kill.");

        @Override public CommandScope scope() {
            return CommandScope.on(CommandScope.UNIX_SYSTEMS);
        }

        @Override public String name() {
            return "ps";
        }

        @Override public CommandGroup group() {
            return CommandGroup.MACHINE;
        }

        @Override public Text summary() {
            return SUMMARY.text();
        }

        @Override public List<Text> description() {
            return List.of(ABOUT.text());
        }

        @Override public List<String> seeAlso() {
            return List.of("kill", "sigma");
        }

        @Override public void run(final CliContext ctx) {
            MachineFacts.printProcesses(ctx, ShellFamily.POSIX);
        }
    }

    @TextHolder
    static final class Kill implements ICliCommand {

        private static final TextKey SUMMARY = TextKey.of("jsc.cli.tool.kill.summary", "stop a running process");
        private static final TextKey USAGE = TextKey.of("jsc.cli.tool.kill.usage", "<pid>");
        private static final TextKey ABOUT = TextKey.of("jsc.cli.tool.kill.about", "Stops the process of that"
                + " number. What it was doing stops where it stands, and what it had not finished stays not done.");
        private static final TextKey JOB_DONE = TextKey.of("jsc.cli.tool.kill.job_done", "[%s] done");
        private static final TextKey NO_SUCH_JOB = TextKey.of("jsc.cli.tool.kill.no_such_job", "no such job: %s");

        @Override public CommandScope scope() {
            return CommandScope.on(CommandScope.UNIX_SYSTEMS);
        }

        @Override public String name() {
            return "kill";
        }

        @Override public CommandGroup group() {
            return CommandGroup.MACHINE;
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
            return List.of("ps");
        }

        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs()) {
                ctx.out().error(CliTexts.USAGE.with(name(), usage()));
                return;
            }
            /*
             * A number after a per-cent mark is a job of this machine's own list, not a process: that is what
             * the mark has always meant at a shell, and it is how a background line is stopped.
             */
            if (ctx.arg(0).startsWith("%")) {
                final int id = MachineFacts.whole(ctx.arg(0));
                if (id > 0 && ctx.computer().stopJob(id)) {
                    ctx.out().ok(JOB_DONE.with(id));
                } else {
                    ctx.out().error(NO_SUCH_JOB.with(ctx.arg(0)));
                }
                return;
            }
            MachineFacts.stopProcess(ctx, ctx.arg(0), ShellFamily.POSIX);
        }
    }

    @TextHolder
    static final class Which implements ICliCommand {

        private static final TextKey USAGE = TextKey.of("jsc.cli.tool.which.usage", "<command>");
        private static final TextKey ABOUT = TextKey.of("jsc.cli.tool.which.about", "Says whether a command is"
                + " here at all and what put it there: the system itself, or a package installed on top of it,"
                + " which is the one to remove or reinstall if it misbehaves.");

        @Override public CommandScope scope() {
            return CommandScope.on(CommandScope.UNIX_SYSTEMS);
        }

        @Override public String name() {
            return "which";
        }

        @Override public CommandGroup group() {
            return CommandGroup.MACHINE;
        }

        @Override public Text summary() {
            return SAYS_WHERE.text();
        }

        @Override public Text usage() {
            return USAGE.text();
        }

        @Override public List<Text> description() {
            return List.of(ABOUT.text());
        }

        @Override public List<String> seeAlso() {
            return List.of("whatis", "man");
        }

        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs()) {
                ctx.out().error(CliTexts.USAGE.with(name(), usage()));
                return;
            }
            MachineFacts.whereIs(ctx, ctx.arg(0), ShellFamily.POSIX);
        }
    }

    @TextHolder
    static final class Du implements ICliCommand {

        private static final TextKey SUMMARY =
                TextKey.of("jsc.cli.tool.du.summary", "how much room the files here take");
        private static final TextKey ABOUT = TextKey.of("jsc.cli.tool.du.about",
                "Counts what the files in this folder take up, one line each and a total at the end.");
        private static final TextKey ENTRY = TextKey.of("jsc.cli.tool.du.entry", "%s entry");
        private static final TextKey ENTRIES = TextKey.of("jsc.cli.tool.du.entries", "%s entries");

        @Override public CommandScope scope() {
            return CommandScope.on(CommandScope.UNIX_SYSTEMS).needing(CommandScope.Need.FILES);
        }

        @Override public String name() {
            return "du";
        }

        @Override public CommandGroup group() {
            return CommandGroup.FILES;
        }

        @Override public Text summary() {
            return SUMMARY.text();
        }

        @Override public List<Text> description() {
            return List.of(ABOUT.text());
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
            ctx.out().dim(names.size() == 1 ? ENTRY.with(names.size()) : ENTRIES.with(names.size()));
        }
    }

    @TextHolder
    static final class Date implements ICliCommand {

        private static final TextKey ABOUT = TextKey.of("jsc.cli.tool.date.about", "Prints the day and the hour"
                + " of the world this machine stands in, which is the clock everything on it is timed by: a job on a"
                + " schedule, a log, a file's hour.");

        @Override public CommandScope scope() {
            return CommandScope.on(CommandScope.UNIX_SYSTEMS);
        }

        @Override public String name() {
            return "date";
        }

        @Override public CommandGroup group() {
            return CommandGroup.MACHINE;
        }

        @Override public Text summary() {
            return WORLD_CLOCK.text();
        }

        @Override public List<Text> description() {
            return List.of(ABOUT.text());
        }

        @Override public void run(final CliContext ctx) {
            ctx.out().line(ctx.computer().worldTime());
        }
    }

    @TextHolder
    static final class Tasklist implements ICliCommand {

        private static final TextKey SUMMARY =
                TextKey.of("jsc.cli.tool.tasklist.summary", "list the tasks this computer is running");
        private static final TextKey ABOUT = TextKey.of("jsc.cli.tool.tasklist.about", "Lists what the machine is"
                + " running, with the number each answers to and what it is holding of the memory it was promised."
                + " Stop one with TASKKILL.");

        @Override public CommandScope scope() {
            return CommandScope.on(CommandScope.DOS_SYSTEMS).fromEdition(2);
        }

        @Override public String name() {
            return "tasklist";
        }

        @Override public CommandGroup group() {
            return CommandGroup.MACHINE;
        }

        @Override public Text summary() {
            return SUMMARY.text();
        }

        @Override public List<Text> description() {
            return List.of(ABOUT.text());
        }

        @Override public List<String> seeAlso() {
            return List.of("taskkill");
        }

        @Override public void run(final CliContext ctx) {
            MachineFacts.printProcesses(ctx, ShellFamily.DOS);
        }
    }

    @TextHolder
    static final class Taskkill implements ICliCommand {

        private static final TextKey SUMMARY = TextKey.of("jsc.cli.tool.taskkill.summary", "stop a running task");
        private static final TextKey USAGE = TextKey.of("jsc.cli.tool.taskkill.usage", "/PID <number>");

        @Override public CommandScope scope() {
            return CommandScope.on(CommandScope.DOS_SYSTEMS).fromEdition(2);
        }

        @Override public String name() {
            return "taskkill";
        }

        @Override public CommandGroup group() {
            return CommandGroup.MACHINE;
        }

        @Override public Text summary() {
            return SUMMARY.text();
        }

        @Override public Text usage() {
            return USAGE.text();
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

    @TextHolder
    static final class Where implements ICliCommand {

        private static final TextKey USAGE = TextKey.of("jsc.cli.tool.where.usage", "<command>");

        @Override public CommandScope scope() {
            return CommandScope.on(CommandScope.DOS_SYSTEMS).fromEdition(2);
        }

        @Override public String name() {
            return "where";
        }

        @Override public CommandGroup group() {
            return CommandGroup.MACHINE;
        }

        @Override public Text summary() {
            return SAYS_WHERE.text();
        }

        @Override public Text usage() {
            return USAGE.text();
        }

        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs()) {
                // The DOS family writes its own command names in capitals.
                ctx.out().error(CliTexts.USAGE.with(name().toUpperCase(Locale.ROOT), usage()));
                return;
            }
            MachineFacts.whereIs(ctx, ctx.arg(0), ShellFamily.DOS);
        }
    }

    /** {@code MEM}: what the machine's memory is spent on, which is what that command was always for. */
    @TextHolder
    static final class Mem implements ICliCommand {

        private static final TextKey SUMMARY =
                TextKey.of("jsc.cli.tool.mem.summary", "how much memory there is and what is in it");
        private static final TextKey ABOUT = TextKey.of("jsc.cli.tool.mem.about", "Prints the memory this computer"
                + " has, how much of it is really held this moment, and how much is promised to what is running,"
                + " which is what says whether one more program fits.");
        private static final TextKey TOTAL = TextKey.of("jsc.cli.tool.mem.total", "Total memory");
        private static final TextKey IN_USE = TextKey.of("jsc.cli.tool.mem.in_use", "In use");
        private static final TextKey COMMITTED = TextKey.of("jsc.cli.tool.mem.committed", "Committed");
        private static final TextKey FREE = TextKey.of("jsc.cli.tool.mem.free", "Free");

        @Override public CommandScope scope() {
            return CommandScope.on(CommandScope.DOS_SYSTEMS);
        }

        @Override public String name() {
            return "mem";
        }

        @Override public CommandGroup group() {
            return CommandGroup.MACHINE;
        }

        @Override public Text summary() {
            return SUMMARY.text();
        }

        @Override public List<Text> description() {
            return List.of(ABOUT.text());
        }

        @Override public void run(final CliContext ctx) {
            final ICliComputer.MemoryUse use = ctx.computer().memory();
            // A size and its unit are data, written alike in every language.
            ctx.out().row(TOTAL.text(), Text.literal(use.totalMb() + " MB"));
            ctx.out().row(IN_USE, RamLedger.heldLabel(use.heldBytes()));
            ctx.out().row(COMMITTED.text(), Text.literal(use.usedMb() + " MB"));
            ctx.out().row(FREE.text(), Text.literal(Math.max(0, use.totalMb() - use.usedMb()) + " MB"));
        }
    }

    static final class DateDos implements ICliCommand {

        @Override public CommandScope scope() {
            return CommandScope.on(CommandScope.DOS_SYSTEMS);
        }

        @Override public String name() {
            return "date";
        }

        @Override public CommandGroup group() {
            return CommandGroup.MACHINE;
        }

        @Override public Text summary() {
            return WORLD_CLOCK.text();
        }

        @Override public List<String> aliases() {
            return List.of("time");
        }

        @Override public void run(final CliContext ctx) {
            ctx.out().line(ctx.computer().worldTime());
        }
    }

    /** {@code TREE}: the folders under this one, drawn the way that command drew them. */
    @TextHolder
    static final class Tree implements ICliCommand {

        private static final TextKey SUMMARY =
                TextKey.of("jsc.cli.tool.tree.summary", "draw the folders under this one");
        private static final TextKey NO_SUBFOLDERS = TextKey.of("jsc.cli.tool.tree.none", "No subfolders exist");

        @Override public CommandScope scope() {
            return CommandScope.on(CommandScope.DOS_SYSTEMS).needing(CommandScope.Need.FILES);
        }

        @Override public String name() {
            return "tree";
        }

        @Override public CommandGroup group() {
            return CommandGroup.FILES;
        }

        @Override public Text summary() {
            return SUMMARY.text();
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
                ctx.out().dim(NO_SUBFOLDERS);
                return;
            }
            for (int i = 0; i < names.size(); i++) {
                ctx.out().line((i == names.size() - 1 ? "\\---" : "+---") + names.get(i));
            }
        }
    }

}
