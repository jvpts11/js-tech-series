/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import dev.jstech.computers.program.cli.sh.TextFilters;
import java.util.List;

/**
 * The network appliance's words for working on lines.
 *
 * <p>The same three tools both older families had, under names that say what they do. They read a file, or
 * whatever a pipe handed them, exactly as the others do: what differs is the word typed, never the work.
 *
 * <p>{@code read} is the pager, and it is a separate verb from {@code seefile} for the reason DOS kept
 * {@code more} apart from {@code type}: putting a long file on the glass all at once is not reading it.
 */
final class NetTextCommands {

    /** Working on lines needs lines, and on this machine they come from a file or from a pipe. */
    private static final CommandScope NET_TEXT =
            CommandScope.on(CommandScope.NET_SYSTEMS).needing(CommandScope.Need.FILES);

    private NetTextCommands() {
    }

    static List<ICliCommand> all() {
        return List.of(new PagerCommand("read", CommandScope.NET_SYSTEMS), new FindText(), new SortLines());
    }

    /** The lines of a file, or of what a pipe handed over, that hold some text. */
    static final class FindText implements ICliCommand {
        @Override public CommandScope scope() {
            return NET_TEXT;
        }

        @Override public String name() {
            return "findtext";
        }

        @Override public CommandGroup group() {
            return CommandGroup.TEXT;
        }

        @Override public String summary() {
            return "the lines that hold some text";
        }

        @Override public String usage() {
            return "<text> [file]";
        }

        @Override public List<String> description() {
            return List.of("Prints the lines holding that text, from the file named or from whatever was",
                    "handed over by a pipe. Case matters unless you say it does not.");
        }

        @Override public List<Option> options() {
            return List.of(new Option("-any", "match however it is written, large or small"),
                    new Option("-without", "the lines that do NOT hold it"));
        }

        @Override public List<Example> examples() {
            return List.of(new Example("findtext cable notes.txt", "the lines of that file about cables"),
                    new Example("listfiles | findtext craft", "the patterns on this disk"));
        }

        @Override public List<String> seeAlso() {
            return List.of("sortlines", "read", "seefile");
        }

        @Override public void run(final CliContext ctx) {
            final List<String> words = PipeCommands.plainWords(ctx);
            if (words.isEmpty()) {
                ctx.out().error("usage: findtext <text> [file]");
                return;
            }
            final List<String> lines = PipeCommands.linesFor(ctx, words.size() > 1 ? words.get(1) : "");
            PipeCommands.print(ctx, TextFilters.holding(lines, words.get(0),
                    PipeCommands.flag(ctx, "-any", ""), PipeCommands.flag(ctx, "-without", "")));
        }
    }

    /** The same lines, in order. */
    static final class SortLines implements ICliCommand {
        @Override public CommandScope scope() {
            return NET_TEXT;
        }

        @Override public String name() {
            return "sortlines";
        }

        @Override public CommandGroup group() {
            return CommandGroup.TEXT;
        }

        @Override public String summary() {
            return "put lines in order";
        }

        @Override public String usage() {
            return "[file]";
        }

        @Override public List<Option> options() {
            return List.of(new Option("-back", "the other way round"),
                    new Option("-once", "drop a line that is already there"));
        }

        @Override public List<String> seeAlso() {
            return List.of("findtext", "read");
        }

        @Override public void run(final CliContext ctx) {
            final List<String> words = PipeCommands.plainWords(ctx);
            PipeCommands.print(ctx, TextFilters.ordered(
                    PipeCommands.linesFor(ctx, words.isEmpty() ? "" : words.get(0)),
                    PipeCommands.flag(ctx, "-back", ""), PipeCommands.flag(ctx, "-once", "")));
        }
    }
}
