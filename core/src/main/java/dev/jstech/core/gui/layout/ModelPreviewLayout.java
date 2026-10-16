/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui.layout;

/**
 * Where everything on the preview of a model sits, on a screen of any size: the title across the top, with when the
 * models were last read at its right; the view of the model on the left, as big as the screen leaves it; the
 * animations, the tree of bones and the four switches (bones, locators, glow, wire) in a column on the right; the
 * timeline and the speed along the bottom. The screen places its widgets from these, and a test lays it out at the
 * smallest screen the game allows and checks nothing collides.
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
    /** The most animations shown at once; the list scrolls past them. */
    public static final int MOST_BUTTONS = 4;
    /** The fewest rows of bones shown. */
    public static final int FEWEST_ROWS = 3;
    /** The width of when the models were last read, at the title's right. */
    public static final int RELOADED_WIDTH = 96;
    /** The width of the animation's name at the left of the timeline. */
    public static final int NAME_WIDTH = 46;
    /** The speed button's width. */
    public static final int SPEED_WIDTH = 82;
    /** A label's height above its part of the column. */
    public static final int LABEL = 12;
    /** The smallest screen the game lays out, in its own units. */
    public static final int SMALLEST_WIDTH = 320;
    public static final int SMALLEST_HEIGHT = 240;

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

    /** How many animations' buttons are shown at once, of {@code count}, on a screen that tall. */
    public static int buttonsShown(final int height, final int count) {
        final int room = switchesTop(height) - buttonsTop() - LABEL - FEWEST_ROWS * TREE_ROW - 8;
        return Math.max(0, Math.min(Math.min(count, MOST_BUTTONS), room / BUTTON_PITCH));
    }

    public static int buttonY(final int index) {
        return buttonsTop() + index * BUTTON_PITCH;
    }

    public static int bonesLabelY(final int height, final int count) {
        return buttonsTop() + buttonsShown(height, count) * BUTTON_PITCH + 4;
    }

    public static int treeTop(final int height, final int count) {
        return bonesLabelY(height, count) + LABEL;
    }

    /** How many rows of the tree of bones fit above the switches. */
    public static int treeRows(final int height, final int count) {
        return Math.max(0, (switchesTop(height) - 4 - treeTop(height, count)) / TREE_ROW);
    }

    /** The top of the two rows of switches. */
    public static int switchesTop(final int height) {
        return height - BOTTOM - 4 - CONTROL - SWITCH_PITCH;
    }

    /** Where switch {@code index} (bones, locators, glow, wire) sits, two to a row. */
    public static int switchX(final int width, final int index) {
        return columnX(width) + (index % 2) * (COLUMN / 2);
    }

    public static int switchY(final int height, final int index) {
        return switchesTop(height) + (index / 2) * SWITCH_PITCH;
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

    /** The preview on a screen that wide and tall, with {@code count} animations. */
    public static GuiLayout layout(final int width, final int height, final int count) {
        final GuiLayout layout = new GuiLayout(width, height);
        layout.text("title", MARGIN, (TITLE_HEIGHT - 9) / 2, (width - RELOADED_WIDTH - 3 * MARGIN) / 6, 1.0F);
        layout.text("reloaded", width - MARGIN - RELOADED_WIDTH, (TITLE_HEIGHT - 7) / 2, RELOADED_WIDTH * 4 / 18,
                0.75F);
        layout.box("view", viewX(), viewY(), viewWidth(width), viewHeight(height));
        layout.text("animations", columnX(width), animationsLabelY(), COLUMN / 6, 1.0F);
        for (int i = 0; i < buttonsShown(height, count); i++) {
            layout.box("animation_" + i, columnX(width), buttonY(i), COLUMN, CONTROL);
        }
        layout.text("bones", columnX(width), bonesLabelY(height, count), COLUMN / 6, 1.0F);
        for (int row = 0; row < treeRows(height, count); row++) {
            layout.text("bone_" + row, columnX(width) + 4, treeTop(height, count) + row * TREE_ROW,
                    (COLUMN - 4) * 4 / 18, 0.75F);
        }
        for (int i = 0; i < 4; i++) {
            layout.box("switch_" + i, switchX(width, i), switchY(height, i), COLUMN / 2 - 2, CONTROL);
        }
        layout.text("name", MARGIN, controlY(height) + 6, NAME_WIDTH / 6, 1.0F);
        layout.box("timeline", timelineX(), controlY(height), timelineWidth(width), CONTROL);
        layout.box("speed", speedX(width), controlY(height), SPEED_WIDTH, CONTROL);
        return layout;
    }
}
