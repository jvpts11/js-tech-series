/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * The small marks a system's controls are told apart by, drawn the way each system drew them: a radio button is a
 * round well on Frames and the Linux desktops and a diamond on CDE; a tree opens with a boxed plus on the older
 * systems and a chevron on the flat ones; a slider's thumb is a raised block, or a dot of the accent where the system
 * is flat. Everything else is the skin's own primitives, so these are only the marks.
 */
final class SigmaGlyphs {

    /** How big a radio button or a check box is. */
    static final int MARK = 9;
    /** How wide the arrow button at the end of a combo box is, and a number box's pair of steppers. */
    static final int ARROW_W = 12;

    /* A round mark as rows of a nine-pixel disc: where each row starts and how long it is. */
    private static final int[] DISC_FROM = {3, 1, 1, 0, 0, 0, 1, 1, 3};
    private static final int[] DISC_LONG = {3, 7, 7, 9, 9, 9, 7, 7, 3};
    private static final int[] DOT_FROM = {1, 0, 0, 1};
    private static final int[] DOT_LONG = {2, 4, 4, 2};

    private SigmaGlyphs() {
    }

    /** A radio button, filled when it is the one picked. */
    static void radio(final GuiGraphics g, final OsSkin skin, final int x, final int y, final boolean on) {
        if (skin.form() == OsSkin.Form.MOTIF) {
            diamond(g, x, y, MARK, skin.edge());
            diamond(g, x + 1, y + 1, MARK - 2, on ? skin.accent() : skin.fieldBg());
            return;
        }
        disc(g, x, y, skin.form() == OsSkin.Form.FLAT && on ? skin.accent() : skin.edge());
        for (int row = 1; row < MARK - 1; row++) {
            final int from = DISC_FROM[row] + 1;
            g.fill(x + from, y + row, x + MARK - from, y + row + 1,
                    skin.form() == OsSkin.Form.FLAT && on ? skin.accent() : skin.fieldBg());
        }
        if (on) {
            final int dot = skin.form() == OsSkin.Form.FLAT ? skin.fieldBg() : skin.text();
            for (int row = 0; row < DOT_FROM.length; row++) {
                g.fill(x + 2 + DOT_FROM[row] + 1, y + 2 + row + 1, x + 2 + DOT_FROM[row] + 1 + DOT_LONG[row],
                        y + 3 + row + 1, dot);
            }
        }
    }

    /** A check box, its tick drawn when it is ticked. */
    static void check(final GuiGraphics g, final OsSkin skin, final int x, final int y, final boolean on) {
        final boolean filled = on && (skin.form() == OsSkin.Form.FLAT || skin.form() == OsSkin.Form.MOTIF);
        g.fill(x, y, x + MARK, y + MARK, skin.edge());
        g.fill(x + 1, y + 1, x + MARK - 1, y + MARK - 1, filled ? skin.accent() : skin.fieldBg());
        if (on && skin.form() != OsSkin.Form.MOTIF) {
            final int ink = filled ? skin.fieldBg() : skin.text();
            // A tick: down two from the left, then up four to the right.
            for (int i = 0; i < 3; i++) {
                g.fill(x + 2 + i, y + 4 + i, x + 3 + i, y + 6 + i, ink);
            }
            for (int i = 0; i < 3; i++) {
                g.fill(x + 5 + i, y + 5 - i, x + 6 + i, y + 7 - i, ink);
            }
        }
    }

    /** The arrow at the end of a combo box or on a stepper, down or up, in a button where the system has one. */
    static void arrowButton(final GuiGraphics g, final Font font, final OsSkin skin, final int x, final int y,
                            final int w, final int h, final boolean down, final boolean pressed) {
        if (skin.form() != OsSkin.Form.FLAT) {
            skin.button(g, font, x, y, w, h, "", false, pressed, false);
        }
        triangle(g, x + w / 2, y + h / 2 + (down ? -1 : 1), down, Math.max(2, Math.min(w, h) / 4),
                skin.form() == OsSkin.Form.FLAT ? skin.dim() : skin.text());
    }

    /** A slider's track and thumb, the thumb where {@code fraction} of the way along puts it. */
    static void slider(final GuiGraphics g, final Font font, final OsSkin skin, final int x, final int y,
                       final int w, final int h, final double fraction, final boolean ticks) {
        final int middle = y + h / 2;
        final int thumbW = skin.form() == OsSkin.Form.FLAT ? 8 : 6;
        final int travel = Math.max(1, w - thumbW);
        final int thumbX = x + (int) Math.round(travel * Math.clamp(fraction, 0, 1));
        if (skin.form() == OsSkin.Form.FLAT) {
            g.fill(x, middle - 1, x + w, middle + 1, skin.edge());
            g.fill(x, middle - 1, thumbX + thumbW / 2, middle + 1, skin.accent());
            g.fill(thumbX, middle - 4, thumbX + thumbW, middle + 4, skin.accent());
            g.fill(thumbX + 2, middle - 2, thumbX + thumbW - 2, middle + 2, skin.fieldBg());
            return;
        }
        skin.field(g, x, middle - 2, w, 4, false);
        if (ticks) {
            for (int i = 0; i <= 4; i++) {
                final int at = x + thumbW / 2 + travel * i / 4;
                g.fill(at, middle + 6, at + 1, middle + 8, skin.dim());
            }
        }
        skin.button(g, font, thumbX, middle - 6, thumbW, 12, "", false, false, false);
    }

    /** What opens or closes a node of a tree: a boxed plus or minus on older systems, a chevron on the flat ones. */
    static void expander(final GuiGraphics g, final OsSkin skin, final int x, final int y, final boolean open) {
        if (skin.form() == OsSkin.Form.FLAT) {
            if (open) {
                triangle(g, x + 4, y + 4, true, 2, skin.dim());
            } else {
                for (int i = 0; i < 3; i++) {
                    g.fill(x + 3 + i, y + 2 + i, x + 4 + i, y + 3 + i, skin.dim());
                    g.fill(x + 3 + i, y + 6 - i, x + 4 + i, y + 7 - i, skin.dim());
                }
            }
            return;
        }
        g.fill(x, y, x + MARK, y + MARK, skin.dim());
        g.fill(x + 1, y + 1, x + MARK - 1, y + MARK - 1, skin.fieldBg());
        g.fill(x + 2, y + 4, x + MARK - 2, y + 5, skin.text());
        if (!open) {
            g.fill(x + 4, y + 2, x + 5, y + MARK - 2, skin.text());
        }
    }

    /** The small arrow beside a column header the table is ordered by. */
    static void sortArrow(final GuiGraphics g, final OsSkin skin, final int x, final int y) {
        triangle(g, x, y, false, 2, skin.dim());
    }

    /* A filled triangle centred on (cx, cy), {@code half} pixels from the middle to each corner of its base. */
    private static void triangle(final GuiGraphics g, final int cx, final int cy, final boolean down, final int half,
                                 final int colour) {
        for (int row = 0; row <= half; row++) {
            final int reach = down ? half - row : row;
            final int y = cy - half / 2 + row;
            g.fill(cx - reach, y, cx + reach + 1, y + 1, colour);
        }
    }

    private static void disc(final GuiGraphics g, final int x, final int y, final int colour) {
        for (int row = 0; row < MARK; row++) {
            g.fill(x + DISC_FROM[row], y + row, x + DISC_FROM[row] + DISC_LONG[row], y + row + 1, colour);
        }
    }

    private static void diamond(final GuiGraphics g, final int x, final int y, final int size, final int colour) {
        final int half = size / 2;
        for (int row = 0; row < size; row++) {
            final int reach = row <= half ? row : size - 1 - row;
            g.fill(x + half - reach, y + row, x + half + reach + 1, y + row + 1, colour);
        }
    }
}
