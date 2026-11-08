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
 * Pure layout of the check a system makes of its disks after a sudden shutdown, on the monitor's own glass: Frames 95's
 * ScanDisk (the grey band, the words over the list of areas, the bar and the buttons at its foot), and the margin the
 * text pages of chkdsk and of fsck are written at. Their lines are the system's, so only where they start is fixed
 * here.
 */
public final class DiskCheckLayout {

    public static final int WIDTH = MonitorGlass.WIDTH;
    public static final int HEIGHT = MonitorGlass.HEIGHT;

    /** The step from one line to the next, and the scale they are written at: every wall of text in the mod's. */
    public static final int ROW = 8;
    public static final float TEXT_SCALE = 0.75f;

    /** ScanDisk's grey band across the top, its title centred in it. */
    public static final int BAND_H = 9;
    public static final int BAND_TEXT_Y = 2;
    /** Where its words start, and how far in. */
    public static final int SCANDISK_MARGIN = 18;
    public static final int SCANDISK_TEXT_Y = 18;
    /** The most rows its words take, wrapped at the glass's width. */
    public static final int SCANDISK_TEXT_ROWS = 5;
    /** The list of areas: the mark in its own column, the area's name beside it. */
    public static final int AREAS_X = 45;
    public static final int AREA_NAME_X = 57;
    public static final int AREAS_Y = SCANDISK_TEXT_Y + SCANDISK_TEXT_ROWS * ROW + 6;
    public static final int AREAS = 6;
    /** The line saying how far it has got, and the bar under it. */
    public static final int PROGRESS_Y = AREAS_Y + AREAS * ROW + 8;
    public static final int BAR_X = AREAS_X;
    public static final int BAR_Y = PROGRESS_Y + ROW + 2;
    public static final int BAR_W = WIDTH - 2 * AREAS_X;
    public static final int BAR_H = 8;
    /** The buttons across the foot. */
    public static final int BUTTONS_Y = HEIGHT - 30;

    /** Where the text pages of chkdsk and fsck are written, from the corner. */
    public static final int PAGE_MARGIN = 6;
    public static final int CONSOLE_MARGIN = 4;

    private DiskCheckLayout() {
    }

    /** The rows of text a console page holds, the newest kept when there are more. */
    public static int consoleRows() {
        return (HEIGHT - 2 * CONSOLE_MARGIN) / ROW;
    }

    /** ScanDisk's page: its band, its words, the list of areas, the progress line and bar, and the buttons. */
    public static GuiLayout layout() {
        final GuiLayout l = new GuiLayout(WIDTH, HEIGHT);
        l.box("band", 0, 0, WIDTH, BAND_H);
        l.box("words", SCANDISK_MARGIN, SCANDISK_TEXT_Y, WIDTH - 2 * SCANDISK_MARGIN, SCANDISK_TEXT_ROWS * ROW);
        l.box("areas", AREAS_X, AREAS_Y, WIDTH - 2 * AREAS_X, AREAS * ROW);
        l.text("progress", AREAS_X, PROGRESS_Y, 40, TEXT_SCALE);
        l.box("bar", BAR_X, BAR_Y, BAR_W, BAR_H);
        l.text("buttons", AREAS_X, BUTTONS_Y, 48, TEXT_SCALE);
        return l;
    }
}
