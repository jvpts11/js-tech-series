/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * The search of a laid-out manual. What the reader types is looked for in the titles and explained words of its index
 * first, then in every line of its pages, so a command in a table or a word in the middle of a paragraph is found as
 * surely as a title; an entry found in its words is listed once, at the page of its first line that holds them, with
 * that line. Letters are matched whatever their case.
 */
public final class GuideSearch {

    /** The most a search lists, so a word on every page does not flood the list. */
    public static final int MOST = 100;

    private GuideSearch() {
    }

    /** Everything the typed words find, the index's lines first; with nothing typed, the whole index. */
    public static List<Hit> find(final GuideBook book, final String typed) {
        final String wanted = typed.strip().toLowerCase(Locale.ROOT);
        final List<Hit> hits = new ArrayList<>();
        final Set<String> listed = new HashSet<>();
        for (final GuideBook.IndexLine line : book.index()) {
            if (wanted.isEmpty() || line.text().toLowerCase(Locale.ROOT).contains(wanted)) {
                hits.add(new Hit(line.text(), line.target(), line.folio(), line.icon(), line.term(),
                        book.pageOf(line.target()).orElse(-1), ""));
                if (!line.term()) {
                    listed.add(line.target());
                }
            }
        }
        if (wanted.isEmpty()) {
            return hits.size() > MOST ? List.copyOf(hits.subList(0, MOST)) : List.copyOf(hits);
        }
        // Each entry by the page it starts on, so a page's words belong to the last entry started at or before it.
        final TreeMap<Integer, GuideBook.IndexLine> entries = new TreeMap<>();
        for (final GuideBook.IndexLine line : book.index()) {
            if (!line.term()) {
                book.pageOf(line.target()).ifPresent(page -> entries.put(page, line));
            }
        }
        for (int page = 0; page < book.indexAt() && hits.size() < MOST; page++) {
            final Map.Entry<Integer, GuideBook.IndexLine> entry = entries.floorEntry(page);
            // A chapter's opening pages come after the last entry of the chapter before, and belong to neither.
            if (entry == null || listed.contains(entry.getValue().target())
                    || !book.pages().get(entry.getKey()).chapter().equals(book.pages().get(page).chapter())) {
                continue;
            }
            for (final GuidePiece piece : book.pages().get(page).pieces()) {
                if (piece instanceof GuidePiece.Text text && text.text().toLowerCase(Locale.ROOT).contains(wanted)) {
                    final GuideBook.IndexLine found = entry.getValue();
                    hits.add(new Hit(found.text(), found.target(), book.pages().get(page).folio(), found.icon(),
                            false, page, text.text().strip()));
                    listed.add(found.target());
                    break;
                }
            }
        }
        return hits.size() > MOST ? List.copyOf(hits.subList(0, MOST)) : List.copyOf(hits);
    }

    /**
     * Something a search found.
     *
     * @param text   the entry's title, or the word explained
     * @param target the entry it leads to
     * @param folio  the page it is on, as printed
     * @param icon   the item drawn beside it, or empty
     * @param term   whether it is a word explained rather than an entry's title
     * @param page   the page it opens, where the words were found
     * @param line   the line of that page the words were found on, or empty when they were found in the index
     */
    public record Hit(String text, String target, String folio, String icon, boolean term, int page, String line) {
    }
}
