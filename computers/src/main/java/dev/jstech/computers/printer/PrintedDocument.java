/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.printer;

import dev.jstech.computers.os.fs.StoredFile;
import dev.jstech.core.text.TextBounds;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * What a printer prints and what the sheet it turns out carries: the document's title, the machine and the program it
 * came from, the printer that printed it, and either its pages of text or one picture. A job in a printer's queue is
 * one of these, and the Printed Paper item holds the one it was printed from, so the sheet reads as it was printed.
 *
 * <p>Everything is bounded on the way in, so a document read back from a save or a packet can never grow past what a
 * printer would have turned out.
 *
 * @param title       the title, a file's name or what the program calls the document
 * @param from        the name of the machine that sent it
 * @param program     the name of the program it was printed from
 * @param printer     the printer's {@link PrinterModel} name, which decides how the sheet looks
 * @param pages       the pages of text, each its lines joined by new lines; one empty page for a picture
 * @param picture     the picture as a Paint file holds it, or empty for a text document
 * @param pictureName the picture's file name, shown under it, or empty
 */
public record PrintedDocument(String title, String from, String program, String printer, List<String> pages,
                              String picture, String pictureName) {

    /** The most pages one document holds; a longer text is cut there. */
    public static final int MAX_PAGES = 64;
    /** The most characters one page holds. */
    public static final int MAX_PAGE_CHARS = 4096;
    /** The most characters of a title, a machine's or a program's name. */
    public static final int MAX_NAME = 64;
    /** The most characters of a picture as Paint saves it: a picture is a file, and holds no more than one. */
    public static final int MAX_PICTURE = StoredFile.MOST_CHARS;
    /** A sheet with nothing on it. */
    public static final PrintedDocument EMPTY = new PrintedDocument("", "", "", "", List.of(""), "", "");

    public PrintedDocument {
        title = TextBounds.clip(title, MAX_NAME);
        from = TextBounds.clip(from, MAX_NAME);
        program = TextBounds.clip(program, MAX_NAME);
        printer = TextBounds.clip(printer, MAX_NAME);
        final List<String> kept = new ArrayList<>();
        for (final String page : pages == null ? List.<String>of() : pages) {
            if (kept.size() >= MAX_PAGES) {
                break;
            }
            kept.add(TextBounds.clip(page, MAX_PAGE_CHARS));
        }
        if (kept.isEmpty()) {
            kept.add("");
        }
        pages = List.copyOf(kept);
        picture = picture == null || picture.length() > MAX_PICTURE ? "" : picture;
        pictureName = TextBounds.clip(pictureName, MAX_NAME);
    }

    /** A document of text, its pages already laid out. */
    public static PrintedDocument text(final String title, final String from, final String program,
                                       final List<String> pages) {
        return new PrintedDocument(title, from, program, "", pages, "", "");
    }

    /** A picture, one to a page. */
    public static PrintedDocument picture(final String title, final String from, final String program,
                                          final String picture, final String pictureName) {
        return new PrintedDocument(title, from, program, "", List.of(""), picture, pictureName);
    }

    /** The same document as printed by {@code model}, which is how its sheet will look. */
    public PrintedDocument printedBy(final PrinterModel model) {
        return new PrintedDocument(title, from, program, model.serializedName(), pages, picture, pictureName);
    }

    /** Whether it is a picture rather than pages of text. */
    public boolean isPicture() {
        return !picture.isEmpty();
    }

    /** How many sheets one copy takes from the tray: one a page, one for a picture. */
    public int sheets() {
        return isPicture() ? 1 : pages.size();
    }

    /** The printer that printed it, or null before it was printed. */
    @Nullable
    public PrinterModel printerModel() {
        return PrinterModel.find(printer);
    }

    /** The first {@code count} lines of print that are not blank, for the sheet's tooltip. */
    public List<String> firstLines(final int count) {
        final List<String> out = new ArrayList<>();
        for (final String page : pages) {
            for (final String line : page.split("\n", -1)) {
                if (out.size() >= count) {
                    return out;
                }
                if (!line.isBlank() && !line.equals(title)) {
                    out.add(line.strip());
                }
            }
        }
        return out;
    }

    /** How many bytes of text it holds, as {@code lpstat} gives a request's size. */
    public int bytes() {
        if (isPicture()) {
            return picture.length();
        }
        int total = 0;
        for (final String page : pages) {
            total += page.length();
        }
        return total;
    }
}
