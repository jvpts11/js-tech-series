/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.tooltip;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

/** A little room between the lines one mod adds to a tooltip and the next mod's, two pixels and no more. */
final class TooltipGap implements TooltipComponent, ClientTooltipComponent {

    /** The one there is. */
    static final TooltipGap GAP = new TooltipGap();

    /** How tall the room is. */
    private static final int TALL = 2;

    private TooltipGap() {
    }

    @Override
    public int getHeight() {
        return TALL;
    }

    @Override
    public int getWidth(final Font font) {
        return 0;
    }
}
