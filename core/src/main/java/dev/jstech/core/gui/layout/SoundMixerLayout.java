/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui.layout;

/**
 * Where everything on the Sound Mixer sits, in the game's own option-screen measures: controls 150 wide in two
 * columns ten apart, or one of 310 across both, 25 between rows, under the tab bar and above a 36 high footer. The
 * screen places its widgets from these, and a test lays each tab out at the smallest screen the game allows and
 * checks nothing collides, so a row added later cannot quietly run into another.
 *
 * <p>Every x is from the left of the centred column ({@link #left(int)}), every y from the top of the screen.
 */
public final class SoundMixerLayout {

    /** A control in one of the two columns. */
    public static final int COLUMN = 150;
    /** Between the two columns. */
    public static final int GAP = 10;
    /** A control across both columns. */
    public static final int FULL = COLUMN * 2 + GAP;
    /** A button's or a slider's height. */
    public static final int CONTROL_HEIGHT = 20;
    /** From one row of controls to the next. */
    public static final int PITCH = 25;
    /** The tab bar across the top. */
    public static final int TAB_BAR_HEIGHT = 24;
    /** The first row of controls under the tab bar. */
    public static final int CONTENT_TOP = 36;
    /** The footer that holds Done, its separator two above it. */
    public static final int FOOTER = 36;
    public static final int DONE_WIDTH = 200;
    /** Below the last channel, before Reset Channels. */
    public static final int RESET_GAP = 8;
    /** Below Reset Channels, before the note on how channels add up. */
    public static final int NOTE_GAP = 16;
    public static final int SEARCH_TOP = 30;
    public static final int FILTER_TOP = 55;
    /** The count of sounds turned off, level with the filter buttons' text. */
    public static final int COUNT_TOP = 61;
    public static final int SHOW_X = 136;
    public static final int SHOW_WIDTH = 76;
    public static final int ALL_ON_X = 216;
    public static final int ALL_ON_WIDTH = 94;
    public static final int LIST_TOP = 82;
    public static final int ROW_HEIGHT = 24;
    public static final int PLAY_WIDTH = 36;
    public static final int TOGGLE_WIDTH = 34;
    /** The search icon's room at the left of the search field, where the typed text starts after it. */
    public static final int SEARCH_TEXT_INSET = 20;
    public static final int OPTIONS_NOTE_TOP = CONTENT_TOP + 3 * PITCH + 11;
    /** The smallest screen the game lays out, in its own units. */
    public static final int SMALLEST_WIDTH = 320;
    public static final int SMALLEST_HEIGHT = 240;

    private SoundMixerLayout() {
    }

    /** The left edge of the centred column on a screen that wide. */
    public static int left(final int screenWidth) {
        return screenWidth / 2 - FULL / 2;
    }

    /** Where channel {@code index} sits: across the two columns, row by row. */
    public static int channelX(final int screenWidth, final int index) {
        return left(screenWidth) + (index % 2) * (COLUMN + GAP);
    }

    public static int channelY(final int index) {
        return CONTENT_TOP + (index / 2) * PITCH;
    }

    /** Where Reset Channels sits under that many channels. */
    public static int resetY(final int channels) {
        return CONTENT_TOP + (channels + 1) / 2 * PITCH + RESET_GAP;
    }

    public static int channelsNoteY(final int channels) {
        return resetY(channels) + CONTROL_HEIGHT + NOTE_GAP;
    }

    /** The list's bottom: the footer's separator. */
    public static int listBottom(final int screenHeight) {
        return screenHeight - FOOTER - 2;
    }

    public static int doneY(final int screenHeight) {
        return screenHeight - FOOTER + (FOOTER - CONTROL_HEIGHT) / 2;
    }

    /** The Channels tab with that many channels, and a note of that many lines. */
    public static GuiLayout channels(final int width, final int height, final int channels, final int noteLines) {
        final GuiLayout layout = frame(width, height);
        for (int i = 0; i < channels; i++) {
            layout.box("channel_" + i, channelX(width, i), channelY(i), COLUMN, CONTROL_HEIGHT);
        }
        layout.box("reset", left(width) + (FULL - COLUMN) / 2, resetY(channels), COLUMN, CONTROL_HEIGHT);
        for (int line = 0; line < noteLines; line++) {
            layout.text("note_" + line, left(width), channelsNoteY(channels) + line * 11, FULL / 6, 1.0F);
        }
        return layout;
    }

    /** The Sounds tab, with as many rows as fit above the footer. */
    public static GuiLayout sounds(final int width, final int height) {
        final GuiLayout layout = frame(width, height);
        final int left = left(width);
        layout.box("search", left, SEARCH_TOP, FULL, CONTROL_HEIGHT);
        layout.text("count", left, COUNT_TOP, 21, 1.0F);
        layout.box("show", left + SHOW_X, FILTER_TOP, SHOW_WIDTH, CONTROL_HEIGHT);
        layout.box("all_on", left + ALL_ON_X, FILTER_TOP, ALL_ON_WIDTH, CONTROL_HEIGHT);
        for (int row = 0; LIST_TOP + 4 + (row + 1) * ROW_HEIGHT <= listBottom(height); row++) {
            final int y = LIST_TOP + 4 + row * ROW_HEIGHT;
            layout.box("play_" + row, left + FULL - PLAY_WIDTH - TOGGLE_WIDTH - 2, y, PLAY_WIDTH, CONTROL_HEIGHT);
            layout.box("toggle_" + row, left + FULL - TOGGLE_WIDTH, y, TOGGLE_WIDTH, CONTROL_HEIGHT);
        }
        return layout;
    }

    /** The Options tab, with a note of that many lines. */
    public static GuiLayout options(final int width, final int height, final int noteLines) {
        final GuiLayout layout = frame(width, height);
        final int left = left(width);
        layout.box("muffle", left, CONTENT_TOP, COLUMN, CONTROL_HEIGHT);
        layout.box("alerts_on_screen", left + COLUMN + GAP, CONTENT_TOP, COLUMN, CONTROL_HEIGHT);
        layout.box("lower_under_alerts", left, CONTENT_TOP + PITCH, COLUMN, CONTROL_HEIGHT);
        layout.box("key", left, CONTENT_TOP + 2 * PITCH, FULL, CONTROL_HEIGHT);
        for (int line = 0; line < noteLines; line++) {
            layout.text("note_" + line, left, OPTIONS_NOTE_TOP + line * 11, FULL / 6, 1.0F);
        }
        return layout;
    }

    /* What every tab has: the tab bar and the footer with Done. */
    private static GuiLayout frame(final int width, final int height) {
        return new GuiLayout(width, height)
                .box("tabs", 0, 0, width, TAB_BAR_HEIGHT - 2)
                .box("done", width / 2 - DONE_WIDTH / 2, doneY(height), DONE_WIDTH, CONTROL_HEIGHT);
    }
}
