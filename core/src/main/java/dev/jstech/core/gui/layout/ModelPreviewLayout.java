/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui.layout;

/**
 * Where everything on the preview of a model sits, on a screen of any size: the title across the top, with the file
 * and when the models were last read at its right; the view of the model on the left, as big as the screen leaves it;
 * down a column on the right, the animations, the tree of bones with each one's triangles, the lines on the model's
 * detail (its triangles against what it may have, with a bar, how it is drawn far away, its size, its colours) and the
 * switches, two to a row; the timeline and the speed along the bottom. A short screen keeps one animation's button and
 * a few rows of the tree, and lets the detail's lines go from the last; the screen places its widgets from these, and
 * a test lays it out at the smallest screen the game allows and checks nothing collides.
 */
public final class ModelPreviewLayout {

    /** The title bar's height. */
    public static final int TITLE_HEIGHT = 20;
    /** Between the screen's edge and what is on it, and between the view and the column. */
    public static final int MARGIN = 6;
    /** The column on the right. */
    public static final int COLUMN = 168;
    /** The bar along the bottom. */
    public static final int BOTTOM = 30;
    /** A button's, a switch's or the timeline's height. */
    public static final int CONTROL = 20;
    /** From one animation's button to the next. */
    public static final int BUTTON_PITCH = 22;
    /** From one row of switches to the next. */
    public static final int SWITCH_PITCH = 22;
    /** A row of the tree of bones, in the small font. */
    public static final int TREE_ROW = 9;
    /** How far a bone stands in from the one it hangs from. */
    public static final int TREE_INDENT = 8;
    /** The width kept at a row's right for the bone's triangles. */
    public static final int COUNT_WIDTH = 28;
    /** A line on the model's detail, in the small font. */
    public static final int DETAIL_ROW = 10;
    /** The bar of the triangles under their line, and the room under it. */
    public static final int BAR = 6;
    /** The bar's own height. */
    public static final int BAR_HEIGHT = 4;
    /** The line that says a model has no animations, where their buttons would be. */
    public static final int NONE_ROW = 12;
    /** The most animations shown at once; the list scrolls past them. */
    public static final int MOST_BUTTONS = 4;
    /** The fewest rows of bones shown. */
    public static final int FEWEST_ROWS = 3;
    /** The width of the file and when the models were last read, at the title's right. */
    public static final int RELOADED_WIDTH = 150;
    /** The width of the animation's name at the left of the timeline. */
    public static final int NAME_WIDTH = 46;
    /** The speed button's width. */
    public static final int SPEED_WIDTH = 82;
    /** A label's height above its part of the column. */
    public static final int LABEL = 12;
    /** The smallest screen the game lays out, in its own units. */
    public static final int SMALLEST_WIDTH = 320;
    public static final int SMALLEST_HEIGHT = 240;

    private static final int GAP = 4;
    private static final int SMALL_HEIGHT = 7;
    private static final int TEXT_HEIGHT = 9;

    private ModelPreviewLayout() {
    }

    public static int viewX() {
        return MARGIN;
    }

    public static int viewY() {
        return TITLE_HEIGHT + 4;
    }

    public static int viewWidth(final int width) {
        return width - COLUMN - 3 * MARGIN;
    }

    public static int viewHeight(final int height) {
        return height - viewY() - BOTTOM - 4;
    }

    /** The left of the column. */
    public static int columnX(final int width) {
        return width - COLUMN - MARGIN;
    }

    public static int animationsLabelY() {
        return viewY() + 2;
    }

    public static int buttonsTop() {
        return animationsLabelY() + LABEL;
    }

    /** How many lines on the detail are shown: all that fit with one animation's button and a few rows of bones. */
    public static int detailsShown(final int height, final Column column) {
        final int room = room(height, column) - animations(Math.min(1, column.animations()), column) - leastTree()
                - BAR - GAP;
        return Math.max(0, Math.min(column.details(), room / DETAIL_ROW));
    }

    /** Whether the hint is shown: only when every line on the detail is. */
    public static boolean hintShown(final int height, final Column column) {
        return column.hint() && detailsShown(height, column) == column.details();
    }

    /** How many animations' buttons are shown at once: up to four, as the room the detail leaves holds. */
    public static int buttonsShown(final int height, final Column column) {
        final int room = room(height, column) - leastTree() - details(detailsShown(height, column));
        return Math.max(0, Math.min(Math.min(column.animations(), MOST_BUTTONS), room / BUTTON_PITCH));
    }

    public static int buttonY(final int index) {
        return buttonsTop() + index * BUTTON_PITCH;
    }

    public static int bonesLabelY(final int height, final Column column) {
        return buttonsTop() + animations(buttonsShown(height, column), column) + GAP;
    }

    public static int treeTop(final int height, final Column column) {
        return bonesLabelY(height, column) + LABEL;
    }

    /** How many rows of the tree of bones fit above the detail. */
    public static int treeRows(final int height, final Column column) {
        return Math.max(0, (detailsTop(height, column) - GAP - treeTop(height, column)) / TREE_ROW);
    }

    /** The top of the lines on the detail. */
    public static int detailsTop(final int height, final Column column) {
        return switchesTop(height, column) - GAP - details(detailsShown(height, column)) + (detailsShown(height,
                column) > 0 ? GAP : 0);
    }

    /** Where line {@code index} of those shown on the detail sits; the bar's room comes after the triangles'. */
    public static int detailY(final int height, final Column column, final int index) {
        return detailsTop(height, column) + index * DETAIL_ROW + (index > barLine(height, column) ? BAR : 0);
    }

    /** The top of the bar of the triangles, under their line. */
    public static int barY(final int height, final Column column) {
        return detailY(height, column, barLine(height, column)) + DETAIL_ROW - 2;
    }

    /** The top of the rows of switches. */
    public static int switchesTop(final int height, final Column column) {
        return height - BOTTOM - 4 - CONTROL - (rows(column.switches()) - 1) * SWITCH_PITCH;
    }

    /** Where switch {@code index} sits, two to a row. */
    public static int switchX(final int width, final int index) {
        return columnX(width) + (index % 2) * (COLUMN / 2);
    }

    public static int switchY(final int height, final Column column, final int index) {
        return switchesTop(height, column) + (index / 2) * SWITCH_PITCH;
    }

    public static int bottomY(final int height) {
        return height - BOTTOM;
    }

    public static int controlY(final int height) {
        return bottomY(height) + (BOTTOM - CONTROL) / 2;
    }

    public static int timelineX() {
        return MARGIN + NAME_WIDTH + 4;
    }

    public static int timelineWidth(final int width) {
        return speedX(width) - 6 - timelineX();
    }

    public static int speedX(final int width) {
        return width - MARGIN - SPEED_WIDTH;
    }

    /** The room the title has, at the left of the file and when the models were read. */
    public static int titleRoom(final int width) {
        return width - RELOADED_WIDTH - 3 * MARGIN;
    }

    /**
     * The preview on a screen that wide and tall, holding {@code column}. The column's lines are laid as solids, as
     * nothing in it is meant to stand on anything else.
     */
    public static GuiLayout layout(final int width, final int height, final Column column) {
        final GuiLayout layout = new GuiLayout(width, height);
        final int x = columnX(width);
        layout.text("title", MARGIN, (TITLE_HEIGHT - 9) / 2, titleRoom(width) / 6, 1.0F);
        layout.text("reloaded", width - MARGIN - RELOADED_WIDTH, (TITLE_HEIGHT - 7) / 2, RELOADED_WIDTH * 4 / 18,
                0.75F);
        layout.box("view", viewX(), viewY(), viewWidth(width), viewHeight(height));
        layout.box("animations", x, animationsLabelY(), COLUMN, TEXT_HEIGHT);
        if (column.animations() == 0) {
            layout.box("no_animations", x + 4, buttonsTop(), COLUMN - 4, SMALL_HEIGHT);
        }
        for (int i = 0; i < buttonsShown(height, column); i++) {
            layout.box("animation_" + i, x, buttonY(i), COLUMN, CONTROL);
        }
        layout.box("bones", x, bonesLabelY(height, column), COLUMN, TEXT_HEIGHT);
        for (int row = 0; row < treeRows(height, column); row++) {
            final int y = treeTop(height, column) + row * TREE_ROW;
            layout.box("bone_" + row, x + 4, y, COLUMN - 4 - COUNT_WIDTH, TREE_ROW);
            layout.box("count_" + row, x + COLUMN - COUNT_WIDTH, y, COUNT_WIDTH, TREE_ROW);
        }
        final int shown = detailsShown(height, column);
        for (int line = 0; line < shown; line++) {
            layout.box("detail_" + line, x, detailY(height, column, line), COLUMN, SMALL_HEIGHT);
        }
        if (shown > 0) {
            layout.box("bar", x, barY(height, column), COLUMN - 2, BAR_HEIGHT);
        }
        for (int i = 0; i < column.switches(); i++) {
            layout.box("switch_" + i, switchX(width, i), switchY(height, column, i), COLUMN / 2 - 2, CONTROL);
        }
        layout.text("name", MARGIN, controlY(height) + 6, NAME_WIDTH / 6, 1.0F);
        layout.box("timeline", timelineX(), controlY(height), timelineWidth(width), CONTROL);
        layout.box("speed", speedX(width), controlY(height), SPEED_WIDTH, CONTROL);
        return layout;
    }

    /* The room of the column from the animations' buttons down to the switches. */
    private static int room(final int height, final Column column) {
        return switchesTop(height, column) - GAP - buttonsTop();
    }

    /* The height of the animations' part under its label: their buttons, or the line that says there are none. */
    private static int animations(final int buttons, final Column column) {
        return column.animations() == 0 ? NONE_ROW : buttons * BUTTON_PITCH;
    }

    /* The least the tree of bones takes: its label and a few rows, with the gaps round them. */
    private static int leastTree() {
        return GAP + LABEL + FEWEST_ROWS * TREE_ROW + GAP;
    }

    /* The height the detail's lines take, with the bar and the gap above them. */
    private static int details(final int lines) {
        return lines == 0 ? 0 : GAP + lines * DETAIL_ROW + BAR;
    }

    /* Which of the lines shown on the detail the bar is under: the triangles', after the hint if it is shown. */
    private static int barLine(final int height, final Column column) {
        return hintShown(height, column) ? 1 : 0;
    }

    private static int rows(final int switches) {
        return Math.max(1, (switches + 1) / 2);
    }

    /**
     * What the column holds, as the screen counts it.
     *
     * @param animations the model's animations
     * @param switches   the switches shown, as the model has what they show
     * @param details    the lines on the model's detail, the hint among them if there is one
     * @param hint       whether the first of those lines is a hint, the first let go on a short screen
     */
    public record Column(int animations, int switches, int details, boolean hint) {
    }
}
