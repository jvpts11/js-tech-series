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
 * Pure layout of the KVM Switch's channel bar: a header naming the switch and how many machines it
 * carries, then one row per channel with its key, its name and its state. The panel grows with the
 * channel count, so the screen and the test both read {@link #height(int)} instead of guessing it.
 *
 * <p>All coordinates are relative to the panel's top-left corner.
 */
public final class KvmChannelLayout {

    public static final int WIDTH = 260;
    public static final int PAD = 10;
    public static final int ROW_H = 20;
    /** A row's own solid, a shade shorter than {@link #ROW_H} to leave a hairline between rows. */
    public static final int ROW_BOX_H = ROW_H - 2;
    /** How wide the mark at a row's left edge is drawn, on the active or hovered row. */
    public static final int MARK_W = 2;
    /** The left margin a title starts at, and the right margin the count ends at. */
    public static final int MARGIN = 12;

    public static final int HEADER_X = 6;
    public static final int HEADER_Y = 6;
    public static final int HEADER_W = WIDTH - 12;
    /** The header bar's own height, drawn by the theme. */
    public static final int HEADER_H = 17;

    public static final int TITLE_X = MARGIN;
    public static final int TITLE_Y = 11;

    /** Where the rows start, under the header and its own air. */
    public static final int ROWS_Y0 = 26;

    /** Where a row's own words sit: the channel key, its name, and how far its state stands from the right edge. */
    public static final int ROW_KEY_X = PAD + 8;
    public static final int ROW_NAME_X = PAD + 34;
    public static final int ROW_STATE_MARGIN = PAD + 8;
    /** How far down a row its words are written, from the row's own top. */
    public static final int ROW_TEXT_DY = 6;

    /** As many channels as a KVM switch ever reports, the numbered keys 1 through 8 that pick one. */
    public static final int MOST_CHANNELS = 8;

    private KvmChannelLayout() {
    }

    /** The y a row of {@code index} starts at. */
    public static int rowY(final int index) {
        return ROWS_Y0 + index * ROW_H;
    }

    /** How tall the whole panel is with that many channels listed. */
    public static int height(final int channels) {
        return PAD * 2 + ROWS_Y0 + channels * ROW_H;
    }

    /** The layout with that many channels listed, each a row as wide as the panel allows. */
    public static GuiLayout layout(final int channels) {
        final GuiLayout l = new GuiLayout(WIDTH, height(channels));
        l.box("header", HEADER_X, HEADER_Y, HEADER_W, HEADER_H);
        l.text("title", TITLE_X, TITLE_Y, 16, 1.0f);
        // "8 machines" is the longest count a switch with MOST_CHANNELS ever reports.
        l.text("count", WIDTH - MARGIN - Math.round(12 * GuiLayout.GLYPH_WIDTH), TITLE_Y, 12, 1.0f);
        for (int i = 0; i < channels; i++) {
            final int y = rowY(i);
            l.box("row" + i, PAD, y, WIDTH - 2 * PAD, ROW_BOX_H);
            l.text("key" + i, ROW_KEY_X, y + ROW_TEXT_DY, 3, 1.0f);
            l.text("name" + i, ROW_NAME_X, y + ROW_TEXT_DY, 20, 1.0f);
            l.text("state" + i, WIDTH - ROW_STATE_MARGIN - Math.round(7 * GuiLayout.GLYPH_WIDTH),
                    y + ROW_TEXT_DY, 7, 1.0f);
        }
        return l;
    }
}
