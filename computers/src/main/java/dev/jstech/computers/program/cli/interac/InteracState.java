/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli.interac;

import java.util.Locale;

/**
 * Where the full-screen view stands: which tab is up, which row is picked, what is being searched for, and
 * what the player has just asked it to do.
 *
 * <p>All of it in one small piece of text, because that is what travels: the terminal asks the machine for a
 * screen by naming the state it wants, and the machine draws that state and hands the rows back. A screen is
 * therefore never out of step with the machine, and the terminal keeps no copy of the network at all.
 *
 * <p>Pure, so what a key does to the state is held to account without a world.
 *
 * @param tab      which tab is up, from zero
 * @param selected which row of it is picked, from zero
 * @param search   what is being searched for, which narrows the rows
 * @param action   what was asked for: {@code get}, {@code put}, {@code craft}, {@code fav} or empty
 * @param amount   how many the action is for
 * @param columns  how wide the glass asking for it is, since the machine draws the screen and not the glass
 * @param rows     how many rows that glass holds, for the same reason
 */
public record InteracState(int tab, int selected, String search, String action, long amount, int columns,
                           int rows) {

    /**
     * The tabs, in the order they are read left to right.
     *
     * <p>Each one is something the machine can answer for outright, which is why the graphical program's
     * status page is not among them: what it says stands in the bar along the top of every tab instead, where
     * it is read without leaving the list.
     */
    public static final String[] TABS = {"Network", "Servers", "Locked", "Ops", "Starred"};

    /** Everything the network holds. */
    public static final int TAB_NETWORK = 0;

    /** The servers of it, and how full each one is. */
    public static final int TAB_SERVERS = 1;

    /** What is being held back from the network's own use. */
    public static final int TAB_LOCKED = 2;

    /** The operations in flight. */
    public static final int TAB_OPS = 3;

    /** What this computer has starred. */
    public static final int TAB_STARRED = 4;

    /** What a path of this kind begins with, so the machine knows a screen is being asked for. */
    public static final String SCHEME = "interac:";

    /** How wide a glass is taken to be when the one asking has not said, which is what a terminal held. */
    public static final int DEFAULT_COLUMNS = 80;

    /** How many rows it is taken to hold for the same reason, which is what a terminal held as well. */
    public static final int DEFAULT_ROWS = 24;

    /** The view as it opens: the network, nothing picked, nothing searched for. */
    public static final InteracState OPENING = new InteracState(0, 0, "", "", 0L);

    public InteracState {
        tab = Math.floorMod(tab, TABS.length);
        selected = Math.max(0, selected);
        search = search == null ? "" : search;
        action = action == null ? "" : action.toLowerCase(Locale.ROOT);
        columns = columns <= 0 ? DEFAULT_COLUMNS : columns;
        rows = rows <= 0 ? DEFAULT_ROWS : rows;
    }

    /** The same, for whoever is not saying how big their glass is. */
    public InteracState(final int tab, final int selected, final String search, final String action,
                        final long amount) {
        this(tab, selected, search, action, amount, DEFAULT_COLUMNS, DEFAULT_ROWS);
    }

    /** The name of the tab that is up. */
    public String tabName() {
        return TABS[this.tab];
    }

    /** The same state on another tab, with nothing picked, since the rows are not the same rows. */
    public InteracState onTab(final int which) {
        return new InteracState(which, 0, this.search, "", 0L, this.columns, this.rows);
    }

    /** The same state with another row picked. */
    public InteracState picking(final int row) {
        return new InteracState(this.tab, row, this.search, "", 0L, this.columns, this.rows);
    }

    /** The same state looking for something else, which starts the rows again from the top. */
    public InteracState searchingFor(final String text) {
        return new InteracState(this.tab, 0, text, "", 0L, this.columns, this.rows);
    }

    /** The same state with something asked of it. */
    public InteracState asking(final String what, final long many) {
        return new InteracState(this.tab, this.selected, this.search, what, many, this.columns, this.rows);
    }

    /** The same state with nothing asked, which is what it becomes once the machine has done it. */
    public InteracState done() {
        return new InteracState(this.tab, this.selected, this.search, "", 0L, this.columns, this.rows);
    }

    /** The same state asked for on a glass of that size. */
    public InteracState on(final int howWide, final int howTall) {
        return new InteracState(this.tab, this.selected, this.search, this.action, this.amount, howWide,
                howTall);
    }

    /**
     * The state as a name a terminal can ask a machine for.
     *
     * <p>The search comes last and whole, so a name with spaces in it needs nothing done to it.
     */
    public String path() {
        return SCHEME + this.tab + ":" + this.selected + ":" + this.action + ":" + this.amount
                + ":" + this.columns + ":" + this.rows + ":" + this.search;
    }

    /** The state that name stands for; anything it cannot read opens the view where it opens. */
    public static InteracState of(final String path) {
        if (path == null || !path.startsWith(SCHEME)) {
            return OPENING;
        }
        final String[] parts = path.substring(SCHEME.length()).split(":", 7);
        if (parts.length < 7) {
            return OPENING;
        }
        return new InteracState(whole(parts[0]), whole(parts[1]), parts[6], parts[2], number(parts[3]),
                whole(parts[4]), whole(parts[5]));
    }

    /** Whether a name asks for one of these screens at all. */
    public static boolean names(final String path) {
        return path != null && path.startsWith(SCHEME);
    }

    private static int whole(final String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (final NumberFormatException notANumber) {
            return 0;
        }
    }

    private static long number(final String text) {
        try {
            return Math.max(0L, Long.parseLong(text.trim()));
        } catch (final NumberFormatException notANumber) {
            return 0L;
        }
    }
}
