/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.fs;

import dev.jstech.core.text.TextBounds;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.jetbrains.annotations.Nullable;

/**
 * A book as a {@code .book} file holds it: the title and author of a signed book, then each page under its own heading.
 * A scanner writes one from a written book or a book and quill, a player edits it like any text, and the Large Scale
 * Printer prints it back: a signed book as a written book, an unsigned one as a book and quill
 * (computers/docs/design/peripherals.md).
 *
 * <p>Everything is bounded as the game bounds a book, so a file edited past what a book holds prints the book it can.
 * A text with no page headings is one page; the header lines are read only above the first heading.
 *
 * <p>This class is pure and carries no Minecraft dependency.
 *
 * @param title  the title of a signed book, or empty for an unsigned one
 * @param author who signed it, or empty for an unsigned one
 * @param pages  the pages, each its text
 */
public record BookFile(String title, String author, List<String> pages) {

    /** The most pages a book holds, as the game's books do. */
    public static final int MOST_PAGES = 100;
    /** The most characters a page holds, as a book and quill's page does. */
    public static final int MOST_PAGE_CHARS = 1024;
    /** The most characters of a title, as the game signs one. */
    public static final int MOST_TITLE = 32;
    /** The most characters of an author's name. */
    public static final int MOST_AUTHOR = 64;
    private static final String TITLE = "Title: ";
    private static final String AUTHOR = "Author: ";
    private static final String PAGE_HEADING = "=== Page %d ===";
    private static final Pattern HEADING = Pattern.compile("^=== Page \\d+ ===$");
    private static final char NEWLINE = '\n';

    public BookFile {
        title = TextBounds.clip(title, MOST_TITLE);
        author = TextBounds.clip(author, MOST_AUTHOR);
        final List<String> kept = new ArrayList<>();
        for (final String page : pages == null ? List.<String>of() : pages) {
            if (kept.size() >= MOST_PAGES) {
                break;
            }
            kept.add(TextBounds.clip(page, MOST_PAGE_CHARS));
        }
        if (kept.isEmpty()) {
            kept.add("");
        }
        pages = List.copyOf(kept);
    }

    /** Whether it was signed: a signed book prints as a written book, an unsigned one as a book and quill. */
    public boolean signed() {
        return !author.isEmpty();
    }

    /** The file's text: the header of a signed book, then every page under its heading. */
    public String write() {
        final StringBuilder out = new StringBuilder();
        if (signed()) {
            out.append(TITLE).append(title).append(NEWLINE).append(AUTHOR).append(author).append(NEWLINE);
        }
        for (int i = 0; i < pages.size(); i++) {
            out.append(String.format(PAGE_HEADING, i + 1)).append(NEWLINE).append(pages.get(i)).append(NEWLINE);
        }
        return out.toString();
    }

    /**
     * The book a file's text holds. Any text is a book: the lines before the first heading give its title and author
     * when they say so, and the rest is its pages; a text with no heading at all is one page.
     */
    public static BookFile read(@Nullable final String content) {
        final String text = content == null ? "" : content;
        final String[] lines = text.split("\n", -1);
        String title = "";
        String author = "";
        final List<String> pages = new ArrayList<>();
        StringBuilder page = null;
        final StringBuilder loose = new StringBuilder();
        for (final String raw : lines) {
            final String line = raw.endsWith("\r") ? raw.substring(0, raw.length() - 1) : raw;
            if (HEADING.matcher(line.strip()).matches()) {
                if (page != null) {
                    pages.add(trimmed(page));
                }
                page = new StringBuilder();
            } else if (page != null) {
                page.append(line).append(NEWLINE);
            } else if (line.startsWith(TITLE)) {
                title = line.substring(TITLE.length()).strip();
            } else if (line.startsWith(AUTHOR)) {
                author = line.substring(AUTHOR.length()).strip();
            } else {
                loose.append(line).append(NEWLINE);
            }
        }
        if (page != null) {
            pages.add(trimmed(page));
        } else {
            pages.add(trimmed(loose));
        }
        return new BookFile(title, author, pages);
    }

    /* A page's text without the line ending its heading's block. */
    private static String trimmed(final StringBuilder page) {
        int end = page.length();
        while (end > 0 && page.charAt(end - 1) == NEWLINE) {
            end--;
        }
        return page.substring(0, end);
    }
}
