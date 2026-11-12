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
 * Where everything of a datacenter section's Diagnostics tab in the Cluster Manager stands, from the top of the tab's
 * pane, in the desktop's own text: the three numbers of the last minute in their tiles, the last hour's chart under
 * its label and over its time line, the table of the alerts standing, and the two notes under it. The chart keeps the
 * height its design gives it when the pane has room, the table shows the alerts that fit, and in a pane too short for
 * all of it the notes give way first and the chart shrinks to its least. Pure, so the tab draws from it and a test
 * holds it to account.
 */
public final class SectionDiagnosticsLayout {

    /** The tiles: how tall, and the room between two. */
    public static final int TILE_H = 32;
    public static final int TILE_GAP = 5;
    /** The room between two blocks of the tab. */
    public static final int GAP = 4;
    /** A line of text: a block's label, the chart's time line, a note. */
    public static final int LINE_H = 11;
    /** The table: its header, and each alert's row. */
    public static final int HEADER_H = 9;
    public static final int ROW_H = 11;
    /** The chart is never shorter than this, and no taller than its design draws it. */
    public static final int MIN_CHART_H = 24;
    public static final int CHART_H = 67;
    /** The notes under the table: the last time the latency went over, and where the alerts also go. */
    public static final int NOTES = 2;
    /** The table's columns from its left: the level, what the alert says, and since when, from the right. */
    public static final int LEVEL_X = 3;
    public static final int WHAT_X = 42;
    public static final int SINCE_W = 64;
    /** The smallest pane the tab is drawn in: the Cluster Manager at its least size. */
    public static final int MIN_W = 222;
    public static final int MIN_H = 123;
    /** The pane at the window's own size. */
    public static final int DEFAULT_W = 232;
    public static final int DEFAULT_H = 153;
    /* From the chart's foot to the first row: its time marks, the table's label and its header. */
    private static final int BELOW_CHART = GAP + LINE_H + 1 + LINE_H - 1 + HEADER_H + 1;

    private SectionDiagnosticsLayout() {
    }

    /** Where each block of the tab stands in a pane {@code w} by {@code h} with {@code alerts} alerts standing. */
    public static Placed place(final int w, final int h, final int alerts) {
        final int tileW = (w - 2 * TILE_GAP) / 3;
        final int chartLabelY = TILE_H + GAP + 1;
        final int chartY = chartLabelY + LINE_H;
        int notes = NOTES;
        int room = h - chartY - BELOW_CHART - GAP - NOTES * LINE_H;
        if (room < MIN_CHART_H + ROW_H) {
            // Too short for everything: the notes give way before the chart and the table do.
            notes = 0;
            room = h - chartY - BELOW_CHART;
        }
        final int fit = Math.max(0, (room - MIN_CHART_H) / ROW_H);
        final int rows = Math.min(Math.max(1, alerts), fit);
        final int chartH = Math.max(MIN_CHART_H, Math.min(CHART_H, room - rows * ROW_H));
        final int axisY = chartY + chartH + GAP;
        final int alertsLabelY = axisY + LINE_H + 1;
        final int headerY = alertsLabelY + LINE_H - 1;
        final int rowsY = headerY + HEADER_H + 1;
        return new Placed(tileW, chartLabelY, chartY, chartH, axisY, alertsLabelY, headerY, rowsY, rows,
                rowsY + rows * ROW_H + GAP, notes);
    }

    /** The tab in a pane {@code w} by {@code h} with {@code alerts} alerts, its solid parts and words, for a test. */
    public static GuiLayout layout(final int w, final int h, final int alerts) {
        final Placed p = place(w, h, alerts);
        final GuiLayout l = new GuiLayout(w, h);
        for (int i = 0; i < 3; i++) {
            l.box("tile" + i, i * (p.tileW() + TILE_GAP), 0, p.tileW(), TILE_H);
        }
        l.box("chart", 0, p.chartY(), w, p.chartH());
        l.text("from", 0, p.axisY(), 7, 1.0f);
        l.box("header", 0, p.headerY(), w, HEADER_H);
        if (p.rows() > 0) {
            l.box("rows", 0, p.rowsY(), w, p.rows() * ROW_H);
        }
        for (int i = 0; i < p.notes(); i++) {
            l.textBox("note" + i, 0, p.notesY() + i * LINE_H, w, LINE_H);
        }
        return l;
    }

    /**
     * Where the blocks of the tab stand, from the top of its pane.
     *
     * @param tileW        how wide each of the three tiles is
     * @param chartLabelY  the line of the chart's label and legend
     * @param chartY       the chart's top
     * @param chartH       how tall the chart is
     * @param axisY        the line of the chart's time marks under it
     * @param alertsLabelY the table's label
     * @param headerY      the table's header
     * @param rowsY        its first row
     * @param rows         how many alerts it shows
     * @param notesY       the first note
     * @param notes        how many notes show: both, or none in a pane too short
     */
    public record Placed(int tileW, int chartLabelY, int chartY, int chartH, int axisY, int alertsLabelY, int headerY,
                         int rowsY, int rows, int notesY, int notes) {
    }
}
