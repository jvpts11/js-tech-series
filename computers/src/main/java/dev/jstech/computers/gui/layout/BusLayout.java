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
 * Pure layout shared by the four bus screens (Export, Import, Input, Receiving): every drawn element's
 * position and size, with no Minecraft dependency, so {@link #layout()} can be unit-tested. Every menu
 * places its slots from these constants and every screen draws its frames from the same ones, so the
 * screens, the menus and the test share one source of truth, so moving a control, a longer caption, or the
 * name field is caught by the test before it ever reaches the game.
 *
 * <p>All four buses have an identical control set (a name field, a ghost filter slot, min/max stock
 * steppers, a mode toggle) so they share one layout; only the title and captions differ, and those are
 * passed in by each screen at draw time.
 *
 * <p>All coordinates are relative to the panel's top-left corner (the screen adds {@code leftPos}/
 * {@code topPos} at draw time).
 */
public final class BusLayout {

    public static final int WIDTH = 176;
    public static final int HEIGHT = 189;

    public static final int HEADER_X = 6;
    public static final int HEADER_Y = 6;
    public static final int HEADER_W = 164;

    // Window caption and its status lamp, drawn on the header bar itself.
    public static final int TITLE_X = 12;
    public static final int TITLE_Y = 11;
    /** The longest title of the four, "CRAFTING RECEIVING BUS", in characters. */
    public static final int TITLE_MOST_CHARS = 22;
    /**
     * The status lamp at the header's right end: a lit square in a dark bezel, its word (linked or offline) in its
     * tooltip, so the longest title has the header to itself.
     */
    public static final int LAMP_SIZE = 5;
    public static final int LAMP_X = HEADER_X + HEADER_W - 12;
    public static final int LAMP_Y = TITLE_Y;

    // Name field: a label and a wide text box on its own row under the header.
    public static final int NAME_LABEL_X = 12;
    public static final int NAME_LABEL_Y = 25;
    public static final int NAME_X = 44;
    public static final int NAME_Y = 22;
    public static final int NAME_W = 122;
    public static final int NAME_H = 13;

    public static final int FILTER_X = 12;
    public static final int FILTER_Y = 44;

    public static final int STEP = 12;
    public static final int MIN_Y = 42;
    public static final int MAX_Y = 60;
    public static final int MINUS_X = 66;
    public static final int PLUS_X = 150;
    public static final int TRACK_X = MINUS_X + STEP + 2;
    public static final int TRACK_W = PLUS_X - MINUS_X - STEP - 4;

    public static final int LABEL_X = 40;

    public static final int MODE_X = 66;
    public static final int MODE_Y = 78;
    public static final int MODE_W = 96;
    public static final int MODE_H = 14;

    public static final int INV_LABEL_X = 8;
    public static final int INV_LABEL_Y = 98;
    public static final int INV_X = 8;
    public static final int INV_Y = 107;

    private BusLayout() {
    }

    /**
     * The full element layout, using the longest variant of each caption (the title at the longest of the four
     * buses'), ready for the overlap/out-of-bounds validators. All four screens draw from the same constants, so a
     * clean result here means the real screens are clean too.
     */
    public static GuiLayout layout() {
        final GuiLayout l = new GuiLayout(WIDTH, HEIGHT)
                .box("nameField", NAME_X, NAME_Y, NAME_W, NAME_H)
                .slot("filterSlot", FILTER_X, FILTER_Y)
                .box("minMinus", MINUS_X, MIN_Y, STEP, STEP)
                .box("minTrack", TRACK_X, MIN_Y, TRACK_W, STEP)
                .box("minPlus", PLUS_X, MIN_Y, STEP, STEP)
                .box("maxMinus", MINUS_X, MAX_Y, STEP, STEP)
                .box("maxTrack", TRACK_X, MAX_Y, TRACK_W, STEP)
                .box("maxPlus", PLUS_X, MAX_Y, STEP, STEP)
                .box("modeToggle", MODE_X, MODE_Y, MODE_W, MODE_H)
                .box("statusLamp", LAMP_X, LAMP_Y, LAMP_SIZE, LAMP_SIZE)
                .playerInventory(INV_X, INV_Y);
        // Captions, each at its longest variant.
        l.text("title", TITLE_X, TITLE_Y, TITLE_MOST_CHARS, 1.0f);
        l.text("nameLabel", NAME_LABEL_X, NAME_LABEL_Y, 4, 1.0f); // "NAME"
        l.text("minLabel", LABEL_X, MIN_Y + 3, 3, 1.0f);         // "MIN"
        l.text("maxLabel", LABEL_X, MAX_Y + 3, 3, 1.0f);         // "MAX"
        l.text("modeLabel", LABEL_X, MODE_Y + 4, 4, 1.0f);       // "MODE"
        final int modeChars = 10;                                // "CONTINUOUS" (longer than "ON DEMAND")
        l.text("modeValue", MODE_X + MODE_W / 2 - Math.round(modeChars * GuiLayout.GLYPH_WIDTH) / 2,
                MODE_Y + 4, modeChars, 1.0f);
        l.text("invLabel", INV_LABEL_X, INV_LABEL_Y, 9, 1.0f);   // "INVENTORY"
        return l;
    }
}
