/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.testkit.computers;

import dev.jstech.computers.os.ShellFamily;
import dev.jstech.computers.program.cli.CliCommands;
import dev.jstech.computers.program.cli.ICliCommand;
import dev.jstech.computers.program.cli.ICliComputer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The lines a journey types at a machine's prompt to run every command its system offers, each with arguments that
 * work: the first line the command's manual page gives to try, or, for a command that works on something the machine
 * must have first, a line of a short sequence that makes it (a folder, a file in it, a copy, a move) before the
 * commands that read, rename and remove it. A command whose page gives no line and needs none runs by its name.
 *
 * <p>Some commands end what the journey is doing, and run elsewhere: {@link #ELSEWHERE} names each with where.
 */
public final class JourneyCommands {

    /** The commands a journey's shell area does not type, each with the area or the reason that covers it. */
    public static final Map<String, String> ELSEWHERE = Map.of(
            "reboot", "restarts the machine: typed last by the client journey",
            "format", "wipes the system's disk: formatting and reinstalling is the systems area",
            "print", "needs a printer: printing is the peripherals area",
            "taskkill", "stops a running Σ program: the programming area starts one and stops it",
            "install", "needs the program's disc in a linked drive: installing from media is the programs area",
            "uninstall", "takes off what install put on: the programs area installs and removes",
            "pckmgr", "needs a package mirror on the network: the programs area serves one");

    /*
     * The sequence of the commands that work on something, for each family of shell: each line leans on the lines
     * before it, in this order, so the folder exists before a file goes in it and a file is copied before the copy is
     * renamed and moved.
     */
    private static final Map<ShellFamily, Map<String, String>> SEQUENCES = Map.of(
            ShellFamily.DOS, dos(), ShellFamily.POSIX, posix(), ShellFamily.NET, net());

    private JourneyCommands() {
    }

    /** The lines to type at {@code computer}'s prompt, in order: the sequence's first, then every other command. */
    public static List<String> lines(final ICliComputer computer) {
        final List<ICliCommand> commands = CliCommands.all().stream().filter(command -> command.available(computer))
                .sorted(Comparator.comparing(ICliCommand::name)).toList();
        final Set<String> offered = new HashSet<>();
        commands.forEach(command -> offered.add(command.name()));
        final Map<String, String> sequence = SEQUENCES.get(computer.shellFamily());
        final List<String> lines = new ArrayList<>();
        sequence.forEach((name, line) -> {
            if (!line.isEmpty() && offered.contains(name.split("#")[0])) {
                lines.add(line);
            }
        });
        // exit leaves a session on another machine, so it follows one opened on a machine the network has.
        if (offered.contains("ssh") && !computer.reachableHosts().isEmpty()) {
            lines.add("ssh " + computer.reachableHosts().getFirst().hostname());
            lines.add("exit");
        }
        for (final ICliCommand command : commands) {
            if (!ELSEWHERE.containsKey(command.name()) && sequence.keySet().stream()
                    .noneMatch(key -> key.split("#")[0].equals(command.name()))) {
                lines.add(command.examples().isEmpty() ? command.name() : command.examples().getFirst().line());
            }
        }
        return lines;
    }

    private static Map<String, String> posix() {
        final Map<String, String> lines = new LinkedHashMap<>();
        lines.put("mkdir", "mkdir journey");
        lines.put("echo", "echo hello from the journey > journey/note.txt");
        lines.put("vi", "vi journey/note.txt");
        lines.put("ee", "ee journey/note.txt");
        lines.put("iql", "iql SELECT 1 Cobblestone");
        lines.put("ssh", "");
        lines.put("exit", "");
        return lines;
    }

    private static Map<String, String> net() {
        final Map<String, String> lines = new LinkedHashMap<>();
        lines.put("iql", "iql SELECT 1 Cobblestone");
        lines.put("ssh", "");
        lines.put("exit", "");
        return lines;
    }

    private static Map<String, String> dos() {
        final Map<String, String> lines = new LinkedHashMap<>();
        lines.put("mkdir", "mkdir journey");
        lines.put("mkdir#box", "mkdir journey\\box");
        lines.put("write", "write journey\\note.txt hello from the journey");
        lines.put("type", "type journey\\note.txt");
        lines.put("more", "more journey\\note.txt");
        lines.put("find", "find \"hello\" journey\\note.txt");
        lines.put("copy", "copy journey\\note.txt journey\\copy.txt");
        lines.put("ren", "ren journey\\copy.txt moved.txt");
        lines.put("move", "move journey\\moved.txt journey\\box");
        lines.put("del", "del journey\\box\\moved.txt");
        lines.put("rmdir", "rmdir journey\\box");
        lines.put("write#query", "write journey\\query.iql SELECT 1 Cobblestone");
        lines.put("run", "run journey\\query.iql");
        lines.put("iql", "iql SELECT 1 Cobblestone");
        lines.put("where", "where dir");
        lines.put("start", "start dir");
        lines.put("del#note", "del journey\\note.txt");
        // ssh and exit are added by the lines themselves: the host is whichever the network has.
        lines.put("ssh", "");
        lines.put("exit", "");
        return lines;
    }
}
