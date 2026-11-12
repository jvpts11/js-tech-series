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
 * The geometry of Quince's Seeker, the file explorer as Quince's systems drew it (computers/docs/design/quince.md), in
 * desktop units, kept out of the screen so a test proves its pieces never overlap.
 *
 * <p>A classic window is all list under a line of figures: the Quince System's with two rules under it, Quince OS 9's
 * over a sunken list. Quince OS X's and quinceOS's has a toolbar with back, forward and the views, the sidebar of the
 * devices and the places at its left, and its figures along the foot.
 */
public final class SeekerLayout {

    /** A toolbar button of Quince OS X's, back or forward, and one segment of the views beside them. */
    public static final int BUTTON_W = 14;
    public static final int BUTTON_H = 11;
    public static final int VIEW_W = 11;
    /** How far apart back and forward stand, and how far the views stand from forward. */
    public static final int BUTTON_GAP = 2;
    public static final int VIEWS_GAP = 10;
    /** How far the first button stands from the sidebar's edge, where the list's own space begins. */
    public static final int BUTTONS_IN = 8;
    /** One row of the sidebar. */
    public static final int SIDEBAR_ROW_H = 11;
    /** How far Quince OS 9's list sits in from the window's sides and foot. */
    public static final int INSET = 3;
    /** Where the figures' words stand: in from a classic window's sides, and down from the top of their line. */
    public static final int FIGURES_IN = 6;
    public static final int FIGURES_DY = 2;
    /** The Quince System's two rules under its figures, down from the top of the window's content. */
    public static final int FIRST_RULE_Y = 11;
    public static final int SECOND_RULE_Y = 13;

    /**
     * The three shapes the Seeker took: how tall the band over its list is, how wide its sidebar, how tall the line
     * of figures along its foot, and the cell each icon of its icon view stands in.
     */
    public enum Form {
        /** The Quince System's: the figures on one line under the title, two rules under them, no sidebar. */
        CLASSIC(14, 0, 0, 72, 48),
        /** Quince OS 9's: the figures centred over the list, which sits sunken in the window. */
        PLATINUM(11, 0, 0, 82, 50),
        /** Quince OS X's and quinceOS's: a toolbar, the sidebar of devices and places, the figures along the foot. */
        X(13, 70, 12, 56, 36);

        private final int head;
        private final int sidebar;
        private final int foot;
        private final int cellW;
        private final int cellH;

        Form(final int head, final int sidebar, final int foot, final int cellW, final int cellH) {
            this.head = head;
            this.sidebar = sidebar;
            this.foot = foot;
            this.cellW = cellW;
            this.cellH = cellH;
        }

        public int head() {
            return head;
        }

        public int sidebar() {
            return sidebar;
        }

        public int foot() {
            return foot;
        }

        public int cellW() {
            return cellW;
        }

        public int cellH() {
            return cellH;
        }

        /* How far the list sits in from the window's right side and its foot: OS 9's sunken frame alone does. */
        private int inset() {
            return this == PLATINUM ? INSET : 0;
        }
    }

    private SeekerLayout() {
    }

    /** The list's left edge: past the sidebar, in from the side on OS 9, at the window's edge on the Quince System. */
    public static int listX(final Form form) {
        return form.sidebar() > 0 ? form.sidebar() + 1 : form.inset();
    }

    public static int listY(final Form form) {
        return form.head();
    }

    public static int listW(final Form form, final int width) {
        return width - listX(form) - form.inset();
    }

    public static int listH(final Form form, final int height) {
        return height - listY(form) - form.foot() - form.inset();
    }

    /** The top of the line of figures along the foot of a window that has one. */
    public static int footY(final Form form, final int height) {
        return height - form.foot();
    }

    /** Back (0) or forward (1) on OS X's toolbar. */
    public static int buttonX(final int i) {
        return Form.X.sidebar() + BUTTONS_IN + i * (BUTTON_W + BUTTON_GAP);
    }

    /** The icon view's segment (0) or the list view's (1), right of forward. */
    public static int viewX(final int i) {
        return buttonX(1) + BUTTON_W + VIEWS_GAP + i * VIEW_W;
    }

    /** The toolbar's buttons' top, in the middle of the band over the list. */
    public static int buttonY() {
        return (Form.X.head() - BUTTON_H) / 2;
    }

    /** The whole window as solid boxes, so a test proves nothing overlaps at a given size. */
    public static GuiLayout layout(final Form form, final int width, final int height) {
        final GuiLayout l = new GuiLayout(width, height);
        if (form == Form.X) {
            l.box("back", buttonX(0), buttonY(), BUTTON_W, BUTTON_H);
            l.box("forward", buttonX(1), buttonY(), BUTTON_W, BUTTON_H);
            l.box("icons", viewX(0), buttonY(), VIEW_W, BUTTON_H);
            l.box("list-view", viewX(1), buttonY(), VIEW_W, BUTTON_H);
            l.box("sidebar", 0, listY(form), form.sidebar(), listH(form, height));
            l.box("figures", 0, footY(form, height), width, form.foot());
        } else {
            l.box("figures", 0, 0, width, form.head());
        }
        l.box("list", listX(form), listY(form), listW(form, width), listH(form, height));
        return l;
    }
}
