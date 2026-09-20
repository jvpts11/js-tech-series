/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import dev.jstech.computers.program.cli.sh.TextFilters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The tools that work on lines: what is handed to them by a pipe, or a file they are given the name of.
 *
 * <p>These are what a pipe is for. Until there was one there was no reason for them, and with one there is no
 * end of reasons: which servers hold something and how many of them, what a listing says about one drive, the
 * last few rows of a log. Each wears its family's name and flags over the one answer in {@link TextFilters}.
 */
final class PipeCommands {

    /** Where a tool that works on lines can be: anywhere with files, since a file is what it reads. */
    private static final CommandScope TEXT_TOOL = CommandScope.everywhere().needing(CommandScope.Need.FILES);

    /** How many lines {@code head} and {@code tail} take when nobody says. */
    private static final int USUALLY = 10;

    private PipeCommands() {
    }

    /** The POSIX tools, which is where these names come from. */
    static List<ICliCommand> posix() {
        return List.of(new Grep(), new Wc(), new Head(), new Tail(), new Sort(),
                new PagerCommand("less", true));
    }

    /** The same tools under the names and switches the DOS family has always written them with. */
    static List<ICliCommand> dos() {
        return List.of(new DosFind(), new DosSort(), new PagerCommand("more", false));
    }

    /**
     * The lines a tool is working on: what a pipe handed it, or the file it was told to read.
     *
     * @param from the argument that may be a file name, or empty when the tool takes none
     */
    private static List<String> linesFor(final CliContext ctx, final String from) {
        if (ctx.hasInput() || from.isEmpty()) {
            return ctx.input();
        }
        final ICliComputer.FsResult read = ctx.computer().readFile(from);
        if (!read.ok()) {
            ctx.out().error(read.message());
            return List.of();
        }
        return List.of(read.message().split("\r?\n", -1));
    }

    private static void print(final CliContext ctx, final List<String> lines) {
        for (final String line : lines) {
            ctx.out().line(line);
        }
    }

    /** The words of a line that are not switches, which is how every one of these reads its arguments. */
    private static List<String> plainWords(final CliContext ctx) {
        final List<String> words = new ArrayList<>();
        for (final String arg : ctx.args()) {
            if (!arg.startsWith("-") && !arg.startsWith("/")) {
                words.add(arg);
            }
        }
        return words;
    }

    private static boolean flag(final CliContext ctx, final String unix, final String dos) {
        for (final String arg : ctx.args()) {
            final String said = arg.toLowerCase(Locale.ROOT);
            if (said.equals(unix) || said.equals(dos)) {
                return true;
            }
        }
        return false;
    }

    /** How many lines a {@code -n} asked for, or the usual ten. */
    private static int many(final CliContext ctx) {
        for (int i = 0; i < ctx.argCount(); i++) {
            final String arg = ctx.arg(i);
            if (arg.equals("-n") && i + 1 < ctx.argCount()) {
                return whole(ctx.arg(i + 1));
            }
            if (arg.startsWith("-") && arg.length() > 1 && Character.isDigit(arg.charAt(1))) {
                return whole(arg.substring(1));
            }
        }
        return USUALLY;
    }

    private static int whole(final String text) {
        try {
            return Math.max(0, Integer.parseInt(text.trim()));
        } catch (final NumberFormatException notANumber) {
            return USUALLY;
        }
    }

    static final class Grep implements ICliCommand {
        @Override public CommandScope scope() {
            return CommandScope.on(CommandScope.UNIX_SYSTEMS).needing(CommandScope.Need.FILES);
        }

        @Override public String name() {
            return "grep";
        }

        @Override public String summary() {
            return "print the lines that hold some text";
        }

        @Override public String usage() {
            return "[-i] [-v] <text> [file]";
        }

        @Override public List<String> description() {
            return List.of("Prints the lines that hold some text: the lines of the file it is given, or the",
                    "lines a pipe hands it. It is what makes a listing worth asking for, since the answer",
                    "can then be narrowed to the one line that matters.");
        }

        @Override public List<Option> options() {
            return List.of(new Option("-i", "without regard to case"),
                    new Option("-v", "the lines that do NOT hold it"));
        }

        @Override public List<Example> examples() {
            return List.of(new Example("interac list | grep oak", "only the rows about oak"),
                    new Example("grep -i error /var/log/cron", "and in a file, whatever case it was written in"));
        }

        @Override public List<String> seeAlso() {
            return List.of("sort", "wc", "head", "tail");
        }

        @Override public void run(final CliContext ctx) {
            final List<String> words = plainWords(ctx);
            if (words.isEmpty()) {
                ctx.out().error("usage: grep " + usage());
                return;
            }
            final List<String> lines = linesFor(ctx, words.size() > 1 ? words.get(1) : "");
            print(ctx, TextFilters.holding(lines, words.get(0), flag(ctx, "-i", ""), flag(ctx, "-v", "")));
        }
    }

    static final class Wc implements ICliCommand {
        @Override public CommandScope scope() {
            return CommandScope.on(CommandScope.UNIX_SYSTEMS).needing(CommandScope.Need.FILES);
        }

        @Override public String name() {
            return "wc";
        }

        @Override public String summary() {
            return "count the lines, words and characters";
        }

        @Override public String usage() {
            return "[-l] [-w] [file]";
        }

        @Override public void run(final CliContext ctx) {
            final List<String> words = plainWords(ctx);
            final TextFilters.Counts counts =
                    TextFilters.counted(linesFor(ctx, words.isEmpty() ? "" : words.get(0)));
            if (flag(ctx, "-l", "")) {
                ctx.out().line(String.valueOf(counts.lines()));
            } else if (flag(ctx, "-w", "")) {
                ctx.out().line(String.valueOf(counts.words()));
            } else {
                ctx.out().line(counts.lines() + " " + counts.words() + " " + counts.letters());
            }
        }
    }

    static final class Head implements ICliCommand {
        @Override public CommandScope scope() {
            return CommandScope.on(CommandScope.UNIX_SYSTEMS).needing(CommandScope.Need.FILES);
        }

        @Override public String name() {
            return "head";
        }

        @Override public String summary() {
            return "print the first lines";
        }

        @Override public String usage() {
            return "[-n count] [file]";
        }

        @Override public void run(final CliContext ctx) {
            final List<String> words = plainWords(ctx);
            print(ctx, TextFilters.first(linesFor(ctx, words.isEmpty() ? "" : words.get(0)), many(ctx)));
        }
    }

    static final class Tail implements ICliCommand {
        @Override public CommandScope scope() {
            return CommandScope.on(CommandScope.UNIX_SYSTEMS).needing(CommandScope.Need.FILES);
        }

        @Override public String name() {
            return "tail";
        }

        @Override public String summary() {
            return "print the last lines";
        }

        @Override public String usage() {
            return "[-n count] [file]";
        }

        @Override public void run(final CliContext ctx) {
            final List<String> words = plainWords(ctx);
            print(ctx, TextFilters.last(linesFor(ctx, words.isEmpty() ? "" : words.get(0)), many(ctx)));
        }
    }

    static final class Sort implements ICliCommand {
        @Override public CommandScope scope() {
            return CommandScope.on(CommandScope.UNIX_SYSTEMS).needing(CommandScope.Need.FILES);
        }

        @Override public String name() {
            return "sort";
        }

        @Override public String summary() {
            return "put the lines in order";
        }

        @Override public String usage() {
            return "[-r] [-u] [file]";
        }

        @Override public void run(final CliContext ctx) {
            final List<String> words = plainWords(ctx);
            print(ctx, TextFilters.ordered(linesFor(ctx, words.isEmpty() ? "" : words.get(0)),
                    flag(ctx, "-r", ""), flag(ctx, "-u", "")));
        }
    }

    /** {@code FIND "text"}: the DOS family's grep, quotes and all. */
    static final class DosFind implements ICliCommand {
        @Override public CommandScope scope() {
            return CommandScope.on(CommandScope.DOS_SYSTEMS).needing(CommandScope.Need.FILES);
        }

        @Override public String name() {
            return "find";
        }

        @Override public String summary() {
            return "print the lines that hold some text";
        }

        @Override public String usage() {
            return "[/I] [/V] \"text\" [file]";
        }

        @Override public void run(final CliContext ctx) {
            final List<String> words = plainWords(ctx);
            if (words.isEmpty()) {
                ctx.out().error("usage: FIND " + usage());
                return;
            }
            final List<String> lines = linesFor(ctx, words.size() > 1 ? words.get(1) : "");
            print(ctx, TextFilters.holding(lines, words.get(0), flag(ctx, "", "/i"), flag(ctx, "", "/v")));
        }
    }

    static final class DosSort implements ICliCommand {
        @Override public CommandScope scope() {
            return CommandScope.on(CommandScope.DOS_SYSTEMS).needing(CommandScope.Need.FILES);
        }

        @Override public String name() {
            return "sort";
        }

        @Override public String summary() {
            return "put the lines in order";
        }

        @Override public String usage() {
            return "[/R] [file]";
        }

        @Override public void run(final CliContext ctx) {
            final List<String> words = plainWords(ctx);
            print(ctx, TextFilters.ordered(linesFor(ctx, words.isEmpty() ? "" : words.get(0)),
                    flag(ctx, "", "/r"), false));
        }
    }
}
