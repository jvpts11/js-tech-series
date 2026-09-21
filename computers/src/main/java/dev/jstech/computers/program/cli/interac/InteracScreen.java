/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli.interac;

import dev.jstech.computers.program.cli.CliText;
import java.util.ArrayList;
import java.util.List;

/**
 * The full-screen view of the network, drawn in text.
 *
 * <p>What the graphical Network Interactor is, on a machine that has no desktop and over a session opened on
 * another one: the same tabs, the same list, the same panel beside it saying where a thing is and what makes
 * it, and the row of keys along the foot that the file managers of that age all had.
 *
 * <p>Pure. It is handed what the machine holds and gives back rows of text of the width it was told, so what
 * the screen looks like is held to account without a world, and the machine draws the same screen for a
 * monitor, a desktop window and a remote session alike.
 */
public final class InteracScreen {

    /** One row of the list: what it is, how much of it, and where it lives. */
    public record Row(String name, String count, String detail) {
    }

    /**
     * What the machine hands over to be drawn.
     *
     * @param glance  the network at a glance, for the bar along the top
     * @param rows    the rows of the tab that is up
     * @param aside   the panel beside the list: what the picked row is, where it is, what makes it
     * @param message what came of the last thing asked, or empty
     * @param asking  the question standing at the foot, or empty
     */
    public record Data(String glance, List<Row> rows, List<String> aside, String message, String asking) {

        public Data {
            rows = List.copyOf(rows);
            aside = List.copyOf(aside);
        }
    }

    /** How few rows of the list are shown however short the glass is; below this it is not a list. */
    public static final int LEAST_BODY = 3;

    /** How wide the panel beside the list is. */
    public static final int ASIDE_W = 29;

    /** The keys along the foot, in the order those managers had them. */
    public static final String[] KEYS = {"Help", "Get", "Put", "Craft", "Lock", "Free", "Fav", "Stop",
            "Find", "Quit"};

    /** What the bar along the top calls the program. */
    private static final String TITLE = " interac  Network Interactor";

    /** How wide the number column of the list is. */
    private static final int COUNT_W = 10;

    /** How wide the column saying where a thing is, when the list is wide enough to keep it. */
    private static final int WHERE_W = 9;

    /** The width of list from which that third column is worth its room. */
    private static final int WHERE_AT = 34;

    /** The width of glass from which the panel beside the list is worth its room. */
    private static final int PANEL_AT = 56;

    /** Which row the tabs are on, counted from the top of the screen. */
    public static final int TAB_ROW = 1;

    /** Which row says what is being looked for and how many rows there are. */
    public static final int SEARCH_ROW = 2;

    /** How many rows stand above the list: the bar, the tabs, the search, the rule and the headings. */
    public static final int HEAD_ROWS = 5;

    /** How many stand under it: a rule, what was said or asked, and the keys. */
    public static final int FOOT_ROWS = 3;

    private InteracScreen() {
    }

    /**
     * The tab whose name that column falls on, or {@code -1} for a column between two of them.
     *
     * <p>Where each tab is written is this class's own business, so where a click on one lands is answered
     * here rather than worked out a second time by whatever is handling the mouse.
     */
    public static int tabAt(final int column) {
        int at = 1;
        for (int i = 0; i < InteracState.TABS.length; i++) {
            final int wide = InteracState.TABS[i].length() + 2;
            if (column >= at && column < at + wide) {
                return i;
            }
            at += wide;
        }
        return -1;
    }

    /** The key along the foot that column falls on, or {@code -1} for a column between two of them. */
    public static int keyAt(final int column) {
        int at = 0;
        for (int i = 0; i < KEYS.length; i++) {
            final int wide = String.valueOf(i + 1).length() + KEYS[i].length() + 1;
            if (column >= at && column < at + wide - 1) {
                return i;
            }
            at += wide;
        }
        return -1;
    }

    /**
     * How many rows of the list a glass that state describes shows at once.
     *
     * <p>Whatever the glass has left once the bar, the tabs, the search, the headings, the rules, the line
     * that talks and the keys have theirs. Those never go: a list with no keys under it is a list nobody can
     * work, and a list with no headings is a column of numbers.
     */
    public static int bodyRows(final InteracState state) {
        return Math.max(LEAST_BODY, state.rows() - HEAD_ROWS - FOOT_ROWS);
    }

    /** How many rows the whole screen has, which is the glass's own unless the glass is very short. */
    public static int screenRows(final InteracState state) {
        return HEAD_ROWS + bodyRows(state) + FOOT_ROWS;
    }

    /** The whole screen, row by row, as wide and as tall as the state says the glass is. */
    public static List<String> render(final InteracState state, final Data data) {
        final int wide = Math.max(40, state.columns());
        /*
         * The panel gives way on a narrow glass rather than pushing the list off it: the list is what the
         * player is reading, and a panel with nothing beside it is no panel at all.
         */
        final int asideW = showsPanel(wide) ? Math.max(12, Math.min(ASIDE_W, wide - 34)) : 0;
        final int listW = wide - asideW;
        final int body = bodyRows(state);
        final List<String> out = new ArrayList<>(screenRows(state));
        out.add(bar(data, wide));
        out.add(tabs(state, wide));
        out.add(searchRow(state, data, wide));
        out.add(rule(listW, wide));
        out.add(beside(columnsOf(listW, "Item", "Count", "Where"), heading(data), listW, asideW));
        for (int i = 0; i < body; i++) {
            final int row = firstShown(state) + i;
            out.add(beside(listRow(data, row, state.selected(), listW), aside(data, i + 1), listW,
                    asideW));
        }
        out.add(rule(listW, wide));
        out.add(CliText.pad(" " + (data.asking().isEmpty() ? data.message() : data.asking()), wide));
        out.add(keys(wide));
        return out;
    }

    /**
     * The bar along the top: what the program is, and the network at a glance at its end.
     *
     * <p>On a glass too narrow for both, the name goes and the glance stays: a player who has just opened
     * the program knows what they opened, and what they cannot know is how the network is.
     */
    private static String bar(final Data data, final int wide) {
        final int room = wide - TITLE.length() - 2;
        if (room < data.glance().length()) {
            return CliText.pad(" " + data.glance(), wide);
        }
        return CliText.pad(TITLE + " " + CliText.padLeft(CliText.pad(data.glance(), room).trim(), room),
                wide);
    }

    /**
     * The row that says what is being looked for, and how many rows there are once it has been.
     *
     * <p>The count is there for the player, who wants to know whether the list goes on past the glass, and it
     * is also what tells a terminal where the list ends: the machine holds the rows and the terminal holds
     * none of them, so this row is how the one tells the other.
     */
    private static String searchRow(final InteracState state, final Data data, final int wide) {
        final int rows = data.rows().size();
        final String count = rows + (rows == 1 ? " row " : " rows ");
        return CliText.pad(" Search: " + state.search() + "_", wide - count.length()) + count;
    }

    /**
     * How many rows the list has, read back off a screen that was drawn.
     *
     * <p>A terminal showing one of these knows only what is on the glass, so where the list ends is read from
     * the glass rather than remembered: it is written there for the player to read anyway.
     */
    public static int rowsSaid(final List<String> screen) {
        if (screen.size() <= SEARCH_ROW) {
            return 0;
        }
        final String row = screen.get(SEARCH_ROW).trim();
        final int space = row.lastIndexOf(' ');
        if (space < 0) {
            return 0;
        }
        int digits = space;
        while (digits > 0 && Character.isDigit(row.charAt(digits - 1))) {
            digits--;
        }
        try {
            return digits == space ? 0 : Integer.parseInt(row.substring(digits, space));
        } catch (final NumberFormatException notANumber) {
            return 0;
        }
    }

    /** The row of tabs, the one that is up marked the way a text screen marks a thing. */
    private static String tabs(final InteracState state, final int wide) {
        final StringBuilder out = new StringBuilder(" ");
        for (int i = 0; i < InteracState.TABS.length; i++) {
            out.append(i == state.tab() ? "[" : " ").append(InteracState.TABS[i])
                    .append(i == state.tab() ? "]" : " ");
        }
        return CliText.pad(out.toString(), wide);
    }

    /** Where the first row shown sits, so the picked row is always on the glass. */
    public static int firstShown(final InteracState state) {
        return Math.max(0, state.selected() - bodyRows(state) + 1);
    }

    private static String listRow(final Data data, final int row, final int picked, final int listW) {
        if (row < 0 || row >= data.rows().size()) {
            return "";
        }
        final Row one = data.rows().get(row);
        final String line = columnsOf(listW, one.name(), one.count(), one.detail());
        return row == picked ? ">" + line.substring(1) : line;
    }

    /**
     * One row of the list laid out in its columns.
     *
     * <p>The name takes what the other two leave, and on a list too narrow for three columns the third goes
     * rather than the first: a row whose name is cut to four letters says nothing at all, and where a thing
     * is stands in the panel beside the list anyway.
     */
    private static String columnsOf(final int listW, final String name, final String count,
                                    final String where) {
        final boolean roomForWhere = listW >= WHERE_AT;
        /* One column of the list belongs to the line between it and the panel, so the row stops short of it. */
        final int nameW = Math.max(4, listW - 2 - COUNT_W - (roomForWhere ? WHERE_W + 2 : 0));
        return " " + CliText.pad(name, nameW) + CliText.padLeft(count, COUNT_W)
                + (roomForWhere ? "  " + CliText.pad(where, WHERE_W) : "");
    }

    private static String aside(final Data data, final int i) {
        return i < data.aside().size() ? data.aside().get(i) : "";
    }

    private static String heading(final Data data) {
        return data.aside().isEmpty() ? "" : data.aside().get(0);
    }

    private static String rule(final int listW, final int wide) {
        if (listW >= wide) {
            return "-".repeat(wide);
        }
        return "-".repeat(listW - 1) + "+" + "-".repeat(Math.max(0, wide - listW));
    }

    /** One row of the list with its line and the panel beside it, or the row alone when there is no panel. */
    private static String beside(final String row, final String panel, final int listW, final int asideW) {
        if (asideW <= 0) {
            return CliText.pad(row, listW);
        }
        return CliText.pad(row, listW - 1) + "|" + CliText.pad(" " + panel, asideW);
    }

    /**
     * Whether a glass that wide keeps the panel beside the list.
     *
     * <p>Below this the panel is more harm than help: a dozen columns cut every line of it short, and the
     * room it takes is room the list needed. The list then has the glass to itself and keeps the column
     * saying where a thing is, which is the half of the panel a player misses most.
     */
    public static boolean showsPanel(final int columns) {
        return columns >= PANEL_AT;
    }

    /**
     * The keys along the foot: the number and what it does, as those managers wrote them.
     *
     * <p>A glass too narrow for all ten loses whole keys off the end rather than half of one: half a word
     * with a mark after it is not a key anybody can read.
     */
    private static String keys(final int wide) {
        final StringBuilder out = new StringBuilder();
        for (int i = 0; i < KEYS.length; i++) {
            final String one = (i + 1) + KEYS[i] + " ";
            if (out.length() + one.length() > wide) {
                break;
            }
            out.append(one);
        }
        return CliText.pad(out.toString(), wide);
    }
}
