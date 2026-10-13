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
 * Where everything sits in the Device Manager of each Frames edition: Frames 95's inside System Properties (its tabs,
 * the three ways of viewing the devices, the tree, the row of buttons under it and OK and Cancel), Frames XP's in its
 * console window (the menu bar, the tree and the status bar) and Frames 11's (the menu row with the view switch at its
 * right, and the tree). The tree's rows and the Properties box are the same in all three.
 */
public final class DeviceManagerLayout {

    /** How tall a row of the tree is, and how far each level is indented. */
    public static final int ROW_H = 10;
    public static final int INDENT = 9;
    /** The box that folds a branch, and the icon after it. */
    public static final int TOGGLE = 7;
    public static final int ICON = 8;
    /** The small font's height, which the rows' text is written in. */
    public static final int TEXT_H = 7;

    /** Frames 95: the window, its tabs, the pane under them and what the pane holds. */
    public static final int W95 = 270;
    public static final int H95 = 214;
    public static final int TAB_X = 4;
    public static final int TAB_Y = 4;
    public static final int TAB_H = 12;
    public static final int PANE_X = 4;
    public static final int PANE_Y = TAB_Y + TAB_H - 1;
    public static final int PANE_W = W95 - 8;
    public static final int FOOT_H = 13;
    public static final int PANE_H = H95 - PANE_Y - FOOT_H - 8;
    public static final int RADIO_X = PANE_X + 6;
    public static final int RADIO_Y = PANE_Y + 5;
    public static final int RADIO_ROW = 9;
    /** The second radio of the first row, after the first's words. */
    public static final int RADIO_X2 = RADIO_X + 110;
    public static final int TREE95_X = PANE_X + 5;
    public static final int TREE95_Y = RADIO_Y + 2 * RADIO_ROW + 3;
    public static final int TREE95_W = PANE_W - 10;
    public static final int BUTTON_H = 13;
    public static final int TREE95_H = PANE_Y + PANE_H - TREE95_Y - BUTTON_H - 10;
    public static final int BUTTONS_Y = TREE95_Y + TREE95_H + 5;
    public static final int BUTTON_W = 52;
    public static final int BUTTON_GAP = 4;
    public static final int FOOT_Y = H95 - FOOT_H - 4;
    public static final int FOOT_W = 44;

    /** Frames XP: the console window, its menu bar, the tree and the status bar along its foot. */
    public static final int WXP = 290;
    public static final int HXP = 220;
    public static final int MENU_H = 10;
    public static final int STATUS_H = 11;
    public static final int TREE_MARGIN = 3;
    public static final int TREEXP_Y = MENU_H + TREE_MARGIN;
    public static final int TREEXP_H = HXP - TREEXP_Y - STATUS_H - TREE_MARGIN;

    /** Frames 11: the window, its menu row with the view switch at the right, and the tree. */
    public static final int W11 = 300;
    public static final int H11 = 230;
    public static final int ROW11_H = 13;
    public static final int SEGMENT_H = 11;
    public static final int SEGMENT_PAD = 8;
    public static final int TREE11_Y = ROW11_H + 4;
    public static final int TREE11_H = H11 - TREE11_Y - 4;

    /** The Properties box, centred in whichever window opened it. */
    public static final int PROPS_W = 190;
    public static final int PROPS_H = 74;
    public static final int PROPS_LINE = 10;

    private DeviceManagerLayout() {
    }

    /** The Properties box's left edge in a window {@code width} wide. */
    public static int propsX(final int width) {
        return (width - PROPS_W) / 2;
    }

    /** The Properties box's top edge in a window {@code height} tall. */
    public static int propsY(final int height) {
        return (height - PROPS_H) / 2;
    }

    /** Where the {@code index}-th button of Frames 95's row sits, from the pane's left. */
    public static int buttonX(final int index) {
        return TREE95_X + index * (BUTTON_W + BUTTON_GAP);
    }

    /** How many rows a tree {@code height} tall shows at once. */
    public static int rowsShown(final int height) {
        return Math.max(1, (height - 4) / ROW_H);
    }

    /** Frames 95's System Properties, with its longest words in every place. */
    public static GuiLayout frames95() {
        final GuiLayout l = new GuiLayout(W95, H95)
                .box("pane", PANE_X, PANE_Y, PANE_W, PANE_H)
                .box("ok", W95 - 2 * FOOT_W - 8, FOOT_Y, FOOT_W, FOOT_H)
                .box("cancel", W95 - FOOT_W - 4, FOOT_Y, FOOT_W, FOOT_H);
        l.text("tabs", TAB_X + 4, TAB_Y + 3, 49 + 8, 0.75f);          // General ... Performance, with gaps
        l.text("radioType", RADIO_X + 8, RADIO_Y, 20, 0.75f);          // "View devices by type"
        l.text("radioConnection", RADIO_X2 + 8, RADIO_Y, 26, 0.75f);   // "View devices by connection"
        l.text("radioPort", RADIO_X + 8, RADIO_Y + RADIO_ROW, 20, 0.75f); // "View devices by port"
        l.text("row", TREE95_X + 2 + 3 * INDENT + TOGGLE + ICON + 4, TREE95_Y + 3, 34, 0.75f);
        return l;
    }

    /** Frames XP's console window, with its longest words in every place. */
    public static GuiLayout framesXp() {
        final GuiLayout l = new GuiLayout(WXP, HXP)
                .box("tree", TREE_MARGIN, TREEXP_Y, WXP - 2 * TREE_MARGIN, TREEXP_H);
        l.text("menus", 4, 1, 26, 1.0f);                               // "File  Action  View  Help"
        l.text("status", 4, HXP - STATUS_H + 2, 60, 0.75f);            // "<name> is disabled: ..."
        return l;
    }

    /** Frames 11's window, with its longest words in every place. */
    public static GuiLayout frames11() {
        final GuiLayout l = new GuiLayout(W11, H11)
                .box("tree", TREE_MARGIN, TREE11_Y, W11 - 2 * TREE_MARGIN, TREE11_H)
                .box("switch", W11 - 4 - 150, 1, 150, SEGMENT_H);
        l.text("menus", 4, 3, 26, 0.75f);                              // "File  Action  View  Help"
        return l;
    }

    /** The Properties box, with its longest line. */
    public static GuiLayout properties() {
        final GuiLayout l = new GuiLayout(PROPS_W, PROPS_H)
                .box("ok", PROPS_W - 44, PROPS_H - 17, 40, BUTTON_H);
        l.text("title", 4, 3, 40, 0.75f);
        l.text("status", 6, 14 + 2 * PROPS_LINE, 32, 0.75f);           // "This device is working properly."
        return l;
    }
}
