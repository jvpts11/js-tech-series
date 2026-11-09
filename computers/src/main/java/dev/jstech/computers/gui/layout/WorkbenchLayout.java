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
 * Where everything sits on an Electronics Workbench's window, in the GUI's pixels from its top left, as the approved
 * screen drew it: the bench's top above (its name plate, the nine trays, the era's tool and the groove to the part's
 * tray, the era's gear, the power strip with its lamp, its draw and its store, and what the bench makes along its
 * front edge), and the game's inventory window below. Pure: the screen draws from these, the menu places its slots by
 * them, and the test checks they keep apart.
 */
public final class WorkbenchLayout {

    /** The window: the bench's top, a gap, the inventory's window. */
    public static final int WIDTH = 264;
    public static final int TOP_HEIGHT = 112;
    public static final int INVENTORY_HEIGHT = 100;
    public static final int HEIGHT = TOP_HEIGHT + 4 + INVENTORY_HEIGHT;
    public static final float SMALL = 0.75F;
    /** The name plate riveted to the top's edge, and its words. */
    public static final int PLATE_X = 6;
    public static final int PLATE_Y = 4;
    public static final int PLATE_HEIGHT = 11;
    public static final int PLATE_PADDING = 5;
    public static final int PLATE_CHARS = 32;
    public static final int TITLE_X = PLATE_X + PLATE_PADDING;
    public static final int TITLE_Y = 6;
    /** What the bench makes, along its front edge, as long as the longest the mod's languages say it. */
    public static final int MAKES_X = 6;
    public static final int MAKES_Y = TOP_HEIGHT - 11;
    public static final int MAKES_CHARS = 52;
    /** The nine trays, three by three: where the first one's item sits. */
    public static final int TRAY_X = 15;
    public static final int TRAY_Y = 27;
    public static final int TRAY_PITCH = 18;
    /** The era's tool beside the trays, and the groove from it to the part's tray with its arrowhead. */
    public static final int TOOL_X = 80;
    public static final int TOOL_Y = 50;
    public static final int GROOVE_X = 74;
    public static final int GROOVE_RIGHT = 110;
    public static final int GROOVE_Y = 66;
    public static final int ARROW_LENGTH = 4;
    /** The part's tray, and where the part sits in it. */
    public static final int PART_TRAY_X = 116;
    public static final int PART_TRAY_Y = 48;
    public static final int PART_TRAY_SIZE = 26;
    public static final int PART_X = PART_TRAY_X + 5;
    public static final int PART_Y = PART_TRAY_Y + 5;
    /** The era's gear on the top, drawn in its picture. */
    public static final int GEAR_X = 158;
    public static final int GEAR_Y = 30;
    public static final int GEAR_WIDTH = 60;
    public static final int GEAR_HEIGHT = 62;
    /** The power strip: its lamp, the display of what it draws, the bar of the energy it holds. */
    public static final int STRIP_X = WIDTH - 40;
    public static final int STRIP_Y = 22;
    public static final int STRIP_RIGHT = WIDTH - 10;
    public static final int STRIP_BOTTOM = 90;
    public static final int LAMP_X = STRIP_X + 6;
    public static final int LAMP_Y = STRIP_Y + 5;
    public static final int WATTS_RIGHT = STRIP_RIGHT - 5;
    public static final int WATTS_Y = STRIP_Y + 11;
    public static final int BAR_X = STRIP_X + 10;
    public static final int BAR_Y = STRIP_Y + 23;
    public static final int BAR_RIGHT = STRIP_RIGHT - 12;
    public static final int BAR_BOTTOM = STRIP_BOTTOM - 5;
    public static final int BAR_SEGMENT = 3;
    /** The inventory's window, its word, and where its first slot's item sits. */
    public static final int INVENTORY_WINDOW_X = (WIDTH - 176) / 2;
    public static final int INVENTORY_WINDOW_Y = TOP_HEIGHT + 4;
    public static final int INVENTORY_WORD_X = INVENTORY_WINDOW_X + 8;
    public static final int INVENTORY_WORD_Y = INVENTORY_WINDOW_Y + 6;
    public static final int INVENTORY_X = INVENTORY_WINDOW_X + 8;
    public static final int INVENTORY_Y = INVENTORY_WINDOW_Y + 18;

    private WorkbenchLayout() {
    }

    /** The window's layout: its trays, its part's slot and the inventory, its tools, gear and strip. */
    public static GuiLayout layout() {
        final GuiLayout layout = new GuiLayout(WIDTH, HEIGHT);
        layout.box("plate", PLATE_X, PLATE_Y,
                Math.round(PLATE_CHARS * GuiLayout.GLYPH_WIDTH * SMALL) + 2 * PLATE_PADDING, PLATE_HEIGHT);
        layout.text("title", TITLE_X, TITLE_Y, PLATE_CHARS, SMALL);
        for (int tray = 0; tray < 9; tray++) {
            layout.slot("tray" + tray, trayX(tray), trayY(tray));
        }
        layout.box("tool", TOOL_X, TOOL_Y, 24, 10);
        layout.box("groove", GROOVE_X, GROOVE_Y - 2, GROOVE_RIGHT + ARROW_LENGTH - GROOVE_X, 10);
        layout.slot("part", PART_X, PART_Y);
        layout.box("gear", GEAR_X, GEAR_Y, GEAR_WIDTH, GEAR_HEIGHT);
        layout.box("strip", STRIP_X, STRIP_Y, STRIP_RIGHT - STRIP_X, STRIP_BOTTOM - STRIP_Y);
        layout.text("makes", MAKES_X, MAKES_Y, MAKES_CHARS, SMALL);
        layout.text("inventoryWord", INVENTORY_WORD_X, INVENTORY_WORD_Y, 9, 1.0F);
        layout.playerInventory(INVENTORY_X, INVENTORY_Y);
        return layout;
    }

    /** Where the item of tray {@code tray} (0 to 8, row by row) sits. */
    public static int trayX(final int tray) {
        return TRAY_X + (tray % 3) * TRAY_PITCH;
    }

    public static int trayY(final int tray) {
        return TRAY_Y + (tray / 3) * TRAY_PITCH;
    }
}
