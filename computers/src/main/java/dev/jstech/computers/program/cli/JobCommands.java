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
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
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
@TextHolder
final class JobCommands {

    /** Work belongs to a machine, so this is wherever a machine is. */
    private static final CommandScope ANY_MACHINE = CommandScope.everywhere();

    /* What each family says about the list, in its own voice: the Unix one terse, the DOS one in sentences. */
    private static final TextKey NOTHING_TO_RUN = TextKey.of("jsc.cli.job.nothing_to_run", "nothing to run");
    private static final TextKey DOS_NOTHING_TO_RUN = TextKey.of("jsc.cli.job.dos.nothing_to_run",
            "A command is needed.");
    private static final TextKey NO_MEMORY = TextKey.of("jsc.cli.job.no_memory",
            "the machine has no memory left for another job");
    private static final TextKey DOS_NO_MEMORY = TextKey.of("jsc.cli.job.dos.no_memory",
            "Not enough memory to start another task.");
    private static final TextKey KEEPS_NONE = TextKey.of("jsc.cli.job.keeps_none", "this machine keeps no jobs");
    private static final TextKey DOS_KEEPS_NONE = TextKey.of("jsc.cli.job.dos.keeps_none",
            "This computer keeps no tasks.");
    private static final TextKey DOS_STARTED = TextKey.of("jsc.cli.job.dos.started", "Started job %s: %s");
    private static final TextKey DOS_ADDED = TextKey.of("jsc.cli.job.dos.added", "Added a new job with ID = %s");
    private static final TextKey NO_JOBS = TextKey.of("jsc.cli.job.no_jobs", "no jobs");
    private static final TextKey DOS_NO_JOBS = TextKey.of("jsc.cli.job.dos.no_jobs",
            "There are no entries in the list.");
    /* The heads of the list's columns, laid out over the rows once they are in the reader's language. */
    private static final TextKey COL_ID = TextKey.of("jsc.cli.job.col.id", "ID");
    private static final TextKey COL_WHEN = TextKey.of("jsc.cli.job.col.when", "WHEN");
    private static final TextKey COL_COMMAND = TextKey.of("jsc.cli.job.col.command", "COMMAND");

    /** Where the list's columns start: the job's number, when it runs, and the line it runs. */
    private static final int WHEN_AT = 6;
    private static final int COMMAND_AT = 22;
    private static final TextKey NO_SUCH = TextKey.of("jsc.cli.job.no_such", "no such job: %s");
    private static final TextKey DOS_NO_SUCH = TextKey.of("jsc.cli.job.dos.no_such", "The job ID does not exist.");
    private static final TextKey DONE = TextKey.of("jsc.cli.job.done", "[%s] done");
    private static final TextKey DOS_DELETED = TextKey.of("jsc.cli.job.dos.deleted", "Deleted job %s.");

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
            ctx.out().error(dosStyle ? DOS_NOTHING_TO_RUN : NOTHING_TO_RUN);
            return;
        }
        final ICliComputer.MemoryUse memory = ctx.computer().memory();
        if (memory.totalMb() > 0 && memory.usedMb() + MachineJobs.JOB_MB > memory.totalMb()) {
            ctx.out().error(dosStyle ? DOS_NO_MEMORY : NO_MEMORY);
            return;
        }
        final MachineJobs.Job job = ctx.computer().addJob(line, when);
        if (job == null) {
            ctx.out().error(dosStyle ? DOS_KEEPS_NONE : KEEPS_NONE);
            return;
        }
        if (when.once()) {
            ctx.out().ok(dosStyle
                    ? DOS_STARTED.with(job.id(), line)
                    : Text.literal("[" + job.id() + "] " + line));
        } else if (dosStyle) {
            ctx.out().ok(DOS_ADDED.with(job.id()));
        } else {
            ctx.out().line(CliLine.of(new CliSpan("[" + job.id() + "] ", CliStyle.OK),
                    new CliSpan(when.shown(), CliStyle.OK), new CliSpan("  " + line, CliStyle.OK)));
        }
    }

    /** The machine's list, in whichever family's columns. */
    static void list(final CliContext ctx, final boolean dosStyle) {
        final List<MachineJobs.Job> jobs = ctx.computer().jobs();
        if (jobs.isEmpty()) {
            ctx.out().dim(dosStyle ? DOS_NO_JOBS : NO_JOBS);
            return;
        }
        ctx.out().line(CliLine.of(new CliSpan(Text.literal(dosStyle ? "" : "  "), CliStyle.HEADER),
                new CliSpan(COL_ID.text(), CliStyle.HEADER), CliSpan.pad(WHEN_AT),
                new CliSpan(COL_WHEN.text(), CliStyle.HEADER), CliSpan.pad(COMMAND_AT),
                new CliSpan(COL_COMMAND.text(), CliStyle.HEADER)));
        for (final MachineJobs.Job job : jobs) {
            ctx.out().line(CliLine.of(CliSpan.plain(String.valueOf(job.id())), CliSpan.pad(WHEN_AT),
                    CliSpan.plain(job.when().shown()), CliSpan.pad(COMMAND_AT), CliSpan.plain(job.line())));
        }
    }

    static void stop(final CliContext ctx, final String written, final boolean dosStyle) {
        final int id = number(written);
        if (id <= 0 || !ctx.computer().stopJob(id)) {
            ctx.out().error(dosStyle ? DOS_NO_SUCH.text() : NO_SUCH.with(written));
            return;
        }
        ctx.out().ok(dosStyle ? DOS_DELETED.with(id) : DONE.with(id));
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
    @TextHolder
    static final class Jobs implements ICliCommand {

        private static final TextKey SUMMARY =
                TextKey.of("jsc.cli.job.jobs.summary", "list the work this machine is doing on its own");
        private static final TextKey ABOUT = TextKey.of("jsc.cli.job.jobs.about",
                "Lists what the machine was left with: lines put in the background with & and lines waiting for"
                        + " their hour. Stop one with kill %%n, whichever kind it is.");
        private static final TextKey EXAMPLE_BACKGROUND = TextKey.of("jsc.cli.job.jobs.example.background",
                "leave it running and get the prompt back");
        private static final TextKey EXAMPLE_JOBS =
                TextKey.of("jsc.cli.job.jobs.example.jobs", "what is still going");
        private static final TextKey EXAMPLE_KILL =
                TextKey.of("jsc.cli.job.jobs.example.kill", "stop the first of them");

        @Override public CommandScope scope() {
            return CommandScope.on(CommandScope.UNIX_SYSTEMS);
        }

        @Override public String name() {
            return "jobs";
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

        @Override public List<ICliCommand.Example> examples() {
            return List.of(new Example("interac craft 64 chest &", EXAMPLE_BACKGROUND),
                    new Example("jobs", EXAMPLE_JOBS),
                    new Example("kill %1", EXAMPLE_KILL));
        }

        @Override public List<String> seeAlso() {
            return List.of("crontab", "kill", "ps");
        }

        @Override public void run(final CliContext ctx) {
            list(ctx, false);
        }
    }

    /** {@code crontab}: the lines this machine runs at an hour. */
    @TextHolder
    static final class Crontab implements ICliCommand {

        private static final TextKey SUMMARY =
                TextKey.of("jsc.cli.job.crontab.summary", "run a command at an hour, every day");
        private static final TextKey USAGE =
                TextKey.of("jsc.cli.job.crontab.usage", "-l | -r <id> | <hour> [days] <command>");
        private static final TextKey ABOUT = TextKey.of("jsc.cli.job.crontab.about",
                "Leaves a line for the machine to run at an hour of the world's own day, every day, or on the days"
                        + " named. The shortest anything repeats is an hour, which is a little under a minute of"
                        + " real time.");
        private static final TextKey ABOUT_DAYS = TextKey.of("jsc.cli.job.crontab.about.days",
                "The hour is written as 06:00 or as 6, and the days as the letters of the week: M, T, W, Th, F, Sa,"
                        + " Su, apart with commas.");
        private static final TextKey OPTION_LIST =
                TextKey.of("jsc.cli.job.crontab.option.list", "list what this machine runs and when");
        private static final TextKey OPTION_REMOVE =
                TextKey.of("jsc.cli.job.crontab.option.remove", "take one off the list");
        private static final TextKey EXAMPLE_MORNING =
                TextKey.of("jsc.cli.job.crontab.example.morning", "every morning at six");
        private static final TextKey EXAMPLE_EVENINGS =
                TextKey.of("jsc.cli.job.crontab.example.evenings", "three evenings a week");
        private static final TextKey NOT_AN_HOUR =
                TextKey.of("jsc.cli.job.crontab.not_an_hour", "%s is not an hour of the day");

        @Override public CommandScope scope() {
            return CommandScope.on(CommandScope.UNIX_SYSTEMS);
        }

        @Override public String name() {
            return "crontab";
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
            return List.of(ABOUT.text(), ABOUT_DAYS.text());
        }

        @Override public List<Option> options() {
            return List.of(new Option("-l", OPTION_LIST), new Option("-r <id>", OPTION_REMOVE));
        }

        @Override public List<Example> examples() {
            return List.of(
                    new Example("crontab 06:00 interac get 64 coal --to local", EXAMPLE_MORNING),
                    new Example("crontab 18:00 M,W,F interac craft 8 chest", EXAMPLE_EVENINGS));
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
                ctx.out().error(CliTexts.SAID_BY.with(name(), NOT_AN_HOUR.with(ctx.arg(0))));
                return;
            }
            final List<Integer> days = JobWhen.daysOf(ctx.arg(1));
            final int from = days.isEmpty() ? 1 : 2;
            add(ctx, ctx.rest(from), JobWhen.at(hour, days), false);
        }
    }

    /** {@code START}: the DOS family's way of leaving a line running. */
    @TextHolder
    static final class Start implements ICliCommand {

        private static final TextKey SUMMARY =
                TextKey.of("jsc.cli.job.start.summary", "run a command without waiting for it");
        private static final TextKey USAGE = TextKey.of("jsc.cli.job.start.usage", "<command>");
        private static final TextKey ABOUT = TextKey.of("jsc.cli.job.start.about",
                "Leaves the machine running a command and gives the prompt straight back. On a system that runs one"
                        + " thing at a time, the machine takes them in turn.");

        @Override public CommandScope scope() {
            return CommandScope.on(CommandScope.DOS_SYSTEMS);
        }

        @Override public String name() {
            return "start";
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
            return List.of("at");
        }

        @Override public void run(final CliContext ctx) {
            add(ctx, ctx.rest(0), JobWhen.AT_ONCE, true);
        }
    }

    /** {@code AT}: and its way of leaving a line for an hour. */
    @TextHolder
    static final class At implements ICliCommand {

        private static final TextKey SUMMARY = TextKey.of("jsc.cli.job.at.summary", "run a command at an hour");
        private static final TextKey USAGE =
                TextKey.of("jsc.cli.job.at.usage", "[hh:mm [/EVERY:days] <command>] [id /DELETE]");
        private static final TextKey ABOUT = TextKey.of("jsc.cli.job.at.about",
                "With nothing after it, lists what this computer is set to run and when. With an hour and a"
                        + " command, leaves that command for that hour of every day, or of the days named after"
                        + " /EVERY. The shortest anything repeats is an hour of the world's clock.");
        private static final TextKey OPTION_EVERY =
                TextKey.of("jsc.cli.job.at.option.every", "only on the days named");
        private static final TextKey OPTION_DELETE =
                TextKey.of("jsc.cli.job.at.option.delete", "take one off the list");
        private static final TextKey EXAMPLE_MORNING =
                TextKey.of("jsc.cli.job.at.example.morning", "every morning at six");
        private static final TextKey EXAMPLE_LIST =
                TextKey.of("jsc.cli.job.at.example.list", "what this computer is set to do");
        private static final TextKey EXAMPLE_DELETE =
                TextKey.of("jsc.cli.job.at.example.delete", "take the second off the list");
        private static final TextKey BAD_TIME =
                TextKey.of("jsc.cli.job.at.bad_time", "The time is not in the right format.");

        @Override public CommandScope scope() {
            return CommandScope.on(CommandScope.DOS_SYSTEMS);
        }

        @Override public String name() {
            return "at";
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

        @Override public List<Option> options() {
            return List.of(new Option("/EVERY:M,W,F", OPTION_EVERY), new Option("<id> /DELETE", OPTION_DELETE));
        }

        @Override public List<Example> examples() {
            return List.of(new Example("AT 06:00 INTERAC GET 64 COAL /LOCAL", EXAMPLE_MORNING),
                    new Example("AT", EXAMPLE_LIST),
                    new Example("AT 2 /DELETE", EXAMPLE_DELETE));
        }

        @Override public List<String> seeAlso() {
            return List.of("start");
        }

        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs()) {
                list(ctx, true);
                return;
            }
            /*
             * Only "AT id /DELETE" takes a job off. The same word further along a line that schedules one is the
             * scheduled command's own, and reading it as a delete took the time for the job's number.
             */
            if (ctx.argCount() == 2 && ctx.arg(1).equalsIgnoreCase("/delete")) {
                stop(ctx, ctx.arg(0), true);
                return;
            }
            final int hour = JobWhen.hourOf(ctx.arg(0));
            if (hour < 0) {
                ctx.out().error(BAD_TIME);
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
