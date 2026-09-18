/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * The size a machine's own words are written at, and the one place that decides it.
 *
 * <p>A self-test, a system reading its start out and a text-mode installer are walls of text, and the machines
 * that printed them fitted far more on a screen than the game's font does whole. At three quarters a wall reads
 * as a machine's own output rather than as captions laid over it, and a self-test that lists a board, a video
 * card and four drives fits on the glass instead of being cut off at the bottom.
 *
 * <p>Here rather than on the screens because the frames an installer is drawn in need it too, and a rule with
 * two copies is a rule that drifts.
 */
public final class TextWall {

    /** How large those words are drawn, against the game's font at full size. */
    public static final float SCALE = 0.75f;

    /** The step from one line of such a wall to the next, which is that font's height at that size. */
    public static final int ROW = 8;

    private TextWall() {
    }

    /** Draws one line with its top left corner at {@code (x, y)}. */
    public static void draw(final GuiGraphics g, final Font font, final String line, final int x, final int y,
                            final int color) {
        if (line.isEmpty()) {
            return;
        }
        g.pose().pushPose();
        g.pose().translate(x, y, 0.0F);
        g.pose().scale(SCALE, SCALE, 1.0F);
        g.drawString(font, line, 0, 0, color, false);
        g.pose().popPose();
    }

    /** The same, centred on {@code cx}. */
    public static void centered(final GuiGraphics g, final Font font, final String line, final int cx,
                                final int y, final int color) {
        draw(g, font, line, cx - width(font, line) / 2, y, color);
    }

    /** The same, ending at {@code right} rather than starting at a left edge. */
    public static void right(final GuiGraphics g, final Font font, final String line, final int right,
                             final int y, final int color) {
        draw(g, font, line, right - width(font, line), y, color);
    }

    /** How wide that line comes out at this size, in screen pixels. */
    public static int width(final Font font, final String line) {
        return Math.round(font.width(line) * SCALE);
    }

    /** How much of the font's own width fits in that many screen pixels, for measuring against a box. */
    public static int room(final int pixels) {
        return Math.round(pixels / SCALE);
    }
}
