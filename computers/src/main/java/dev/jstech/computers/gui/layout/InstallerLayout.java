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
    /** How far a checklist's words sit in from its ticks. */
    public static final int CHECK_INDENT = 9;
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

    /**
     * bsdinstall's own dialog box: a plain ground with the grey box inset from the monitor's edge, its shadow
     * cast a few pixels down and right, and its own content inset again inside that.
     */
    public static final int BSD_DIALOG_INSET_X = 14;
    public static final int BSD_DIALOG_INSET_TOP = 20;
    public static final int BSD_DIALOG_INSET_BOTTOM = 38;
    public static final int BSD_DIALOG_SHADOW = 3;
    public static final int BSD_DIALOG_CONTENT_X = 10;
    public static final int BSD_DIALOG_CONTENT_TOP = 8;
    public static final int BSD_DIALOG_CONTENT_HEIGHT_MARGIN = 30;
    /** How far up from the dialog's own foot its button row sits. */
    public static final int BSD_BUTTON_ROW_DY = 14;
    /** How tall one of bsdinstall's own buttons stands, and the gap kept between two side by side. */
    public static final int BSD_BUTTON_H = 14;
    public static final int BSD_BUTTON_GAP = 4;

    /**
     * UNIX System V's own console: the banner in reverse video across the top, the content under it, and the
     * line of keys (or the reverse-video bar the last page swaps it for) along the foot.
     */
    public static final int SYSV_BANNER_TOP = 4;
    public static final int SYSV_BANNER_H = 9;
    public static final int SYSV_CONTENT_GAP = 6;
    public static final int SYSV_FOOT_H = 12;
    public static final int SYSV_CONTENT_INSET = 4;

    private InstallerLayout() {
    }

    public static int eraseDialogX() {
        return (WIDTH - ERASE_DIALOG_W) / 2;
    }

    public static int eraseDialogY() {
        return (HEIGHT - ERASE_DIALOG_H) / 2;
    }

    /**
     * Where bsdinstall's button row starts: its OK button and, when the page offers one, Cancel beside it, the
     * pair centred under the dialog the way the real dialogs draw it.
     *
     * @param dialogX the dialog's left edge
     * @param dialogW how wide the dialog is
     * @param okW     how wide the OK button stands
     * @param cancelW how wide Cancel stands, or 0 on a page that offers none
     */
    public static int bsdButtonRowX(final int dialogX, final int dialogW, final int okW, final int cancelW) {
        final int rowW = okW + (cancelW > 0 ? BSD_BUTTON_GAP + cancelW : 0);
        return dialogX + (dialogW - rowW) / 2;
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

    /**
     * bsdinstall's own dialog, its heading and its two buttons sized to the longest word either language gives
     * them, so a heading or a button that grows past the dialog it sits in is caught here rather than on the
     * glass. The dialog's own background is not itself an element: it is what every one of these sits on, not
     * a thing beside them, so it takes no part in the overlap check.
     *
     * @param longestHeading the longest heading any bsdinstall page titles its dialog with
     * @param longestButton  the longest word any of its buttons carries between its angle brackets
     */
    public static GuiLayout bsdDialogLayout(final String longestHeading, final String longestButton) {
        final int dx = BSD_DIALOG_INSET_X;
        final int dy = BSD_DIALOG_INSET_TOP;
        final int dw = WIDTH - 2 * BSD_DIALOG_INSET_X;
        final int dh = HEIGHT - BSD_DIALOG_INSET_BOTTOM;
        final GuiLayout l = new GuiLayout(WIDTH, HEIGHT);
        l.text("heading", dx, dy - 4, longestHeading.length() + 2, 1.0f);
        final int by = dy + dh - BSD_BUTTON_ROW_DY;
        // The two angle brackets a button's label sits between, at the same glyph estimate the audit uses.
        final int buttonW = Math.round((longestButton.length() + 2) * GuiLayout.GLYPH_WIDTH) + 8;
        final int nextX = bsdButtonRowX(dx, dw, buttonW, buttonW);
        l.box("next", nextX, by, buttonW, BSD_BUTTON_H);
        l.box("cancel", nextX + buttonW + BSD_BUTTON_GAP, by, buttonW, BSD_BUTTON_H);
        return l;
    }
}
