/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.guide;

import dev.jstech.core.client.audio.AudioEngine;
import dev.jstech.core.gui.ManualButtonLook;
import dev.jstech.core.gui.layout.ManualScreenLayout;
import dev.jstech.core.guide.GuideSounds;
import dev.jstech.core.guide.GuideStyle;
import dev.jstech.core.guide.GuideTexts;
import dev.jstech.core.guide.IGuideText;
import dev.jstech.core.guide.TextSize;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * A reader's marks on an open manual: the ribbons of their bookmarks hanging from the binder's top edge over the pages
 * they mark, the list of them open under its button on the binder, with the row that marks the page shown, and the
 * notes they write in a page's margin on yellow paper in a blue pencil, written as they type. What it keeps lives in
 * {@link ManualMarks}; the screen tells it what is shown and where.
 */
final class ManualMarksView {

    private final String manual;
    private final Font font;
    private final GuideStyle style;
    private final IGuideText text;
    private boolean listOpen;
    /* The page whose note is being written, and what has been written so far. */
    @Nullable
    private ManualPlaces.Place editing;
    private final StringBuilder typed = new StringBuilder();

    /** The most lines a note takes in the margin. */
    private static final int NOTE_LINES = 6;
    private static final int CARET_MILLIS = 500;
    private static final double ROW_LIGHT = 0.8;
    private static final double TAPE_LIGHT = 0.6;
    private static final int TAPE_WIDTH = 16;
    private static final int SWATCH = 4;
    private static final String CARET = "_";
    private static final String ELLIPSIS = "...";

    ManualMarksView(final String manual, final Font font, final GuideStyle style, final IGuideText text) {
        this.manual = manual;
        this.font = font;
        this.style = style;
        this.text = text;
    }

    /** Whether the list of bookmarks is open. */
    boolean listOpen() {
        return this.listOpen;
    }

    /** Opens the list of bookmarks, or closes it. */
    void toggleList() {
        this.listOpen = !this.listOpen;
    }

    /** Whether a note is being written. */
    boolean writing() {
        return this.editing != null;
    }

    /** Starts writing the note of the page at {@code place}, with what it says already. */
    void write(final ManualPlaces.Place place) {
        this.keep();
        this.editing = place;
        this.typed.setLength(0);
        this.typed.append(ManualMarks.note(this.manual, place).orElse(""));
    }

    /** Keeps the note being written, if one is, and stops writing it. */
    void keep() {
        if (this.editing != null) {
            ManualMarks.write(this.manual, this.editing, this.typed.toString());
            this.editing = null;
            this.typed.setLength(0);
        }
    }

    /** A letter typed into the note being written; whether one is. */
    boolean typed(final char letter) {
        if (this.editing == null) {
            return false;
        }
        if (letter >= ' ' && this.typed.length() < ManualMarks.MOST_NOTE) {
            this.typed.append(letter);
        }
        return true;
    }

    /**
     * A key pressed while a note is written: Backspace takes a letter back, Enter or Escape keeps the note, and
     * Ctrl+V puts in what was copied; whether one is being written.
     */
    boolean key(final int key) {
        if (this.editing == null) {
            return false;
        }
        switch (key) {
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (!this.typed.isEmpty()) {
                    this.typed.setLength(this.typed.length() - 1);
                }
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER, GLFW.GLFW_KEY_ESCAPE -> this.keep();
            case GLFW.GLFW_KEY_V -> {
                if (Screen.hasControlDown()) {
                    final String copied = Minecraft.getInstance().keyboardHandler.getClipboard()
                            .replaceAll("\\s+", " ");
                    this.typed.append(copied, 0, Math.min(copied.length(),
                            ManualMarks.MOST_NOTE - this.typed.length()));
                }
            }
            default -> {
            }
        }
        return true;
    }

    /** The ribbons of the bookmarks of the pages shown, hanging from the binder's top edge over each. */
    void drawRibbons(final GuiGraphics g, final Host host) {
        final GuidePalettes.Marks c = GuidePalettes.MARKS.get();
        final List<ManualMarks.Bookmark> bookmarks = ManualMarks.bookmarks(this.manual);
        for (final Shown page : host.shown()) {
            int slot = 0;
            for (final ManualMarks.Bookmark bookmark : bookmarks) {
                if (bookmark.place().equals(page.place())) {
                    ribbon(g, ManualScreenLayout.ribbon(host.geometry(), page.right(), slot++),
                            c.ribbon(bookmark.colour()));
                }
            }
        }
    }

    /** The notes on the pages shown, and the one being written. */
    void drawNotes(final GuiGraphics g, final Host host) {
        for (final Shown page : host.shown()) {
            final boolean writing = page.place().equals(this.editing);
            final Optional<String> note = writing ? Optional.of(this.typed.toString())
                    : ManualMarks.note(this.manual, page.place());
            if (note.isPresent()) {
                this.note(g, page.rect(), note.get(), writing);
            }
        }
    }

    /** The list of bookmarks, open under its button: each with its colour, what it marks and its page. */
    void drawList(final GuiGraphics g, final Host host, final int mx, final int my) {
        if (!this.listOpen) {
            return;
        }
        final GuidePalettes.Marks c = GuidePalettes.MARKS.get();
        final List<Row> rows = host.rows();
        final int shownRows = Math.max(1, rows.size());
        final ManualScreenLayout.Rect list = ManualScreenLayout.bookmarkList(host.geometry(), shownRows);
        g.fill(list.x() + 1, list.y() + 1, list.x() + list.width() + 1, list.y() + list.height() + 1, c.lift());
        g.fill(list.x(), list.y(), list.x() + list.width(), list.y() + list.height(), host.colour(GuideStyle.PAPER));
        final int rule = host.colour(GuideStyle.RULE);
        outline(g, list, rule);
        GuideFonts.draw(g, this.font, this.text.text(GuideTexts.BOOKMARKS.key()), TextSize.BODY, this.style,
                list.x() + ManualScreenLayout.LIST_PAD, list.y() + ManualScreenLayout.LIST_PAD,
                host.colour(GuideStyle.HEADING));
        final int ink = host.colour(GuideStyle.INK);
        if (rows.isEmpty()) {
            final ManualScreenLayout.Rect none = ManualScreenLayout.bookmarkRow(list, 0, 1);
            GuideFonts.draw(g, this.font, this.text.text(GuideTexts.NO_BOOKMARKS.key()), TextSize.SMALL, this.style,
                    none.x(), none.y() + 2, host.colour(GuideStyle.FAINT));
        }
        for (int i = 0; i < rows.size(); i++) {
            final ManualScreenLayout.Rect at = ManualScreenLayout.bookmarkRow(list, i, rows.size());
            final Row row = rows.get(i);
            if (at.contains(mx, my)) {
                g.fill(at.x() - 1, at.y(), at.x() + at.width() + 1, at.y() + at.height(),
                        ManualButtonLook.lighten(host.colour(GuideStyle.LINK), ROW_LIGHT));
            }
            g.fill(at.x(), at.y() + 1, at.x() + SWATCH, at.y() + at.height() - 2, c.ribbon(row.colour()));
            final int folioWidth = GuideFonts.width(this.font, row.folio(), TextSize.SMALL, this.style);
            GuideFonts.draw(g, this.font, this.fit(row.title(), at.width() - SWATCH - folioWidth - 8), TextSize.SMALL,
                    this.style, at.x() + SWATCH + 3, at.y() + 2, ink);
            GuideFonts.draw(g, this.font, row.folio(), TextSize.SMALL, this.style, at.x() + at.width() - folioWidth,
                    at.y() + 2, ink);
        }
        final int last = Math.max(1, rows.size());
        final ManualScreenLayout.Rect add = ManualScreenLayout.bookmarkRow(list, last, last);
        g.fill(add.x(), add.y() - 2, add.x() + add.width(), add.y() - 1, rule);
        GuideFonts.draw(g, this.font, this.text.text(GuideTexts.MARK_PAGE.key()), TextSize.SMALL, this.style, add.x(),
                add.y() + 2, add.contains(mx, my) ? host.colour(GuideStyle.HEADING) : host.colour(GuideStyle.LINK));
    }

    /**
     * What a click does to the marks: in the open list, goes to a bookmark's page, takes it off with the right button,
     * or marks the page shown, and a click off the list closes it; with the right button on a page, writes its note.
     * Whether it took the click.
     */
    boolean click(final Host host, final double mx, final double my, final int button) {
        if (this.listOpen) {
            final List<Row> rows = host.rows();
            final int shownRows = Math.max(1, rows.size());
            final ManualScreenLayout.Rect list = ManualScreenLayout.bookmarkList(host.geometry(), shownRows);
            if (list.contains(mx, my)) {
                for (int i = 0; i < rows.size(); i++) {
                    if (ManualScreenLayout.bookmarkRow(list, i, rows.size()).contains(mx, my)) {
                        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                            ManualMarks.unmark(this.manual, rows.get(i).place());
                        } else {
                            this.listOpen = false;
                            host.goToPage(rows.get(i).page());
                        }
                        AudioEngine.playOnScreen(GuideSounds.CLICK);
                        return true;
                    }
                }
                if (ManualScreenLayout.bookmarkRow(list, shownRows, shownRows).contains(mx, my)
                        && !host.shown().isEmpty()) {
                    ManualMarks.mark(this.manual, host.shown().getFirst().place());
                    AudioEngine.playOnScreen(GuideSounds.CLICK);
                }
                return true;
            }
            if (!ManualScreenLayout.bookmarks(host.geometry()).contains(mx, my)) {
                this.listOpen = false;
                return true;
            }
        }
        for (final Shown page : host.shown()) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && page.rect().contains(mx, my)) {
                this.write(page.place());
                return true;
            }
            final Optional<String> note = ManualMarks.note(this.manual, page.place());
            if (note.isPresent() && this.noteRect(page.rect(), note.get()).contains(mx, my)) {
                this.write(page.place());
                return true;
            }
        }
        if (this.editing != null) {
            // A click anywhere else keeps the note.
            this.keep();
        }
        return false;
    }

    /** The note of the page at {@code place} as it is drawn in that page's margin. */
    ManualScreenLayout.Rect noteRect(final ManualScreenLayout.Rect page, final String note) {
        return ManualScreenLayout.note(page, this.lines(note).size());
    }

    private void note(final GuiGraphics g, final ManualScreenLayout.Rect page, final String said,
                      final boolean writing) {
        final GuidePalettes.Marks c = GuidePalettes.MARKS.get();
        final boolean blink = writing && Util.getMillis() / CARET_MILLIS % 2 == 0;
        final List<String> lines = this.lines(writing && blink ? said + CARET : said);
        final ManualScreenLayout.Rect slip = ManualScreenLayout.note(page, lines.size());
        g.fill(slip.x() + 1, slip.y() + 1, slip.x() + slip.width() + 1, slip.y() + slip.height() + 1, c.lift());
        g.fill(slip.x(), slip.y(), slip.x() + slip.width(), slip.y() + slip.height(), c.note());
        g.fill(slip.x(), slip.y() + slip.height() - 1, slip.x() + slip.width(), slip.y() + slip.height(),
                c.noteEdge());
        final int tape = slip.x() + (slip.width() - TAPE_WIDTH) / 2;
        g.fill(tape, slip.y() - 2, tape + TAPE_WIDTH, slip.y() + 3, ManualButtonLook.lighten(c.note(), TAPE_LIGHT));
        if (said.isEmpty() && !blink) {
            GuideFonts.draw(g, this.font, this.text.text(GuideTexts.NOTE_EMPTY.key()), TextSize.SMALL, this.style,
                    slip.x() + ManualScreenLayout.NOTE_PAD, slip.y() + ManualScreenLayout.NOTE_PAD,
                    ManualButtonLook.lighten(c.pencil(), TAPE_LIGHT));
            return;
        }
        for (int i = 0; i < lines.size(); i++) {
            GuideFonts.draw(g, this.font, lines.get(i), TextSize.SMALL, this.style,
                    slip.x() + ManualScreenLayout.NOTE_PAD,
                    slip.y() + ManualScreenLayout.NOTE_PAD + i * ManualScreenLayout.NOTE_LINE, c.pencil());
        }
    }

    /** A note's words broken into the lines its paper holds, at most as many as it takes, the last cut short. */
    private List<String> lines(final String said) {
        final int room = ManualScreenLayout.NOTE_WIDTH - 2 * ManualScreenLayout.NOTE_PAD;
        final List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (final String word : said.split(" ", -1)) {
            final String tried = line.isEmpty() ? word : line + " " + word;
            if (GuideFonts.width(this.font, tried, TextSize.SMALL, this.style) <= room) {
                line = new StringBuilder(tried);
                continue;
            }
            if (!line.isEmpty()) {
                lines.add(line.toString());
            }
            // A word longer than the paper is wide goes on in pieces.
            String rest = word;
            while (GuideFonts.width(this.font, rest, TextSize.SMALL, this.style) > room && rest.length() > 1) {
                int cut = rest.length() - 1;
                while (cut > 1 && GuideFonts.width(this.font, rest.substring(0, cut), TextSize.SMALL, this.style)
                        > room) {
                    cut--;
                }
                lines.add(rest.substring(0, cut));
                rest = rest.substring(cut);
            }
            line = new StringBuilder(rest);
        }
        lines.add(line.toString());
        if (lines.size() > NOTE_LINES) {
            final List<String> cut = new ArrayList<>(lines.subList(0, NOTE_LINES));
            cut.set(NOTE_LINES - 1, this.fit(cut.getLast() + ELLIPSIS, room));
            return cut;
        }
        return lines;
    }

    private String fit(final String words, final int room) {
        if (GuideFonts.width(this.font, words, TextSize.SMALL, this.style) <= room) {
            return words;
        }
        String cut = words;
        while (!cut.isEmpty() && GuideFonts.width(this.font, cut + ELLIPSIS, TextSize.SMALL, this.style) > room) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut + ELLIPSIS;
    }

    /* A ribbon of fabric, its left edge shaded, cut in a V at its end. */
    private static void ribbon(final GuiGraphics g, final ManualScreenLayout.Rect at, final int fabric) {
        final int end = at.y() + at.height() - ManualScreenLayout.RIBBON_NOTCH;
        g.fill(at.x(), at.y(), at.x() + at.width(), end, fabric);
        g.fill(at.x(), at.y(), at.x() + 1, end, ManualButtonLook.darken(fabric, 0.25));
        for (int k = 0; k < ManualScreenLayout.RIBBON_NOTCH; k++) {
            g.fill(at.x() + k, end + k, at.x() + k + 1, end + ManualScreenLayout.RIBBON_NOTCH, fabric);
            g.fill(at.x() + at.width() - 1 - k, end + k, at.x() + at.width() - k, end + ManualScreenLayout.RIBBON_NOTCH,
                    fabric);
        }
    }

    private static void outline(final GuiGraphics g, final ManualScreenLayout.Rect at, final int colour) {
        g.fill(at.x(), at.y(), at.x() + at.width(), at.y() + 1, colour);
        g.fill(at.x(), at.y() + at.height() - 1, at.x() + at.width(), at.y() + at.height(), colour);
        g.fill(at.x(), at.y(), at.x() + 1, at.y() + at.height(), colour);
        g.fill(at.x() + at.width() - 1, at.y(), at.x() + at.width(), at.y() + at.height(), colour);
    }

    /** What the screen tells the marks: the binder, the pages shown, the bookmarks as the list shows them. */
    interface Host {

        /** The binder laid out. */
        ManualScreenLayout.Geometry geometry();

        /** The pages shown, the left first. */
        List<Shown> shown();

        /** The bookmarks, as the list shows them. */
        List<Row> rows();

        /** Shows the spread holding that page. */
        void goToPage(int page);

        /** A role's colour in the book's style. */
        int colour(String role);
    }

    /**
     * A page shown.
     *
     * @param page  its number in the book, from 0
     * @param rect  where it stands on the binder
     * @param place where it stands in its entry, section or chapter
     * @param right whether it is the right page of the spread
     */
    record Shown(int page, ManualScreenLayout.Rect rect, ManualPlaces.Place place, boolean right) {
    }

    /**
     * A bookmark as the list shows it.
     *
     * @param place  where its page stands
     * @param colour its ribbon's colour
     * @param title  what its page is: its entry's, section's or chapter's title
     * @param folio  its page's number as the book prints it
     * @param page   its page in the book
     */
    record Row(ManualPlaces.Place place, int colour, String title, String folio, int page) {
    }
}
