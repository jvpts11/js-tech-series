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
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Links written inside a manual's sentences, so a text can lead to another page in its own words, as a guide does:
 * {@code see [Installing a system](jsc:installing)} reads "see Installing a system (1.4.1)", the words and the number
 * leading to that entry, and {@code ([](jsc:installing))} reads "(1.4.1)". A link may lead to an entry, a section or a
 * chapter, a chapter named by its mod's namespace alone.
 *
 * <p>The number is the one the target has in the manual being read, so the same sentence reads "1.4.1" in a mod's
 * own manual and "3.4.1" in one holding every chapter. A target that manual does not hold but another book does reads
 * as its words and that book, "Sigma (in The Book of Sigma)", and leads to it; one no book holds reads as its words
 * alone and leads nowhere. A translation keeps the brackets and the target and translates the words.
 *
 * <p>A key of the keyboard is written between double brackets, {@code press [[Ctrl]]+[[S]]}, and is drawn as a key;
 * read as plain text it is the key's name in single brackets, "[Ctrl]+[S]". A translation may rename a key, as a
 * keyboard of its language does.
 */
public final class GuideLinks {

    private static final Pattern LINK = Pattern.compile("\\[([^\\]]*)]\\(([a-z0-9_.-]+(?::[a-z0-9_./-]+)?)\\)");
    private static final Pattern KEY = Pattern.compile("\\[\\[([^\\[\\]]+)]]");

    private GuideLinks() {
    }

    /**
     * The sentence in runs of words, each run plain or one link's words and number.
     *
     * @param numbers the number a target has in the manual being read, or null when that manual does not hold it
     */
    public static List<Run> runs(final String sentence, final Function<String, String> numbers) {
        return runs(sentence, numbers, target -> null);
    }

    /**
     * The sentence in runs of words, each run plain or one link's words and where it leads.
     *
     * @param numbers   the number a target has in the manual being read, or null when that manual does not hold it
     * @param elsewhere for a target the manual does not hold, the words saying which book does ("in The Book of
     *                  Sigma"), or null when none does
     */
    public static List<Run> runs(final String sentence, final Function<String, String> numbers,
                                 final Function<String, String> elsewhere) {
        return runs(sentence, numbers, elsewhere, target -> null);
    }

    /**
     * The sentence in runs of words, each run plain or one link's words and where it leads; a link with no words of
     * its own to a target another book holds is written with that target's title.
     *
     * @param titles for a target the manual does not hold, its title in the book that does, or null when unknown
     */
    public static List<Run> runs(final String sentence, final Function<String, String> numbers,
                                 final Function<String, String> elsewhere, final Function<String, String> titles) {
        final List<Run> runs = new ArrayList<>();
        final Matcher matcher = LINK.matcher(sentence);
        int at = 0;
        while (matcher.find()) {
            if (matcher.start() > at) {
                keyed(runs, sentence.substring(at, matcher.start()));
            }
            final String words = matcher.group(1).strip();
            final String target = matcher.group(2);
            final String number = numbers.apply(target);
            final String title = words.isEmpty() ? titles.apply(target) : null;
            final String shown = !words.isEmpty() ? words
                    : title != null && !title.isEmpty() ? title : GuideIds.path(target);
            if (number != null) {
                runs.add(new Run(words.isEmpty() ? number : words + " (" + number + ")", target));
            } else {
                final String book = elsewhere.apply(target);
                runs.add(book == null || book.isEmpty() ? new Run(shown, "")
                        : new Run(shown + " (" + book + ")", target));
            }
            at = matcher.end();
        }
        if (at < sentence.length()) {
            keyed(runs, sentence.substring(at));
        }
        return runs;
    }

    /** The sentence as plain words, each link written as its words and number. */
    public static String plain(final String sentence, final Function<String, String> numbers) {
        return plain(sentence, numbers, target -> null);
    }

    /**
     * The sentence as plain words, each link written as its words and number, or the book that holds it, and each key
     * as its name in brackets.
     */
    public static String plain(final String sentence, final Function<String, String> numbers,
                               final Function<String, String> elsewhere) {
        return plain(sentence, numbers, elsewhere, target -> null);
    }

    /** The sentence as plain words, a link with no words of its own to another book written with its title. */
    public static String plain(final String sentence, final Function<String, String> numbers,
                               final Function<String, String> elsewhere, final Function<String, String> titles) {
        final StringBuilder out = new StringBuilder();
        for (final Run run : runs(sentence, numbers, elsewhere, titles)) {
            out.append(run.key() ? "[" + run.text() + "]" : run.text());
        }
        return out.toString();
    }

    /** What the sentence's links lead to, in the order they are written. */
    public static List<String> targets(final String sentence) {
        final List<String> targets = new ArrayList<>();
        final Matcher matcher = LINK.matcher(sentence);
        while (matcher.find()) {
            targets.add(matcher.group(2));
        }
        return targets;
    }

    /** Plain words with the keys written in them, as runs: the words, and each key a run of its own. */
    private static void keyed(final List<Run> runs, final String words) {
        final Matcher matcher = KEY.matcher(words);
        int at = 0;
        while (matcher.find()) {
            if (matcher.start() > at) {
                runs.add(new Run(words.substring(at, matcher.start()), ""));
            }
            runs.add(new Run(matcher.group(1).strip(), "", true));
            at = matcher.end();
        }
        if (at < words.length()) {
            runs.add(new Run(words.substring(at), ""));
        }
    }

    /**
     * A run of a sentence.
     *
     * @param text   its words, as they are drawn; for a key, the key's name
     * @param target where it leads, or empty for plain words
     * @param key    whether it is a key of the keyboard, drawn as a key and never broken
     */
    public record Run(String text, String target, boolean key) {

        /** Words, or a link's words. */
        public Run(final String text, final String target) {
            this(text, target, false);
        }
    }
}
