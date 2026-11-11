/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.fs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class BookFileTest {

    @Test
    void write_thenRead_givesTheSameSignedBook() {
        final BookFile book = new BookFile("Mine Log", "Steve", List.of("Day 1: iron.", "Day 2:\ndiamonds!"));
        final BookFile back = BookFile.read(book.write());
        assertEquals(book, back);
        assertTrue(back.signed());
    }

    @Test
    void write_ofAnUnsignedBookHasNoHeaderAndReadsBackUnsigned() {
        final BookFile quill = new BookFile("", "", List.of("draft"));
        final String text = quill.write();
        assertFalse(text.contains("Title: "), text);
        final BookFile back = BookFile.read(text);
        assertFalse(back.signed());
        assertEquals(List.of("draft"), back.pages());
    }

    @Test
    void read_ofATextWithNoHeadingIsOnePage() {
        final BookFile book = BookFile.read("just some words\nover two lines\n");
        assertEquals(List.of("just some words\nover two lines"), book.pages());
        assertFalse(book.signed());
    }

    @Test
    void read_takesTheHeaderOnlyAboveTheFirstHeading() {
        final BookFile book = BookFile.read("Title: T\nAuthor: A\n=== Page 1 ===\nAuthor: not a header\n");
        assertEquals("T", book.title());
        assertEquals("A", book.author());
        assertEquals(List.of("Author: not a header"), book.pages());
    }

    @Test
    void read_toleratesWindowsLineEndings() {
        final BookFile book = BookFile.read("Title: T\r\nAuthor: A\r\n=== Page 1 ===\r\nhello\r\n");
        assertEquals("T", book.title());
        assertEquals(List.of("hello"), book.pages());
    }

    @Test
    void constructor_boundsPagesAndTheirLengthAsTheGameDoes() {
        final List<String> pages = new ArrayList<>();
        for (int i = 0; i < BookFile.MOST_PAGES + 5; i++) {
            pages.add("x".repeat(BookFile.MOST_PAGE_CHARS + 10));
        }
        final BookFile book = new BookFile("t".repeat(50), "a".repeat(80), pages);
        assertEquals(BookFile.MOST_PAGES, book.pages().size());
        assertTrue(book.pages().getFirst().length() <= BookFile.MOST_PAGE_CHARS);
        assertTrue(book.title().length() <= BookFile.MOST_TITLE);
        assertTrue(book.author().length() <= BookFile.MOST_AUTHOR);
    }

    @Test
    void constructor_givesAnEmptyBookOnePage() {
        assertEquals(List.of(""), new BookFile("", "", List.of()).pages());
        assertEquals(List.of(""), BookFile.read(null).pages());
    }
}
