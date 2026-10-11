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
 * at the front, the operations strip under the case, and four readouts on its front panel (capacity, queues, RAM
 * buffer, network). The menu seats its slots from here and the screen draws from here, so the two can never disagree.
 */
public final class MainframeLayout {

    /** The front panel's readouts: capacity, queues, RAM buffer, network. */
    public static final int READOUTS = 4;

    private MainframeLayout() {
    }

    /** Where everything stands on the Mainframe's screen. */
    public static AssemblyGeometry geometry() {
        return AssemblyGeometry.mainframe(READOUTS);
    }

    /** The layout the menu seats its slots from: every seat, the panels, the inventory and the captions. */
    public static GuiLayout layout() {
        return geometry().layout("gpu");
    }
}
