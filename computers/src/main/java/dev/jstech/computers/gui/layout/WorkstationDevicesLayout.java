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
 * Where CDE's Devices dialog, opened from Workstation Info, puts things: one sunken well headed Devices with a fact
 * per port, labels right-aligned against their values as in Workstation Info, and Disable, Enable and Close along the
 * foot.
 *
 * <p>Everything is measured from the top left of the dialog's content.
 */
public final class WorkstationDevicesLayout {

    /** The content, which the window adds its frame and title bar around. */
    public static final int W = WorkstationInfoLayout.W;
    public static final int FRAME_W = WorkstationInfoLayout.FRAME_W;
    public static final int FRAME_H = WorkstationInfoLayout.FRAME_H;
    public static final int ROW_H = WorkstationInfoLayout.ROW_H;

    /** How many devices the well shows at once; a Vintage workstation has four, a later one with hubs more. */
    public static final int ROWS = 14;

    public static final int LABEL_RIGHT = WorkstationInfoLayout.LABEL_RIGHT;
    public static final int VALUE_X = WorkstationInfoLayout.VALUE_X;

    private static final int PAD = 4;
    private static final int INNER = 5;
    private static final int EDGE = 3;
    private static final int HEAD_H = 8;
    private static final int BUTTON_W = 48;
    private static final int BUTTON_H = 14;
    private static final int BUTTON_GAP = 6;

    /** The well, and the whole content under it with the buttons. */
    public static final int WELL_H = EDGE + HEAD_H + ROWS * ROW_H + EDGE;
    public static final int H = PAD + WELL_H + 5 + BUTTON_H + PAD;

    private WorkstationDevicesLayout() {
    }

    /** The well. */
    public static Rect well() {
        return new Rect(PAD, PAD, W - PAD * 2, WELL_H);
    }

    /** Where the heading is written. */
    public static int headY() {
        return PAD + EDGE;
    }

    /** Where device {@code row} of those shown is written. */
    public static int rowY(final int row) {
        return PAD + EDGE + HEAD_H + row * ROW_H;
    }

    /** Where the heading and the labels begin. */
    public static int innerX() {
        return PAD + INNER;
    }

    /** How wide a value may run before it would leave the well. */
    public static int valueWidth() {
        return W - PAD - INNER - VALUE_X;
    }

    /** Disable, the first of the three buttons centred along the foot. */
    public static Rect disable() {
        return button(0);
    }

    /** Enable, the second. */
    public static Rect enable() {
        return button(1);
    }

    /** Close, the last, which is the default. */
    public static Rect close() {
        return button(2);
    }

    /** The dialog as solids that may not overlap: the well and the buttons. */
    public static GuiLayout layout() {
        final GuiLayout l = new GuiLayout(W, H);
        final Rect well = well();
        l.box("well", well.x(), well.y(), well.w(), well.h());
        final Rect disable = disable();
        l.box("disable", disable.x(), disable.y(), disable.w(), disable.h());
        final Rect enable = enable();
        l.box("enable", enable.x(), enable.y(), enable.w(), enable.h());
        final Rect close = close();
        // The default button wears a ring two pixels out, which must clear the well above it too.
        l.box("close", close.x() - 2, close.y() - 2, close.w() + 4, close.h() + 4);
        return l;
    }

    /* The {@code index}-th of the three buttons along the foot, the row of them centred. */
    private static Rect button(final int index) {
        final int left = (W - 3 * BUTTON_W - 2 * BUTTON_GAP) / 2;
        return new Rect(left + index * (BUTTON_W + BUTTON_GAP), H - PAD - BUTTON_H, BUTTON_W, BUTTON_H);
    }
}
