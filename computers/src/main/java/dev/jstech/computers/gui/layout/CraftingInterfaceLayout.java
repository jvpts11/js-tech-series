/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

import dev.jstech.core.gui.layout.GuiLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Where everything in a Crafting Interface's window goes, with no Minecraft in it so it can be tested. The window has
 * the bus windows' frame (a header with the lamp of its link, the tabs Configure, Activity and Software) and no
 * inventory. Configure holds its name and, one under the other, scrolling when they do not fit: the patterns it holds,
 * the inputs of the one picked with the router of each, its mode, what it feeds, the Receiving Buses that credit it,
 * paused or running, its most jobs, its warnings and what it is doing now.
 */
public final class CraftingInterfaceLayout {

    public static final int WIDTH = 210;
    /** The tallest the window gets: a screen of the game's usual scale holds it whole. */
    public static final int MAX_HEIGHT = 240;
    public static final int HEADER_X = 6;
    public static final int HEADER_Y = 6;
    public static final int HEADER_W = WIDTH - 12;
    public static final int TITLE_X = 12;
    public static final int TITLE_Y = 11;
    public static final int LAMP_SIZE = 5;
    public static final int LAMP_X = HEADER_X + HEADER_W - 12;
    public static final int LAMP_Y = TITLE_Y;
    public static final int TAB_Y = 26;
    public static final int TAB_H = 11;
    public static final int TABS = 3;
    public static final int TAB_CONFIGURE = 0;
    public static final int TAB_ACTIVITY = 1;
    public static final int TAB_SOFTWARE = 2;
    public static final int NAME_Y = 41;
    public static final int NAME_H = 11;
    public static final int NAME_X = 46;
    public static final int NAME_W = WIDTH - 54;
    public static final int LABEL_X = 8;
    public static final int CONTROL_X = 46;
    public static final int RIGHT = WIDTH - 14;
    public static final int ROW_W = RIGHT - LABEL_X;
    public static final int SCROLL_X = RIGHT + 3;
    public static final int SCROLL_W = 3;
    public static final int CONFIGURE_VIEW_Y = 56;
    public static final int VIEW_Y = 41;
    public static final int ROW = 12;
    public static final int CONTROL_H = 10;
    public static final int LINE = 8;
    public static final int CELL = 18;
    /** How many pattern cells go in a row before the next starts. */
    public static final int CELLS_PER_ROW = (RIGHT - CONTROL_X) / CELL;
    /** An input's row: its cell, its name and amount over why its router carries it, and the router to pick. */
    public static final int MAPPING_H = 20;
    public static final int PICK_X = 112;
    public static final int PICK_W = RIGHT - PICK_X;
    /** A stepper: minus, the value, plus, then what the value means. */
    public static final int MINUS_X = 46;
    public static final int STEP_W = 9;
    public static final int VALUE_X = 57;
    public static final int VALUE_W = 28;
    public static final int PLUS_X = 87;
    public static final int NOTE_X = 100;
    public static final int OPTION_GAP = 2;
    public static final int OPTION_PAD = 6;
    /** Activity: two lines for each job, ten in view, the note under them. */
    public static final int ENTRY_H = 18;
    public static final int ENTRIES_SHOWN = 9;
    public static final int ACTIVITY_VIEW_H = ENTRY_H * ENTRIES_SHOWN;
    public static final int ACTIVITY_NOTE_Y = VIEW_Y + ACTIVITY_VIEW_H + 4;
    public static final int ACTIVITY_HEIGHT = ACTIVITY_NOTE_Y + 2 * LINE + 7;
    /** Software: a word over each box of code. */
    public static final int SECTION_LABEL_H = 9;
    public static final int BOX_PAD = 3;
    public static final int SECTION_GAP = 4;
    private static final int[] TAB_X = {8, 64, 114};
    private static final int[] TAB_W = {54, 48, 48};

    private CraftingInterfaceLayout() {
    }

    /** One row of the Configure tab: what it is, where it starts in the rows, how tall it is, and which one. */
    public record Row(Kind kind, int y, int height, int index) {
    }

    /** What a row of the Configure tab is. */
    public enum Kind {
        PATTERNS, PATTERN_NOTE, INPUTS_LABEL, MAPPING, MODE, MODE_NOTE, FEEDS, RECEIVING, STATE, JOBS, WARNING, NOW
    }

    /**
     * What decides the Configure rows: how many patterns it holds at most, and the lines each paragraph takes in the
     * window's letters.
     *
     * @param capacity      how many patterns it holds at most
     * @param noteLines     the lines of the note under the patterns
     * @param inputs        how many inputs the pattern picked has, 0 when it feeds its machine directly
     * @param modeNoteLines the lines of the note under the mode
     * @param feedsLines    the lines of what it feeds
     * @param warningLines  the lines of each warning
     * @param nowLines      the lines of what it is doing now
     */
    public record Shape(int capacity, int noteLines, int inputs, int modeNoteLines, int feedsLines,
                        List<Integer> warningLines, int nowLines) {

        public Shape {
            warningLines = List.copyOf(warningLines);
        }

        /** The rows at their most: every pattern, nine inputs, every paragraph long and three warnings. */
        public static Shape most(final int capacity) {
            return new Shape(capacity, 2, 9, 2, 2, List.of(3, 3, 3), 2);
        }
    }

    /** The Configure rows of that shape, in order, from the top of the scrolled area. */
    public static List<Row> rows(final Shape shape) {
        final List<Row> rows = new ArrayList<>();
        final int[] y = {0};
        add(rows, y, Kind.PATTERNS, cellRows(shape.capacity()) * CELL + 2, 0);
        add(rows, y, Kind.PATTERN_NOTE, paragraph(shape.noteLines()), 0);
        if (shape.inputs() > 0) {
            add(rows, y, Kind.INPUTS_LABEL, LINE + 2, 0);
            for (int i = 0; i < shape.inputs(); i++) {
                add(rows, y, Kind.MAPPING, MAPPING_H + 1, i);
            }
        }
        add(rows, y, Kind.MODE, ROW, 0);
        add(rows, y, Kind.MODE_NOTE, paragraph(shape.modeNoteLines()), 0);
        add(rows, y, Kind.FEEDS, Math.max(1, shape.feedsLines()) * LINE + 4, 0);
        add(rows, y, Kind.RECEIVING, ROW, 0);
        add(rows, y, Kind.STATE, ROW, 0);
        add(rows, y, Kind.JOBS, ROW, 0);
        for (int i = 0; i < shape.warningLines().size(); i++) {
            add(rows, y, Kind.WARNING, Math.max(1, shape.warningLines().get(i)) * LINE + 8, i);
        }
        add(rows, y, Kind.NOW, Math.max(1, shape.nowLines()) * LINE + 4, 0);
        return rows;
    }

    /** How tall the rows of that shape are together. */
    public static int contentHeight(final Shape shape) {
        final List<Row> rows = rows(shape);
        final Row last = rows.get(rows.size() - 1);
        return last.y() + last.height();
    }

    /** How many rows of cells the patterns take. */
    public static int cellRows(final int capacity) {
        return Math.max(1, (capacity + CELLS_PER_ROW - 1) / CELLS_PER_ROW);
    }

    /** How tall the Configure rows' area is for a window of that shape: up to what the window holds. */
    public static int configureView(final Shape shape) {
        return Math.min(MAX_HEIGHT - CONFIGURE_VIEW_Y - 7, contentHeight(shape));
    }

    /** The window's height on Configure. */
    public static int configureHeight(final Shape shape) {
        return CONFIGURE_VIEW_Y + configureView(shape) + 7;
    }

    /** The Software tab's area, as tall as its sections up to what the window holds. */
    public static int softwareView(final int contentHeight) {
        return Math.max(ROW, Math.min(MAX_HEIGHT - VIEW_Y - 7, contentHeight));
    }

    /** The window's height on Software. */
    public static int softwareHeight(final int contentHeight) {
        return VIEW_Y + softwareView(contentHeight) + 7;
    }

    /** How tall a box of code of that many lines is. */
    public static int codeBox(final int lines) {
        return Math.max(1, lines) * LINE + 2 * BOX_PAD;
    }

    public static int tabX(final int tab) {
        return TAB_X[tab];
    }

    public static int tabW(final int tab) {
        return TAB_W[tab];
    }

    /** Where the options of a toggle of these widths go, from {@code x}: the left edge of each. */
    public static int[] options(final int x, final int... widths) {
        final int[] at = new int[widths.length];
        int left = x;
        for (int i = 0; i < widths.length; i++) {
            at[i] = left;
            left += widths[i] + OPTION_GAP;
        }
        return at;
    }

    /** The window's frame on Configure, for the checks: header, title, lamp, tabs, name and the rows' area. */
    public static GuiLayout frame(final Shape shape) {
        final int view = configureView(shape);
        final GuiLayout l = new GuiLayout(WIDTH, configureHeight(shape))
                .box("header", HEADER_X, HEADER_Y, HEADER_W, 17)
                .box("nameField", NAME_X, NAME_Y, NAME_W, NAME_H)
                .box("rows", LABEL_X, CONFIGURE_VIEW_Y, ROW_W, view)
                .box("scrollbar", SCROLL_X, CONFIGURE_VIEW_Y, SCROLL_W, view);
        for (int tab = 0; tab < TABS; tab++) {
            l.box("tab" + tab, TAB_X[tab], TAB_Y, TAB_W[tab], TAB_H);
        }
        l.text("title", TITLE_X, TITLE_Y, 18, 1.0f);
        l.text("nameLabel", LABEL_X, NAME_Y + 2, 4, 0.75f);
        return l;
    }

    /** The Configure rows of that shape laid out flat, unscrolled, each control a box and each word a line of text. */
    public static GuiLayout content(final Shape shape) {
        final GuiLayout l = new GuiLayout(WIDTH, contentHeight(shape));
        for (final Row row : rows(shape)) {
            final String n = row.kind().name().toLowerCase() + row.index();
            final int y = row.y();
            switch (row.kind()) {
                case PATTERNS -> {
                    l.text(n + "_label", LABEL_X, y + 2, 8, 0.75f);
                    l.text(n + "_count", LABEL_X, y + 11, 7, 0.75f);
                    for (int i = 0; i < shape.capacity(); i++) {
                        l.box(n + "_cell" + i, CONTROL_X + i % CELLS_PER_ROW * CELL,
                                y + 1 + i / CELLS_PER_ROW * CELL, CELL, CELL);
                    }
                }
                case PATTERN_NOTE, MODE_NOTE -> {
                    for (int line = 0; line < (row.height() - 2) / LINE; line++) {
                        l.text(n + "_" + line, LABEL_X, y + 1 + line * LINE, 40, 0.75f);
                    }
                }
                case INPUTS_LABEL -> l.text(n, LABEL_X, y + 1, 40, 0.75f);
                case MAPPING -> {
                    l.box(n + "_cell", LABEL_X + 1, y + 1, CELL, CELL);
                    l.text(n + "_name", LABEL_X + CELL + 4, y + 2, 16, 0.75f);
                    l.text(n + "_why", LABEL_X + CELL + 4, y + 11, 16, 0.75f);
                    l.box(n + "_pick", PICK_X, y + 4, PICK_W, CONTROL_H);
                }
                case MODE -> toggle(l, n, y, 4, 9, 13);
                case STATE -> toggle(l, n, y, 5, 7, 6);
                case JOBS -> {
                    l.text(n + "_label", LABEL_X, y + 2, 8, 0.75f);
                    l.box(n + "_minus", MINUS_X, y, STEP_W, CONTROL_H);
                    l.box(n + "_value", VALUE_X, y, VALUE_W, CONTROL_H);
                    l.box(n + "_plus", PLUS_X, y, STEP_W, CONTROL_H);
                    l.text(n + "_note", NOTE_X, y + 2, 18, 0.75f);
                }
                case FEEDS, NOW -> {
                    l.text(n + "_label", LABEL_X, y + 2, 5, 0.75f);
                    for (int line = 0; line < (row.height() - 4) / LINE; line++) {
                        l.text(n + "_" + line, CONTROL_X, y + 2 + line * LINE, 26, 0.75f);
                    }
                }
                case RECEIVING -> {
                    l.text(n + "_label", LABEL_X, y + 2, 9, 0.75f);
                    l.text(n + "_value", CONTROL_X + 4, y + 2, 25, 0.75f);
                }
                case WARNING -> l.box(n, LABEL_X, y, ROW_W, row.height() - 2);
            }
        }
        return l;
    }

    /* A row's word and its options, each as wide as its longest English word with the room beside it. */
    private static void toggle(final GuiLayout l, final String n, final int y, final int labelChars,
                               final int... optionChars) {
        l.text(n + "_label", LABEL_X, y + 2, labelChars, 0.75f);
        final int[] widths = new int[optionChars.length];
        for (int i = 0; i < optionChars.length; i++) {
            widths[i] = Math.round(optionChars[i] * GuiLayout.GLYPH_WIDTH * 0.75f) + OPTION_PAD;
        }
        final int[] at = options(CONTROL_X, widths);
        for (int i = 0; i < at.length; i++) {
            l.box(n + "_option" + i, at[i], y, widths[i], CONTROL_H);
        }
    }

    private static void add(final List<Row> rows, final int[] y, final Kind kind, final int height, final int index) {
        rows.add(new Row(kind, y[0], height, index));
        y[0] += height;
    }

    private static int paragraph(final int lines) {
        return Math.max(1, lines) * LINE + 2;
    }
}
