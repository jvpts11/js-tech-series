/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

import dev.jstech.core.gui.layout.GuiLayout;

/**
 * Where the power strip sits on an opened monitor screen: a narrow strip outside the monitor's frame on its left, with
 * the Power button over the Restart button, square, in the style of the frame. Positions are relative to the strip's
 * own top left; {@link #stripX} and {@link #stripY} place it against the frame.
 */
public final class PowerStripLayout {

    public static final int W = 18;
    public static final int H = 42;
    /** The two square buttons, and how far they stand in from the strip's edges. */
    public static final int BUTTON = 14;
    public static final int PAD = 2;
    public static final int POWER_Y = 4;
    public static final int RESTART_Y = POWER_Y + BUTTON + 5;
    /** The air between the strip and the frame. */
    public static final int GAP = 2;
    /** How far below the frame's top edge the strip starts. */
    public static final int DOWN = 8;
    /** Where on the screen the strip may come closest to the edge, when the frame leaves no room beside it. */
    public static final int EDGE = 1;

    private PowerStripLayout() {
    }

    /** The strip's left edge, beside a frame whose left edge is {@code frameX}, never off the screen. */
    public static int stripX(final int frameX) {
        return Math.max(EDGE, frameX - GAP - W);
    }

    public static int stripY(final int frameY) {
        return frameY + DOWN;
    }

    /** The left edge of both buttons, relative to the strip. */
    public static int buttonX() {
        return (W - BUTTON) / 2;
    }

    /** The strip as solids: its body and the two buttons inside it. */
    public static GuiLayout layout() {
        final GuiLayout l = new GuiLayout(W, H);
        l.box("power", buttonX(), POWER_Y, BUTTON, BUTTON);
        l.box("restart", buttonX(), RESTART_Y, BUTTON, BUTTON);
        return l;
    }
}
