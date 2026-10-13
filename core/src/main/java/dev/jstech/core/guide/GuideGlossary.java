/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The glossary of a manual: every word one of its entries explains, gathered from all of them, in the reader's
 * language. The manual prints it at its back, each word with what it means and the page that explains it, and a
 * word's first use in any other entry leads to that page, so a reader meeting a word they do not know is one click from
 * what it means.
 */
final class GuideGlossary {

    private final List<Item> items;
    private final List<Item> longestFirst;

    private GuideGlossary(final List<Item> items) {
        this.items = items.stream().sorted(Comparator.comparing(item -> item.term().toLowerCase(Locale.ROOT)))
                .toList();
        this.longestFirst = items.stream().sorted(Comparator.comparingInt((Item item) -> item.term().length())
                .reversed()).toList();
    }

    /** The words the entries of those contents explain, the first explanation of a word kept. */
    static GuideGlossary of(final GuideContents contents, final IGuideText text) {
        final Map<String, Item> found = new LinkedHashMap<>();
        for (final GuideContents.Chapter chapter : contents.chapters()) {
            for (final GuideContents.Section section : chapter.sections()) {
                for (final GuideEntry entry : section.entries()) {
                    for (final GuideBlock block : entry.blocks()) {
                        if (block instanceof GuideBlock.Define define) {
                            final String term = text.text(define.termKey()).strip();
                            if (!term.isEmpty()) {
                                found.putIfAbsent(term.toLowerCase(Locale.ROOT), new Item(term,
                                        define.definitionKey(), entry.id()));
                            }
                        }
                    }
                }
            }
        }
        return new GuideGlossary(new ArrayList<>(found.values()));
    }

    /** Whether no entry explains a word, and so the manual has no glossary. */
    boolean isEmpty() {
        return this.items.isEmpty();
    }

    /** The words, alphabetical. */
    List<Item> items() {
        return this.items;
    }

    /**
     * The runs of a sentence of an entry with the first use in that entry of each word another entry explains made a
     * link to the entry that explains it, its words as they are written. A word is matched whole and whatever its case,
     * the longest words first, so "network engine" is found before "network"; words in a link are left alone.
     *
     * @param entry the entry the sentence is in, whose own words lead nowhere
     * @param used  the words already led from in the entry, to which those found here are added
     */
    List<GuideLinks.Run> linked(final List<GuideLinks.Run> runs, final String entry, final Set<String> used) {
        final List<GuideLinks.Run> out = new ArrayList<>();
        for (final GuideLinks.Run run : runs) {
            if (run.key() || !run.target().isEmpty()) {
                out.add(run);
                continue;
            }
            final String text = run.text();
            final String lower = text.toLowerCase(Locale.ROOT);
            int written = 0;
            int from = 0;
            while (true) {
                // The word starting first, the longest of those starting there, read left to right.
                Item best = null;
                int bestAt = -1;
                for (final Item item : this.longestFirst) {
                    final int at = item.entry().equals(entry) ? -1
                            : wordAt(lower, item.term().toLowerCase(Locale.ROOT), from);
                    if (at >= 0 && (bestAt < 0 || at < bestAt)) {
                        best = item;
                        bestAt = at;
                    }
                }
                if (best == null) {
                    break;
                }
                final String key = best.term().toLowerCase(Locale.ROOT);
                final int end = bestAt + key.length();
                /*
                 * A word that is part of a name, as "Task" is of "Task Manager", or of a path or a name in code, as
                 * "gentoo" is of /mnt/gentoo, is the name's and leads nowhere.
                 */
                if (!key.contains(" ") && inAName(text, bestAt, end) || inCode(text, bestAt, end)) {
                    from = end;
                    continue;
                }
                // A word already led from is passed over whole, so no shorter word is found inside it.
                if (used.add(key)) {
                    if (bestAt > written) {
                        out.add(new GuideLinks.Run(text.substring(written, bestAt), ""));
                    }
                    out.add(new GuideLinks.Run(text.substring(bestAt, end), best.entry()));
                    written = end;
                }
                from = end;
            }
            if (written < text.length()) {
                out.add(new GuideLinks.Run(text.substring(written), ""));
            }
        }
        return out;
    }

    /**
     * Whether the word standing there, from {@code at} to {@code end}, is part of a longer name written with
     * capitals: followed by a word with a capital ("Task" in "Task Manager"), or after one that does not open the
     * sentence ("Computer" in "a Personal Computer").
     */
    static boolean inAName(final String text, final int at, final int end) {
        if (end + 1 < text.length() && text.charAt(end) == ' ' && Character.isUpperCase(text.charAt(end + 1))) {
            return true;
        }
        final int gap = at - 1;
        if (gap < 1 || text.charAt(gap) != ' ') {
            return false;
        }
        int wordStart = gap;
        while (wordStart > 0 && Character.isLetter(text.charAt(wordStart - 1))) {
            wordStart--;
        }
        if (wordStart == gap || !Character.isUpperCase(text.charAt(wordStart))) {
            return false;
        }
        int before = wordStart - 1;
        while (before >= 0 && text.charAt(before) == ' ') {
            before--;
        }
        // The first word of a sentence has its capital for the sentence, not for a name.
        return before >= 0 && ".!?:(\"".indexOf(text.charAt(before)) < 0;
    }

    /**
     * Whether the word standing there, from {@code at} to {@code end}, is part of a path, a command or a name in code:
     * joined to what is before or after it by a slash, an underscore, a hyphen or a bar ("/mnt/gentoo",
     * "grub-install", "stock_report", "system|program"), or by a point or a colon going on into more of the name.
     */
    static boolean inCode(final String text, final int at, final int end) {
        final boolean before = at > 0 && "/_-|\\.:".indexOf(text.charAt(at - 1)) >= 0;
        final boolean after = end < text.length() && "/_-|\\".indexOf(text.charAt(end)) >= 0
                || end + 1 < text.length() && ".:".indexOf(text.charAt(end)) >= 0
                && Character.isLetterOrDigit(text.charAt(end + 1));
        return before || after;
    }

    /** Where the word stands whole in the lower-cased text, from {@code from} on, or -1. */
    static int wordAt(final String lower, final String word, final int from) {
        int at = lower.indexOf(word, from);
        while (at >= 0) {
            final boolean startsWhole = at == 0 || !Character.isLetterOrDigit(lower.charAt(at - 1));
            final int end = at + word.length();
            final boolean endsWhole = end == lower.length() || !Character.isLetterOrDigit(lower.charAt(end));
            if (startsWhole && endsWhole) {
                return at;
            }
            at = lower.indexOf(word, at + 1);
        }
        return -1;
    }

    /**
     * A word of the glossary.
     *
     * @param term          the word, in the reader's language
     * @param definitionKey what it means, a sentence's key
     * @param entry         the entry that explains it
     */
    record Item(String term, String definitionKey, String entry) {
    }
}
