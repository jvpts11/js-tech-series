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
 * The Crafting Computer's assembly screen: its era's tower case, its expansion slots, its cooling face, and six
 * readouts on its front panel (capacity, the Crafting Card, the recipe ROM, network, draw and temperature on the
 * board's face; the cooling face's five). The menu seats its slots from here and the screen draws from here, so the two
 * can never disagree.
 */
public final class CraftingComputerLayout {

    /** The front panel's readouts: capacity, craft, recipe ROM, network, draw, temperature. */
    public static final int READOUTS = 6;

    private CraftingComputerLayout() {
    }

    /** Where everything stands on a Crafting Computer's screen of that era, its case cooling as {@code cooling}. */
    public static AssemblyGeometry geometry(final Era era, final CaseCooling cooling) {
        return AssemblyGeometry.tower(era, TowerBoards.craftingComputer(era), cooling, READOUTS);
    }

    /** The layout the menu seats its board's slots from: every seat, the panels, the inventory and the captions. */
    public static GuiLayout layout(final Era era, final CaseCooling cooling) {
        return geometry(era, cooling).layout("pcie");
    }
}
