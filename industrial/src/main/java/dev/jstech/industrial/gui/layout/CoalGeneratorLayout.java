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
 * The Coal Generator's screen: the FE gauge on the left, the flame over the fuel slot in the middle, and the player's
 * inventory below. The menu places its slots from here and the screen draws from here.
 */
public final class CoalGeneratorLayout {

    public static final int WIDTH = MachineFrame.WIDTH;
    public static final int HEIGHT = MachineFrame.HEIGHT;
    /** Where the inventory's label sits, just above the grid. */
    public static final int INVENTORY_LABEL_Y = MachineFrame.INVENTORY_LABEL_Y;
    /** The title, the Coal Generator's name, in characters. */
    private static final int TITLE_CHARS = 14;

    private CoalGeneratorLayout() {
    }

    public static GuiLayout layout() {
        final GuiLayout layout = MachineFrame.begin(TITLE_CHARS);
        layout.box("flame", 81, 38, 14, 14);
        layout.slot("fuel", 80, 53);
        return MachineFrame.finish(layout);
    }
}
