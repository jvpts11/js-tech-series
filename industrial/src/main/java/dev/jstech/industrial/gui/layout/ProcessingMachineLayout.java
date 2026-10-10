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
 * The screen of a processing machine (the Compressor, the Electric Furnace, the Macerator): the FE gauge on the
 * left, the input slot, the progress bar and the output slot across the middle, and the player's inventory below.
 * The menu places its slots from here and the screen draws from here.
 */
public final class ProcessingMachineLayout {

    public static final int WIDTH = MachineFrame.WIDTH;
    public static final int HEIGHT = MachineFrame.HEIGHT;
    /** Where the inventory's label sits, just above the grid. */
    public static final int INVENTORY_LABEL_Y = MachineFrame.INVENTORY_LABEL_Y;
    /** The longest title the screen shows, the Electric Furnace's, in characters. */
    private static final int TITLE_CHARS = 16;

    private ProcessingMachineLayout() {
    }

    public static GuiLayout layout() {
        final GuiLayout layout = MachineFrame.begin(TITLE_CHARS);
        layout.slot("input", 56, 35);
        layout.box("progress", 79, 38, 24, 8);
        layout.slot("output", 116, 35);
        return MachineFrame.finish(layout);
    }
}
