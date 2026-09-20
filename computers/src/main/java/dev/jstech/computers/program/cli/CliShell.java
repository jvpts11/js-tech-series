/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import dev.jstech.computers.os.ShellFamily;
import dev.jstech.computers.program.cli.man.ManPage;
import dev.jstech.computers.program.cli.sh.ShLine;
import dev.jstech.computers.program.cli.sh.ShRunner;
import dev.jstech.computers.program.job.JobWhen;
import dev.jstech.computers.program.job.MachineJobs;
import dev.jstech.computers.program.tty.ITtyProcess;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The command interpreter: it owns a set of {@link ICliCommand}s, looks one up by name or alias, and runs it against an {@link ICliComputer}, collecting the styled output. Pure logic with no Minecraft types, so the whole parse-and-dispatch path is unit-tested with a fake computer.
 */
public final class CliShell {

    private final Map<String, ICliCommand> byName = new LinkedHashMap<>();
    private final Map<String, ICliCommand> lookup = new LinkedHashMap<>();
    private final int width;

    public CliShell(final List<ICliCommand> commands, final int width) {
        this.width = width;
        for (final ICliCommand command : commands) {
            final String name = command.name().toLowerCase(Locale.ROOT);
            byName.put(name, command);
            lookup.put(name, command);
            for (final String alias : command.aliases()) {
                lookup.put(alias.toLowerCase(Locale.ROOT), command);
            }
        }
    }

    /** The commands this shell knows, in registration order, for {@code help} and tab completion. */
    public List<ICliCommand> commands() {
        return List.copyOf(byName.values());
    }

    public ICliCommand find(final String name) {
        return name == null ? null : lookup.get(name.toLowerCase(Locale.ROOT));
    }

    /**
     * Interprets one command line against the given computer.
     *
     * @return the styled output plus whether the console should be cleared first
     */
    public Response run(final String line, final ICliComputer computer) {
        final CliOutput out = new CliOutput(width);
        /*
         * What the machine has to say for itself is said before whatever the player typed (an empty Enter
         * included), the way a shell shows a finished job ahead of the next prompt.
         */
        for (final String notice : computer.drainNotices()) {
            out.ok(notice);
        }
        /*
         * What the shell does to a line before anything else sees it: puts in what a name stands for, and,
         * on the family whose shell does that, opens out a word with a star in it into the names it matches.
         */
        final boolean live = computer.liveInstall() != null;
        final List<String> tokens = live
                ? CliTokenizer.tokenize(line) : ShRunner.expand(computer, CliTokenizer.tokenize(line));
        if (tokens.isEmpty()) {
            return new Response(out.lines(), false);
        }
        /*
         * A live medium's installer is a shell of its own, swapped in whole: its verbs take the line as it was
         * typed, arrows and all, because on a real one the arrow is part of the step being taught. So the line
         * is only taken apart when an installed system is the one reading it.
         */
        /*
         * A line that ends in & is not run here at all: it is left with the machine, which runs it on its own
         * tick and hands the prompt straight back. That is what the mark has always meant on this family.
         */
        if (!live && !tokens.isEmpty() && tokens.get(tokens.size() - 1).equals("&")
                && computer.shellFamily() == ShellFamily.POSIX) {
            return backgrounded(tokens.subList(0, tokens.size() - 1), computer, out);
        }
        final ShLine whole = live ? ShLine.NOTHING : ShLine.of(tokens);
        if (!whole.isEmpty() && !whole.isSimple()) {
            return ShRunner.run(this, whole, computer, out);
        }
        final String word = tokens.get(0);
        // A bare drive qualifier like "D:" switches the current drive (DOS-style), not a command.
        if (tokens.size() == 1 && word.length() == 2 && word.charAt(1) == ':'
                && Character.isLetter(word.charAt(0))) {
            final ICliComputer.FsResult switched = computer.changeDrive(word.charAt(0));
            if (!switched.ok()) {
                out.error(switched.message());
            }
            return new Response(out.lines(), false);
        }
        /*
         * A line that is nothing but NAME=value is not a command at all: it is how the POSIX shells have
         * always given a name a value, and a player who types it expects it to have worked, not to be told
         * there is no such command.
         */
        if (tokens.size() == 1 && computer.shellFamily() == ShellFamily.POSIX
                && VariableCommands.isAssignment(word)) {
            final ICliComputer.OpResult set = VariableCommands.assignmentOf(word, computer);
            if (!set.ok()) {
                out.error(set.message());
            }
            return new Response(out.lines(), false);
        }
        final ICliCommand command = find(word);
        /*
         * A command that is not available on this computer (another distribution's package manager, an
         * uninstalled program's verbs) does not exist here, exactly like an unknown word.
         */
        if (command == null || !command.available(computer)) {
            out.error("command not found: " + word);
            out.dim("type 'help' to list commands");
            return new Response(out.lines(), false);
        }
        final List<String> args = new ArrayList<>(tokens.subList(1, tokens.size()));
        /*
         * Asking a command what it does: --help anywhere, and /? on the family that has written it that way
         * since there was a DOS. Answered here rather than by every command, so no command can be written
         * that forgets to answer, and so the words are the ones in its manual page and nowhere else.
         *
         * <p>The switch belongs to that family alone, not to everything that is not Unix: the network
         * appliance speaks neither, and teaches what it has its own way.
         */
        if (args.contains("--help")
                || (computer.shellFamily() == ShellFamily.DOS && args.contains("/?"))) {
            out.accent(command.name() + (command.usage().isEmpty() ? "" : " " + command.usage()));
            for (final String said : ManPage.lines(command, false)) {
                out.line(said);
            }
            return new Response(out.lines(), false);
        }
        final CliContext context = new CliContext(args, computer, out, this);
        try {
            command.run(context);
        } catch (final RuntimeException unexpected) {
            /*
             * A command must not throw for ordinary errors; if one does anyway, the shell stays alive
             * and reports it rather than tearing down the session.
             */
            out.error("error running '" + word + "': " + unexpected.getMessage());
        }
        final boolean clear = command instanceof IClearMarker;
        /*
         * A command that gives the terminal away says so by being one; which file is in the arguments it
         * was just run with, which the shell already has, so nothing has to be remembered anywhere.
         */
        final String file = command instanceof IHandOver giving ? giving.fileOf(computer, args) : null;
        final CliShell.HandOver handOver = file == null ? null : new HandOver(command.name(), file);
        return new Response(out.lines(), clear, handOver, out.started());
    }

    /**
     * Leaves a line with the machine instead of running it, and says under what number.
     *
     * <p>A job costs the machine a megabyte while it has it, which is what says how many a computer can be
     * left with; a machine with none left says so rather than quietly dropping the line.
     */
    private Response backgrounded(final List<String> tokens, final ICliComputer computer, final CliOutput out) {
        final ICliComputer.MemoryUse memory = computer.memory();
        if (memory.totalMb() > 0 && memory.usedMb() + MachineJobs.JOB_MB > memory.totalMb()) {
            out.error("the machine has no memory left for another job");
            return new Response(out.lines(), false);
        }
        final MachineJobs.Job job = computer.addJob(String.join(" ", tokens), JobWhen.AT_ONCE);
        if (job == null) {
            out.error("this machine keeps no jobs");
        } else {
            out.ok("[" + job.id() + "] " + job.line());
        }
        return new Response(out.lines(), false);
    }

    /**
     * Implemented by the clear-screen command so the shell can tell the console to wipe its scrollback
     * after running it, without a magic line or a special style leaking into the output model.
     */
    public interface IClearMarker {
    }

    /**
     * Implemented by a command that gives the terminal away rather than printing to it.
     *
     * <p>Which editor and which file is decided by the machine; drawing it is the screen's half. A
     * terminal that has never heard of the editor named simply carries on with its prompt.
     *
     * <p>The file comes out of the arguments the shell just handed over, so nothing here has to remember
     * anything between one line and the next.
     */
    public interface IHandOver {

        /**
         * The file the terminal is given away on, or null to keep the terminal after all.
         *
         * <p>It is named the way the player typed it, against the folder the prompt is in; the terminal that
         * draws the editor asks the machine for it by its whole name, since it knows nothing about where the
         * prompt stands. A command whose files are not the disk's names them its own way.
         */
        default String fileOf(final ICliComputer computer, final List<String> args) {
            return args.isEmpty() ? null
                    : DosPath.resolve(computer.currentLocation(), args.getFirst()).storagePath();
        }
    }

    /** The terminal was given to {@code editor}, on {@code path}. */
    public record HandOver(String editor, String path) {
    }

    /**
     * The result of one line: the output to print, whether to clear the console before printing it, the
     * editor the terminal was given to, if it was, and the tool the command left running, if it left one.
     */
    public record Response(List<CliLine> lines, boolean clearScreen, HandOver handOver, ITtyProcess started) {

        /** A reply that only printed. */
        public Response(final List<CliLine> lines, final boolean clearScreen) {
            this(lines, clearScreen, null, null);
        }

        /** A reply that printed and may have given the terminal to an editor. */
        public Response(final List<CliLine> lines, final boolean clearScreen, final HandOver handOver) {
            this(lines, clearScreen, handOver, null);
        }
    }
}
