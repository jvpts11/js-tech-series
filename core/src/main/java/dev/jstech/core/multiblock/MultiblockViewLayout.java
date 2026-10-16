/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock;

import dev.jstech.core.gui.layout.GuiLayout;
import dev.jstech.core.text.TextKey;

/**
 * Where everything of a multiblock shown in a recipe viewer stands, as the approved design lays it out, in pixels
 * from the recipe's top left: the 3D preview with its name and its hint on the left; on the right the layer's buttons,
 * the choices of a place where more than one block will do, and how it may stand; under them its materials with how
 * many of each, its count and its size; at the foot the buttons that show it in the world and open its page. Pure, so
 * JEI and EMI draw it alike and its layout is tested without the game.
 */
public final class MultiblockViewLayout {

    public static final int WIDTH = 186;
    public static final int HEIGHT = 186;
    /** A slot's side, border included. */
    public static final int SLOT = 18;
    public static final int PREVIEW_WIDTH = 120;
    public static final int PREVIEW_HEIGHT = 112;
    /** Where the preview's name and its hint stand inside it. */
    public static final int PREVIEW_INSET = 3;
    public static final int HINT_Y = PREVIEW_HEIGHT - 10;
    public static final int SIDE_X = 124;
    public static final int LAYER_LABEL_Y = 2;
    public static final int BUTTON_WIDTH = 29;
    public static final int BUTTON_HEIGHT = 13;
    public static final int BUTTONS_Y = 12;
    /** The most layers with a button each; a taller multiblock steps through its layers with two. */
    public static final int MOST_LAYER_BUTTONS = 5;
    public static final int CHOICE_LABEL_Y = 59;
    public static final int CHOICES_Y = 78;
    /** The most blocks of a choice shown side by side. */
    public static final int MOST_CHOICES = 3;
    public static final int TURNS_Y = 99;
    public static final int LINE = 9;
    /** The most lines that say how it may stand. */
    public static final int TURN_LINES = 3;
    public static final int MATERIALS_LABEL_Y = 116;
    public static final int MATERIALS_Y = 126;
    public static final int MATERIALS_PER_ROW = 6;
    /** The most materials shown, in two rows. */
    public static final int MOST_MATERIALS = 2 * MATERIALS_PER_ROW;
    public static final int COUNT_X = 112;
    public static final int BLOCKS_Y = 131;
    public static final int SIZE_Y = 141;
    public static final int FOOT_Y = 167;
    public static final int FOOT_WIDTH = 89;
    public static final int FOOT_HEIGHT = 19;
    public static final int PAGE_X = WIDTH - FOOT_WIDTH;
    /** How many layer buttons stand in a row. */
    private static final int BUTTON_COLUMNS = 2;
    private static final int BUTTON_GAP = 4;
    private static final int BUTTON_PITCH = 15;
    /* The stepping buttons' places: the layer shown, the one below, the one above. */
    private static final int STEP_SHOWN = 1;
    private static final int STEP_DOWN = 2;
    private static final int STEP_UP = 3;

    private MultiblockViewLayout() {
    }

    /** How many layer buttons a multiblock of {@code layers} layers has: "All" and one each, or "All" and three. */
    public static int layerButtons(final int layers) {
        return stepping(layers) ? STEP_UP + 1 : layers + 1;
    }

    /** Whether a multiblock of {@code layers} layers steps through them rather than having a button for each. */
    public static boolean stepping(final int layers) {
        return layers > MOST_LAYER_BUTTONS;
    }

    /** Where layer button {@code index} stands, "All" the first, two to a row. */
    public static Point layerButton(final int index) {
        return new Point(SIDE_X + index % BUTTON_COLUMNS * (BUTTON_WIDTH + BUTTON_GAP),
                BUTTONS_Y + index / BUTTON_COLUMNS * BUTTON_PITCH);
    }

    /** The layer button at a point, or -1 for none. */
    public static int layerButtonAt(final double x, final double y, final int layers) {
        for (int index = 0; index < layerButtons(layers); index++) {
            final Point at = layerButton(index);
            if (x >= at.x() && x < at.x() + BUTTON_WIDTH && y >= at.y() && y < at.y() + BUTTON_HEIGHT) {
                return index;
            }
        }
        return -1;
    }

    /**
     * The layer shown once layer button {@code index} is pressed while {@code shown} is (0 for every layer): its own
     * layer, or for a multiblock that steps, the one below or above, from every layer to the first or the last.
     */
    public static int pressed(final int index, final int shown, final int layers) {
        if (index == 0) {
            return 0;
        }
        if (!stepping(layers)) {
            return index;
        }
        return switch (index) {
            case STEP_DOWN -> shown <= 1 ? layers : shown - 1;
            case STEP_UP -> shown >= layers ? 1 : shown + 1;
            default -> shown;
        };
    }

    /** Whether layer button {@code index} stands for the layer shown, {@code shown} (0 for every layer). */
    public static boolean lit(final int index, final int shown, final int layers) {
        if (index == 0) {
            return shown == 0;
        }
        return stepping(layers) ? index == STEP_SHOWN && shown > 0 : index == shown;
    }

    /** Where block {@code index} of the choice shown stands. */
    public static Point choice(final int index) {
        return new Point(SIDE_X + index * SLOT, CHOICES_Y);
    }

    /** Where material {@code index} stands, six to a row. */
    public static Point material(final int index) {
        return new Point(index % MATERIALS_PER_ROW * SLOT, MATERIALS_Y + index / MATERIALS_PER_ROW * SLOT);
    }

    /** Whether a point is on the preview. */
    public static boolean onPreview(final double x, final double y) {
        return x >= 0 && x < PREVIEW_WIDTH && y >= 0 && y < PREVIEW_HEIGHT;
    }

    /** Whether a point is on the button that shows it in the world. */
    public static boolean onShow(final double x, final double y) {
        return onFoot(x, y, 0);
    }

    /** Whether a point is on the button that opens its page. */
    public static boolean onPage(final double x, final double y) {
        return onFoot(x, y, PAGE_X);
    }

    /** The layout of the most it shows, its longest words in, for the test that nothing on it overlaps. */
    public static GuiLayout layout() {
        final GuiLayout layout = new GuiLayout(WIDTH, HEIGHT);
        layout.box("preview", 0, 0, PREVIEW_WIDTH, PREVIEW_HEIGHT);
        for (int index = 0; index <= MOST_LAYER_BUTTONS; index++) {
            final Point at = layerButton(index);
            layout.box("layer" + index, at.x(), at.y(), BUTTON_WIDTH, BUTTON_HEIGHT);
        }
        for (int index = 0; index < MOST_CHOICES; index++) {
            final Point at = choice(index);
            layout.box("choice" + index, at.x(), at.y(), SLOT, SLOT);
        }
        for (int index = 0; index < MOST_MATERIALS; index++) {
            final Point at = material(index);
            layout.box("material" + index, at.x(), at.y(), SLOT, SLOT);
        }
        layout.box("show", 0, FOOT_Y, FOOT_WIDTH, FOOT_HEIGHT);
        layout.box("page", PAGE_X, FOOT_Y, FOOT_WIDTH, FOOT_HEIGHT);
        layout.text("hint", PREVIEW_INSET, HINT_Y, chars(MultiblockViewTexts.CLICK), 1.0F);
        layout.text("layerWord", SIDE_X, LAYER_LABEL_Y, chars(MultiblockViewTexts.LAYER), 1.0F);
        layout.text("places", SIDE_X, CHOICE_LABEL_Y, chars(MultiblockViewTexts.PLACES), 1.0F);
        layout.text("either", SIDE_X, CHOICE_LABEL_Y + LINE, chars(MultiblockViewTexts.EITHER), 1.0F);
        layout.text("turns", SIDE_X, TURNS_Y, chars(MultiblockViewTexts.TURNS_AND), 1.0F);
        layout.text("mirrors", SIDE_X, TURNS_Y + LINE, chars(MultiblockViewTexts.NO_MIRROR), 1.0F);
        layout.text("lies", SIDE_X, TURNS_Y + 2 * LINE, chars(MultiblockViewTexts.LIES_DOWN), 1.0F);
        layout.text("materials", 0, MATERIALS_LABEL_Y, chars(MultiblockViewTexts.MATERIALS), 1.0F);
        layout.text("blocks", COUNT_X, BLOCKS_Y, chars(MultiblockViewTexts.BLOCKS) + 1, 1.0F);
        layout.text("size", COUNT_X, SIZE_Y, chars(MultiblockViewTexts.SIZE), 1.0F);
        return layout;
    }

    private static boolean onFoot(final double x, final double y, final int left) {
        return x >= left && x < left + FOOT_WIDTH && y >= FOOT_Y && y < FOOT_Y + FOOT_HEIGHT;
    }

    /* How many letters a key's English holds, a placeholder counted as two. */
    private static int chars(final TextKey key) {
        return key.english().replace("%s", "99").length();
    }

    /**
     * A corner, of a slot or a button.
     *
     * @param x across from the recipe's left edge
     * @param y down from its top edge
     */
    public record Point(int x, int y) {
    }
}
