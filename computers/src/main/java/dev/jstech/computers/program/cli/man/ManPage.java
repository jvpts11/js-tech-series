/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli.man;

import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.computers.program.cli.CliSpan;
import dev.jstech.computers.program.cli.CliStyle;
import dev.jstech.computers.program.cli.ICliCommand;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A command's manual page, made of what the command says about itself.
 *
 * <p>There is one body of text about a command and it lives on the command. This lays it out the way a manual
 * page has been laid out since there were manual pages, and everything that teaches a player reads this same
 * page: {@code man} at a Unix prompt, {@code HELP} and {@code /?} at a DOS one, and the Help window on a
 * desktop. A command that gains an option gains it in all of them at once, because there is nowhere else for
 * the words to be.
 *
 * <p>The page is text still to be put in a language, so each reader gets it in their own. Pure: a command in,
 * lines out. Which commands a machine has is somebody else's answer.
 */
@TextHolder
public final class ManPage {

    /** How far the text of a section is set in, which is what a manual page has always done. */
    private static final String INDENT = "     ";

    /*
     * The headings, as each family writes them: the Unix manual in capitals, the DOS help in a title's case. Both are
     * declared rather than worked out from one, since how a heading is written in capitals is the language's to say.
     */
    private static final TextKey NAME = TextKey.of("jsc.man.name", "NAME");
    private static final TextKey NAME_TITLE = TextKey.of("jsc.man.name.title", "Name");
    private static final TextKey SYNOPSIS = TextKey.of("jsc.man.synopsis", "SYNOPSIS");
    private static final TextKey SYNOPSIS_TITLE = TextKey.of("jsc.man.synopsis.title", "Synopsis");
    private static final TextKey DESCRIPTION = TextKey.of("jsc.man.description", "DESCRIPTION");
    private static final TextKey DESCRIPTION_TITLE = TextKey.of("jsc.man.description.title", "Description");
    private static final TextKey OPTIONS = TextKey.of("jsc.man.options", "OPTIONS");
    private static final TextKey OPTIONS_TITLE = TextKey.of("jsc.man.options.title", "Options");
    private static final TextKey EXAMPLES = TextKey.of("jsc.man.examples", "EXAMPLES");
    private static final TextKey EXAMPLES_TITLE = TextKey.of("jsc.man.examples.title", "Examples");
    private static final TextKey SEE_ALSO = TextKey.of("jsc.man.see_also", "SEE ALSO");
    private static final TextKey SEE_ALSO_TITLE = TextKey.of("jsc.man.see_also.title", "See also");

    private ManPage() {
    }

    /** The whole page, section by section. */
    public static List<Section> of(final ICliCommand command) {
        final List<Section> page = new ArrayList<>();
        page.add(new Section(Heading.NAME,
                List.of(CliLine.of(CliSpan.plain(command.name() + " - "), CliSpan.plain(command.summary())))));
        if (!command.usage().isEmpty()) {
            page.add(new Section(Heading.SYNOPSIS, List.of(synopsis(command, CliStyle.PLAIN))));
        }
        if (!command.description().isEmpty()) {
            final List<CliLine> lines = new ArrayList<>();
            for (final Text paragraph : command.description()) {
                if (!lines.isEmpty()) {
                    lines.add(CliLine.plain(""));
                }
                lines.add(CliLine.plain(paragraph));
            }
            page.add(new Section(Heading.DESCRIPTION, lines));
        }
        if (!command.options().isEmpty()) {
            final List<CliLine> lines = new ArrayList<>();
            for (final ICliCommand.Option option : command.options()) {
                lines.add(CliLine.plain(option.flag()));
                lines.add(CliLine.of(CliSpan.plain("  "), CliSpan.plain(option.what())));
            }
            page.add(new Section(Heading.OPTIONS, lines));
        }
        if (!command.examples().isEmpty()) {
            final List<CliLine> lines = new ArrayList<>();
            for (final ICliCommand.Example example : command.examples()) {
                lines.add(CliLine.plain(example.line()));
                if (!example.what().isEmpty()) {
                    lines.add(CliLine.of(CliSpan.plain("  "), CliSpan.plain(example.what())));
                }
            }
            page.add(new Section(Heading.EXAMPLES, lines));
        }
        if (!command.seeAlso().isEmpty()) {
            page.add(new Section(Heading.SEE_ALSO, List.of(CliLine.plain(String.join(", ", command.seeAlso())))));
        }
        return page;
    }

    /**
     * The page as lines, which is what a terminal prints: each heading, and under it its lines set in.
     *
     * @param upperHeadings whether the headings are the Unix manual's capitals rather than the DOS help's title case
     */
    public static List<CliLine> lines(final ICliCommand command, final boolean upperHeadings) {
        final List<CliLine> out = new ArrayList<>();
        for (final Section section : of(command)) {
            out.add(CliLine.of(upperHeadings ? section.heading().upper() : section.heading().title(),
                    CliStyle.PLAIN));
            for (final CliLine line : section.lines()) {
                out.add(CliLine.build().plain(INDENT).add(line).done());
            }
        }
        return out;
    }

    /** How the command is typed: its name, and after it what it takes, in that colour. */
    public static CliLine synopsis(final ICliCommand command, final CliStyle style) {
        if (command.usage().isEmpty()) {
            return new CliLine(command.name(), style);
        }
        return CliLine.of(new CliSpan(command.name() + " ", style), new CliSpan(command.usage(), style));
    }

    /** The one line {@code whatis} answers with. */
    public static CliLine whatis(final ICliCommand command) {
        return CliLine.of(CliSpan.plain(command.name() + " (1) - "), CliSpan.plain(command.summary()));
    }

    /**
     * Whether a command's page answers to that word, which is the question {@code apropos} asks of every page.
     *
     * <p>The name and the one-line summary, because that is what those tools have always searched and because
     * searching a whole page turns every list into every command. The summary is searched as the machine keeps
     * it, in English, since the machine does not know whose language the word was typed in.
     */
    public static boolean answersTo(final ICliCommand command, final String text) {
        final String wanted = text.toLowerCase(Locale.ROOT);
        return command.name().toLowerCase(Locale.ROOT).contains(wanted)
                || command.summary().english().toLowerCase(Locale.ROOT).contains(wanted);
    }

    /** The parts a page has, in the order it has them. */
    public enum Heading {
        NAME, SYNOPSIS, DESCRIPTION, OPTIONS, EXAMPLES, SEE_ALSO;

        /** The heading as the Unix manual writes it. */
        public TextKey upper() {
            return switch (this) {
                case NAME -> ManPage.NAME;
                case SYNOPSIS -> ManPage.SYNOPSIS;
                case DESCRIPTION -> ManPage.DESCRIPTION;
                case OPTIONS -> ManPage.OPTIONS;
                case EXAMPLES -> ManPage.EXAMPLES;
                case SEE_ALSO -> ManPage.SEE_ALSO;
            };
        }

        /** The heading as the DOS help writes it. */
        public TextKey title() {
            return switch (this) {
                case NAME -> NAME_TITLE;
                case SYNOPSIS -> SYNOPSIS_TITLE;
                case DESCRIPTION -> DESCRIPTION_TITLE;
                case OPTIONS -> OPTIONS_TITLE;
                case EXAMPLES -> EXAMPLES_TITLE;
                case SEE_ALSO -> SEE_ALSO_TITLE;
            };
        }
    }

    /** One part of a page: its heading and what is under it. */
    public record Section(Heading heading, List<CliLine> lines) {

        public Section {
            lines = List.copyOf(lines);
        }
    }
}
