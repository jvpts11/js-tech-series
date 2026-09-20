/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import dev.jstech.computers.program.job.JobWhen;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The network appliance's words for the software on it and the work it is left with.
 *
 * <p>Its package manager is {@code netgetter}, which is the same mirror the other families fetch from under
 * the name this one gave it. Work left behind is {@code runbackground} and {@code schedule}, said as what
 * they do rather than as the letters the older families used.
 *
 * <p>And {@code showcommands}, which is how this machine teaches. The DOS family had {@code HELP} and a
 * switch; the Unix ones have manual pages and {@code apropos}; an appliance with no manuals says everything
 * it can do, grouped by what a thing is for, on one screen.
 */
final class NetSoftwareCommands {

    /** Software on the appliance, which wherever a package comes from needs the network to come from it. */
    private static final CommandScope NET_ANY = CommandScope.on(CommandScope.NET_SYSTEMS);

    /** What the groups of {@code showcommands} are called, in the order a person meets them. */
    private static final String[] GROUPS = {"FILES", "TEXT", "MACHINE", "NETWORK", "SOFTWARE"};

    private NetSoftwareCommands() {
    }

    static List<ICliCommand> all() {
        /*
         * The package manager is the Frames family's, wearing this family's name: one manager over one
         * mirror, so a fix to how a package installs is a fix on both machines.
         */
        return List.of(new SoftwareCommands.Pckmgr("netgetter", CommandScope.NET_SYSTEMS),
                new Uninstall(), new RunBackground(), new Schedule(), new ShowCommands());
    }

    /** Takes a program off the machine. */
    static final class Uninstall implements ICliCommand {
        @Override public CommandScope scope() {
            return NET_ANY;
        }

        @Override public String name() {
            return "uninstall";
        }

        @Override public String summary() {
            return "take a program off this machine";
        }

        @Override public String usage() {
            return "<program>";
        }

        @Override public List<String> seeAlso() {
            return List.of("netgetter", "programs");
        }

        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs()) {
                ctx.out().error("usage: uninstall <program>   (see 'programs')");
                return;
            }
            final ICliComputer.OpResult result = ctx.computer().packageRemove(ctx.arg(0));
            ctx.out().styled(result.message(), result.ok() ? CliStyle.OK : CliStyle.ERROR);
        }
    }

    /** Leaves the machine running a line and gives the prompt straight back. */
    static final class RunBackground implements ICliCommand {
        @Override public CommandScope scope() {
            return NET_ANY;
        }

        @Override public String name() {
            return "runbackground";
        }

        @Override public String summary() {
            return "run a line without waiting for it";
        }

        @Override public String usage() {
            return "<line>";
        }

        @Override public List<String> description() {
            return List.of("Leaves the machine with the line and hands the prompt back at once. This machine",
                    "runs one thing at a time, so it takes them in the order they were left.");
        }

        @Override public List<Example> examples() {
            return List.of(new Example("runbackground interac craft 64 chest",
                    "ask for them and carry on"));
        }

        @Override public List<String> seeAlso() {
            return List.of("schedule", "tasklist", "end");
        }

        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs()) {
                ctx.out().error("usage: runbackground <line>");
                return;
            }
            JobCommands.add(ctx, ctx.rest(0), JobWhen.AT_ONCE, false);
        }
    }

    /** Leaves a line for an hour, and lists or forgets what was left. */
    static final class Schedule implements ICliCommand {
        @Override public CommandScope scope() {
            return NET_ANY;
        }

        @Override public String name() {
            return "schedule";
        }

        @Override public String summary() {
            return "leave a line for an hour of the world's clock";
        }

        @Override public String usage() {
            return "[hh:mm [days] <line>] [forget <number>]";
        }

        @Override public List<String> description() {
            return List.of("With nothing after it, says what this machine is already set to do. With an hour",
                    "and a line, leaves that line to run at that hour, every day unless days are named.",
                    "The shortest anything repeats is an hour of the world's clock.");
        }

        @Override public List<Example> examples() {
            return List.of(new Example("schedule 06:00 interac get 64 coal --to local",
                            "every morning at six"),
                    new Example("schedule 18:00 M,W,F interac craft 8 chest", "three evenings a week"),
                    new Example("schedule", "what it is set to do"),
                    new Example("schedule forget 2", "take the second off the list"));
        }

        @Override public List<String> seeAlso() {
            return List.of("runbackground", "worldtime", "end");
        }

        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs()) {
                JobCommands.list(ctx, false);
                return;
            }
            if (ctx.arg(0).equalsIgnoreCase("forget")) {
                JobCommands.stop(ctx, ctx.argCount() > 1 ? ctx.arg(1) : "", false);
                return;
            }
            final int hour = JobWhen.hourOf(ctx.arg(0));
            if (hour < 0) {
                ctx.out().error("say the hour as 06:00");
                return;
            }
            List<Integer> days = List.of();
            int from = 1;
            if (ctx.argCount() > 1 && JobWhen.daysOf(ctx.arg(1)).size() > 0
                    && !ctx.arg(1).contains(":")) {
                final List<Integer> named = JobWhen.daysOf(ctx.arg(1));
                if (!named.isEmpty()) {
                    days = named;
                    from = 2;
                }
            }
            JobCommands.add(ctx, ctx.rest(from), JobWhen.at(hour, days), false);
        }
    }

    /**
     * Everything this machine can run, grouped by what a thing is for.
     *
     * <p>Read off the same filter every other listing reads, so it never teaches a verb the machine does not
     * have, and never hides one it does.
     */
    static final class ShowCommands implements ICliCommand {
        @Override public CommandScope scope() {
            return NET_ANY;
        }

        @Override public String name() {
            return "showcommands";
        }

        @Override public String summary() {
            return "everything this machine can run";
        }

        @Override public List<String> description() {
            return List.of("Lists every word this machine answers to, gathered by what the thing is for.",
                    "An appliance keeps no manuals, so this is where it says what it has.");
        }

        @Override public List<String> seeAlso() {
            return List.of("findcommand", "netgetter");
        }

        @Override public void run(final CliContext ctx) {
            final Map<String, List<String>> byGroup = new LinkedHashMap<>();
            for (final String group : GROUPS) {
                byGroup.put(group, new ArrayList<>());
            }
            for (final ICliCommand command : ctx.shell().commands()) {
                if (!command.available(ctx.computer())) {
                    continue;
                }
                byGroup.get(groupOf(command.name())).add(command.name());
            }
            for (final Map.Entry<String, List<String>> group : byGroup.entrySet()) {
                if (group.getValue().isEmpty()) {
                    continue;
                }
                group.getValue().sort(String::compareTo);
                ctx.out().row(group.getKey(), String.join("  ", group.getValue()));
            }
        }

        /**
         * Which heading a word belongs under.
         *
         * <p>By the word itself rather than by where its class lives: a command an add-on registered has no
         * class of ours to be grouped by, and the player reading this has only the word either way.
         */
        private static String groupOf(final String name) {
            return switch (name.toLowerCase(Locale.ROOT)) {
                case "listfiles", "seefile", "read", "delete", "copy", "rename", "write", "format" -> "FILES";
                case "findtext", "sortlines" -> "TEXT";
                case "memory", "tasklist", "end", "worldtime", "clear", "findcommand", "status", "reboot",
                        "hostname", "whoami", "devices", "config" -> "MACHINE";
                case "interac", "iql", "net", "ssh", "exit", "gateway", "cluster" -> "NETWORK";
                default -> "SOFTWARE";
            };
        }
    }
}
