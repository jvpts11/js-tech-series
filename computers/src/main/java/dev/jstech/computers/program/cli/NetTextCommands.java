/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import dev.jstech.computers.program.cli.sh.TextFilters;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
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
    @TextHolder
    static final class FindText implements ICliCommand {

        private static final TextKey SUMMARY =
                TextKey.of("jsc.cli.nettext.findtext.summary", "the lines that hold some text");
        private static final TextKey USAGE = TextKey.of("jsc.cli.nettext.findtext.usage", "<text> [file]");
        private static final TextKey ABOUT = TextKey.of("jsc.cli.nettext.findtext.about",
                "Prints the lines holding that text, from the file named or from whatever was handed over by a"
                        + " pipe. Case matters unless you say it does not.");
        private static final TextKey OPTION_ANY = TextKey.of("jsc.cli.nettext.findtext.option.any",
                "match however it is written, large or small");
        private static final TextKey OPTION_WITHOUT =
                TextKey.of("jsc.cli.nettext.findtext.option.without", "the lines that do NOT hold it");
        private static final TextKey EXAMPLE_FILE = TextKey.of("jsc.cli.nettext.findtext.example.file",
                "the lines of that file about cables");
        private static final TextKey EXAMPLE_PIPE =
                TextKey.of("jsc.cli.nettext.findtext.example.pipe", "the patterns on this disk");

        @Override public CommandScope scope() {
            return NET_TEXT;
        }

        @Override public String name() {
            return "findtext";
        }

        @Override public CommandGroup group() {
            return CommandGroup.TEXT;
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
            return List.of(new Option("-any", OPTION_ANY), new Option("-without", OPTION_WITHOUT));
        }

        @Override public List<Example> examples() {
            return List.of(new Example("findtext cable notes.txt", EXAMPLE_FILE),
                    new Example("listfiles | findtext craft", EXAMPLE_PIPE));
        }

        @Override public List<String> seeAlso() {
            return List.of("sortlines", "read", "seefile");
        }

        @Override public void run(final CliContext ctx) {
            final List<String> words = PipeCommands.plainWords(ctx);
            if (words.isEmpty()) {
                ctx.out().error(CliTexts.USAGE.with(name(), usage()));
                return;
            }
            final List<String> lines = PipeCommands.linesFor(ctx, words.size() > 1 ? words.get(1) : "");
            PipeCommands.print(ctx, TextFilters.holding(lines, words.get(0),
                    PipeCommands.flag(ctx, "-any", ""), PipeCommands.flag(ctx, "-without", "")));
        }
    }

    /** The same lines, in order. */
    @TextHolder
    static final class SortLines implements ICliCommand {

        private static final TextKey SUMMARY = TextKey.of("jsc.cli.nettext.sortlines.summary", "put lines in order");
        private static final TextKey USAGE = TextKey.of("jsc.cli.nettext.sortlines.usage", "[file]");
        private static final TextKey OPTION_BACK =
                TextKey.of("jsc.cli.nettext.sortlines.option.back", "the other way round");
        private static final TextKey OPTION_ONCE =
                TextKey.of("jsc.cli.nettext.sortlines.option.once", "drop a line that is already there");

        @Override public CommandScope scope() {
            return NET_TEXT;
        }

        @Override public String name() {
            return "sortlines";
        }

        @Override public CommandGroup group() {
            return CommandGroup.TEXT;
        }

        @Override public Text summary() {
            return SUMMARY.text();
        }

        @Override public Text usage() {
            return USAGE.text();
        }

        @Override public List<Option> options() {
            return List.of(new Option("-back", OPTION_BACK), new Option("-once", OPTION_ONCE));
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
