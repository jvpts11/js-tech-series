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
 * Where the Network Manager's Log tab puts things: the switch between the Operations and the network's events along
 * the top, and under it, for the events, the three filters, the four columns (the time, the level, the machine, what
 * happened) over the list, and the count with Print and Export under the list. Every x is measured from the tab's left
 * edge, every y from its top.
 *
 * <p>The filters need no words before them, since what each offers says what it filters: "All levels", "All
 * machines", "Last day". Pure, and the tab and its test read the same numbers.
 */
public final class NetworkEventLogLayout {

    public static final int GAP = 4;
    /** The switch between the Operations and the events. */
    public static final int SWITCH_H = 13;
    public static final int SWITCH_W = 120;
    public static final int FILTER_Y = SWITCH_H + 3;
    public static final int FILTER_H = 12;
    public static final int LEVEL_FILTER_W = 62;
    public static final int MACHINE_FILTER_W = 104;
    public static final int PERIOD_FILTER_W = 72;
    public static final int HEADER_Y = FILTER_Y + FILTER_H + 3;
    public static final int HEADER_H = 11;
    public static final int LIST_Y = HEADER_Y + HEADER_H;
    public static final int ROW_H = 12;
    /** The time: a clock ({@code 12:04}) or a day, up to {@code Day 1234}. */
    public static final int TIME_W = 48;
    /** The level: room for ERROR. */
    public static final int LEVEL_W = 34;
    /** The machine: room for {@code SRV-3fa2c1}; a longer name is cut. */
    public static final int MACHINE_W = 70;
    /** The scrollbar beside the list. */
    public static final int BAR_W = 3;
    /** The line under the list: the count, Print and Export. */
    public static final int FOOT_H = 14;
    public static final int BUTTON_H = 11;
    public static final int PRINT_W = 40;
    public static final int EXPORT_W = 44;
    /** The narrowest tab the filters fit in side by side. */
    public static final int MIN_W = LEVEL_FILTER_W + MACHINE_FILTER_W + PERIOD_FILTER_W + 2 * GAP;

    private NetworkEventLogLayout() {
    }

    public static int machineFilterX() {
        return LEVEL_FILTER_W + GAP;
    }

    public static int periodFilterX() {
        return machineFilterX() + MACHINE_FILTER_W + GAP;
    }

    public static int levelX() {
        return TIME_W + GAP;
    }

    public static int machineX() {
        return levelX() + LEVEL_W + GAP;
    }

    public static int eventX() {
        return machineX() + MACHINE_W + GAP;
    }

    /** How wide what happened may be in a tab {@code w} wide before it is cut. */
    public static int eventW(final int w) {
        return w - BAR_W - eventX();
    }

    /** How tall the list is in a tab {@code h} tall. */
    public static int listH(final int h) {
        return Math.max(ROW_H, h - LIST_Y - FOOT_H);
    }

    /** Where the count and the buttons under the list stand, in a tab {@code h} tall. */
    public static int footY(final int h) {
        return h - FOOT_H + 2;
    }

    public static int exportX(final int w) {
        return w - EXPORT_W;
    }

    public static int printX(final int w) {
        return exportX(w) - GAP - PRINT_W;
    }

    /**
     * The events in a tab {@code w} wide and {@code h} tall: the switch, the filters, the column names, the cells of
     * the first and the last row the list shows, each a solid so that no two columns meet, the count and the two
     * buttons.
     */
    public static GuiLayout events(final int w, final int h) {
        final GuiLayout layout = new GuiLayout(w, h);
        layout.box("switch", 0, 0, SWITCH_W, SWITCH_H);
        layout.box("levelFilter", 0, FILTER_Y, LEVEL_FILTER_W, FILTER_H);
        layout.box("machineFilter", machineFilterX(), FILTER_Y, MACHINE_FILTER_W, FILTER_H);
        layout.box("periodFilter", periodFilterX(), FILTER_Y, PERIOD_FILTER_W, FILTER_H);
        layout.text("timeHeading", 0, HEADER_Y + 1, 4, 1.0f);
        layout.text("levelHeading", levelX(), HEADER_Y + 1, 5, 1.0f);
        layout.text("machineHeading", machineX(), HEADER_Y + 1, 7, 1.0f);
        layout.text("eventHeading", eventX(), HEADER_Y + 1, 5, 1.0f);
        final int rows = listH(h) / ROW_H;
        row(layout, w, 0);
        if (rows > 1) {
            row(layout, w, rows - 1);
        }
        layout.text("count", 0, footY(h) + 2, 10, 1.0f);
        layout.box("print", printX(w), footY(h), PRINT_W, BUTTON_H);
        layout.box("export", exportX(w), footY(h), EXPORT_W, BUTTON_H);
        return layout;
    }

    /* The cells of the list's row {@code index}, named after it. */
    private static void row(final GuiLayout layout, final int w, final int index) {
        final int y = LIST_Y + index * ROW_H;
        layout.box("timeCell" + index, 0, y, TIME_W, ROW_H - 1);
        layout.box("levelCell" + index, levelX(), y, LEVEL_W, ROW_H - 1);
        layout.box("machineCell" + index, machineX(), y, MACHINE_W, ROW_H - 1);
        layout.box("eventCell" + index, eventX(), y, eventW(w), ROW_H - 1);
    }
}
