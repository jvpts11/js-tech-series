/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

/**
 * The rules a manual's running text keeps, for a mod's tests to hold its chapters to: it speaks to its reader, it gives
 * every number what the number measures or counts, and a word it uses before the entry explaining it leads there, so
 * a reader going through the book in order is never left with a word they cannot look up.
 *
 * <p>What a sentence means is the writer's to judge; these are the parts of good writing a machine can see. Pure: the
 * words in, what breaks a rule out.
 */
public final class GuideProse {

    /** A number: digits, with points, commas or colons between groups of them ("1,024", "6.22", "12:30"). */
    private static final Pattern NUMBER = Pattern.compile("\\d+(?:[.,:]\\d+)*");
    /** The words that speak to a reader. */
    private static final Pattern YOU = Pattern.compile("\\b(you|your|yours|yourself)\\b");
    /** What a machine says or a reader types, in double quotes. */
    private static final Pattern QUOTED = Pattern.compile("\"[^\"]*\"");
    /** What stands between two numbers of one list or range, which the second shares the first one's meaning across. */
    private static final Pattern JOINED = Pattern.compile(",\\s*|,?\\s+(?:or|and|to|through)\\s+");
    /**
     * The signs of a piece of code, beside which a number is a value the code holds, not a count of anything: "2 + 3",
     * "total = 5;", "(int) 2.5".
     */
    private static final String CODE_SIGNS = "=+*/<>;|&^!%)";
    /** How many words back a sign of code makes the number a value of that code: "2 + 3 * 4 is 14, not 20". */
    private static final int CODE_REACH = 5;
    /**
     * The nouns that say what the number after them is, a measure or a numbered thing: "version 2", "a strength of 15",
     * "hours from 0 to 23", "line 1".
     */
    private static final Set<String> MEASURES = Set.of("amount", "base", "bound", "cap", "chapter", "code", "column",
            "count", "counted", "day", "edition", "figure", "hour", "hours", "index", "interval", "key", "length",
            "level", "line", "listing", "number", "numbered", "numbers", "offer", "page", "part", "partition", "place",
            "port", "priority", "process", "quantity", "release", "revision", "row", "score", "section", "size",
            "slot", "stage", "step", "strength", "table", "thread", "unit", "value", "version", "versions", "volume");
    /**
     * The small words that may stand between a measure and its number: "a strength of 15", "hours from 0", "the count
     * starts at 1".
     */
    private static final Set<String> LINKING = Set.of("a", "about", "above", "always", "an", "are", "as", "at",
            "becomes", "below", "between", "by", "counts", "from", "gives", "is", "means", "of", "over", "reports",
            "returns", "shows", "start", "starting", "starts", "than", "the", "to", "under", "up", "was", "whole");

    private GuideProse() {
    }

    /** Whether a piece of running text speaks to its reader, as an explanation does: "you", "your". */
    public static boolean speaksToTheReader(final String text) {
        return YOU.matcher(text.toLowerCase(Locale.ROOT)).find();
    }

    /**
     * The numbers of a sentence that stand bare, without what they measure or count: the 4 of "it holds 4." where "it
     * holds 4 memory modules." says what. A number in a name ("MC-DOS 6.22", "Frames 95", "S1001"), in brackets, in
     * quotes, in a link, in a key or in a figure the manual reads from its mod is never bare, and neither is a time of
     * day ("06:00") or a version ("1.0.0"), a value in a piece of code ("2 + 3", "-F 32"), a number its measure names
     * before it ("version 2", "a strength from 0 to 15"), or one that goes on a list or a range whose first number
     * says what it is.
     */
    public static List<String> bareNumbers(final String sentence) {
        final String plain = plainOf(sentence);
        final List<MatchResult> numbers = NUMBER.matcher(plain).results().toList();
        final boolean[] said = new boolean[numbers.size()];
        for (int i = 0; i < numbers.size(); i++) {
            final MatchResult number = numbers.get(i);
            final int start = number.start();
            final int end = number.end();
            said[i] = insideBrackets(plain, start) || insideQuotes(plain, start) || saysWhatItIs(number.group())
                    || touchesAName(plain, start, end) || measured(plain, end) || named(plain, start)
                    || inCode(plain, start, end) || measuredBefore(plain, start);
        }
        // A list or a range shares what one of its numbers says: "from 0 to 15", "1, 1 or 2 levels".
        for (int i = 1; i < numbers.size(); i++) {
            said[i] |= said[i - 1] && joined(plain, numbers.get(i - 1), numbers.get(i));
        }
        for (int i = numbers.size() - 2; i >= 0; i--) {
            said[i] |= said[i + 1] && joined(plain, numbers.get(i), numbers.get(i + 1));
        }
        final List<String> bare = new ArrayList<>();
        for (int i = 0; i < numbers.size(); i++) {
            if (!said[i]) {
                bare.add(numbers.get(i).group());
            }
        }
        return bare;
    }

    /**
     * The words a book's entries explain that an entry printed before the one explaining them already uses, where
     * nothing in that entry leads to the explanation, each with the first entry using it and the one explaining it. A
     * first use the manual makes a link of, as it makes one of a word's first use in a sentence, a use inside a link
     * of the writer's, or one beside a link to the explaining entry is one click from what the word means, and that is
     * enough. What a machine says, quoted, is not the book using a word, and neither is a word inside a longer word
     * the book explains: "engine" in "Automation Engine".
     */
    public static List<Early> usedBeforeExplained(final GuideContents contents, final IGuideText text) {
        final List<GuideEntry> entries = new ArrayList<>();
        for (final GuideContents.Chapter chapter : contents.chapters()) {
            for (final GuideContents.Section section : chapter.sections()) {
                entries.addAll(section.entries());
            }
        }
        final GuideGlossary glossary = GuideGlossary.of(contents, text);
        final List<String> explained = glossary.items().stream().map(item -> item.term().toLowerCase(Locale.ROOT))
                .toList();
        final Map<String, String> own = new HashMap<>();
        for (final GuideEntry entry : entries) {
            own.put(entry.id(), QUOTED.matcher(wordsOf(entry, text)).replaceAll(" "));
        }
        final List<Early> wrong = new ArrayList<>();
        for (final GuideGlossary.Item item : glossary.items()) {
            final String word = item.term().toLowerCase(Locale.ROOT);
            for (final GuideEntry entry : entries) {
                if (entry.id().equals(item.entry())) {
                    break;
                }
                if (uses(own.get(entry.id()), word, explained)) {
                    if (!leadsFrom(entry, glossary, text).reach(item.entry(), word)) {
                        wrong.add(new Early(item.term(), entry.id(), item.entry()));
                    }
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
                case GuideBlock.Caution caution -> List.of(caution.key());
                case GuideBlock.Steps steps -> steps.keys();
                case GuideBlock.Checklist checklist -> checklist.lines().stream()
                        .flatMap(line -> line.stateKey().isEmpty() ? List.of(line.itemKey()).stream()
                                : List.of(line.itemKey(), line.stateKey()).stream()).toList();
                case GuideBlock.Card card -> card.steps().stream()
                        .flatMap(step -> step.textKey().isEmpty() ? List.of(step.nameKey()).stream()
                                : List.of(step.nameKey(), step.textKey()).stream()).toList();
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

    /**
     * Whether the text holds the word whole somewhere it is not part of a longer word the book explains, nor of a
     * name written with capitals, a path or a name in code.
     */
    private static boolean uses(final String text, final String word, final List<String> explained) {
        final String lower = text.toLowerCase(Locale.ROOT);
        int at = GuideGlossary.wordAt(lower, word, 0);
        while (at >= 0) {
            if (!insideALongerWord(lower, at, word, explained)
                    && (word.contains(" ") || !GuideGlossary.inAName(text, at, at + word.length()))
                    && !GuideGlossary.inCode(text, at, at + word.length())) {
                return true;
            }
            at = GuideGlossary.wordAt(lower, word, at + 1);
        }
        return false;
    }

    /** Whether the word at that place is part of a longer explained word standing there: "Automation Engine". */
    private static boolean insideALongerWord(final String text, final int at, final String word,
                                             final List<String> explained) {
        for (final String longer : explained) {
            if (longer.length() <= word.length()) {
                continue;
            }
            int inside = longer.indexOf(word);
            while (inside >= 0) {
                final int start = at - inside;
                if (start >= 0 && GuideGlossary.wordAt(text, longer, start) == start) {
                    return true;
                }
                inside = longer.indexOf(word, inside + 1);
            }
        }
        return false;
    }

    /**
     * Where an entry's running text leads as a reader sees it drawn: its own links, and the first use of each word
     * another entry explains, in the sentences the manual makes such links in. A problem's own words and a word being
     * explained are drawn without them.
     */
    private static Leads leadsFrom(final GuideEntry entry, final GuideGlossary glossary, final IGuideText text) {
        final List<String> keys = new ArrayList<>();
        for (final GuideBlock block : entry.blocks()) {
            switch (block) {
                case GuideBlock.Paragraph paragraph -> keys.add(paragraph.key());
                case GuideBlock.Note note -> keys.add(note.key());
                case GuideBlock.Warning warning -> keys.add(warning.key());
                case GuideBlock.Caution caution -> keys.add(caution.key());
                case GuideBlock.Card card -> card.steps().forEach(step -> {
                    if (!step.textKey().isEmpty()) {
                        keys.add(step.textKey());
                    }
                });
                case GuideBlock.Steps steps -> keys.addAll(steps.keys());
                case GuideBlock.Problems problems -> problems.problems().forEach(problem -> keys.add(problem.fixKey()));
                case GuideBlock.Define define -> keys.add(define.definitionKey());
                default -> {
                }
            }
        }
        final Set<String> used = new HashSet<>();
        final Set<String> targets = new HashSet<>();
        final List<String> linkWords = new ArrayList<>();
        for (final String key : keys) {
            // Every target is given a number, so each link keeps where it leads whichever book holds it.
            for (final GuideLinks.Run run : glossary.linked(GuideLinks.runs(text.text(key), target -> ""),
                    entry.id(), used)) {
                if (!run.target().isEmpty()) {
                    targets.add(run.target());
                    linkWords.add(run.text().toLowerCase(Locale.ROOT));
                }
            }
        }
        return new Leads(targets, linkWords);
    }

    /**
     * A sentence with its links written as their words, its keys as their names and its figures left out. A link
     * with no words of its own is drawn as a number, so it adds no words.
     */
    private static String plainOf(final String sentence) {
        // A figure the manual reads from its mod is written with what it measures, so it is left out here.
        final String noFacts = sentence.replaceAll("\\{\\{[^}]*}}", " ");
        return GuideLinks.plain(noFacts, target -> "", target -> null);
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

    /** Whether the place is inside double quotes, where what a machine prints or a reader types stands as written. */
    private static boolean insideQuotes(final String text, final int at) {
        int quotes = 0;
        for (int i = 0; i < at; i++) {
            if (text.charAt(i) == '"') {
                quotes++;
            }
        }
        return quotes % 2 == 1;
    }

    /** Whether a number says by its shape what it is: a time of day ("06:00"), or a version ("1.0.0", "6.11.5"). */
    private static boolean saysWhatItIs(final String number) {
        return number.contains(":") || number.indexOf('.') != number.lastIndexOf('.');
    }

    /**
     * Whether the number is a value in a piece of code: beside one of its signs ("2 + 3", "total = 5;"), a few words
     * after one ("7 / 2 is 3"), or after an option of a command line ("mkfs.fat -F 32", "taskkill /PID 12").
     */
    private static boolean inCode(final String text, final int start, final int end) {
        int after = end;
        while (after < text.length() && text.charAt(after) == ' ') {
            after++;
        }
        if (after < text.length() && CODE_SIGNS.indexOf(text.charAt(after)) >= 0) {
            return true;
        }
        int at = start;
        for (int words = 0; words < CODE_REACH; words++) {
            while (at > 0 && text.charAt(at - 1) == ' ') {
                at--;
            }
            int wordStart = at;
            while (wordStart > 0 && text.charAt(wordStart - 1) != ' ') {
                wordStart--;
            }
            if (wordStart == at) {
                return false;
            }
            final String word = text.substring(wordStart, at);
            final boolean option = word.length() > 1 && (word.charAt(0) == '-' || word.charAt(0) == '/');
            if (words == 0 && (option || CODE_SIGNS.indexOf(word.charAt(word.length() - 1)) >= 0)
                    || word.chars().allMatch(sign -> CODE_SIGNS.indexOf(sign) >= 0)) {
                return true;
            }
            // A sentence's end is where its piece of code ended too.
            if (word.endsWith(".") || word.endsWith(":")) {
                return false;
            }
            at = wordStart;
        }
        return false;
    }

    /** Whether only a list's or a range's joining words stand between two numbers. */
    private static boolean joined(final String text, final MatchResult first, final MatchResult second) {
        return JOINED.matcher(text.substring(first.end(), second.start())).matches();
    }

    /**
     * Whether a noun naming a measure or a numbered thing stands before the number, maybe across a few small words,
     * or a member of a piece of code whose value it is: "version 2", "a strength of 15", "hours from 0",
     * "Mainframe.PeakToday is 0".
     */
    private static boolean measuredBefore(final String text, final int start) {
        int at = start;
        for (int words = 0; words < 4; words++) {
            while (at > 0 && text.charAt(at - 1) == ' ') {
                at--;
            }
            int wordStart = at;
            while (wordStart > 0 && Character.isLetter(text.charAt(wordStart - 1))) {
                wordStart--;
            }
            if (wordStart == at) {
                return false;
            }
            final String word = text.substring(wordStart, at).toLowerCase(Locale.ROOT);
            final boolean member = wordStart > 1 && text.charAt(wordStart - 1) == '.'
                    && Character.isLetter(text.charAt(wordStart - 2));
            if (MEASURES.contains(word) || member) {
                return true;
            }
            if (!LINKING.contains(word)) {
                return false;
            }
            at = wordStart;
        }
        return false;
    }

    /** Whether the number is written into a word: "x86", "MC-DOS", "S1001", "16x", "4MB", "3rd", "gateway_1". */
    private static boolean touchesAName(final String text, final int start, final int end) {
        final boolean before = start > 0 && (Character.isLetter(text.charAt(start - 1))
                || "-/#_".indexOf(text.charAt(start - 1)) >= 0);
        final boolean after = end < text.length() && (Character.isLetter(text.charAt(end))
                || "%°_".indexOf(text.charAt(end)) >= 0);
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

    /**
     * Where an entry's running text leads.
     *
     * @param targets   the entries its links lead to
     * @param linkWords the words of each of its links, lower-cased
     */
    private record Leads(Set<String> targets, List<String> linkWords) {

        /** Whether a reader gets from the entry to that explanation, or meets the word as a link already. */
        boolean reach(final String explainer, final String word) {
            return this.targets.contains(explainer)
                    || this.linkWords.stream().anyMatch(words -> GuideGlossary.wordAt(words, word, 0) >= 0);
        }
    }
}
