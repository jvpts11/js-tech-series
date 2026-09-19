/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

import dev.jstech.computers.gui.layout.CdeFrontPanelLayout.Rect;
import dev.jstech.core.gui.layout.GuiLayout;

/**
 * Where CDE's Exit dialog puts things: a window in the middle of the desktop, what it has to say centred in it,
 * and the three ways on in a row along its foot, the one a bare Enter takes first.
 *
 * <p>The drawing and the click handling both ask here, so the button a player sees and the one they hit are the
 * same rectangle.
 */
public final class CdeExitLayout {

    /** The three ways on, left to right. */
    public static final int SHUT_DOWN = 0;
    public static final int RESTART = 1;
    public static final int CANCEL = 2;
    public static final int BUTTONS = 3;

    public static final int W = 220;
    public static final int H = 92;
    public static final int TITLE_H = 14;

    /** The most lines the dialog says, and how far apart they stand. */
    public static final int LINES = 3;
    public static final int LINE_H = 10;

    private static final int TEXT_Y = TITLE_H + 8;
    /** The last line is a warning about the others, so it stands a little apart from them. */
    private static final int WARNING_GAP = 4;
    private static final int BUTTON_W = 60;
    private static final int BUTTON_H = 14;
    private static final int BUTTON_GAP = 10;
    private static final int FOOT_PAD = 10;

    private CdeExitLayout() {
    }

    /** The whole dialog, centred on a desktop that size. */
    public static Rect dialog(final int sw, final int sh) {
        return new Rect((sw - W) / 2, (sh - H) / 2, W, H);
    }

    /** The top of line {@code index} of what the dialog says, counted from nought. */
    public static int lineY(final int index, final int sw, final int sh) {
        return dialog(sw, sh).y() + TEXT_Y + index * LINE_H + (index == LINES - 1 ? WARNING_GAP : 0);
    }

    /** One of the three buttons along the foot. */
    public static Rect button(final int index, final int sw, final int sh) {
        final Rect dialog = dialog(sw, sh);
        final int row = BUTTONS * BUTTON_W + (BUTTONS - 1) * BUTTON_GAP;
        return new Rect(dialog.x() + (W - row) / 2 + index * (BUTTON_W + BUTTON_GAP),
                dialog.y() + H - FOOT_PAD - BUTTON_H, BUTTON_W, BUTTON_H);
    }

    /** The button under that point, or -1 when it is on none. */
    public static int buttonAt(final double px, final double py, final int sw, final int sh) {
        for (int i = 0; i < BUTTONS; i++) {
            if (button(i, sw, sh).holds(px, py)) {
                return i;
            }
        }
        return -1;
    }

    /** The dialog as solids that may not overlap: its lines as wide as the room they have, and its buttons. */
    public static GuiLayout layout(final int sw, final int sh) {
        final GuiLayout l = new GuiLayout(sw, sh);
        final Rect dialog = dialog(sw, sh);
        for (int i = 0; i < LINES; i++) {
            l.box("line_" + i, dialog.x() + FOOT_PAD, lineY(i, sw, sh), W - FOOT_PAD * 2, LINE_H - 1);
        }
        for (int i = 0; i < BUTTONS; i++) {
            final Rect r = button(i, sw, sh);
            // The default button wears a ring two pixels out, which must clear its neighbours as well.
            l.box("button_" + i, r.x() - 2, r.y() - 2, r.w() + 4, r.h() + 4);
        }
        return l;
    }
}
