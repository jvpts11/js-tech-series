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
 * The WAN Gateway Computer's assembly screen: the Mainframe's cabinet with four sockets, eight banks and six cards in
 * two I/O cages (the WAN Interface Cards among them), four disks at the front, no operations strip, a cooling face with
 * a cooler over each socket, and on its front panel its capacity, its lines, its network, its draw and its
 * temperature. Its service panel has the auto start switch and, under it, a lamp for each of its four lines. The menu
 * seats its slots from here and the screen draws from here, so the two can never disagree.
 */
public final class WanGatewayLayout {

    /** The front panel's readouts on the face that shows the most, the cooling face's six. */
    public static final int READOUTS = 6;

    private WanGatewayLayout() {
    }

    /** Where everything stands on the gateway's screen. */
    public static AssemblyGeometry geometry() {
        return AssemblyGeometry.wanGateway(READOUTS);
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
