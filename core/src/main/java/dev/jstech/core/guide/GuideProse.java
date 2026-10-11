/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The rules a manual's running text keeps, for a mod's tests to hold its chapters to: it speaks to its reader, it gives
 * every number what the number measures or counts, and it explains a word in the first entry that uses it, so a reader
 * going through the book in order never meets a word before it is explained.
 *
 * <p>What a sentence means is the writer's to judge; these are the parts of good writing a machine can see. Pure: the
 * words in, what breaks a rule out.
 */
public final class GuideProse {

    /** A number: digits, with points, commas or colons between groups of them ("1,024", "6.22", "12:30"). */
    private static final Pattern NUMBER = Pattern.compile("\\d+(?:[.,:]\\d+)*");
    /** The words that speak to a reader. */
    private static final Pattern YOU = Pattern.compile("\\b(you|your|yours|yourself)\\b");

    private GuideProse() {
    }

    /** Whether a piece of running text speaks to its reader, as an explanation does: "you", "your". */
    public static boolean speaksToTheReader(final String text) {
        return YOU.matcher(text.toLowerCase(Locale.ROOT)).find();
    }

    /**
     * The numbers of a sentence that stand bare, without what they measure or count: the 4 of "it holds 4." where "it
     * holds 4 memory modules." says what. A number in a name ("MC-DOS 6.22", "Frames 95", "S1001"), in brackets, in a
     * link, in a key or in a figure the manual reads from its mod is never bare.
     */
    public static List<String> bareNumbers(final String sentence) {
        final String plain = plainOf(sentence);
        final List<String> bare = new ArrayList<>();
        final Matcher found = NUMBER.matcher(plain);
        while (found.find()) {
            final int start = found.start();
            final int end = found.end();
            if (insideBrackets(plain, start) || touchesAName(plain, start, end) || measured(plain, end)
                    || named(plain, start)) {
                continue;
            }
            bare.add(found.group());
        }
        return bare;
    }

    /**
     * The words a book's entries explain that an entry printed before the one explaining them already uses, each with
     * the first entry using it and the one explaining it.
     */
    public static List<Early> usedBeforeExplained(final GuideContents contents, final IGuideText text) {
        final List<GuideEntry> entries = new ArrayList<>();
        for (final GuideContents.Chapter chapter : contents.chapters()) {
            for (final GuideContents.Section section : chapter.sections()) {
                entries.addAll(section.entries());
            }
        }
        final List<Early> wrong = new ArrayList<>();
        for (final GuideGlossary.Item item : GuideGlossary.of(contents, text).items()) {
            final String word = item.term().toLowerCase(Locale.ROOT);
            for (final GuideEntry entry : entries) {
                if (entry.id().equals(item.entry())) {
                    break;
                }
                if (GuideGlossary.wordAt(wordsOf(entry, text).toLowerCase(Locale.ROOT), word, 0) >= 0) {
                    wrong.add(new Early(item.term(), entry.id(), item.entry()));
                    break;
                }
            }
        }
        return wrong;
    }

    /** Every sentence of an entry a reader reads as running text, in the reader's language, one after another. */
    public static String wordsOf(final GuideEntry entry, final IGuideText text) {
        final StringBuilder words = new StringBuilder();
        for (final GuideBlock block : entry.blocks()) {
            final List<String> keys = switch (block) {
                case GuideBlock.Paragraph paragraph -> List.of(paragraph.key());
                case GuideBlock.Note note -> List.of(note.key());
                case GuideBlock.Warning warning -> List.of(warning.key());
                case GuideBlock.Steps steps -> steps.keys();
                case GuideBlock.Problems problems -> problems.problems().stream()
                        .flatMap(problem -> List.of(problem.problemKey(), problem.fixKey()).stream()).toList();
                case GuideBlock.Define define -> List.of(define.termKey(), define.definitionKey());
                default -> List.of();
            };
            for (final String key : keys) {
                words.append(plainOf(text.text(key))).append(' ');
            }
        }
        return words.toString();
    }

    /** A sentence with its links written as their words, its keys as their names and its figures left out. */
    private static String plainOf(final String sentence) {
        // A figure the manual reads from its mod is written with what it measures, so it is left out here.
        final String noFacts = sentence.replaceAll("\\{\\{[^}]*}}", " ");
        return GuideLinks.plain(noFacts, target -> null, target -> null);
    }

    /** Whether the place is inside round or square brackets, where numbers of other things stand. */
    private static boolean insideBrackets(final String text, final int at) {
        int round = 0;
        int square = 0;
        for (int i = 0; i < at; i++) {
            switch (text.charAt(i)) {
                case '(' -> round++;
                case ')' -> round = Math.max(0, round - 1);
                case '[' -> square++;
                case ']' -> square = Math.max(0, square - 1);
                default -> {
                }
            }
        }
        return round > 0 || square > 0;
    }

    /** Whether the number is written into a word: "x86", "MC-DOS", "S1001", "16x", "4MB", "3rd". */
    private static boolean touchesAName(final String text, final int start, final int end) {
        final boolean before = start > 0 && (Character.isLetter(text.charAt(start - 1))
                || text.charAt(start - 1) == '-' || text.charAt(start - 1) == '/' || text.charAt(start - 1) == '#');
        final boolean after = end < text.length() && (Character.isLetter(text.charAt(end))
                || text.charAt(end) == '%' || text.charAt(end) == '°');
        return before || after;
    }

    /** Whether a word follows the number, saying what it measures or counts: "4 modules", "12 MB". */
    private static boolean measured(final String text, final int end) {
        int at = end;
        while (at < text.length() && text.charAt(at) == ' ') {
            at++;
        }
        return at < text.length() && (Character.isLetter(text.charAt(at)) || text.charAt(at) == '%'
                || text.charAt(at) == '°' || text.charAt(at) == '-' || text.charAt(at) == '×');
    }

    /** Whether the number follows a name it belongs to, as "Frames 95" or "Table 3-7" does: a word with a capital. */
    private static boolean named(final String text, final int start) {
        int wordEnd = start;
        while (wordEnd > 0 && text.charAt(wordEnd - 1) == ' ') {
            wordEnd--;
        }
        if (wordEnd == 0) {
            return false;
        }
        int wordStart = wordEnd;
        while (wordStart > 0 && !Character.isWhitespace(text.charAt(wordStart - 1))) {
            wordStart--;
        }
        return Character.isUpperCase(text.charAt(wordStart));
    }

    /**
     * A word met before it is explained.
     *
     * @param term        the word
     * @param usedIn      the first entry that uses it
     * @param explainedIn the entry that explains it, printed after
     */
    public record Early(String term, String usedIn, String explainedIn) {
    }
}
