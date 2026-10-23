/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

import dev.jstech.core.gui.layout.GuiLayout;
import dev.jstech.core.jstechseries.tier.HardwareEra;

/**
 * The Personal Computer's assembly screen: its era's tower case, its graphics cards' slots, and three readouts on its
 * front panel (capacity, RAM buffer, network). The menu seats its slots from here and the screen draws from here, so
 * the two can never disagree.
 */
public final class PersonalComputerLayout {

    /** The front panel's readouts: capacity, RAM buffer, network. */
    public static final int READOUTS = 3;

    private PersonalComputerLayout() {
    }

    /** Where everything stands on the screen of a Personal Computer of that era. */
    public static AssemblyGeometry geometry(final HardwareEra era) {
        return AssemblyGeometry.tower(era, READOUTS);
    }

    /** The layout the menu seats its slots from: every seat, the panels, the inventory and the captions. */
    public static GuiLayout layout(final HardwareEra era) {
        return geometry(era).layout("gpu");
    }
}
