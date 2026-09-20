/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import dev.jstech.computers.program.job.JobWhen;
import dev.jstech.computers.program.job.MachineJobs;
import java.util.List;
import java.util.Locale;

/**
 * Leaving a machine with work: a line to run now, without waiting for it, and a line to run at an hour.
 *
 * <p>A computer that only does what somebody types while they stand at it is a tool. One that goes on working
 * with nobody there is a computer, and this is the difference between the two. Both families get it in their
 * own words over one list: {@code &}, {@code jobs} and {@code crontab} on one side, {@code START} and
 * {@code AT} on the other.
 */
final class JobCommands {

    /** Work belongs to a machine, so this is wherever a machine is. */
    private static final CommandScope ANY_MACHINE = CommandScope.everywhere();

    private JobCommands() {
    }

    static List<ICliCommand> posix() {
        return List.of(new Jobs(), new Crontab());
    }

    static List<ICliCommand> dos() {
        return List.of(new Start(), new At());
    }

    /**
     * Puts a line in the machine's list, or says why it cannot go there.
     *
     * <p>A job costs the machine a megabyte for as long as it has it, which is what says how many a computer
     * may be left with: a small machine holds a few and a large one holds many, and neither is a number
     * anybody had to invent.
     */
    static void add(final CliContext ctx, final String line, final JobWhen when, final boolean dosStyle) {
        if (line.isBlank()) {
            ctx.out().error(dosStyle ? "A command is needed." : "nothing to run");
            return;
        }
        final ICliComputer.MemoryUse memory = ctx.computer().memory();
        if (memory.totalMb() > 0 && memory.usedMb() + MachineJobs.JOB_MB > memory.totalMb()) {
            ctx.out().error(dosStyle
                    ? "Not enough memory to start another task."
                    : "the machine has no memory left for another job");
            return;
        }
        final MachineJobs.Job job = ctx.computer().addJob(line, when);
        if (job == null) {
            ctx.out().error(dosStyle ? "This computer keeps no tasks." : "this machine keeps no jobs");
            return;
        }
        if (when.once()) {
            ctx.out().ok(dosStyle
                    ? "Started job " + job.id() + ": " + line
                    : "[" + job.id() + "] " + line);
        } else {
            ctx.out().ok(dosStyle
                    ? "Added a new job with ID = " + job.id()
                    : "[" + job.id() + "] " + when.label() + "  " + line);
        }
    }

    /** The machine's list, in whichever family's columns. */
    static void list(final CliContext ctx, final boolean dosStyle) {
        final List<MachineJobs.Job> jobs = ctx.computer().jobs();
        if (jobs.isEmpty()) {
            ctx.out().dim(dosStyle ? "There are no entries in the list." : "no jobs");
            return;
        }
        ctx.out().header(CliText.pad(dosStyle ? "ID" : "  ID", 6) + CliText.pad("WHEN", 16) + "COMMAND");
        for (final MachineJobs.Job job : jobs) {
            ctx.out().line(CliText.pad(String.valueOf(job.id()), 6)
                    + CliText.pad(job.when().label(), 16) + job.line());
        }
    }

    static void stop(final CliContext ctx, final String written, final boolean dosStyle) {
        final int id = number(written);
        if (id <= 0 || !ctx.computer().stopJob(id)) {
            ctx.out().error(dosStyle ? "The job ID does not exist." : "no such job: " + written);
            return;
        }
        ctx.out().ok(dosStyle ? "Deleted job " + id + "." : "[" + id + "] done");
    }

    /** A number written on its own or after the mark a shell puts before a job's number. */
    private static int number(final String written) {
        try {
            return Integer.parseInt(written.trim().replace("%", ""));
        } catch (final NumberFormatException notANumber) {
            return -1;
        }
    }

    /** {@code jobs}: what this machine is doing on its own. */
    static final class Jobs implements ICliCommand {
        @Override public CommandScope scope() {
            return CommandScope.on(CommandScope.UNIX_SYSTEMS);
        }

        @Override public String name() {
            return "jobs";
        }

        @Override public String summary() {
            return "list the work this machine is doing on its own";
        }

        @Override public List<String> description() {
            return List.of("Lists what the machine was left with: lines put in the background with & and lines",
                    "waiting for their hour. Stop one with kill %n, whichever kind it is.");
        }

        @Override public List<ICliCommand.Example> examples() {
            return List.of(new Example("interac craft 64 chest &", "leave it running and get the prompt back"),
                    new Example("jobs", "what is still going"),
                    new Example("kill %1", "stop the first of them"));
        }

        @Override public List<String> seeAlso() {
            return List.of("crontab", "kill", "ps");
        }

        @Override public void run(final CliContext ctx) {
            list(ctx, false);
        }
    }

    /** {@code crontab}: the lines this machine runs at an hour. */
    static final class Crontab implements ICliCommand {
        @Override public CommandScope scope() {
            return CommandScope.on(CommandScope.UNIX_SYSTEMS);
        }

        @Override public String name() {
            return "crontab";
        }

        @Override public String summary() {
            return "run a command at an hour, every day";
        }

        @Override public String usage() {
            return "-l | -r <id> | <hour> [days] <command>";
        }

        @Override public List<String> description() {
            return List.of("Leaves a line for the machine to run at an hour of the world's own day, every day,",
                    "or on the days named. The shortest anything repeats is an hour, which is a little under a",
                    "minute of real time.",
                    "",
                    "The hour is written as 06:00 or as 6, and the days as the letters of the week: M, T, W,",
                    "Th, F, Sa, Su, apart with commas.");
        }

        @Override public List<Option> options() {
            return List.of(new Option("-l", "list what this machine runs and when"),
                    new Option("-r <id>", "take one off the list"));
        }

        @Override public List<Example> examples() {
            return List.of(
                    new Example("crontab 06:00 interac get 64 coal --to local", "every morning at six"),
                    new Example("crontab 18:00 M,W,F interac craft 8 chest", "three evenings a week"));
        }

        @Override public List<String> seeAlso() {
            return List.of("jobs", "date");
        }

        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs() || ctx.arg(0).equals("-l")) {
                list(ctx, false);
                return;
            }
            if (ctx.arg(0).equals("-r")) {
                stop(ctx, ctx.arg(1), false);
                return;
            }
            final int hour = JobWhen.hourOf(ctx.arg(0));
            if (hour < 0) {
                ctx.out().error("crontab: " + ctx.arg(0) + " is not an hour of the day");
                return;
            }
            final List<Integer> days = JobWhen.daysOf(ctx.arg(1));
            final int from = days.isEmpty() ? 1 : 2;
            add(ctx, ctx.rest(from), JobWhen.at(hour, days), false);
        }
    }

    /** {@code START}: the DOS family's way of leaving a line running. */
    static final class Start implements ICliCommand {
        @Override public CommandScope scope() {
            return CommandScope.on(CommandScope.DOS_SYSTEMS);
        }

        @Override public String name() {
            return "start";
        }

        @Override public String summary() {
            return "run a command without waiting for it";
        }

        @Override public String usage() {
            return "<command>";
        }

        @Override public List<String> description() {
            return List.of("Leaves the machine running a command and gives the prompt straight back. On a system",
                    "that runs one thing at a time, the machine takes them in turn.");
        }

        @Override public List<String> seeAlso() {
            return List.of("at");
        }

        @Override public void run(final CliContext ctx) {
            add(ctx, ctx.rest(0), JobWhen.AT_ONCE, true);
        }
    }

    /** {@code AT}: and its way of leaving a line for an hour. */
    static final class At implements ICliCommand {
        @Override public CommandScope scope() {
            return CommandScope.on(CommandScope.DOS_SYSTEMS);
        }

        @Override public String name() {
            return "at";
        }

        @Override public String summary() {
            return "run a command at an hour";
        }

        @Override public String usage() {
            return "[hh:mm [/EVERY:days] <command>] [id /DELETE]";
        }

        @Override public List<String> description() {
            return List.of("With nothing after it, lists what this computer is set to run and when. With an hour",
                    "and a command, leaves that command for that hour of every day, or of the days named after",
                    "/EVERY. The shortest anything repeats is an hour of the world's clock.");
        }

        @Override public List<Option> options() {
            return List.of(new Option("/EVERY:M,W,F", "only on the days named"),
                    new Option("<id> /DELETE", "take one off the list"));
        }

        @Override public List<Example> examples() {
            return List.of(new Example("AT 06:00 INTERAC GET 64 COAL /LOCAL", "every morning at six"),
                    new Example("AT", "what this computer is set to do"),
                    new Example("AT 2 /DELETE", "take the second off the list"));
        }

        @Override public List<String> seeAlso() {
            return List.of("start");
        }

        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs()) {
                list(ctx, true);
                return;
            }
            for (int i = 0; i < ctx.argCount(); i++) {
                if (ctx.arg(i).equalsIgnoreCase("/delete")) {
                    stop(ctx, ctx.arg(0), true);
                    return;
                }
            }
            final int hour = JobWhen.hourOf(ctx.arg(0));
            if (hour < 0) {
                ctx.out().error("The time is not in the right format.");
                return;
            }
            List<Integer> days = List.of();
            int from = 1;
            if (ctx.arg(1).toLowerCase(Locale.ROOT).startsWith("/every:")) {
                days = JobWhen.daysOf(ctx.arg(1).substring("/every:".length()));
                from = 2;
            }
            add(ctx, ctx.rest(from), JobWhen.at(hour, days), true);
        }
    }
}
