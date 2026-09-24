/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

import dev.jstech.core.gui.layout.GuiLayout;
import dev.jstech.core.text.TextKey;

/**
 * Where CDE's Front Panel puts things: a raised slab at the bottom centre of the desktop, controls either side
 * of the workspace switch in its middle, and the way out beside the switch.
 *
 * <p>The panel is as wide as what it holds and no wider, which is what told it from a taskbar at a glance, and
 * the band it stands in is kept clear of windows the whole width of the desktop. The drawing and the click
 * handling both ask here, so the control a player sees and the one they hit are the same rectangle.
 */
public final class CdeFrontPanelLayout {

    /** The controls, left to right, with the switch standing between the fourth and the fifth, the trash last. */
    public enum Control {
        CLOCK(0, CdeFrontPanelTexts.CLOCK), DATE(1, CdeFrontPanelTexts.CALENDAR),
        FILES(2, CdeFrontPanelTexts.FILE_MANAGER), EDITOR(3, CdeFrontPanelTexts.TEXT_EDITOR),
        STYLE(4, CdeFrontPanelTexts.STYLE_MANAGER), APPLICATIONS(5, CdeFrontPanelTexts.APPLICATIONS),
        TRASH(6, CdeFrontPanelTexts.TRASH_CAN),
        /* The control that was held back until there was a viewer for it to open, which there now is. */
        HELP(7, CdeFrontPanelTexts.HELP_VIEWER);

        /** Its place along the panel, counted from the left and said outright rather than read off the order. */
        private final int place;

        /** What resting the pointer on it says, since the panel is pictures and nothing else. */
        private final TextKey tip;

        Control(final int place, final TextKey tip) {
            this.place = place;
            this.tip = tip;
        }

        public int place() {
            return this.place;
        }

        public TextKey tip() {
            return this.tip;
        }

        /** Whether the control stands left of the workspace switch. */
        public boolean leftOfSwitch() {
            return this.place < LEFT_CONTROLS;
        }
    }

    /** A rectangle on the desktop. */
    public record Rect(int x, int y, int w, int h) {

        public boolean holds(final double px, final double py) {
            return px >= this.x && px < this.x + this.w && py >= this.y && py < this.y + this.h;
        }
    }

    /** How many workspaces the switch offers, which CDE fixed at four. */
    public static final int WORKSPACES = 4;

    /** The band at the foot of the desktop the panel stands in, which windows keep out of. */
    public static final int BAND_H = 50;

    public static final int PANEL_H = 46;
    /**
     * How wide one control is.
     *
     * <p>Narrow enough that the whole panel, controls and workspace switch together, stands on the smallest
     * glass a desktop is drawn on. A panel wider than the screen is a panel with its ends cut off, and the
     * ends are where the clock and the Help are.
     */
    public static final int CONTROL_W = 30;
    /** The strip at the head of a control where the arrow of its subpanel sits. */
    public static final int ARROW_H = 9;

    private static final int PAD = 3;
    private static final int GAP = 2;
    private static final int WORKSPACE_W = 38;
    private static final int EXIT_W = 28;
    private static final int SWITCH_W = PAD + WORKSPACE_W + GAP + WORKSPACE_W + GAP + EXIT_W + PAD;
    private static final int LEFT_CONTROLS = 4;
    private static final int PANEL_W = PAD * 2 + Control.values().length * CONTROL_W + SWITCH_W
            + Control.values().length * GAP;

    private CdeFrontPanelLayout() {
    }

    /** The whole slab, centred along the foot of a desktop that size. */
    public static Rect panel(final int sw, final int sh) {
        return new Rect((sw - PANEL_W) / 2, sh - BAND_H + (BAND_H - PANEL_H) / 2, PANEL_W, PANEL_H);
    }

    /** One control's raised square. */
    public static Rect control(final Control control, final int sw, final int sh) {
        final Rect panel = panel(sw, sh);
        final int before = control.place() * (CONTROL_W + GAP)
                + (control.leftOfSwitch() ? 0 : SWITCH_W + GAP);
        return new Rect(panel.x() + PAD + before, panel.y() + PAD, CONTROL_W, PANEL_H - PAD * 2);
    }

    /** The well the switch and the way out stand in. */
    public static Rect switchWell(final int sw, final int sh) {
        final Rect panel = panel(sw, sh);
        return new Rect(panel.x() + PAD + LEFT_CONTROLS * (CONTROL_W + GAP), panel.y() + PAD, SWITCH_W,
                PANEL_H - PAD * 2);
    }

    /** The button of workspace {@code index}, counted from nought: two over two, One and Two above. */
    public static Rect workspace(final int index, final int sw, final int sh) {
        final Rect well = switchWell(sw, sh);
        final int rowH = (well.h() - PAD * 2 - GAP) / 2;
        return new Rect(well.x() + PAD + (index % 2) * (WORKSPACE_W + GAP),
                well.y() + PAD + (index / 2) * (rowH + GAP), WORKSPACE_W, rowH);
    }

    /** The way out, as tall as the two rows of workspaces beside it. */
    public static Rect exit(final int sw, final int sh) {
        final Rect well = switchWell(sw, sh);
        return new Rect(well.x() + well.w() - PAD - EXIT_W, well.y() + PAD, EXIT_W, well.h() - PAD * 2);
    }

    /** The control under that point, or null when the point is on no control. */
    public static Control controlAt(final double px, final double py, final int sw, final int sh) {
        for (final Control control : Control.values()) {
            if (control(control, sw, sh).holds(px, py)) {
                return control;
            }
        }
        return null;
    }

    /**
     * Where a tip of that width stands for a control: centred over it, just above the panel, and kept on the
     * desktop at either end.
     */
    public static Rect tip(final Control control, final int width, final int height, final int sw, final int sh) {
        final Rect r = control(control, sw, sh);
        final int x = Math.max(0, Math.min(sw - width, r.x() + (r.w() - width) / 2));
        return new Rect(x, panel(sw, sh).y() - height - 2, width, height);
    }

    /** The whole panel as solids that may not overlap, on a desktop that size. */
    public static GuiLayout layout(final int sw, final int sh) {
        final GuiLayout l = new GuiLayout(sw, sh);
        for (final Control control : Control.values()) {
            final Rect r = control(control, sw, sh);
            l.box(control.name(), r.x(), r.y(), r.w(), r.h());
        }
        for (int i = 0; i < WORKSPACES; i++) {
            final Rect r = workspace(i, sw, sh);
            l.box("workspace_" + i, r.x(), r.y(), r.w(), r.h());
        }
        final Rect exit = exit(sw, sh);
        l.box("exit", exit.x(), exit.y(), exit.w(), exit.h());
        return l;
    }
}
