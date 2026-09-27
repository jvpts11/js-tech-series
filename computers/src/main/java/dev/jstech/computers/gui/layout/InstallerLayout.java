/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

import dev.jstech.computers.gui.MonitorGlass;
import dev.jstech.core.gui.layout.GuiLayout;

/**
 * Pure layout of the OS installer screen. What is fixed here is the glass, a list row's own pitch and its
 * fixed-width columns (shared by every page that draws a table), and the one dialog the screen ever draws
 * itself: the erase confirmation. Every page's own content (welcome, disk list, name field, the several shapes
 * a copy in progress takes) is drawn inside the frame {@code InstallerFrames} hands the screen, whose own
 * position is that installer's chrome and not this screen's geometry, so it is not enumerated here.
 */
public final class InstallerLayout {

    public static final int WIDTH = MonitorGlass.WIDTH;
    public static final int HEIGHT = MonitorGlass.HEIGHT;

    /** How far apart the rows of a list sit, which the mouse also has to know to find the one under it. */
    public static final int ROW = 11;
    /** Where the Size column ends, counted back from the right edge of the table. */
    public static final int SIZE_COLUMN = 120;
    /** The same for the Free column, which sits between Size and Holds. */
    public static final int FREE_COLUMN = 60;
    /** The clear space kept between a drive's name and whatever is written to the right of it. */
    public static final int COLUMN_GAP = 6;

    /** The name field the Settings page draws for the machine's own name: its box, and the room typed text gets. */
    public static final int NAME_FIELD_W = 150;
    public static final int NAME_FIELD_H = 13;
    /** How far the name field sits under the row of its caption, and how far its text is inset from its edges. */
    public static final int NAME_FIELD_DY = 2;
    public static final int NAME_FIELD_TEXT_INSET = 3;

    /**
     * The dotted leaders that tie a question to its answer: how far in from the right the answer stops, and the gap
     * kept between the dots and the words on either side.
     */
    public static final int LEADER_RIGHT_MARGIN = 20;
    public static final int LEADER_GAP = 3;

    /** The table header a card-styled disk list draws: the rule under it, the row it takes, and its inset. */
    public static final int TABLE_HEADER_RULE_DY = 9;
    public static final int TABLE_HEADER_GAP = 13;
    public static final int TABLE_INSET = 4;

    /** How tall the work page's own progress bar stands, in the looks that draw one instead of a card's track. */
    public static final int WORK_BAR_H = 9;
    /** The space under that bar before the line that says how far along it is. */
    public static final int WORK_BAR_GAP = 6;

    /** The card-styled work page's own track, and the two lines of words under it. */
    public static final int CARD_BAR_H = 6;
    /** The space between the last step and that track. */
    public static final int CARD_BAR_GAP = 8;
    public static final int CARD_LINE1_DY = 12;
    public static final int CARD_LINE2_DY = 22;

    /** How much taller a bare terminal's row stands over the wall's own row, since a page of it reads sparser. */
    public static final int TEXT_ROW_EXTRA = 2;

    /** The erase confirmation, the one dialog this screen draws over its own page rather than inside a frame. */
    public static final int ERASE_DIALOG_W = 250;
    public static final int ERASE_DIALOG_H = 74;
    public static final int ERASE_TEXT_X = 10;
    public static final int ERASE_LINE1_DY = 10;
    public static final int ERASE_LINE2_DY = 26;
    public static final int ERASE_LINE3_DY = 36;
    public static final int ERASE_LINE4_DY = 46;
    public static final int ERASE_LINE5_DY = 60;

    private InstallerLayout() {
    }

    public static int eraseDialogX() {
        return (WIDTH - ERASE_DIALOG_W) / 2;
    }

    public static int eraseDialogY() {
        return (HEIGHT - ERASE_DIALOG_H) / 2;
    }

    /** The glass, and the erase dialog centred on it with its five lines of words. */
    public static GuiLayout layout() {
        final GuiLayout l = new GuiLayout(WIDTH, HEIGHT);
        final int dx = eraseDialogX();
        final int dy = eraseDialogY();
        l.box("eraseDialog", dx, dy, ERASE_DIALOG_W, ERASE_DIALOG_H);
        l.text("eraseLine1", dx + ERASE_TEXT_X, dy + ERASE_LINE1_DY, 40, 1.0f);
        l.text("eraseLine2", dx + ERASE_TEXT_X, dy + ERASE_LINE2_DY, 40, 1.0f);
        l.text("eraseLine3", dx + ERASE_TEXT_X, dy + ERASE_LINE3_DY, 40, 1.0f);
        l.text("eraseLine4", dx + ERASE_TEXT_X, dy + ERASE_LINE4_DY, 40, 1.0f);
        l.text("eraseLine5", dx + ERASE_TEXT_X, dy + ERASE_LINE5_DY, 40, 1.0f);
        return l;
    }
}
