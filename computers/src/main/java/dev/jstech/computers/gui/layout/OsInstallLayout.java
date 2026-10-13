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
 * Pure layout of the OS install screen: a small dialog with a title bar, then one of three bodies depending
 * on how the copy stands. Working shows the four steps and the progress bar; done and failed each show two
 * lines of words and a pair of buttons at the same foot row. The three bodies never show together, so each has
 * its own {@link GuiLayout} rather than one shared layout where a done button would sit on a failed one.
 */
public final class OsInstallLayout {

    public static final int WIDTH = 320;
    public static final int HEIGHT = 176;

    public static final int TITLE_BAR_H = 16;
    public static final int TITLE_X = 8;
    public static final int TITLE_Y = 5;

    /** Where the body starts under the title bar. */
    public static final int CONTENT_TOP = 26;

    /**
     * The four beats a copy walks through, each on its own row. The screen draws this many steps from its step list,
     * and checks when it loads that the list holds exactly this many.
     */
    public static final int STEP_COUNT = 4;
    public static final int STEP_LABEL_X = 10;
    public static final int STEP_ROW_H = 12;
    /** How far the "done"/percent word stands from the dialog's right edge. */
    public static final int STEP_STATUS_RIGHT_MARGIN = 60;

    /** The gap kept between the last step and the progress bar. */
    public static final int AFTER_STEPS_GAP = 12;
    public static final int BAR_X = 10;
    public static final int BAR_H = 8;
    /** How far under the bar the keep-the-medium hint is written. */
    public static final int BAR_TO_HINT_DY = 16;

    /** The two lines a finished or a failed copy writes under its title. */
    public static final int LINE2_DY = 16;
    public static final int LINE3_DY = 27;
    /** How far apart a failure's wrapped lines sit, once the first of them is written. */
    public static final int FAILURE_WRAP_LINE_H = 11;
    /** How wide a failure's reason is wrapped: the dialog less the margin on each side. */
    public static final int FAILURE_WRAP_W = WIDTH - 2 * STEP_LABEL_X;

    /** Both button rows sit this far above the dialog's own bottom edge. */
    public static final int BUTTON_Y_MARGIN = 26;
    public static final int BUTTON_H = 16;

    public static final int DONE_PRIMARY_X = 10;
    public static final int DONE_PRIMARY_W = 96;
    public static final int DONE_SECONDARY_X = 114;
    public static final int DONE_SECONDARY_W = 110;

    public static final int FAILED_PRIMARY_X = 10;
    public static final int FAILED_PRIMARY_W = 110;
    public static final int FAILED_SECONDARY_X = 128;
    public static final int FAILED_SECONDARY_W = 96;

    private OsInstallLayout() {
    }

    /** Where the working phase's status word (or the finished mark) starts, on the right of the dialog. */
    public static int stepStatusX() {
        return WIDTH - STEP_STATUS_RIGHT_MARGIN;
    }

    /** The row every dialog's buttons sit on, this far above the bottom edge. */
    public static int buttonY() {
        return HEIGHT - BUTTON_Y_MARGIN;
    }

    public static int barW() {
        return WIDTH - 2 * BAR_X;
    }

    /** As many of a failure's wrapped lines as fit above the button row; the screen caps the reason there. */
    public static int mostFailureLines() {
        return (buttonY() - CONTENT_TOP - LINE2_DY) / FAILURE_WRAP_LINE_H;
    }

    /** The four steps and the progress bar underneath them, while the copy is running. */
    public static GuiLayout workingLayout() {
        final GuiLayout l = dialog();
        int ty = CONTENT_TOP;
        for (int i = 0; i < STEP_COUNT; i++) {
            l.text("step" + i, STEP_LABEL_X, ty, 30, 1.0f);
            l.text("step" + i + "Status", stepStatusX(), ty, 10, 1.0f);
            ty += STEP_ROW_H;
        }
        ty += AFTER_STEPS_GAP;
        l.box("progressTrack", BAR_X, ty, barW(), BAR_H);
        l.text("keepMedium", BAR_X, ty + BAR_TO_HINT_DY, 40, 1.0f);
        return l;
    }

    /** The two lines and the reboot/back-to-setup buttons, once the copy is over. */
    public static GuiLayout doneLayout() {
        final GuiLayout l = dialog();
        l.text("line1", STEP_LABEL_X, CONTENT_TOP, 40, 1.0f);
        l.text("line2", STEP_LABEL_X, CONTENT_TOP + LINE2_DY, 40, 1.0f);
        l.text("line3", STEP_LABEL_X, CONTENT_TOP + LINE3_DY, 40, 1.0f);
        l.box("primary", DONE_PRIMARY_X, buttonY(), DONE_PRIMARY_W, BUTTON_H);
        l.box("secondary", DONE_SECONDARY_X, buttonY(), DONE_SECONDARY_W, BUTTON_H);
        return l;
    }

    /** The refusal, its wrapped reason, and the back-to-setup/close buttons, when the write was refused. */
    public static GuiLayout failedLayout(final int wrappedLines) {
        final GuiLayout l = dialog();
        l.text("line1", STEP_LABEL_X, CONTENT_TOP, 40, 1.0f);
        int ly = CONTENT_TOP + LINE2_DY;
        final int widest = Math.round(FAILURE_WRAP_W / GuiLayout.GLYPH_WIDTH);
        for (int i = 0; i < wrappedLines; i++) {
            l.text("failure" + i, STEP_LABEL_X, ly, widest, 1.0f);
            ly += FAILURE_WRAP_LINE_H;
        }
        l.box("primary", FAILED_PRIMARY_X, buttonY(), FAILED_PRIMARY_W, BUTTON_H);
        l.box("secondary", FAILED_SECONDARY_X, buttonY(), FAILED_SECONDARY_W, BUTTON_H);
        return l;
    }

    private static GuiLayout dialog() {
        final GuiLayout l = new GuiLayout(WIDTH, HEIGHT);
        l.box("titleBar", 1, 1, WIDTH - 2, TITLE_BAR_H);
        l.text("title", TITLE_X, TITLE_Y, 40, 1.0f);
        return l;
    }
}
