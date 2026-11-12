/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

import dev.jstech.core.gui.layout.GuiLayout;

/**
 * Where Settings > Programs puts its parts (computers/docs/design/programs.md): the heading, with how many updates the
 * Mirror has at its right, then a row per installed program, its name at the left and Uninstall at the right, with
 * Update before it and a line under the name for a program the Mirror has newer.
 *
 * <p>Every place is measured from the top left of the page's content, which scrolls when the programs are more than
 * the window holds, and the width of a word is the font's, given by the caller.
 */
public final class SettingsProgramsLayout {

    /* The heading's line and the room under it. */
    public static final int HEADING_H = 13;
    /* A program's row, and one with the update's line under its name. */
    public static final int ROW_H = 12;
    public static final int UPDATE_ROW_H = 21;
    /* A row's buttons: how tall, the room round a word, the gap between them and before them, and how far up. */
    public static final int BUTTON_H = 11;
    public static final int BUTTON_PAD = 8;
    public static final int GAP = 3;
    public static final int BUTTON_Y = -1;
    /* The name's line; the update's line under it, how far in, and the smaller letters it and the count take. */
    public static final int NAME_Y = 1;
    public static final int LINE_H = 8;
    public static final int BADGE_Y = 11;
    public static final int BADGE_X = 6;
    public static final float SMALL = 0.75f;
    /* How far round its row the band behind an update reaches. */
    public static final int BAND_PAD = 2;

    private SettingsProgramsLayout() {
    }

    /** How tall a program's row is: one line, or two with the update's under the name. */
    public static int rowHeight(final boolean update) {
        return update ? UPDATE_ROW_H : ROW_H;
    }

    /** How tall the page's content is: the heading, then {@code programs} rows, {@code updates} of them updated. */
    public static int contentHeight(final int programs, final int updates) {
        return HEADING_H + (programs - updates) * ROW_H + updates * UPDATE_ROW_H;
    }

    /** How wide a button is for its word. */
    public static int buttonWidth(final int wordW) {
        return wordW + BUTTON_PAD;
    }

    /** Where Update stands, before Uninstall at the right of a page {@code w} wide. */
    public static int updateX(final int w, final int uninstallW, final int updateW) {
        return w - uninstallW - GAP - updateW;
    }

    /** How much room a program's name has, clear of its buttons. */
    public static int nameRoom(final int w, final int uninstallW, final int updateW, final boolean update) {
        return (update ? updateX(w, uninstallW, updateW) : w - uninstallW) - GAP;
    }

    /** How tall a line of the smaller letters is. */
    public static int smallLineHeight() {
        return Math.round(LINE_H * SMALL);
    }

    /**
     * Where the count starts: against the page's right edge, or clear of the heading on a page too narrow for both,
     * where its end is cut rather than written over the heading.
     */
    public static int countX(final int w, final int headingW, final int countW) {
        return Math.max(w - countW, headingW + GAP);
    }

    /**
     * The page's content {@code w} wide: the heading {@code headingW} wide with the count {@code countW} at its right,
     * then a row per program, those {@code updated} with Update and their update's line {@code badgeW} wide, and
     * buttons whose words are that wide.
     */
    public static GuiLayout layout(final int w, final int headingW, final int countW, final boolean[] updated,
                                   final int uninstallWordW, final int updateWordW, final int badgeW) {
        int updates = 0;
        for (final boolean update : updated) {
            updates += update ? 1 : 0;
        }
        final GuiLayout l = new GuiLayout(w, contentHeight(updated.length, updates));
        l.box("heading", 0, 0, headingW, LINE_H);
        final int countX = countX(w, headingW, countW);
        l.box("count", countX, NAME_Y, w - countX, smallLineHeight());
        final int uninstallW = buttonWidth(uninstallWordW);
        final int updateW = buttonWidth(updateWordW);
        int y = HEADING_H;
        for (int i = 0; i < updated.length; i++) {
            l.box("name" + i, 0, y + NAME_Y, nameRoom(w, uninstallW, updateW, updated[i]), LINE_H);
            l.box("uninstall" + i, w - uninstallW, y + BUTTON_Y, uninstallW, BUTTON_H);
            if (updated[i]) {
                l.box("update" + i, updateX(w, uninstallW, updateW), y + BUTTON_Y, updateW, BUTTON_H);
                // Cut at the page's edge, as its label is, on a page too narrow for all of it.
                l.box("badge" + i, BADGE_X, y + BADGE_Y, Math.min(badgeW, w - BADGE_X), smallLineHeight());
            }
            y += rowHeight(updated[i]);
        }
        return l;
    }
}
