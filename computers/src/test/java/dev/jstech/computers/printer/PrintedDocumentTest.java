/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.printer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class PrintedDocumentTest {

    @Test
    void constructor_boundsEveryPart() {
        final List<String> pages = new ArrayList<>(Collections.nCopies(PrintedDocument.MAX_PAGES + 3,
                "z".repeat(PrintedDocument.MAX_PAGE_CHARS + 9)));
        final PrintedDocument document = new PrintedDocument("t".repeat(PrintedDocument.MAX_NAME + 4), null, "",
                "", pages, "p".repeat(PrintedDocument.MAX_PICTURE + 1), "");
        assertEquals(PrintedDocument.MAX_NAME, document.title().length());
        assertEquals("", document.from());
        assertEquals(PrintedDocument.MAX_PAGES, document.pages().size());
        assertEquals(PrintedDocument.MAX_PAGE_CHARS, document.pages().getFirst().length());
        assertEquals("", document.picture());
    }

    @Test
    void constructor_givesANoPageDocumentOneBlankPage() {
        assertEquals(List.of(""), new PrintedDocument("", "", "", "", List.of(), "", "").pages());
    }

    @Test
    void sheets_areOneAPageAndOneForAPicture() {
        assertEquals(3, PrintedDocument.text("a", "lab", "Editor", List.of("1", "2", "3")).sheets());
        assertEquals(1, PrintedDocument.picture("house.pix", "lab", "Paint", "JSPIX1", "house.pix").sheets());
    }

    @Test
    void printedBy_givesThePrinterItsLook() {
        final PrintedDocument document = PrintedDocument.text("a", "lab", "Editor", List.of("x"));
        assertNull(document.printerModel());
        assertEquals(PrinterModel.EPSILON_FX_80, document.printedBy(PrinterModel.EPSILON_FX_80).printerModel());
    }

    @Test
    void firstLines_passOverTheTitleAndBlankLines() {
        final PrintedDocument document = PrintedDocument.text("LOG", "lab", "Editor",
                List.of("LOG\n\nfirst\n  second  ", "third\nfourth"));
        assertEquals(List.of("first", "second", "third"), document.firstLines(3));
    }

    @Test
    void isPicture_onlyWithAPicture() {
        assertTrue(PrintedDocument.picture("p", "", "", "JSPIX1", "p").isPicture());
        assertFalse(PrintedDocument.text("p", "", "", List.of("x")).isPicture());
    }

    @Test
    void bytes_countThePrint() {
        assertEquals(7, PrintedDocument.text("t", "", "", List.of("abc", "defg")).bytes());
    }

    @Test
    void bytes_countUtf8BytesNotCharacters() {
        assertEquals(6, PrintedDocument.text("t", "", "", List.of("a" + (char) 0xE9 + (char) 0x20AC)).bytes());
    }
}
