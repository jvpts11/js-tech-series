/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

import dev.jstech.core.gui.layout.GuiLayout;
import dev.jstech.core.jstechseries.tier.Era;

/**
 * The Cluster Management Computer's assembly screen: its era's tower case, its expansion slots, and four readouts on its
 * front panel (capacity, network, the clusters in reach, the management software). The menu seats its slots from here
 * and the screen draws from here, so the two can never disagree.
 */
public final class ClusterManagementComputerLayout {

    /** The front panel's readouts: capacity, network, clusters, management. */
    public static final int READOUTS = 4;

    private ClusterManagementComputerLayout() {
    }

    /** Where everything stands on the screen of a Cluster Management Computer of that era. */
    public static AssemblyGeometry geometry(final Era era) {
        return AssemblyGeometry.tower(era, READOUTS);
    }

    /** The layout the menu seats its slots from: every seat, the panels, the inventory and the captions. */
    public static GuiLayout layout(final Era era) {
        return geometry(era).layout("pcie");
    }
}
