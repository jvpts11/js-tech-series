/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

import dev.jstech.computers.gui.MonitorGlass;
import dev.jstech.computers.os.boot.BootMenu;
import dev.jstech.core.gui.layout.GuiLayout;

/**
 * Where FreeBSD's boot loader puts things on the glass: a ruled box of numbered entries on the left with its
 * heading written over the top rule, the system's lockup on the right, and the count along the bottom.
 *
 * <p>The box is as tall as the most entries a menu may hold, so it never has to grow, and the lockup keeps
 * clear of it whatever the entries say, since an entry is clipped to the box it is in.
 */
public final class LoaderMenuLayout {

    public static final int WIDTH = MonitorGlass.WIDTH;
    public static final int HEIGHT = MonitorGlass.HEIGHT;

    /** The ruled box, and the heading that sits over its top rule on a ground of its own. */
    public static final int BOX_X = 15;
    public static final int BOX_Y = 19;
    public static final int BOX_W = 219;
    public static final int BOX_H = 169;
    public static final int TITLE_X = 27;
    public static final int TITLE_Y = 15;

    /** The entries, a number and a name to a row. */
    public static final int ITEMS_X = 27;
    public static final int ITEMS_Y = 35;
    public static final int ITEM_PITCH = 11;

    /** The lockup beside the box: the sphere with its two horns and the name under it, as one picture. */
    public static final int LOCKUP_W = 120;
    public static final int LOCKUP_H = 124;
    public static final int LOCKUP_X = 247;
    public static final int LOCKUP_Y = 38;

    /** The line that counts down, under the box, and the longest thing that line ever says. */
    public static final int FOOT_X = 15;
    public static final int FOOT_Y = 203;
    public static final String FOOT_PAUSED = "Autoboot paused. Press [Enter] to boot or a number to choose.";

    /** The scale everything but the name is written at, which is the machine screens' own. */
    private static final float TEXT_SCALE = 0.75f;

    private LoaderMenuLayout() {
    }

    /** The room an entry has across the box, in pixels, which is what its row is clipped to. */
    public static int itemRoom() {
        return BOX_X + BOX_W - ITEMS_X - 6;
    }

    /** The layout with that many entries, each as wide as a row may be, and the longest line under them. */
    public static GuiLayout layout(final int entries) {
        final GuiLayout l = new GuiLayout(WIDTH, HEIGHT)
                .box("box", BOX_X, BOX_Y, BOX_W, BOX_H)
                .box("lockup", LOCKUP_X, LOCKUP_Y, LOCKUP_W, LOCKUP_H);
        l.text("title", TITLE_X, TITLE_Y, "Welcome to FreeBSD".length(), TEXT_SCALE);
        final int rowChars = (int) (itemRoom() / (GuiLayout.GLYPH_WIDTH * TEXT_SCALE));
        for (int i = 0; i < Math.min(entries, BootMenu.MOST_ENTRIES); i++) {
            l.text("item_" + i, ITEMS_X, ITEMS_Y + i * ITEM_PITCH, rowChars, TEXT_SCALE);
        }
        l.text("foot", FOOT_X, FOOT_Y, FOOT_PAUSED.length(), TEXT_SCALE);
        return l;
    }

    /** Where the last row of a full menu ends, which has to be inside the box that holds the rows. */
    public static int lastRowBottom() {
        return ITEMS_Y + (BootMenu.MOST_ENTRIES - 1) * ITEM_PITCH + Math.round(GuiLayout.LINE_HEIGHT * TEXT_SCALE);
    }
}
