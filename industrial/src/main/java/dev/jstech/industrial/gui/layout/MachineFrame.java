/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.gui.layout;

import dev.jstech.core.gui.layout.GuiLayout;

/**
 * The frame every Industrial machine screen shares: its size, the title, the FE gauge on the left and the player's
 * inventory with its label below. A machine's layout adds only what is its own between {@link #begin} and
 * {@link #finish}, so a change to the shared frame is made once.
 */
final class MachineFrame {

    static final int WIDTH = 176;
    static final int HEIGHT = 166;
    /** Where the inventory's label sits, just above the grid. */
    static final int INVENTORY_LABEL_Y = HEIGHT - 94;
    /** The inventory's label, in characters. */
    private static final int INVENTORY_CHARS = 9;

    private MachineFrame() {
    }

    /** A layout with the title, {@code titleChars} characters at most, and the FE gauge placed. */
    static GuiLayout begin(final int titleChars) {
        final GuiLayout layout = new GuiLayout(WIDTH, HEIGHT);
        layout.text("title", 8, 6, titleChars, 1.0f);
        layout.box("energy", 8, 16, 10, 52);
        return layout;
    }

    /** Adds the inventory's label and the player's inventory to a layout the machine has filled in. */
    static GuiLayout finish(final GuiLayout layout) {
        layout.text("inventory", 8, INVENTORY_LABEL_Y, INVENTORY_CHARS, 1.0f);
        layout.playerInventory(8, 84);
        return layout;
    }
}
