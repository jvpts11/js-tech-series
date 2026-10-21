/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.studio;

import dev.jstech.core.client.gui.component.Draw;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * The studio's marks: a tick for a part that is right, a box with "!" for a warning or an error, and the small
 * triangle of a row of the tree that opens.
 */
final class StudioMarks {

    /** How wide and tall a mark is. */
    static final int WIDTH = 7;
    static final int HEIGHT = 9;

    /* The pixels of a tick, each two tall, left to right. */
    private static final int[] TICK = {4, 5, 6, 5, 4, 3, 2};
    private static final String SIGN = "!";
    /** How far a triangle reaches. */
    private static final int ARROW = 3;

    private StudioMarks() {
    }

    /** Draws the mark of {@code mark} with its top-left corner at {@code (x, y)}; none draws nothing. */
    static void draw(final GuiGraphics g, final Font font, final int x, final int y, final StudioSheet.Mark mark) {
        final StudioPalette.Colours colours = StudioPalette.get();
        switch (mark) {
            case NONE -> {
            }
            case FINE -> {
                for (int i = 0; i < TICK.length; i++) {
                    g.fill(x + i, y + TICK[i], x + i + 1, y + TICK[i] + 2, colours.fine());
                }
            }
            case WARNING -> sign(g, font, x, y, colours.warning(), colours.warningText());
            case ERROR -> sign(g, font, x, y, colours.error(), colours.errorText());
        }
    }

    /** Draws the triangle of a row that opens, pointing down when it is open, right when it is closed. */
    static void arrow(final GuiGraphics g, final int x, final int y, final boolean open, final int colour) {
        for (int i = 0; i < ARROW; i++) {
            if (open) {
                g.fill(x + i, y + i, x + 2 * ARROW - 1 - i, y + i + 1, colour);
            } else {
                g.fill(x + i, y + i, x + i + 1, y + 2 * ARROW - 1 - i, colour);
            }
        }
    }

    private static void sign(final GuiGraphics g, final Font font, final int x, final int y, final int box,
                             final int text) {
        g.fill(x, y, x + WIDTH, y + HEIGHT, box);
        Draw.text(g, font, SIGN, x + (WIDTH - font.width(SIGN)) / 2 + 1, y + 1, text, box);
    }
}
