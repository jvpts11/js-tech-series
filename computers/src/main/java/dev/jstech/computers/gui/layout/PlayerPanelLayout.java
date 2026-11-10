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
 * Where the panel of a desktop a player wrote puts its parts, and its menu its rows
 * (computers/docs/design/player-systems.md): the picture and the name of the desktop at the panel's left, which open
 * the menu, the clock at its right, and the menu hung under a panel along the top or over one along the foot, its
 * programs, a rule, and Shut down.
 *
 * <p>Every place is in the desktop's own units, measured from the panel or the menu it belongs to.
 */
public final class PlayerPanelLayout {

    /* The panel: the room at either end, the menu's picture and the room after it. */
    public static final int PAD_X = 6;
    public static final int ICON = 10;
    public static final int ICON_GAP = 4;

    /* The menu: its left edge, the gap to the panel, the room above and below its rows, and each row. */
    public static final int MENU_X = 2;
    public static final int MENU_GAP = 1;
    public static final int MENU_PAD = 4;
    public static final int ROW_H = 13;
    /* A row's mark (or its program's picture) and its words; the rule over Shut down; the narrowest menu. */
    public static final int MARK_X = 6;
    public static final int MARK = 5;
    public static final int PICTURE = 8;
    public static final int TEXT_X = MARK_X + PICTURE + 5;
    public static final int TEXT_RIGHT = 10;
    public static final int RULE_H = 7;
    public static final int MENU_MIN_W = 96;

    /* The letters a line of the desktop's type is drawn in, for the width a test gives a row's words. */
    private static final int CHAR_W = 6;

    private PlayerPanelLayout() {
    }

    /** How tall the menu is with that many programs in it, Shut down and its rule below them. */
    public static int menuHeight(final int programs) {
        return MENU_PAD * 2 + programs * ROW_H + RULE_H + ROW_H;
    }

    /** How wide the menu is for its widest words. */
    public static int menuWidth(final int widestText) {
        return Math.max(MENU_MIN_W, TEXT_X + widestText + TEXT_RIGHT);
    }

    /** The top of the menu: under a panel along the top, or over one along the foot of a desktop that tall. */
    public static int menuY(final boolean top, final int barH, final int height, final int menuH) {
        return top ? barH + MENU_GAP : height - barH - MENU_GAP - menuH;
    }

    /** The top of a program's row in a menu whose top is {@code menuY}. */
    public static int rowY(final int menuY, final int row) {
        return menuY + MENU_PAD + row * ROW_H;
    }

    /** The top of Shut down, under the programs and the rule. */
    public static int shutDownY(final int menuY, final int programs) {
        return rowY(menuY, programs) + RULE_H;
    }

    /**
     * The row at a height inside the menu: a program's index, {@code programs} for Shut down, or -1 for none, the
     * rule and the room above and below the rows included.
     */
    public static int rowAt(final int menuY, final int programs, final int y) {
        final int first = rowY(menuY, 0);
        if (y >= first && y < first + programs * ROW_H) {
            return (y - first) / ROW_H;
        }
        final int shut = shutDownY(menuY, programs);
        return y >= shut && y < shut + ROW_H ? programs : -1;
    }

    /**
     * A desktop {@code width} by {@code height} with its panel {@code barH} tall along the top or the foot and its menu
     * open on {@code programs} programs whose longest name is {@code widestChars} letters, and a clock that long.
     */
    public static GuiLayout layout(final int width, final int height, final int barH, final boolean top,
                                   final int programs, final int widestChars, final int clockChars) {
        final GuiLayout l = new GuiLayout(width, height);
        final int barY = top ? 0 : height - barH;
        final int textY = barY + (barH - 8) / 2;
        l.box("panel", 0, barY, width, barH);
        l.text("name", PAD_X + ICON + ICON_GAP, textY, widestChars, 1.0f);
        l.text("clock", width - PAD_X - clockChars * CHAR_W, textY, clockChars, 1.0f);
        final int menuH = menuHeight(programs);
        final int menuY = menuY(top, barH, height, menuH);
        final int menuW = menuWidth(widestChars * CHAR_W);
        l.box("menu", MENU_X, menuY, menuW, menuH);
        for (int i = 0; i < programs; i++) {
            l.text("row" + i, MENU_X + TEXT_X, rowY(menuY, i) + 2, widestChars, 1.0f);
        }
        l.text("shut_down", MENU_X + TEXT_X, shutDownY(menuY, programs) + 2, 9, 1.0f);
        return l;
    }
}
