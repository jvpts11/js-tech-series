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
    static final class Clear implements ICliCommand, CliShell.IClearMarker {
        @Override public CommandScope scope() {
            return NET_MACHINE;
        }

        @Override public String name() {
            return "clear";
        }

        @Override public CommandGroup group() {
            return CommandGroup.MACHINE;
        }

        @Override public String summary() {
            return "wipe the glass";
        }

        @Override public void run(final CliContext ctx) {
            /* The shell wipes the scrollback because this command is the marker; there is nothing to print. */
        }
    }

    /** What the memory is spent on: what is fitted, what was promised, and what is really in it. */
    static final class Memory implements ICliCommand {
        @Override public CommandScope scope() {
            return NET_MACHINE;
        }

        @Override public String name() {
            return "memory";
        }

        @Override public CommandGroup group() {
            return CommandGroup.MACHINE;
        }

        @Override public String summary() {
            return "what this machine's memory is spent on";
        }

        @Override public List<String> description() {
            return List.of("Three numbers, and they are not the same one. Fitted is the memory in the machine.",
                    "Promised is what has been set aside for the things it agreed to run. Held is what those",
                    "things are really using at this moment, which is the one that moves while you watch.");
        }

        @Override public List<String> seeAlso() {
            return List.of("tasklist", "end");
        }

        @Override public void run(final CliContext ctx) {
            final ICliComputer.MemoryUse use = ctx.computer().memory();
            ctx.out().row("Fitted", use.totalMb() + " mB");
            ctx.out().row("Promised", use.usedMb() + " mB");
            ctx.out().row("Held", RamLedger.heldLabel(use.heldBytes()));
            ctx.out().row("Free", Math.max(0, use.totalMb() - use.usedMb()) + " mB");
        }
    }

    /** What the machine is running. */
    static final class Tasklist implements ICliCommand {
        @Override public CommandScope scope() {
            return NET_MACHINE;
        }

        @Override public String name() {
            return "tasklist";
        }

        @Override public CommandGroup group() {
            return CommandGroup.MACHINE;
        }

        @Override public String summary() {
            return "what this machine is running";
        }

        @Override public List<String> description() {
            return List.of("Lists what the machine is running, the number each answers to, and what each is",
                    "holding of the memory it was promised. Stop one with end.");
        }

        @Override public List<String> seeAlso() {
            return List.of("end", "memory");
        }

        @Override public void run(final CliContext ctx) {
            MachineFacts.printProcesses(ctx, ShellFamily.NET);
        }
    }

    /** Stops one of them. */
    static final class End implements ICliCommand {
        @Override public CommandScope scope() {
            return NET_MACHINE;
        }

        @Override public String name() {
            return "end";
        }

        @Override public CommandGroup group() {
            return CommandGroup.MACHINE;
        }

        @Override public String summary() {
            return "stop something this machine is running";
        }

        @Override public String usage() {
            return "<number>";
        }

        @Override public List<String> description() {
            return List.of("Stops the thing of that number, which tasklist tells you. What it was doing stops",
                    "where it stands, and what it had not finished stays not done.");
        }

        @Override public List<Example> examples() {
            return List.of(new Example("end 4", "stop the fourth thing tasklist shows"));
        }

        @Override public List<String> seeAlso() {
            return List.of("tasklist", "runbackground");
        }

        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs()) {
                ctx.out().error("usage: end <number>");
                return;
            }
            /*
             * A number after a per-cent mark is a line this machine was left with, not a running thing. The
             * appliance takes it either way, since a player reading its own job list sees the mark there.
             */
            if (ctx.arg(0).startsWith("%")) {
                final int id = MachineFacts.whole(ctx.arg(0));
                if (id > 0 && ctx.computer().stopJob(id)) {
                    ctx.out().ok("[" + id + "] done");
                } else {
                    ctx.out().error("this machine was left with no job " + ctx.arg(0));
                }
                return;
            }
            MachineFacts.stopProcess(ctx, ctx.arg(0), ShellFamily.NET);
        }
    }

    /** Where a command came from. */
    static final class FindCommand implements ICliCommand {
        @Override public CommandScope scope() {
            return NET_MACHINE;
        }

        @Override public String name() {
            return "findcommand";
        }

        @Override public CommandGroup group() {
            return CommandGroup.MACHINE;
        }

        @Override public String summary() {
            return "say where a command comes from";
        }

        @Override public String usage() {
            return "<command>";
        }

        @Override public List<String> description() {
            return List.of("Says whether a command is on this machine at all and what put it there: the system",
                    "itself, or a package installed over it, which is the one to remove if it misbehaves.");
        }

        @Override public List<String> seeAlso() {
            return List.of("showcommands", "netgetter");
        }

        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs()) {
                ctx.out().error("usage: findcommand <command>");
                return;
            }
            MachineFacts.whereIs(ctx, ctx.arg(0), ShellFamily.NET);
        }
    }

    /** The day and hour of the world this machine stands in. */
    static final class WorldTime implements ICliCommand {
        @Override public CommandScope scope() {
            return NET_MACHINE;
        }

        @Override public String name() {
            return "worldtime";
        }

        @Override public CommandGroup group() {
            return CommandGroup.MACHINE;
        }

        @Override public String summary() {
            return "the day and hour of the world";
        }

        @Override public List<String> description() {
            return List.of("There is one clock here and everything on the machine is timed by it: a line left",
                    "for an hour, a file's stamp, a log. This is that clock.");
        }

        @Override public List<String> seeAlso() {
            return List.of("schedule", "listfiles");
        }

        @Override public void run(final CliContext ctx) {
            ctx.out().line(ctx.computer().worldTime());
        }
    }

    /** Erases a disk. */
    static final class Format implements ICliCommand {
        @Override public CommandScope scope() {
            return NET_DISK;
        }

        @Override public String name() {
            return "format";
        }

        @Override public CommandGroup group() {
            return CommandGroup.FILES;
        }

        @Override public String summary() {
            return "erase everything on a disk";
        }

        @Override public String usage() {
            return "<drive> [yes]";
        }

        @Override public List<String> description() {
            return List.of("Erases a disk and everything on it. A shell that remembers nothing between lines",
                    "cannot ask you twice, so saying yes after the drive is how you mean it.");
        }

        @Override public List<Example> examples() {
            return List.of(new Example("format d", "what it would destroy, and nothing done"),
                    new Example("format d yes", "and then it is done"));
        }

        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs()) {
                ctx.out().error("usage: format <drive> [yes]");
                return;
            }
            final String said = ctx.arg(0).toUpperCase(Locale.ROOT);
            if (said.isEmpty() || !Character.isLetter(said.charAt(0))) {
                ctx.out().error("there is no drive called " + ctx.arg(0));
                return;
            }
            final char drive = said.charAt(0);
            if (ctx.argCount() < 2 || !ctx.arg(1).equalsIgnoreCase("yes")) {
                ctx.out().styled("Everything on drive " + drive + " would be lost.", CliStyle.ERROR);
                ctx.out().dim("Say 'format " + Character.toLowerCase(drive) + " yes' to do it.");
                return;
            }
            final ICliComputer.OpResult result = ctx.computer().formatDrive(drive);
            for (final String line : result.message().english().split("\n", -1)) {
                ctx.out().styled(line, result.ok() ? CliStyle.OK : CliStyle.ERROR);
            }
        }
    }

    /** Runs a stored IQL script against the network. */
    static final class Run implements ICliCommand {
        @Override public CommandScope scope() {
            return NET_DISK;
        }

        @Override public String name() {
            return "run";
        }

        @Override public String summary() {
            return "run a stored script against the network";
        }

        @Override public String usage() {
            return "<file>";
        }

        @Override public List<String> seeAlso() {
            return List.of("iql", "listfiles", "runbackground");
        }

        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs()) {
                ctx.out().error("usage: run <file>");
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
