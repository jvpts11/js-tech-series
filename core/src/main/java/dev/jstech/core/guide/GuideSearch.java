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
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * The search of a laid-out manual. What the reader types is looked for in the titles and explained words of its index
 * first, then in every line of its pages, so a command in a table or a word in the middle of a paragraph is found as
 * surely as a title; an entry found in its words is listed once, at the page of its first line that holds them, with
 * that line. Then come the recipes its pages show: those that make what was typed, and after them those it goes into.
 * Letters are matched whatever their case.
 */
public final class GuideSearch {

    /** The most a search lists, so a word on every page does not flood the list. */
    public static final int MOST = 100;

    private GuideSearch() {
    }

    /** Everything the typed words find in the book's words, the index's lines first; with nothing typed, the index. */
    public static List<Hit> find(final GuideBook book, final String typed) {
        return find(book, typed, IRecipeWords.NONE);
    }

    /**
     * Everything the typed words find: the index's lines, the entries holding them in their words, the recipes making
     * what they name and the recipes it goes into; with nothing typed, the whole index.
     */
    public static List<Hit> find(final GuideBook book, final String typed, final IRecipeWords recipes) {
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
            return cut(hits);
        }
        // Each entry by the page it starts on, so a page's words belong to the last entry started at or before it.
        final TreeMap<Integer, GuideBook.IndexLine> entries = new TreeMap<>();
        for (final GuideBook.IndexLine line : book.index()) {
            if (!line.term()) {
                book.pageOf(line.target()).ifPresent(page -> entries.put(page, line));
            }
        }
        for (int page = 0; page < book.indexAt() && hits.size() < MOST; page++) {
            final Optional<GuideBook.IndexLine> entry = entryAt(book, entries, page);
            if (entry.isEmpty() || listed.contains(entry.get().target())) {
                continue;
            }
            for (final GuidePiece piece : book.pages().get(page).pieces()) {
                final String line = lineHolding(piece, wanted);
                if (!line.isEmpty()) {
                    final GuideBook.IndexLine found = entry.get();
                    hits.add(new Hit(found.text(), found.target(), book.pages().get(page).folio(), found.icon(),
                            false, page, line));
                    listed.add(found.target());
                    break;
                }
            }
        }
        final List<Hit> made = new ArrayList<>();
        final List<Hit> used = new ArrayList<>();
        for (int page = 0; page < book.indexAt(); page++) {
            final Optional<GuideBook.IndexLine> entry = entryAt(book, entries, page);
            for (final GuidePiece piece : book.pages().get(page).pieces()) {
                if (entry.isPresent() && piece instanceof GuidePiece.Recipes rows) {
                    found(book, entry.get(), page, rows, recipes, wanted, made, used);
                }
            }
        }
        hits.addAll(made);
        hits.addAll(used);
        return cut(hits);
    }

    /** How many of the hits were found that way. */
    public static int count(final List<Hit> hits, final Found found) {
        int count = 0;
        for (final Hit hit : hits) {
            if (hit.found() == found) {
                count++;
            }
        }
        return count;
    }

    /* The recipes of a page's rows that make the words, and those the words go into, one hit each. */
    private static void found(final GuideBook book, final GuideBook.IndexLine entry, final int page,
                              final GuidePiece.Recipes rows, final IRecipeWords recipes, final String wanted,
                              final List<Hit> made, final List<Hit> used) {
        final List<IRecipeWords.RecipeWords> shown = recipes.of(rows.type(), rows.output());
        for (int i = rows.first(); i < rows.first() + rows.count() && i < shown.size(); i++) {
            final IRecipeWords.RecipeWords recipe = shown.get(i);
            final String making = holding(recipe.makes(), wanted);
            final boolean taking = making.isEmpty() && !holding(recipe.takes(), wanted).isEmpty();
            if (making.isEmpty() && !taking) {
                continue;
            }
            // A recipe says the name that was looked for; a use, what it goes into; both what works it.
            final String name = taking ? recipe.makes().stream().findFirst().orElse("") : making;
            final String line = recipe.kind().isEmpty() ? name : name + ", " + recipe.kind();
            final Hit hit = new Hit(entry.text(), entry.target(), book.pages().get(page).folio(),
                    recipe.icon().isEmpty() ? entry.icon() : recipe.icon(), false, page, line,
                    taking ? Found.USE : Found.RECIPE);
            (taking ? used : made).add(hit);
        }
    }

    /*
     * The entry a page belongs to: the last one started at or before it. A chapter's opening pages come after the
     * last entry of the chapter before, and belong to none.
     */
    private static Optional<GuideBook.IndexLine> entryAt(final GuideBook book,
                                                         final TreeMap<Integer, GuideBook.IndexLine> entries,
                                                         final int page) {
        final Map.Entry<Integer, GuideBook.IndexLine> entry = entries.floorEntry(page);
        if (entry == null || !book.pages().get(entry.getKey()).chapter().equals(book.pages().get(page).chapter())) {
            return Optional.empty();
        }
        return Optional.of(entry.getValue());
    }

    /** The first of the names holding the words, or empty. */
    private static String holding(final List<String> names, final String wanted) {
        for (final String name : names) {
            if (name.toLowerCase(Locale.ROOT).contains(wanted)) {
                return name;
            }
        }
        return "";
    }

    /** The line of a piece holding the words, a session's row among them, or empty when it holds none. */
    private static String lineHolding(final GuidePiece piece, final String wanted) {
        if (piece instanceof GuidePiece.Text text && text.text().toLowerCase(Locale.ROOT).contains(wanted)) {
            return text.text().strip();
        }
        if (piece instanceof GuidePiece.Session session) {
            for (final List<GuidePiece.SessionRun> row : session.rows()) {
                final StringBuilder written = new StringBuilder();
                row.forEach(run -> written.append(run.text()));
                if (written.toString().toLowerCase(Locale.ROOT).contains(wanted)) {
                    return written.toString().strip();
                }
            }
        }
        return "";
    }

    private static List<Hit> cut(final List<Hit> hits) {
        return hits.size() > MOST ? List.copyOf(hits.subList(0, MOST)) : List.copyOf(hits);
    }

    /** What a hit was found in. */
    public enum Found {
        /** An entry's title or a word explained in the index, or an entry's words. */
        PAGE,
        /** A recipe making what was looked for. */
        RECIPE,
        /** A recipe what was looked for goes into. */
        USE
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
     * @param line   the line of that page the words were found on, or the recipe found; empty when they were found
     *               in the index
     * @param found  what it was found in
     */
    public record Hit(String text, String target, String folio, String icon, boolean term, int page, String line,
                      Found found) {

        /** Something found in the book's words. */
        public Hit(final String text, final String target, final String folio, final String icon, final boolean term,
                   final int page, final String line) {
            this(text, target, folio, icon, term, page, line, Found.PAGE);
        }
    }
}
