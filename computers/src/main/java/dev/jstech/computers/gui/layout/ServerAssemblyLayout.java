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
 * A rack server's assembly screen: its chassis with four sockets, eight banks and six cards in two I/O cages and its
 * wall of fans where a tower has its drives (a server's drives are the rack's), and five readouts on its front panel
 * (orchestration, queues, RAM buffer, draw, storage). The menu seats its slots from here and the screen draws from here,
 * so the two can never disagree.
 */
public final class ServerAssemblyLayout {

    /** The front panel's readouts: orchestration, queues, RAM buffer, draw, storage. */
    public static final int READOUTS = 5;

    private ServerAssemblyLayout() {
    }

    /** Where everything stands on a server's screen. */
    public static AssemblyGeometry geometry() {
        return AssemblyGeometry.server(READOUTS);
    }

    /** The layout the menu seats its slots from: every seat, the panels, the inventory and the captions. */
    public static GuiLayout layout() {
        return geometry().layout("gpu");
    }
}
