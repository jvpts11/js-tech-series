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
 * Where the Network Manager shows how its nodes are linked: the Devices tab's four columns (the node, its type, its
 * link, its status) over the list, the line under the list that says why a node lost its link, and the size of the
 * Map's legend. Every x here is measured from the list's left edge, every y from the top of the column names.
 *
 * <p>Below {@link #TYPE_FROM} the TYPE column gives its room to the names, since the square of colour before each name
 * already tells the kind. Pure, and the tab and its test read the same numbers.
 */
public final class NetworkLinksLayout {

    public static final int ROW_H = 12;
    public static final int HEADER_H = 12;
    public static final int LINE_H = 10;
    /** Where a name starts, right of the square of its kind's colour. */
    public static final int NAME_X = 10;
    /** The type: room for MAINFRAME and the like; a longer one is cut. */
    public static final int TYPE_W = 66;
    /** The link: the square of an Optical Network Card, then the cable and its speed, or that its link is down. */
    public static final int LINK_W = 112;
    public static final int STATUS_W = 46;
    public static final int GAP = 6;
    /** The square that marks a node with an Optical Network Card, and the room it takes before the link's words. */
    public static final int CARD_SQUARE = 4;
    public static final int CARD_ROOM = CARD_SQUARE + 3;
    /** The narrowest list that keeps the TYPE column. */
    public static final int TYPE_FROM = 330;
    /** The line under the list, two lines of words in a frame. */
    public static final int FOOTER_LINES = 2;
    public static final int FOOTER_H = FOOTER_LINES * LINE_H + 6;
    public static final int FOOTER_GAP = 4;
    /** The Map's legend: a frame, a heading, and a line for each kind of link or mark it explains. */
    public static final int LEGEND_PAD = 4;
    public static final int LEGEND_SWATCH_W = 12;

    private NetworkLinksLayout() {
    }

    /** Whether a list {@code w} wide shows the TYPE column. */
    public static boolean showsType(final int w) {
        return w >= TYPE_FROM;
    }

    public static int statusX(final int w) {
        return w - STATUS_W;
    }

    public static int linkX(final int w) {
        return statusX(w) - GAP - LINK_W;
    }

    public static int typeX(final int w) {
        return linkX(w) - GAP - TYPE_W;
    }

    /** How wide a name may be before it is cut. */
    public static int nameW(final int w) {
        return (showsType(w) ? typeX(w) : linkX(w)) - GAP - NAME_X;
    }

    /** How tall the list is in a tab {@code h} tall, with the line under it or not. */
    public static int listH(final int h, final boolean footer) {
        return h - HEADER_H - (footer ? FOOTER_H + FOOTER_GAP : 0);
    }

    /** Where the line under the list starts, in a tab {@code h} tall. */
    public static int footerY(final int h) {
        return h - FOOTER_H;
    }

    /** How wide the legend is for entries whose widest words are {@code widest} pixels. */
    public static int legendW(final int widest) {
        return LEGEND_PAD * 2 + LEGEND_SWATCH_W + 4 + widest;
    }

    /** How tall the legend is with a heading and {@code entries} lines. */
    public static int legendH(final int entries) {
        return LEGEND_PAD * 2 + (entries + 1) * LINE_H;
    }

    /**
     * The Devices tab {@code w} wide and {@code h} tall: the column names, the first and the last row the list shows,
     * each cell of them a solid so that no two columns may meet, and the line under the list when a node lost its link.
     */
    public static GuiLayout devices(final int w, final int h, final boolean footer) {
        final GuiLayout layout = new GuiLayout(w, h);
        layout.text("nodeHeading", NAME_X, 2, 4, 1.0f);
        if (showsType(w)) {
            layout.text("typeHeading", typeX(w), 2, 4, 1.0f);
        }
        layout.text("linkHeading", linkX(w), 2, 4, 1.0f);
        layout.text("statusHeading", statusX(w), 2, 6, 1.0f);
        final int rows = Math.max(1, listH(h, footer) / ROW_H);
        row(layout, w, 0);
        if (rows > 1) {
            row(layout, w, rows - 1);
        }
        if (footer) {
            layout.box("footer", 0, footerY(h), w, FOOTER_H);
        }
        return layout;
    }

    /* The cells of the list's row {@code index}, named after it. */
    private static void row(final GuiLayout layout, final int w, final int index) {
        final int y = HEADER_H + index * ROW_H;
        layout.box("nameCell" + index, NAME_X, y, nameW(w), ROW_H - 1);
        if (showsType(w)) {
            layout.box("typeCell" + index, typeX(w), y, TYPE_W, ROW_H - 1);
        }
        layout.box("linkCell" + index, linkX(w), y, LINK_W, ROW_H - 1);
        layout.box("statusCell" + index, statusX(w), y, STATUS_W, ROW_H - 1);
    }
}
