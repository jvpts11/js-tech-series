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
import java.util.List;

/**
 * Where CDE's Workstation Info puts things inside its window: a sunken well for each group of facts, one under
 * the other, each headed by its name with its facts below it, labels right-aligned against their values, and the
 * one button along the foot.
 *
 * <p>Everything is measured from the top left of the window's content. The value column is as wide as a network's
 * id written in the small text, which is the longest fact the window has to show on one line.
 */
public final class WorkstationInfoLayout {

    /** The content, which the window adds its frame and title bar around. */
    public static final int W = 284;
    public static final int H = 163;
    public static final int FRAME_W = 8;
    public static final int FRAME_H = 22;

    public static final int ROW_H = 8;

    /** Where the labels end, right-aligned, and where the values begin; the longest label is Operating System. */
    public static final int LABEL_RIGHT = 83;
    public static final int VALUE_X = 87;

    /** How many facts each group has: the workstation, the system and the hardware. */
    public static final List<Integer> GROUPS = List.of(3, 3, 5);

    private static final int PAD = 4;
    private static final int INNER = 5;
    private static final int EDGE = 3;
    private static final int HEAD_H = 8;
    private static final int GAP = 3;
    private static final int METER_OFFSET = 46;
    private static final int METER_W = 44;
    private static final int METER_H = 5;
    private static final int BUTTON_W = 48;
    private static final int BUTTON_H = 14;

    private WorkstationInfoLayout() {
    }

    /** The well of group {@code index}, counted from the top. */
    public static Rect group(final int index) {
        int y = PAD;
        for (int i = 0; i < index; i++) {
            y += heightOf(GROUPS.get(i)) + GAP;
        }
        return new Rect(PAD, y, W - PAD * 2, heightOf(GROUPS.get(index)));
    }

    /** Where the heading of a group is written. */
    public static int headY(final int group) {
        return group(group).y() + EDGE;
    }

    /** Where fact {@code row} of a group is written. */
    public static int rowY(final int group, final int row) {
        return group(group).y() + EDGE + HEAD_H + row * ROW_H;
    }

    /** Where the heading and the labels begin at the left of a well. */
    public static int innerX() {
        return PAD + INNER;
    }

    /** How wide a value may run before it would leave its well. */
    public static int valueWidth() {
        return W - PAD - INNER - VALUE_X;
    }

    /** The meter beside a fact's value, which only the memory in use has. */
    public static Rect meter(final int group, final int row) {
        return new Rect(VALUE_X + METER_OFFSET, rowY(group, row) + 1, METER_W, METER_H);
    }

    /** The one button, Close, centred along the foot. */
    public static Rect close() {
        return new Rect((W - BUTTON_W) / 2, H - PAD - BUTTON_H, BUTTON_W, BUTTON_H);
    }

    /** The whole window as solids that may not overlap: the wells, the meter and the button. */
    public static GuiLayout layout() {
        final GuiLayout l = new GuiLayout(W, H);
        for (int i = 0; i < GROUPS.size(); i++) {
            final Rect r = group(i);
            l.box("group_" + i, r.x(), r.y(), r.w(), r.h());
        }
        final Rect close = close();
        // The default button wears a ring two pixels out, which must clear the well above it too.
        l.box("close", close.x() - 2, close.y() - 2, close.w() + 4, close.h() + 4);
        return l;
    }

    private static int heightOf(final int rows) {
        return EDGE + HEAD_H + rows * ROW_H + EDGE;
    }
}
