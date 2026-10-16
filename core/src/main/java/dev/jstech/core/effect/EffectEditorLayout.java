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
 * Where everything stands on the card of the effects editor, as the approved design lays it out beside the block the
 * effect plays on: the effect's name and file at its top, its emitters each with a box to turn it on and off, the
 * chosen emitter's four sliders, what moves it, and Save and Done at its foot. The card stands at the right of the
 * screen; on a short screen its rows are drawn closer, and the emitters' list shows as many as fit. Pure: the screen
 * reads its places from here, and a test checks nothing overlaps.
 *
 * @param x       the card's left, on the screen
 * @param y       the card's top
 * @param height  the card's height
 * @param pitch   how far apart its rows are
 * @param shown   how many of the emitters the list shows
 */
public record EffectEditorLayout(int x, int y, int height, int pitch, int shown) {

    /** The card's width, and how far it stands from the screen's top and right. */
    public static final int WIDTH = 190;
    public static final int MARGIN = 8;
    public static final int RIGHT_MARGIN = 6;
    /** The rows' pitch as designed, and the closest they are drawn. */
    public static final int PITCH = 22;
    public static final int LEAST_PITCH = 18;
    /** The words' places at the card's top. */
    public static final int TITLE_Y = 6;
    public static final int FILE_Y = 17;
    public static final int EMITTERS_Y = 28;
    public static final int LIST_Y = 40;
    public static final int INSET = 7;
    public static final int SLIDERS = 4;
    public static final int MOVES = 3;
    public static final int HEADING_GAP = 4;
    public static final int HEADING_HEIGHT = 12;
    public static final int BUTTON_WIDTH = 86;
    public static final int BUTTON_HEIGHT = 20;
    public static final int BUTTON_GAP = 4;
    public static final int BUTTONS_GAP = 8;
    public static final int FOOT = 12;

    /** The card for an effect of {@code emitters} emitters on a screen {@code width} by {@code height}. */
    public static EffectEditorLayout of(final int emitters, final int width, final int height) {
        final int room = height - 2 * MARGIN;
        for (int pitch = PITCH; pitch >= LEAST_PITCH; pitch--) {
            final int fixed = fixed(pitch);
            final int rows = Math.max(1, Math.min(emitters, (room - fixed) / pitch));
            if (fixed + rows * pitch <= room || pitch == LEAST_PITCH) {
                return new EffectEditorLayout(width - WIDTH - RIGHT_MARGIN, MARGIN, fixed + rows * pitch, pitch,
                        rows);
            }
        }
        throw new IllegalStateException("unreachable");
    }

    /** The top of emitter row {@code row} of those shown, on the screen. */
    public int row(final int row) {
        return this.y + LIST_Y + row * this.pitch;
    }

    /** The top of the chosen emitter's heading. */
    public int heading() {
        return row(this.shown) + HEADING_GAP;
    }

    /** The top of slider {@code index}. */
    public int slider(final int index) {
        return heading() + HEADING_HEIGHT + index * this.pitch;
    }

    /** The top of "Moved by". */
    public int movedBy() {
        return slider(SLIDERS) + HEADING_GAP;
    }

    /** The top of what-moves box {@code index}. */
    public int move(final int index) {
        return movedBy() + HEADING_HEIGHT + index * this.pitch;
    }

    /** The top of the Save and Done buttons. */
    public int buttons() {
        return move(MOVES) + BUTTONS_GAP;
    }

    /** A widget's height: its row's, less a gap. */
    public int widgetHeight() {
        return this.pitch - 2;
    }

    /** The card's places, as a layout a test can check. */
    public GuiLayout layout() {
        final GuiLayout layout = new GuiLayout(WIDTH, this.height);
        final int left = INSET;
        final int inner = WIDTH - 2 * INSET;
        layout.text("title", left, TITLE_Y, inner / 6, 1.0F);
        layout.text("file", left, FILE_Y, inner / 5, 0.75F);
        layout.text("emitters", left, EMITTERS_Y, inner / 6, 1.0F);
        for (int i = 0; i < this.shown; i++) {
            layout.box("emitter" + i, left + 1, row(i) - this.y, inner, widgetHeight());
        }
        layout.text("heading", left, heading() - this.y, inner / 6, 1.0F);
        for (int i = 0; i < SLIDERS; i++) {
            layout.box("slider" + i, left, slider(i) - this.y, inner, widgetHeight());
        }
        layout.text("movedBy", left, movedBy() - this.y, inner / 6, 1.0F);
        for (int i = 0; i < MOVES; i++) {
            layout.box("move" + i, left + 1, move(i) - this.y, inner, widgetHeight());
        }
        layout.box("save", left, buttons() - this.y, BUTTON_WIDTH, BUTTON_HEIGHT);
        layout.box("done", left + BUTTON_WIDTH + BUTTON_GAP, buttons() - this.y, BUTTON_WIDTH, BUTTON_HEIGHT);
        return layout;
    }

    /* The card's height but for its emitters' rows, at {@code pitch}. */
    private static int fixed(final int pitch) {
        return LIST_Y + HEADING_GAP + HEADING_HEIGHT + SLIDERS * pitch + HEADING_GAP + HEADING_HEIGHT + MOVES * pitch
                + BUTTONS_GAP + BUTTON_HEIGHT + FOOT;
    }
}
