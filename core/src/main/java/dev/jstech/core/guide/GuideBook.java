/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;

/**
 * A manual laid out: its pages in order, where every entry, section and chapter starts, and its index.
 *
 * @param pages    every page, the contents first and the index last
 * @param targets  the page each entry, section and chapter starts on, by its id (a chapter by its namespace)
 * @param numbers  the number each entry and section has in the manual ("3.2.6", or "JI-102" on a drawing), by its id
 * @param index    every line of the index, alphabetical: the entries and the words they explain
 * @param contents the first page of the contents
 * @param indexAt  the first page of the index
 */
public record GuideBook(List<Page> pages, Map<String, Integer> targets, Map<String, String> numbers,
                        List<IndexLine> index, int contents, int indexAt) {

    public GuideBook {
        pages = List.copyOf(pages);
        targets = Map.copyOf(targets);
        numbers = Map.copyOf(numbers);
        index = List.copyOf(index);
    }

    /** The page an entry, a section or a chapter starts on, when the manual holds it. */
    public OptionalInt pageOf(final String target) {
        final Integer page = this.targets.get(target);
        return page == null ? OptionalInt.empty() : OptionalInt.of(page);
    }

    /**
     * One page.
     *
     * @param folio   its number as printed at its foot ("3-14", "iii", "X-2"), or a drawing's number ("JI-102")
     * @param header  what its head says: the chapter on a left page, the section on a right one
     * @param chapter the namespace of the chapter it belongs to, or empty for the contents and the index
     * @param pieces  what is drawn on it
     * @param title   what a drawing's title block calls the page: its entry's title, or the list's
     * @param sheet   which sheet of its drawing the page is, from one
     * @param sheets  how many sheets its drawing has
     */
    public record Page(String folio, String header, String chapter, List<GuidePiece> pieces, String title, int sheet,
                       int sheets) {

        public Page {
            Objects.requireNonNull(folio, "folio");
            header = header == null ? "" : header;
            chapter = chapter == null ? "" : chapter;
            pieces = List.copyOf(pieces);
            title = title == null ? "" : title;
        }

        /** A page of the front matter or the index, outside any drawing. */
        public Page(final String folio, final String header, final String chapter, final List<GuidePiece> pieces) {
            this(folio, header, chapter, pieces, header, 1, 1);
        }
    }

    /**
     * A line of the index.
     *
     * @param text   what it reads: an entry's title or a word an entry explains
     * @param folio  the page it is on, as printed
     * @param target the id of the entry it leads to
     * @param icon   the item drawn beside it, or empty
     * @param term   whether it is a word explained rather than an entry's title
     */
    public record IndexLine(String text, String folio, String target, String icon, boolean term) {
    }
}
