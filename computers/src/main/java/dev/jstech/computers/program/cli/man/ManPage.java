/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli.man;

import dev.jstech.computers.program.cli.ICliCommand;
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
 * <p>Pure: a command in, lines out. Which commands a machine has is somebody else's answer.
 */
public final class ManPage {

    /** How far the text of a section is set in, which is what a manual page has always done. */
    private static final String INDENT = "     ";

    private ManPage() {
    }

    /** The whole page, section by section. */
    public static List<Section> of(final ICliCommand command) {
        final List<Section> page = new ArrayList<>();
        page.add(new Section("NAME", List.of(command.name() + " - " + command.summary())));
        if (!command.usage().isEmpty()) {
            page.add(new Section("SYNOPSIS", List.of(command.name() + " " + command.usage())));
        }
        if (!command.description().isEmpty()) {
            page.add(new Section("DESCRIPTION", command.description()));
        }
        if (!command.options().isEmpty()) {
            final List<String> lines = new ArrayList<>();
            for (final ICliCommand.Option option : command.options()) {
                lines.add(option.flag());
                lines.add("  " + option.what());
            }
            page.add(new Section("OPTIONS", lines));
        }
        if (!command.examples().isEmpty()) {
            final List<String> lines = new ArrayList<>();
            for (final ICliCommand.Example example : command.examples()) {
                lines.add(example.line());
                if (!example.what().isEmpty()) {
                    lines.add("  " + example.what());
                }
            }
            page.add(new Section("EXAMPLES", lines));
        }
        if (!command.seeAlso().isEmpty()) {
            page.add(new Section("SEE ALSO", List.of(String.join(", ", command.seeAlso()))));
        }
        return page;
    }

    /**
     * The page as plain lines, which is what a terminal prints.
     *
     * @param heading how a heading is written, since the families write them differently
     */
    public static List<String> lines(final ICliCommand command, final boolean upperHeadings) {
        final List<String> out = new ArrayList<>();
        for (final Section section : of(command)) {
            out.add(upperHeadings ? section.heading() : title(section.heading()));
            for (final String line : section.lines()) {
                out.add(INDENT + line);
            }
        }
        return out;
    }

    /** The one line {@code whatis} answers with. */
    public static String whatis(final ICliCommand command) {
        return command.name() + " (1) - " + command.summary();
    }

    /**
     * Whether a command's page answers to that word, which is the question {@code apropos} asks of every page.
     *
     * <p>The name and the one-line summary, because that is what those tools have always searched and because
     * searching a whole page turns every list into every command.
     */
    public static boolean answersTo(final ICliCommand command, final String text) {
        final String wanted = text.toLowerCase(Locale.ROOT);
        return command.name().toLowerCase(Locale.ROOT).contains(wanted)
                || command.summary().toLowerCase(Locale.ROOT).contains(wanted);
    }

    /** "SEE ALSO" written the way a family that does not shout writes it. */
    private static String title(final String heading) {
        final String lower = heading.toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    /** One part of a page: its heading and what is under it. */
    public record Section(String heading, List<String> lines) {

        public Section {
            lines = List.copyOf(lines);
        }
    }
}
