/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

import dev.jstech.computers.hardware.RigGeometry;
import dev.jstech.core.gui.layout.GuiLayout;
import dev.jstech.core.jstechseries.tier.Era;

/**
 * The Mining Computer's assembly screen (computers/docs/design/teracoin.md): its era's open frame with as many risers
 * as the frame carries, the mining board's row under them, and three readouts on its front panel (hashrate, draw, and
 * the cards on its risers) over the hint that its cards are seated in the world. The menu seats its slots from here
 * and the screen draws from here, so the two can never disagree.
 */
public final class MiningComputerLayout {

    /** The front panel's readouts. */
    public static final int READOUTS = 3;

    private MiningComputerLayout() {
    }

    /** Where everything stands on a Mining Computer's screen of that era. */
    public static AssemblyGeometry geometry(final Era era) {
        return AssemblyGeometry.rig(RigGeometry.of(era).risers(), READOUTS);
    }

    /** The layout the menu seats its slots from: every seat, the panels, the inventory, the captions and the hint. */
    public static GuiLayout layout(final Era era) {
        return geometry(era).layout("gpu");
    }
}
