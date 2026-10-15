/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui.layout;

/**
 * Where the chooser of styles puts its parts: the screens a choice is for down the left, the styles in the middle, the
 * preview of the style under the pointer on the right, and Done at the foot. Pure, so a unit test checks that nothing
 * overlaps in the worst case.
 */
public final class StyleChooserLayout {

    /** The chooser's size. */
    public static final int WIDTH = 320;
    public static final int HEIGHT = 200;
    /** The room inside its edge. */
    public static final int PAD = 7;
    /** The height of its title's line. */
    public static final int TITLE = 14;
    /** How wide the list of whose screens the choice is for is. */
    public static final int SCOPES_WIDTH = 96;
    /** How wide the list of styles is. */
    public static final int STYLES_WIDTH = 104;
    /** The room between two columns. */
    public static final int GUTTER = 4;
    /** How tall a row of a list is. */
    public static final int ROW = 12;
    /** Done's size. */
    public static final int DONE_WIDTH = 60;
    public static final int DONE_HEIGHT = 20;

    private StyleChooserLayout() {
    }

    /** The top of the three columns. */
    public static int columnsTop() {
        return PAD + TITLE;
    }

    /** How tall the three columns are. */
    public static int columnsHeight() {
        return HEIGHT - columnsTop() - PAD - DONE_HEIGHT - GUTTER;
    }

    /** The left of the list of styles. */
    public static int stylesX() {
        return PAD + SCOPES_WIDTH + GUTTER;
    }

    /** The left of the preview. */
    public static int previewX() {
        return stylesX() + STYLES_WIDTH + GUTTER;
    }

    /** How wide the preview is. */
    public static int previewWidth() {
        return WIDTH - PAD - previewX();
    }

    /** How many rows a list shows. */
    public static int rows() {
        return columnsHeight() / ROW;
    }

    /** Done's left. */
    public static int doneX() {
        return WIDTH - PAD - DONE_WIDTH;
    }

    /** Done's top. */
    public static int doneY() {
        return HEIGHT - PAD - DONE_HEIGHT;
    }

    /** The whole chooser, as the overlap check reads it. */
    public static GuiLayout layout() {
        final GuiLayout layout = new GuiLayout(WIDTH, HEIGHT);
        layout.text("title", PAD, PAD, 20, 1f);
        layout.box("scopes", PAD, columnsTop(), SCOPES_WIDTH, columnsHeight());
        layout.box("styles", stylesX(), columnsTop(), STYLES_WIDTH, columnsHeight());
        layout.box("preview", previewX(), columnsTop(), previewWidth(), columnsHeight());
        layout.box("done", doneX(), doneY(), DONE_WIDTH, DONE_HEIGHT);
        return layout;
    }
}
