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
import java.util.List;
import java.util.Locale;

/**
 * The network appliance's words for the machine it is: what is running, what it is holding, what hour it is,
 * and how to wipe a disk.
 *
 * <p>The same questions every family asks, answered by the same {@link MachineFacts} they all ask, said here
 * without abbreviation. A machine of this kind never shouted a switch at anybody, so there are none: where
 * the DOS family wrote {@code TASKKILL /PID 4}, this says {@code end 4}.
 */
final class NetMachineCommands {

    /** These are about the machine itself, so they are on the appliance whatever else it has. */
    private static final CommandScope NET_MACHINE = CommandScope.on(CommandScope.NET_SYSTEMS);

    /** Wiping a disk needs a disk, which is the one thing here that does. */
    private static final CommandScope NET_DISK =
            CommandScope.on(CommandScope.NET_SYSTEMS).needing(CommandScope.Need.FILES);

    private NetMachineCommands() {
    }

    static List<ICliCommand> all() {
        return List.of(new Clear(), new Memory(), new Tasklist(), new End(), new FindCommand(),
                new WorldTime(), new Format(), new Run());
    }

    /** Wipes the glass. */
    @TextHolder
    static final class Clear implements ICliCommand, CliShell.IClearMarker {

        private static final TextKey SUMMARY = TextKey.of("jsc.cli.netmachine.clear.summary", "wipe the glass");

        @Override public CommandScope scope() {
            return NET_MACHINE;
        }

        @Override public String name() {
            return "clear";
        }

        @Override public CommandGroup group() {
            return CommandGroup.MACHINE;
        }

        @Override public Text summary() {
            return SUMMARY.text();
        }

        @Override public void run(final CliContext ctx) {
            /* The shell wipes the scrollback because this command is the marker; there is nothing to print. */
        }
    }

    /** What the memory is spent on: what is fitted, what was promised, and what is really in it. */
    @TextHolder
    static final class Memory implements ICliCommand {

        private static final TextKey SUMMARY =
                TextKey.of("jsc.cli.netmachine.memory.summary", "what this machine's memory is spent on");
        private static final TextKey ABOUT = TextKey.of("jsc.cli.netmachine.memory.about",
                "Three numbers, and they are not the same one. Fitted is the memory in the machine. Promised is"
                        + " what has been set aside for the things it agreed to run. Held is what those things are"
                        + " really using at this moment, which is the one that moves while you watch.");
        private static final TextKey FITTED = TextKey.of("jsc.cli.netmachine.memory.fitted", "Fitted");
        private static final TextKey PROMISED = TextKey.of("jsc.cli.netmachine.memory.promised", "Promised");
        private static final TextKey HELD = TextKey.of("jsc.cli.netmachine.memory.held", "Held");
        private static final TextKey FREE = TextKey.of("jsc.cli.netmachine.memory.free", "Free");

        @Override public CommandScope scope() {
            return NET_MACHINE;
        }

        @Override public String name() {
            return "memory";
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
            return List.of("tasklist", "end");
        }

        @Override public void run(final CliContext ctx) {
            final ICliComputer.MemoryUse use = ctx.computer().memory();
            ctx.out().row(FITTED.text(), Text.literal(use.totalMb() + " mB"));
            ctx.out().row(PROMISED.text(), Text.literal(use.usedMb() + " mB"));
            ctx.out().row(HELD, RamLedger.heldLabel(use.heldBytes()));
            ctx.out().row(FREE.text(), Text.literal(Math.max(0, use.totalMb() - use.usedMb()) + " mB"));
        }
    }

    /** What the machine is running. */
    @TextHolder
    static final class Tasklist implements ICliCommand {

        private static final TextKey SUMMARY =
                TextKey.of("jsc.cli.netmachine.tasklist.summary", "what this machine is running");
        private static final TextKey ABOUT = TextKey.of("jsc.cli.netmachine.tasklist.about",
                "Lists what the machine is running, the number each answers to, and what each is holding of the"
                        + " memory it was promised. Stop one with end.");

        @Override public CommandScope scope() {
            return NET_MACHINE;
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
            return List.of("end", "memory");
        }

        @Override public void run(final CliContext ctx) {
            MachineFacts.printProcesses(ctx, ShellFamily.NET);
        }
    }

    /** Stops one of them. */
    @TextHolder
    static final class End implements ICliCommand {

        private static final TextKey SUMMARY =
                TextKey.of("jsc.cli.netmachine.end.summary", "stop something this machine is running");
        private static final TextKey USAGE = TextKey.of("jsc.cli.netmachine.end.usage", "<number>");
        private static final TextKey ABOUT = TextKey.of("jsc.cli.netmachine.end.about",
                "Stops the thing of that number, which tasklist tells you. What it was doing stops where it stands,"
                        + " and what it had not finished stays not done.");
        private static final TextKey EXAMPLE_FOURTH =
                TextKey.of("jsc.cli.netmachine.end.example.fourth", "stop the fourth thing tasklist shows");
        private static final TextKey DONE = TextKey.of("jsc.cli.netmachine.end.done", "[%s] done");
        private static final TextKey NO_JOB =
                TextKey.of("jsc.cli.netmachine.end.no_job", "this machine was left with no job %s");

        @Override public CommandScope scope() {
            return NET_MACHINE;
        }

        @Override public String name() {
            return "end";
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

        @Override public List<Example> examples() {
            return List.of(new Example("end 4", EXAMPLE_FOURTH));
        }

        @Override public List<String> seeAlso() {
            return List.of("tasklist", "runbackground");
        }

        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs()) {
                ctx.out().error(CliTexts.USAGE.with(name(), usage()));
                return;
            }
            /*
             * A number after a per-cent mark is a line this machine was left with, not a running thing. The
             * appliance takes it either way, since a player reading its own job list sees the mark there.
             */
            if (ctx.arg(0).startsWith("%")) {
                final int id = MachineFacts.whole(ctx.arg(0));
                if (id > 0 && ctx.computer().stopJob(id)) {
                    ctx.out().ok(DONE.with(id));
                } else {
                    ctx.out().error(NO_JOB.with(ctx.arg(0)));
                }
                return;
            }
            MachineFacts.stopProcess(ctx, ctx.arg(0), ShellFamily.NET);
        }
    }

    /** Where a command came from. */
    @TextHolder
    static final class FindCommand implements ICliCommand {

        private static final TextKey SUMMARY =
                TextKey.of("jsc.cli.netmachine.findcommand.summary", "say where a command comes from");
        private static final TextKey USAGE = TextKey.of("jsc.cli.netmachine.findcommand.usage", "<command>");
        private static final TextKey ABOUT = TextKey.of("jsc.cli.netmachine.findcommand.about",
                "Says whether a command is on this machine at all and what put it there: the system itself, or a"
                        + " package installed over it, which is the one to remove if it misbehaves.");

        @Override public CommandScope scope() {
            return NET_MACHINE;
        }

        @Override public String name() {
            return "findcommand";
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
            return List.of("showcommands", "netgetter");
        }

        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs()) {
                ctx.out().error(CliTexts.USAGE.with(name(), usage()));
                return;
            }
            MachineFacts.whereIs(ctx, ctx.arg(0), ShellFamily.NET);
        }
    }

    /** The day and hour of the world this machine stands in. */
    @TextHolder
    static final class WorldTime implements ICliCommand {

        private static final TextKey SUMMARY =
                TextKey.of("jsc.cli.netmachine.worldtime.summary", "the day and hour of the world");
        private static final TextKey ABOUT = TextKey.of("jsc.cli.netmachine.worldtime.about",
                "There is one clock here and everything on the machine is timed by it: a line left for an hour, a"
                        + " file's stamp, a log. This is that clock.");

        @Override public CommandScope scope() {
            return NET_MACHINE;
        }

        @Override public String name() {
            return "worldtime";
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
            return List.of("schedule", "listfiles");
        }

        @Override public void run(final CliContext ctx) {
            ctx.out().line(ctx.computer().worldTime());
        }
    }

    /** Erases a disk. */
    @TextHolder
    static final class Format implements ICliCommand {

        private static final TextKey SUMMARY =
                TextKey.of("jsc.cli.netmachine.format.summary", "erase everything on a disk");
        private static final TextKey USAGE = TextKey.of("jsc.cli.netmachine.format.usage", "<drive> [yes]");
        private static final TextKey ABOUT = TextKey.of("jsc.cli.netmachine.format.about",
                "Erases a disk and everything on it. A shell that remembers nothing between lines cannot ask you"
                        + " twice, so saying yes after the drive is how you mean it.");
        private static final TextKey EXAMPLE_ASK = TextKey.of("jsc.cli.netmachine.format.example.ask",
                "what it would destroy, and nothing done");
        private static final TextKey EXAMPLE_YES =
                TextKey.of("jsc.cli.netmachine.format.example.yes", "and then it is done");
        private static final TextKey NO_DRIVE =
                TextKey.of("jsc.cli.netmachine.format.no_drive", "there is no drive called %s");
        private static final TextKey WOULD_LOSE =
                TextKey.of("jsc.cli.netmachine.format.would_lose", "Everything on drive %s would be lost.");
        private static final TextKey SAY_YES =
                TextKey.of("jsc.cli.netmachine.format.say_yes", "Say 'format %s yes' to do it.");

        @Override public CommandScope scope() {
            return NET_DISK;
        }

        @Override public String name() {
            return "format";
        }

        @Override public CommandGroup group() {
            return CommandGroup.FILES;
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

        @Override public List<Example> examples() {
            return List.of(new Example("format d", EXAMPLE_ASK), new Example("format d yes", EXAMPLE_YES));
        }

        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs()) {
                ctx.out().error(CliTexts.USAGE.with(name(), usage()));
                return;
            }
            final String said = ctx.arg(0).toUpperCase(Locale.ROOT);
            if (said.isEmpty() || !Character.isLetter(said.charAt(0))) {
                ctx.out().error(NO_DRIVE.with(ctx.arg(0)));
                return;
            }
            final char drive = said.charAt(0);
            if (ctx.argCount() < 2 || !ctx.arg(1).equalsIgnoreCase("yes")) {
                ctx.out().styled(WOULD_LOSE.with(String.valueOf(drive)), CliStyle.ERROR);
                ctx.out().dim(SAY_YES.with(String.valueOf(Character.toLowerCase(drive))));
                return;
            }
            final ICliComputer.OpResult result = ctx.computer().formatDrive(drive);
            ctx.out().styled(result.message(), result.ok() ? CliStyle.OK : CliStyle.ERROR);
        }
    }

    /** Runs a stored IQL script against the network. */
    @TextHolder
    static final class Run implements ICliCommand {

        private static final TextKey SUMMARY =
                TextKey.of("jsc.cli.netmachine.run.summary", "run a stored script against the network");
        private static final TextKey USAGE = TextKey.of("jsc.cli.netmachine.run.usage", "<file>");

        @Override public CommandScope scope() {
            return NET_DISK;
        }

        @Override public String name() {
            return "run";
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
            return List.of("iql", "listfiles", "runbackground");
        }

        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs()) {
                ctx.out().error(CliTexts.USAGE.with(name(), usage()));
                return;
            }
            final ICliComputer.FsResult result = ctx.computer().runScript(ctx.arg(0));
            if (!result.ok()) {
                ctx.out().error(result.message());
                return;
            }
            final ICliComputer.OpResult op = result.opResult();
            ctx.out().styled(op != null ? op.message() : result.message(),
                    op == null || op.ok() ? CliStyle.OK : CliStyle.ERROR);
        }
    }
}
