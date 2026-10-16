/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.effect;

import dev.jstech.core.gui.layout.GuiLayout;

/**
 * Where everything stands on the card of a new effect, as the approved design lays it out beside the block looked at:
 * the block, where on it the effect starts, its file, the starting points to choose from, and Create. Pure: the
 * screen reads its places from here, and a test checks nothing overlaps.
 */
public final class NewEffectLayout {

    /** The card: its width, its height, and how far it stands from the screen's top and right. */
    public static final int WIDTH = EffectEditorLayout.WIDTH;
    public static final int HEIGHT = 226;
    public static final int MARGIN = EffectEditorLayout.MARGIN;
    public static final int RIGHT_MARGIN = EffectEditorLayout.RIGHT_MARGIN;
    public static final int INSET = 7;
    public static final int INNER = WIDTH - 2 * INSET;
    public static final int TITLE_Y = 6;
    public static final int SUBTITLE_Y = 17;
    public static final int BLOCK_WORD_Y = 27;
    public static final int BLOCK_Y = 34;
    public static final int FIELD_HEIGHT = 16;
    public static final int FROM_Y = 54;
    public static final int FROM_HEIGHT = 18;
    public static final int FILE_WORD_Y = 76;
    public static final int FILE_Y = 83;
    public static final int START_Y = 105;
    /** The starting points' list: its first row, each row's pitch, and how many there are. */
    public static final int LIST_Y = 118;
    public static final int LIST_PITCH = 12;
    public static final int STARTS = 6;
    public static final int CREATE_Y = 196;
    public static final int CREATE_HEIGHT = 20;

    private NewEffectLayout() {
    }

    /** The card's left on a screen {@code width} wide. */
    public static int x(final int width) {
        return width - WIDTH - RIGHT_MARGIN;
    }

    /** The card's places, as a layout a test can check. */
    public static GuiLayout layout() {
        final GuiLayout layout = new GuiLayout(WIDTH, HEIGHT);
        layout.text("title", INSET, TITLE_Y, INNER / 6, 1.0F);
        layout.text("subtitle", INSET, SUBTITLE_Y, INNER / 5, 0.75F);
        layout.text("blockWord", INSET, BLOCK_WORD_Y, INNER / 5, 0.75F);
        layout.box("block", INSET, BLOCK_Y, INNER, FIELD_HEIGHT);
        layout.box("from", INSET, FROM_Y, INNER, FROM_HEIGHT);
        layout.text("fileWord", INSET, FILE_WORD_Y, INNER / 5, 0.75F);
        layout.box("file", INSET, FILE_Y, INNER, FIELD_HEIGHT);
        layout.text("startFrom", INSET, START_Y, INNER / 6, 1.0F);
        for (int i = 0; i < STARTS; i++) {
            layout.box("start" + i, INSET, LIST_Y + i * LIST_PITCH - 2, INNER, LIST_PITCH);
        }
        layout.box("create", INSET, CREATE_Y, INNER, CREATE_HEIGHT);
        return layout;
    }
}
