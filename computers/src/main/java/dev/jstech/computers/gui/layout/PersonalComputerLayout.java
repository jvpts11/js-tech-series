/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

import dev.jstech.computers.hardware.CaseCooling;
import dev.jstech.computers.hardware.TowerBoards;
import dev.jstech.core.gui.layout.GuiLayout;
import dev.jstech.core.jstechseries.tier.Era;

/**
 * The Personal Computer's assembly screen: its era's tower case, its graphics cards' slots, its cooling face, and five
 * readouts on its front panel (capacity, RAM buffer, network, draw and temperature on the board's face; temperature,
 * heat, budget, surroundings and clock on the cooling face). The menu seats its slots from here and the screen draws
 * from here, so the two can never disagree.
 */
public final class PersonalComputerLayout {

    /** The front panel's readouts, five on either face. */
    public static final int READOUTS = 5;

    private PersonalComputerLayout() {
    }

    /** Where everything stands on a Personal Computer's screen of that era, its case cooling as {@code cooling}. */
    public static AssemblyGeometry geometry(final Era era, final CaseCooling cooling) {
        return AssemblyGeometry.tower(era, TowerBoards.personalComputer(era), cooling, READOUTS);
    }

    /** The layout the menu seats its board's slots from: every seat, the panels, the inventory and the captions. */
    public static GuiLayout layout(final Era era, final CaseCooling cooling) {
        return geometry(era, cooling).layout("gpu");
    }
}
