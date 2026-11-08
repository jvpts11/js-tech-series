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
 * The Mainframe's assembly screen: its cabinet with four sockets, eight banks and six cards in two I/O cages, four disks
 * at the front, the operations strip under the case, a cooling face with a cooler over each socket, and six readouts on
 * its front panel (capacity, queues, RAM buffer, network, draw and temperature on the board's face; temperature, heat,
 * budget, the cabinet's share, surroundings and clock on the cooling face). The menu seats its slots from here and the
 * screen draws from here, so the two can never disagree.
 */
public final class MainframeLayout {

    /** The front panel's readouts, six on either face. */
    public static final int READOUTS = 6;

    private MainframeLayout() {
    }

    /** Where everything stands on the Mainframe's screen. */
    public static AssemblyGeometry geometry() {
        return AssemblyGeometry.mainframe(READOUTS);
    }

    /** The layout the menu seats its board's slots from: every seat, the panels, the inventory and the captions. */
    public static GuiLayout layout() {
        return geometry().layout("gpu");
    }

    /** The layout the menu seats its coolers from: a cooler over each of the four sockets. */
    public static GuiLayout coolingLayout() {
        return geometry().coolingLayout();
    }
}
