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
 * Pure layout of the crash pages, on the monitor's own glass: Frames 95's grey title over its text, the Stop screen of
 * Frames XP and 7 written from the top corner, the sad face of Frames 10 and 11 over their sentence, percentage, code
 * and stop code, and a kernel's console. The words are the page's, so only where each part stands is fixed here, and
 * the rows the newer Frames' sentence wraps to, which the face, the percentage and the code are stacked under.
 */
public final class CrashLayout {

    public static final int WIDTH = MonitorGlass.WIDTH;
    public static final int HEIGHT = MonitorGlass.HEIGHT;

    /** The step from one line of text to the next, and the scale lines are written at: every wall of text's. */
    public static final int ROW = 8;
    public static final float TEXT_SCALE = 0.75f;

    /* Frames 95: the grey title centred a third of the way down, the text under it within a wide margin. */
    public static final int BAND_Y = 72;
    public static final int BAND_H = 9;
    public static final int BAND_PAD = 4;
    public static final int F95_MARGIN = 30;
    public static final int F95_TEXT_Y = BAND_Y + 16;
    /** How far the points' text stands in from their asterisks. */
    public static final int F95_POINT_INDENT = 10;
    public static final int F95_RESTART_GAP = 10;

    /* Frames XP and 7: the screen written from the top corner, edge to edge. */
    public static final int NT_MARGIN = 6;

    /* Frames 10 and 11: the face, then the sentence at a larger size, the percentage, and the code with its notes. */
    public static final int MODERN_X = 27;
    public static final int FACE_Y = 22;
    public static final float FACE_SCALE = 4.5f;
    public static final int FACE_W = 36;
    public static final int FACE_H = 36;
    public static final int SENTENCE_Y = 70;
    public static final float SENTENCE_SCALE = 1.5f;
    public static final int SENTENCE_ROW = 15;
    public static final int SENTENCE_W = WIDTH - 2 * MODERN_X;
    /** The most rows the sentence wraps to in any language the page is read in. */
    public static final int MOST_SENTENCE_ROWS = 4;
    public static final int PERCENT_GAP = 4;
    public static final int PERCENT_H = 12;
    public static final int QR_GAP = 6;
    /** One module of the code, and the light margin of modules a reader needs around it to find it. */
    public static final int QR_MODULE = 2;
    public static final int QR_QUIET = 2;
    public static final int INFO_GAP = 8;

    /* A kernel's console: written from the top corner, each line wrapped at the glass's width. */
    public static final int CONSOLE_MARGIN = 4;

    private CrashLayout() {
    }

    /** Where the percentage stands under a sentence of {@code rows} rows. */
    public static int percentY(final int rows) {
        return SENTENCE_Y + rows * SENTENCE_ROW + PERCENT_GAP;
    }

    /** Where the code's light margin starts, under the percentage. */
    public static int qrY(final int rows) {
        return percentY(rows) + PERCENT_H + QR_GAP;
    }

    /** The code's whole side, its light margin included, for a code of {@code modules} modules a side. */
    public static int qrSide(final int modules) {
        return (modules + 2 * QR_QUIET) * QR_MODULE;
    }

    /** Where the notes beside the code start. */
    public static int infoX(final int modules) {
        return MODERN_X + qrSide(modules) + INFO_GAP;
    }

    /** How many console lines the glass holds. */
    public static int consoleRows() {
        return (HEIGHT - 2 * CONSOLE_MARGIN) / ROW;
    }

    /**
     * The parts that must not overlap on the newer Frames' page with the sentence at its longest and a code of
     * {@code modules} modules a side, and Frames 95's title, each against the glass's edges.
     */
    public static GuiLayout layout(final int modules) {
        final GuiLayout l = new GuiLayout(WIDTH, HEIGHT);
        final int rows = MOST_SENTENCE_ROWS;
        l.box("face", MODERN_X, FACE_Y, FACE_W, FACE_H);
        l.box("sentence", MODERN_X, SENTENCE_Y, SENTENCE_W, rows * SENTENCE_ROW);
        l.box("percent", MODERN_X, percentY(rows), SENTENCE_W / 2, PERCENT_H);
        l.box("code", MODERN_X, qrY(rows), qrSide(modules), qrSide(modules));
        // The notes beside the code: the visit line on two rows, a gap, and three short lines.
        l.box("notes", infoX(modules), qrY(rows), WIDTH - MODERN_X - infoX(modules), 7 * ROW);
        return l;
    }

    /** Frames 95's grey title around a title {@code titleWidth} wide, centred on the glass. */
    public static GuiLayout frames95(final int titleWidth) {
        final GuiLayout l = new GuiLayout(WIDTH, HEIGHT);
        final int w = titleWidth + 2 * BAND_PAD;
        l.box("band", (WIDTH - w) / 2, BAND_Y - 2, w, BAND_H);
        l.box("text", F95_MARGIN, F95_TEXT_Y, WIDTH - 2 * F95_MARGIN, 8 * ROW);
        return l;
    }
}
