/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

import dev.jstech.computers.bus.BusAbilities;
import dev.jstech.computers.bus.BusFeature;
import dev.jstech.core.gui.layout.GuiLayout;
import dev.jstech.core.tier.HardwareEra;

import java.util.ArrayList;
import java.util.List;

/**
 * Where everything in a bus's window goes, with no Minecraft in it so it can be tested. The window has three tabs:
 * Configure, the settings over the player's inventory; Activity, what the bus did lately; and Software, the same
 * settings as software writes them. A bus of an era shows the rows its era can be set to, one under the other; when
 * they do not all fit above the inventory, the rows scroll, so the window never grows past what a screen holds.
 *
 * <p>The menu places the inventory from {@link #inventoryY}, the screen draws from the same numbers, and the test
 * checks them, so a row that moves, a longer word or a new setting is caught before it reaches the game. Coordinates
 * are the window's; the rows' are the scrolled area's, from its top, and the screen adds where that area is.
 */
public final class BusLayout {

    public static final int WIDTH = 176;
    /** The tallest a bus window gets: a screen of the game's usual scale holds it whole. */
    public static final int MAX_HEIGHT = 256;

    public static final int HEADER_X = 6;
    public static final int HEADER_Y = 6;
    public static final int HEADER_W = 164;
    public static final int TITLE_X = 12;
    public static final int TITLE_Y = 11;
    /** The longest title of the four, "CRAFTING RECEIVING BUS", in characters. */
    public static final int TITLE_MOST_CHARS = 22;
    /** The link lamp at the header's right end, its word in its tooltip. */
    public static final int LAMP_SIZE = 5;
    public static final int LAMP_X = HEADER_X + HEADER_W - 12;
    public static final int LAMP_Y = TITLE_Y;

    /** The three tabs under the header. */
    public static final int TAB_Y = 26;
    public static final int TAB_H = 11;
    public static final int TABS = 3;
    public static final int TAB_CONFIGURE = 0;
    public static final int TAB_ACTIVITY = 1;
    public static final int TAB_SOFTWARE = 2;

    /** The name, on Configure only, above the rows. */
    public static final int NAME_Y = 41;
    public static final int NAME_H = 11;
    public static final int NAME_X = 46;
    public static final int NAME_W = 122;

    /** Where a row's word goes, and where its controls start. */
    public static final int LABEL_X = 8;
    public static final int CONTROL_X = 46;
    /** The right edge of the rows; the scrollbar runs beside it. */
    public static final int RIGHT = 162;
    public static final int ROW_W = RIGHT - LABEL_X;
    public static final int SCROLL_X = 165;
    public static final int SCROLL_W = 3;

    /** Where the rows start on Configure, and on Activity and Software, which have no name row. */
    public static final int CONFIGURE_VIEW_Y = 56;
    public static final int VIEW_Y = 41;

    /** A row of controls: ten tall, one every twelve. */
    public static final int ROW = 12;
    public static final int CONTROL_H = 10;
    /** A line of the small letters in a paragraph. */
    public static final int LINE = 8;
    /** The space between a toggle's options. */
    public static final int OPTION_GAP = 2;
    /** What a toggle option or a chip has beside its word. */
    public static final int OPTION_PAD = 6;

    /** A stepper: minus, the value, plus, then what the value means. */
    public static final int MINUS_X = 46;
    public static final int STEP_W = 9;
    public static final int VALUE_X = 57;
    public static final int VALUE_W = 28;
    public static final int PLUS_X = 87;
    public static final int NOTE_X = 100;

    /** The filter's cells, in a row. */
    public static final int CELL = 18;

    /** A Transition item row: its cell, its name, and its own keep and max under the name. */
    public static final int ITEM_H = 20;
    public static final int ITEM_CELL_X = 9;
    public static final int ITEM_NAME_X = 30;
    public static final int ITEM_KEEP_X = 30;
    public static final int ITEM_KEEP_MINUS = 51;
    public static final int ITEM_KEEP_VALUE = 60;
    public static final int ITEM_KEEP_PLUS = 81;
    public static final int ITEM_MAX_X = 94;
    public static final int ITEM_MAX_MINUS = 111;
    public static final int ITEM_MAX_VALUE = 120;
    public static final int ITEM_MAX_PLUS = 141;
    public static final int ITEM_STEP = 8;
    public static final int ITEM_VALUE_W = 20;

    /** A condition's bar and the button that takes it away. */
    public static final int REMOVE_X = 152;
    public static final int REMOVE_W = 9;

    /**
     * The condition being written: what kind on its first line, its own fields on the second, and ADD and CANCEL at
     * the right of the third, as wide as their words (these are the English ones).
     */
    public static final int EDIT_ADD_W = 24;
    public static final int EDIT_CANCEL_W = 31;
    public static final int EDIT_CANCEL_X = RIGHT - EDIT_CANCEL_W;
    public static final int EDIT_ADD_X = EDIT_CANCEL_X - 3 - EDIT_ADD_W;
    public static final int EDIT_FIELD_W = 84;
    public static final int EDIT_MINUS_X = 96;
    public static final int EDIT_VALUE_X = 107;
    public static final int EDIT_PLUS_X = 137;
    public static final int HOURS_FROM_MINUS = 30;
    public static final int HOURS_FROM_VALUE = 41;
    public static final int HOURS_FROM_PLUS = 67;
    public static final int HOURS_TO_X = 82;
    public static final int HOURS_TO_MINUS = 96;
    public static final int HOURS_TO_VALUE = 107;
    public static final int HOURS_TO_PLUS = 133;
    public static final int HOURS_VALUE_W = 24;

    /** A tag being added: its field and ADD beside the chips. */
    public static final int TAG_FIELD_W = 92;
    public static final int TAG_ADD_X = 142;
    public static final int TAG_ADD_W = 20;

    /** The most conditions a bus has; the window offers no more past them. */
    public static final int MOST_CONDITIONS = 4;

    /** The tallest the Configure rows get before they scroll, under the name and over the inventory. */
    public static final int CONFIGURE_VIEW_MOST = 104;
    /** What the inventory takes under the rows: its word, its grid, its hotbar and the window's edge. */
    public static final int INVENTORY_BLOCK = 96;
    public static final int INV_X = 8;

    /** Activity: a line each for what moved and why, eight in view, the note under them. */
    public static final int ENTRY_H = 18;
    public static final int ENTRIES_SHOWN = 8;
    public static final int ACTIVITY_VIEW_H = ENTRY_H * ENTRIES_SHOWN;
    public static final int ACTIVITY_NOTE_Y = VIEW_Y + ACTIVITY_VIEW_H + 4;
    public static final int ACTIVITY_NOTE_LINES = 3;
    public static final int ACTIVITY_HEIGHT = ACTIVITY_NOTE_Y + ACTIVITY_NOTE_LINES * LINE + 7;

    /** Software: a word over each box of code, a line of code each eight, under its word. */
    public static final int SECTION_LABEL_H = 9;
    public static final int BOX_PAD = 3;
    public static final int SECTION_GAP = 4;
    public static final int SOFTWARE_VIEW_MOST = MAX_HEIGHT - VIEW_Y - 7;

    /* The most lines a paragraph is given room for when the window's size is set: longer ones scroll. */
    private static final int INTRO_LINES = 3;
    private static final int NOTE_LINES = 4;
    /* The lines an External Storage Bus's access takes: its three options do not fit beside its word on one. */
    private static final int ACCESS_LINES = 2;
    /* A crafting part's window at its most: its lines with their notes, and its warning boxes. */
    private static final int MOST_CRAFT_LINES = 6;
    private static final int MOST_CRAFT_BOXES = 3;
    private static final int CRAFT_BOX_LINES = 3;
    private static final int[] TAB_X = {8, 64, 114};
    private static final int[] TAB_W = {54, 48, 48};

    private BusLayout() {
    }

    /** One row of the Configure tab: what it is, where it starts in the rows, how tall it is, and which one. */
    public record Row(Kind kind, int y, int height, int index) {
    }

    /** What a row of the Configure tab is. */
    public enum Kind {
        INTRO, NOW, HOLDS, FILTER, ITEMS_LABEL, ITEM, ADD_ITEM, TAGS, TAG_EDITOR, MATCH, FUZZY_NOTE, FILTER_MODE,
        ACCESS, KEEP, MAX, MODE, POWER, PRIORITY, CONDITIONS_LABEL, CONDITION, ADD_CONDITION, CONDITION_EDITOR,
        SPEED, NOTE, CRAFT_LINE
    }

    /**
     * What kind of bus a window is for: one that moves (an Import or an Export Bus); a crafting part, a Crafting Input
     * Router or a Crafting Receiving Bus, which has its filter and the lines of what it is part of; or an External
     * Storage Bus, which lends the network an inventory.
     */
    public enum Window {
        MOVER, ROUTER, RECEIVING, EXTERNAL;

        /** Whether it is a crafting part's window. */
        public boolean crafting() {
            return this == ROUTER || this == RECEIVING;
        }
    }

    /**
     * What decides the Configure rows: the bus's abilities, what kind of bus it is, and how much of each thing it has
     * now, with the lines its paragraphs take in the window's letters.
     *
     * @param abilities      what the bus's era can be set to
     * @param window         what kind of bus it is
     * @param introLines     the lines the paragraph of a Vintage bus takes
     * @param itemRows       the Transition rows shown, one per item listed and any empty one between them
     * @param tagLines       the lines the Advanced bus's tags take
     * @param editingTag     whether a tag is being typed
     * @param fuzzyLines     the lines the loose match's note takes
     * @param conditions     how many conditions the bus has
     * @param editingCondition whether a condition is being written
     * @param noteLines      the lines the note at the end of an external bus's rows takes
     * @param crafting       for a crafting part, how tall each line of what it is part of is, in order
     */
    public record Shape(BusAbilities abilities, Window window, int introLines, int itemRows, int tagLines,
                        boolean editingTag, int fuzzyLines, int conditions, boolean editingCondition,
                        int noteLines, List<Integer> crafting) {

        public Shape {
            crafting = List.copyOf(crafting);
        }

        /** A bus that is no crafting part. */
        public Shape(final BusAbilities abilities, final Window window, final int introLines, final int itemRows,
                     final int tagLines, final boolean editingTag, final int fuzzyLines, final int conditions,
                     final boolean editingCondition, final int noteLines) {
            this(abilities, window, introLines, itemRows, tagLines, editingTag, fuzzyLines, conditions,
                    editingCondition, noteLines, List.of());
        }

        /** The rows at their most: every item, every condition, both editors open, every paragraph long. */
        public static Shape most(final BusAbilities abilities, final Window window) {
            final List<Integer> crafting = new ArrayList<>();
            if (window.crafting()) {
                for (int i = 0; i < MOST_CRAFT_LINES; i++) {
                    crafting.add(craftLine(true));
                }
                for (int i = 0; i < MOST_CRAFT_BOXES; i++) {
                    crafting.add(craftBox(CRAFT_BOX_LINES));
                }
            }
            return new Shape(abilities, window, INTRO_LINES, BusAbilities.FILTER_SLOTS, 2, true, 3,
                    MOST_CONDITIONS - 1, true, NOTE_LINES, crafting);
        }
    }

    /** How tall a crafting line of a word and a value is: one row, or two when a note goes under the value. */
    public static int craftLine(final boolean withNote) {
        return withNote ? 2 * LINE + 4 : ROW;
    }

    /** How tall a crafting warning or note box of that many lines is. */
    public static int craftBox(final int lines) {
        return Math.max(1, lines) * LINE + 6;
    }

    /** What a bus of {@code era} in a window of that kind can be set to. */
    public static BusAbilities abilities(final HardwareEra era, final Window window) {
        return window == Window.EXTERNAL ? BusAbilities.external(era) : BusAbilities.of(era);
    }

    /** The Configure rows of a bus of that shape, in order, from the top of the scrolled area. */
    public static List<Row> rows(final Shape shape) {
        final BusAbilities can = shape.abilities();
        final List<Row> rows = new ArrayList<>();
        final int[] y = {0};
        if (shape.window().crafting()) {
            add(rows, y, Kind.FILTER, CELL + 2, 0);
            for (int i = 0; i < shape.crafting().size(); i++) {
                add(rows, y, Kind.CRAFT_LINE, shape.crafting().get(i), i);
            }
            return rows;
        }
        if (shape.window() == Window.EXTERNAL) {
            externalRows(shape, rows, y);
            return rows;
        }
        if (!can.can(BusFeature.FILTER)) {
            add(rows, y, Kind.INTRO, paragraph(shape.introLines()), 0);
            add(rows, y, Kind.NOW, CELL + 2, 0);
        } else if (can.can(BusFeature.ITEM_QUANTITIES)) {
            add(rows, y, Kind.ITEMS_LABEL, LINE + 1, 0);
            for (int i = 0; i < shape.itemRows(); i++) {
                add(rows, y, Kind.ITEM, ITEM_H + 1, i);
            }
            if (shape.itemRows() < BusAbilities.FILTER_SLOTS) {
                add(rows, y, Kind.ADD_ITEM, ROW, 0);
            }
        } else {
            add(rows, y, Kind.FILTER, CELL + 2, 0);
        }
        if (can.can(BusFeature.TAGS)) {
            add(rows, y, Kind.TAGS, ROW * Math.max(1, shape.tagLines()), 0);
            if (shape.editingTag()) {
                add(rows, y, Kind.TAG_EDITOR, ROW, 0);
            }
        }
        if (can.can(BusFeature.FUZZY)) {
            add(rows, y, Kind.MATCH, ROW, 0);
            add(rows, y, Kind.FUZZY_NOTE, paragraph(shape.fuzzyLines()), 0);
        }
        if (can.can(BusFeature.FILTER)) {
            add(rows, y, Kind.FILTER_MODE, ROW, 0);
        }
        if (can.can(BusFeature.QUANTITIES)) {
            add(rows, y, Kind.KEEP, ROW, 0);
            add(rows, y, Kind.MAX, ROW, 0);
        }
        add(rows, y, Kind.MODE, ROW, 0);
        add(rows, y, Kind.POWER, ROW, 0);
        if (can.can(BusFeature.PRIORITY)) {
            add(rows, y, Kind.PRIORITY, ROW, 0);
        }
        if (can.can(BusFeature.CONDITIONS)) {
            add(rows, y, Kind.CONDITIONS_LABEL, LINE + 1, 0);
            for (int i = 0; i < shape.conditions(); i++) {
                add(rows, y, Kind.CONDITION, ROW, i);
            }
            if (shape.editingCondition()) {
                add(rows, y, Kind.CONDITION_EDITOR, 3 * ROW, 0);
            } else if (shape.conditions() < MOST_CONDITIONS) {
                add(rows, y, Kind.ADD_CONDITION, ROW, 0);
            }
        }
        add(rows, y, Kind.SPEED, CONTROL_H, 0);
        return rows;
    }

    /*
     * An External Storage Bus's rows: what the inventory holds; then the Vintage bus's paragraph, or what the network
     * sees of it, which way it may use it and in what turn it fills it, and the note that it is slower.
     */
    private static void externalRows(final Shape shape, final List<Row> rows, final int[] y) {
        final BusAbilities can = shape.abilities();
        add(rows, y, Kind.HOLDS, ROW, 0);
        if (!can.can(BusFeature.FILTER)) {
            add(rows, y, Kind.INTRO, paragraph(shape.introLines()), 0);
            return;
        }
        add(rows, y, Kind.FILTER, CELL + 2, 0);
        if (can.can(BusFeature.TAGS)) {
            add(rows, y, Kind.TAGS, ROW * Math.max(1, shape.tagLines()), 0);
            if (shape.editingTag()) {
                add(rows, y, Kind.TAG_EDITOR, ROW, 0);
            }
        }
        if (can.can(BusFeature.FUZZY)) {
            add(rows, y, Kind.MATCH, ROW, 0);
            add(rows, y, Kind.FUZZY_NOTE, paragraph(shape.fuzzyLines()), 0);
        }
        add(rows, y, Kind.FILTER_MODE, ROW, 0);
        if (can.can(BusFeature.ACCESS)) {
            add(rows, y, Kind.ACCESS, ACCESS_LINES * ROW, 0);
        }
        if (can.can(BusFeature.PRIORITY)) {
            add(rows, y, Kind.PRIORITY, ROW, 0);
        }
        add(rows, y, Kind.NOTE, paragraph(shape.noteLines()), 0);
    }

    /** How tall the rows of that shape are together. */
    public static int contentHeight(final Shape shape) {
        final List<Row> rows = rows(shape);
        final Row last = rows.get(rows.size() - 1);
        return last.y() + last.height();
    }

    /**
     * How tall the Configure rows' area is for a bus of these abilities: as tall as its rows at their most, up to
     * {@link #CONFIGURE_VIEW_MOST}. It is fixed for the bus, so the inventory under it never moves while the window is
     * open; rows past it scroll.
     */
    public static int configureView(final BusAbilities abilities, final Window window) {
        return Math.min(CONFIGURE_VIEW_MOST, contentHeight(Shape.most(abilities, window)));
    }

    /** The window's height on Configure. */
    public static int configureHeight(final BusAbilities abilities, final Window window) {
        return CONFIGURE_VIEW_Y + configureView(abilities, window) + INVENTORY_BLOCK;
    }

    /** Where the inventory's word goes on Configure. */
    public static int inventoryLabelY(final BusAbilities abilities, final Window window) {
        return CONFIGURE_VIEW_Y + configureView(abilities, window) + 3;
    }

    /** Where the inventory's grid starts on Configure; the menu places the player's slots from it. */
    public static int inventoryY(final BusAbilities abilities, final Window window) {
        return CONFIGURE_VIEW_Y + configureView(abilities, window) + 13;
    }

    /** The Software tab's rows' area, as tall as its sections up to what the window holds. */
    public static int softwareView(final int contentHeight) {
        return Math.max(ROW, Math.min(SOFTWARE_VIEW_MOST, contentHeight));
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

    /**
     * Where chips of these widths go when they start at {@link #CONTROL_X} and wrap at {@link #RIGHT}: an x and a y for
     * each, the y in rows from the first. A chip wider than the room starts its own row.
     */
    public static List<int[]> chips(final List<Integer> widths) {
        final List<int[]> at = new ArrayList<>();
        int x = CONTROL_X;
        int line = 0;
        for (final int w : widths) {
            if (x > CONTROL_X && x + w > RIGHT) {
                x = CONTROL_X;
                line++;
            }
            at.add(new int[] {x, line});
            x += w + OPTION_GAP + 1;
        }
        return at;
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

    /**
     * The window's frame on Configure, for the checks: header, title, lamp, tabs, name, the rows' area and its
     * scrollbar, and the inventory.
     */
    public static GuiLayout frame(final BusAbilities abilities, final Window window) {
        final int view = configureView(abilities, window);
        final GuiLayout l = new GuiLayout(WIDTH, configureHeight(abilities, window))
                .box("header", HEADER_X, HEADER_Y, HEADER_W, 17)
                .box("nameField", NAME_X, NAME_Y, NAME_W, NAME_H)
                .box("rows", LABEL_X, CONFIGURE_VIEW_Y, ROW_W, view)
                .box("scrollbar", SCROLL_X, CONFIGURE_VIEW_Y, SCROLL_W, view)
                .playerInventory(INV_X, inventoryY(abilities, window));
        for (int tab = 0; tab < TABS; tab++) {
            l.box("tab" + tab, TAB_X[tab], TAB_Y, TAB_W[tab], TAB_H);
        }
        l.text("title", TITLE_X, TITLE_Y, TITLE_MOST_CHARS, 1.0f);
        l.text("nameLabel", LABEL_X, NAME_Y + 2, 4, 0.75f);
        l.text("invLabel", INV_X, inventoryLabelY(abilities, window), 9, 0.75f);
        return l;
    }

    /**
     * The Configure rows of that shape laid out flat, unscrolled, each control a box and each word a line of text at
     * its longest English length, for the checks.
     */
    public static GuiLayout content(final Shape shape) {
        final GuiLayout l = new GuiLayout(WIDTH, contentHeight(shape));
        for (final Row row : rows(shape)) {
            final String n = row.kind().name().toLowerCase() + row.index();
            final int y = row.y();
            switch (row.kind()) {
                case INTRO, FUZZY_NOTE, NOTE -> {
                    for (int line = 0; line < (row.height() - 2) / LINE; line++) {
                        l.text(n + "_" + line, LABEL_X, y + 1 + line * LINE, 34, 0.75f);
                    }
                }
                case HOLDS -> {
                    l.text(n + "_label", LABEL_X, y + 2, 5, 0.75f);
                    l.text(n + "_value", CONTROL_X, y + 2, 17, 0.75f);
                }
                case ACCESS -> {
                    // "READ AND WRITE" and "READ ONLY" on the first line, "WRITE ONLY" under them.
                    l.text(n + "_label", LABEL_X, y + 2, 6, 0.75f);
                    final int[] widths = {Math.round(13 * GuiLayout.GLYPH_WIDTH * 0.75f) + OPTION_PAD,
                            Math.round(9 * GuiLayout.GLYPH_WIDTH * 0.75f) + OPTION_PAD,
                            Math.round(10 * GuiLayout.GLYPH_WIDTH * 0.75f) + OPTION_PAD};
                    final List<int[]> at = chips(List.of(widths[0], widths[1], widths[2]));
                    for (int i = 0; i < at.size(); i++) {
                        l.box(n + "_option" + i, at.get(i)[0], y + at.get(i)[1] * ROW, widths[i], CONTROL_H);
                    }
                }
                case NOW -> {
                    l.text(n + "_label", LABEL_X, y + 7, 5, 0.75f);
                    l.box(n + "_cell", CONTROL_X, y + 1, CELL, CELL);
                    l.text(n + "_name", CONTROL_X + CELL + 3, y + 3, 20, 0.75f);
                    l.text(n + "_moved", CONTROL_X + CELL + 3, y + 11, 12, 0.75f);
                }
                case FILTER -> {
                    l.text(n + "_label", LABEL_X, y + 7, 6, 0.75f);
                    for (int i = 0; i < BusAbilities.FILTER_SLOTS; i++) {
                        l.box(n + "_cell" + i, CONTROL_X + i * CELL, y + 1, CELL, CELL);
                    }
                }
                case ITEMS_LABEL, CONDITIONS_LABEL -> l.text(n, LABEL_X, y, 34, 0.75f);
                case ITEM -> {
                    l.box(n + "_cell", ITEM_CELL_X, y + 1, CELL, CELL);
                    l.text(n + "_name", ITEM_NAME_X, y + 2, 28, 0.75f);
                    l.text(n + "_keep", ITEM_KEEP_X, y + 12, 4, 0.75f);
                    l.box(n + "_keepMinus", ITEM_KEEP_MINUS, y + 11, ITEM_STEP, ITEM_STEP);
                    l.box(n + "_keepValue", ITEM_KEEP_VALUE, y + 11, ITEM_VALUE_W, ITEM_STEP);
                    l.box(n + "_keepPlus", ITEM_KEEP_PLUS, y + 11, ITEM_STEP, ITEM_STEP);
                    l.text(n + "_max", ITEM_MAX_X, y + 12, 3, 0.75f);
                    l.box(n + "_maxMinus", ITEM_MAX_MINUS, y + 11, ITEM_STEP, ITEM_STEP);
                    l.box(n + "_maxValue", ITEM_MAX_VALUE, y + 11, ITEM_VALUE_W, ITEM_STEP);
                    l.box(n + "_maxPlus", ITEM_MAX_PLUS, y + 11, ITEM_STEP, ITEM_STEP);
                }
                case ADD_ITEM, ADD_CONDITION -> l.box(n, LABEL_X, y, ROW_W, CONTROL_H + 1);
                case TAGS -> {
                    l.text(n + "_label", LABEL_X, y + 2, 4, 0.75f);
                    final List<int[]> at = chips(List.of(70, 70, 50, 50, 30));
                    for (int i = 0; i < at.size() && at.get(i)[1] * ROW < row.height(); i++) {
                        l.box(n + "_chip" + i, at.get(i)[0], y + at.get(i)[1] * ROW,
                                i < 2 ? 70 : i < 4 ? 50 : 30, CONTROL_H);
                    }
                }
                case TAG_EDITOR -> {
                    l.box(n + "_field", CONTROL_X, y, TAG_FIELD_W, CONTROL_H);
                    l.box(n + "_add", TAG_ADD_X, y, TAG_ADD_W, CONTROL_H);
                }
                case MATCH -> toggle(l, n, y, 5, 5, 5);
                // "ONLY THESE" and "ALL BUT THESE", counted as the letters they are as wide as: a space is narrower.
                case FILTER_MODE -> toggle(l, n, y, 0, 10, 12);
                case KEEP, MAX, PRIORITY -> {
                    l.text(n + "_label", LABEL_X, y + 2, 8, 0.75f);
                    l.box(n + "_minus", MINUS_X, y, STEP_W, CONTROL_H);
                    l.box(n + "_value", VALUE_X, y, VALUE_W, CONTROL_H);
                    l.box(n + "_plus", PLUS_X, y, STEP_W, CONTROL_H);
                    l.text(n + "_note", NOTE_X, y + 2, 13, 0.75f);
                }
                case MODE -> toggle(l, n, y, 4, 10, 9);
                case POWER -> toggle(l, n, y, 5, 2, 3);
                case CONDITION -> {
                    l.box(n + "_bar", LABEL_X, y, REMOVE_X - 1 - LABEL_X, CONTROL_H + 1);
                    l.box(n + "_remove", REMOVE_X, y + 1, REMOVE_W, REMOVE_W);
                }
                case CONDITION_EDITOR -> {
                    final int[] kinds = options(LABEL_X, 29, 29, 29);
                    for (int i = 0; i < kinds.length; i++) {
                        l.box(n + "_kind" + i, kinds[i], y, 29, CONTROL_H);
                    }
                    l.box(n + "_add", EDIT_ADD_X, y + 2 * ROW, EDIT_ADD_W, CONTROL_H);
                    l.box(n + "_cancel", EDIT_CANCEL_X, y + 2 * ROW, EDIT_CANCEL_W, CONTROL_H);
                    l.box(n + "_field", LABEL_X, y + ROW, EDIT_FIELD_W, CONTROL_H);
                    l.box(n + "_minus", EDIT_MINUS_X, y + ROW, STEP_W, CONTROL_H);
                    l.box(n + "_value", EDIT_VALUE_X, y + ROW, VALUE_W, CONTROL_H);
                    l.box(n + "_plus", EDIT_PLUS_X, y + ROW, STEP_W, CONTROL_H);
                }
                case SPEED -> {
                    l.text(n + "_label", LABEL_X, y + 2, 5, 0.75f);
                    l.text(n + "_value", CONTROL_X, y + 2, 7, 0.75f);
                    l.text(n + "_note", CONTROL_X + 32, y + 2, 21, 0.75f);
                }
                case CRAFT_LINE -> {
                    // A word and its value, with a note under the value, or a box wrapped to the row's width.
                    if (row.height() == craftLine(true) || row.height() == craftLine(false)) {
                        l.text(n + "_label", LABEL_X, y + 2, 8, 0.75f);
                        l.text(n + "_value", CONTROL_X, y + 2, 20, 0.75f);
                        if (row.height() == craftLine(true)) {
                            l.text(n + "_note", CONTROL_X, y + 2 + LINE, 20, 0.75f);
                        }
                    } else {
                        l.box(n + "_box", LABEL_X, y, ROW_W, row.height() - 2);
                    }
                }
            }
        }
        return l;
    }

    /* A row's word and its options, each as wide as its longest English word with the room beside it. */
    private static void toggle(final GuiLayout l, final String n, final int y, final int labelChars,
                               final int... optionChars) {
        if (labelChars > 0) {
            l.text(n + "_label", LABEL_X, y + 2, labelChars, 0.75f);
        }
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
