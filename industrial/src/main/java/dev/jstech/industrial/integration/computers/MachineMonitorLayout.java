/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.integration.computers;

import dev.jstech.core.gui.layout.GuiLayout;

/**
 * Where the Machine Monitor's window puts things, in either of its two looks. The classic one, on Frames 95 and its
 * peers: the menus along the top, Acknowledge, Acknowledge all and the alerts shown under them, a count of each kind
 * of alert, the alerts' list, and under it the panel of the machine chosen, with the status line at the foot. The
 * modern one, on Frames 10 and 11 and the Linux desktops: no menus, a search box beside the buttons, and the panel to
 * the right of the list. Every x is measured from the window's left edge, every y from its top; small text is drawn
 * at {@link #SMALL}. Pure, and the window and its test read the same numbers.
 */
public final class MachineMonitorLayout {

    public static final int GAP = 4;
    /** Small text: the counts, the list, the panel and the status line. */
    public static final float SMALL = 0.75f;
    /** Each look's window as it opens. */
    public static final int CLASSIC_W = 352;
    public static final int CLASSIC_H = 214;
    public static final int MODERN_W = 420;
    public static final int MODERN_H = 214;
    /** The classic look's menu bar, as tall as every window's is, and the room of each of its names. */
    public static final int MENU_H = 10;
    public static final int MENU_ITEM_W = 40;
    /** How wide a menu's entries are when it opens: room for "Acknowledge all". */
    public static final int MENU_W = 92;
    public static final int BUTTON_H = 12;
    public static final int ACK_W = 74;
    public static final int ACK_ALL_W = 98;
    /** "Show:", before the classic look's choice of alerts. */
    public static final int SHOW_W = 30;
    public static final int FILTER_W = 96;
    public static final int SEARCH_W = 120;
    /** The line of counts, each a lamp and a word in small text. */
    public static final int CHIP_H = 9;
    public static final int CHIP_GAP = 6;
    public static final int LAMP = 5;
    public static final int HEADER_H = 10;
    public static final int ROW_H = 10;
    /** The lamp of an alert's grade, the time ({@code 14:02}), the machine, where it stands and the alert. */
    public static final int LAMP_W = 8;
    public static final int TIME_W = 26;
    public static final int MACHINE_W = 76;
    public static final int WHERE_W = 64;
    public static final int ALERT_W = 56;
    public static final int BAR_W = 3;
    /** A line of the panel, in small text. */
    public static final int LINE_H = 9;
    /** The classic panel under the list: two columns of five lines. */
    public static final int CLASSIC_DETAIL_H = 5 * LINE_H + 2 * GAP;
    /** The modern panel beside the list. */
    public static final int MODERN_DETAIL_W = 112;
    public static final int STATUS_H = 10;

    private MachineMonitorLayout() {
    }

    /** Where Acknowledge and its neighbours stand: under the menus in the classic look, at the top in the modern. */
    public static int toolsY(final boolean classic) {
        return classic ? MENU_H + 2 : 0;
    }

    public static int ackAllX() {
        return ACK_W + GAP;
    }

    /** The classic look's "Show:", and after it the choice of alerts. */
    public static int showX() {
        return ackAllX() + ACK_ALL_W + 2 * GAP;
    }

    public static int filterX(final boolean classic) {
        return classic ? showX() + SHOW_W + GAP : searchX() + SEARCH_W + GAP;
    }

    /** The modern look's search box. */
    public static int searchX() {
        return ackAllX() + ACK_ALL_W + GAP;
    }

    public static int chipsY(final boolean classic) {
        return toolsY(classic) + BUTTON_H + 3;
    }

    public static int headerY(final boolean classic) {
        return chipsY(classic) + CHIP_H + 2;
    }

    public static int listY(final boolean classic) {
        return headerY(classic) + HEADER_H;
    }

    /** How wide the list is in a window {@code w} wide: the whole of it, or what the modern panel leaves. */
    public static int tableW(final int w, final boolean classic) {
        return classic ? w : w - MODERN_DETAIL_W - GAP;
    }

    /** How tall the list is in a window {@code h} tall. */
    public static int listH(final int h, final boolean classic) {
        final int under = classic ? GAP + CLASSIC_DETAIL_H + GAP + STATUS_H : GAP + STATUS_H;
        return Math.max(ROW_H, h - listY(classic) - under);
    }

    public static int statusY(final int h) {
        return h - STATUS_H;
    }

    /** The panel of the machine chosen: under the list in the classic look, beside it in the modern. */
    public static int[] detail(final int w, final int h, final boolean classic) {
        if (classic) {
            return new int[] {0, listY(true) + listH(h, true) + GAP, w, CLASSIC_DETAIL_H};
        }
        final int y = headerY(false);
        return new int[] {w - MODERN_DETAIL_W, y, MODERN_DETAIL_W, statusY(h) - GAP - y};
    }

    public static int timeX() {
        return LAMP_W;
    }

    public static int machineX() {
        return timeX() + TIME_W + GAP;
    }

    public static int whereX() {
        return machineX() + MACHINE_W + GAP;
    }

    public static int alertX() {
        return whereX() + WHERE_W + GAP;
    }

    public static int readingX() {
        return alertX() + ALERT_W + GAP;
    }

    /** How wide the reading may be in a list {@code tableW} wide before it is cut. */
    public static int readingW(final int tableW) {
        return tableW - BAR_W - readingX();
    }

    /**
     * The window {@code w} wide and {@code h} tall in either look: the menus, the buttons, the choice of alerts and
     * the search box, the counts at their longest, the column names, the cells of the first and the last row the list
     * shows, each a solid so that no two columns meet, the panel and the status line.
     */
    public static GuiLayout window(final int w, final int h, final boolean classic) {
        final GuiLayout layout = new GuiLayout(w, h);
        final int tools = toolsY(classic);
        if (classic) {
            layout.box("menus", 0, 0, 2 * MENU_ITEM_W, MENU_H);
            layout.text("show", showX(), tools + 2, 5, 1.0f);
        } else {
            layout.box("search", searchX(), tools, SEARCH_W, BUTTON_H);
        }
        layout.box("acknowledge", 0, tools, ACK_W, BUTTON_H);
        layout.box("acknowledgeAll", ackAllX(), tools, ACK_ALL_W, BUTTON_H);
        layout.box("filter", filterX(classic), tools, FILTER_W, BUTTON_H);
        // The five counts at their longest, "Undervoltage 99" and its peers, one after the other.
        int chip = 0;
        for (final int letters : new int[] {14, 13, 15, 11, 10}) {
            layout.text("chip" + chip, chipX(chip), chipsY(classic), letters, SMALL);
            chip++;
        }
        final int tableW = tableW(w, classic);
        final int headerY = headerY(classic);
        layout.text("timeHeading", timeX(), headerY + 1, 4, 1.0f);
        layout.text("machineHeading", machineX(), headerY + 1, 7, 1.0f);
        layout.text("whereHeading", whereX(), headerY + 1, 5, 1.0f);
        layout.text("alertHeading", alertX(), headerY + 1, 5, 1.0f);
        layout.text("readingHeading", readingX(), headerY + 1, 7, 1.0f);
        final int rows = listH(h, classic) / ROW_H;
        row(layout, tableW, classic, 0);
        if (rows > 1) {
            row(layout, tableW, classic, rows - 1);
        }
        final int[] panel = detail(w, h, classic);
        layout.box("detail", panel[0], panel[1], panel[2], panel[3]);
        layout.text("status", 0, statusY(h) + 1, 30, SMALL);
        return layout;
    }

    /** Where the count of kind {@code index} begins, at the longest counts any kind may show. */
    public static int chipX(final int index) {
        final int[] letters = {14, 13, 15, 11, 10};
        int x = 0;
        for (int i = 0; i < index; i++) {
            x += LAMP + 2 + Math.round(letters[i] * GuiLayout.GLYPH_WIDTH * SMALL) + CHIP_GAP;
        }
        return x;
    }

    /* The cells of the list's row {@code index}, named after it. */
    private static void row(final GuiLayout layout, final int tableW, final boolean classic, final int index) {
        final int y = listY(classic) + index * ROW_H;
        layout.box("lampCell" + index, 0, y, LAMP_W - 1, ROW_H - 1);
        layout.box("timeCell" + index, timeX(), y, TIME_W, ROW_H - 1);
        layout.box("machineCell" + index, machineX(), y, MACHINE_W, ROW_H - 1);
        layout.box("whereCell" + index, whereX(), y, WHERE_W, ROW_H - 1);
        layout.box("alertCell" + index, alertX(), y, ALERT_W, ROW_H - 1);
        layout.box("readingCell" + index, readingX(), y, readingW(tableW), ROW_H - 1);
    }
}
