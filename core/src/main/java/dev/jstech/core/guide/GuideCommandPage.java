/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide;

import dev.jstech.core.text.Text;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * The parts of a command's page as a manual prints them, worked out once for the pages of a book and for the page read
 * as text, so the two say the same: what it is for as a sentence, its options and examples as tables of what each
 * does, the systems it runs on, and the commands to read next.
 */
final class GuideCommandPage {

    private GuideCommandPage() {
    }

    /** The entry of those contents that prints each command's page, by the command's name; the first of two. */
    static Map<String, String> entries(final GuideContents contents, final IGuideText text) {
        final Map<String, String> entries = new HashMap<>();
        for (final GuideContents.Chapter chapter : contents.chapters()) {
            for (final GuideContents.Section section : chapter.sections()) {
                for (final GuideEntry entry : section.entries()) {
                    for (final GuideBlock block : entry.blocks()) {
                        if (block instanceof GuideBlock.Command command) {
                            text.command(command.source()).ifPresent(found -> entries.putIfAbsent(found.name(),
                                    entry.id()));
                        }
                    }
                }
            }
        }
        return entries;
    }

    /** What the command is for, in the reader's language, as a sentence of its own; empty when it says nothing. */
    static String lead(final GuideCommand command, final IGuideText text) {
        final String said = text.said(command.summary()).strip();
        if (said.isEmpty()) {
            return "";
        }
        // A command says what it is for as a man page's one line does, in small letters with no stop.
        final String first = said.substring(0, said.offsetByCodePoints(0, 1));
        final String sentence = first.toUpperCase(Locale.ROOT) + said.substring(first.length());
        return Character.isLetterOrDigit(sentence.codePointBefore(sentence.length())) ? sentence + "." : sentence;
    }

    /** How it is typed: its name, and after it what it takes, in the reader's language. */
    static String synopsis(final GuideCommand command, final IGuideText text) {
        final String usage = text.said(command.usage()).strip();
        return usage.isEmpty() ? command.name() : command.name() + " " + usage;
    }

    /** What it does, a paragraph each, in the reader's language. */
    static List<String> description(final GuideCommand command, final IGuideText text) {
        final List<String> paragraphs = new ArrayList<>();
        for (final Text paragraph : command.description()) {
            final String said = text.said(paragraph).strip();
            if (!said.isEmpty()) {
                paragraphs.add(said);
            }
        }
        return paragraphs;
    }

    /** Its options as a table: each as it is typed, and what it does. */
    static GuideBlock.Table options(final GuideCommand command) {
        final List<GuideBlock.TableRow> rows = new ArrayList<>();
        for (final GuideCommand.Option option : command.options()) {
            rows.add(new GuideBlock.TableRow(List.of(new GuideBlock.GuideValue.Literal(option.flag()),
                    new GuideBlock.GuideValue.Said(option.what()))));
        }
        return new GuideBlock.Table("", List.of(GuideTexts.OPTION.key(), GuideTexts.WHAT_IT_DOES.key()), rows);
    }

    /** Its examples as a table: each line as typed, and what it does. */
    static GuideBlock.Table examples(final GuideCommand command) {
        final List<GuideBlock.TableRow> rows = new ArrayList<>();
        for (final GuideCommand.Example example : command.examples()) {
            rows.add(new GuideBlock.TableRow(List.of(new GuideBlock.GuideValue.Literal(example.line()),
                    new GuideBlock.GuideValue.Said(example.what()))));
        }
        return new GuideBlock.Table("", List.of(GuideTexts.EXAMPLE.key(), GuideTexts.WHAT_IT_DOES.key()), rows);
    }

    /** "Runs on MC-DOS 6.22, Frames 95 and Frames 98.", or empty when it names no system. */
    static String runsOn(final GuideCommand command, final IGuideText text) {
        final List<String> systems = new ArrayList<>();
        for (final Text system : command.systems()) {
            systems.add(text.said(system));
        }
        return systems.isEmpty() ? "" : text.text(GuideTexts.RUNS_ON.key(), joined(systems, text));
    }

    /**
     * "Read next: copy, move and ren.", each name written as {@code named} writes it (a link, where the manual has
     * that command's page), or empty when it names none.
     */
    static String readNext(final GuideCommand command, final IGuideText text, final UnaryOperator<String> named) {
        final List<String> names = new ArrayList<>();
        for (final String name : command.seeAlso()) {
            names.add(named.apply(name));
        }
        return names.isEmpty() ? "" : text.text(GuideTexts.READ_NEXT.key(), joined(names, text));
    }

    /** Words joined the way a sentence lists them: "a", "a and b", "a, b and c". */
    private static String joined(final List<String> words, final IGuideText text) {
        if (words.size() == 1) {
            return words.getFirst();
        }
        return String.join(", ", words.subList(0, words.size() - 1)) + " " + text.text(GuideTexts.AND.key()) + " "
                + words.getLast();
    }
}
