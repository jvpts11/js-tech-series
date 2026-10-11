/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.guide;

import dev.jstech.core.guide.GuideStyle;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

/**
 * The manual key's bar as a line of an item's tooltip, under the line that says to hold the key: it fills while the key
 * is held over the item, in the look of the manual the item's page is in.
 *
 * @param kind the look of the manual it opens
 */
record HoldBarTooltip(GuideStyle.HoldBar kind) implements TooltipComponent, ClientTooltipComponent {

    /* A pixel of room over the bar and two under it, so it sits in the rhythm of the text lines around it. */
    private static final int ABOVE = 1;
    private static final int BELOW = 2;

    @Override
    public int getHeight() {
        return ABOVE + GuideHoldBar.HEIGHT + BELOW;
    }

    @Override
    public int getWidth(final Font font) {
        return GuideHoldBar.WIDTH;
    }

    @Override
    public void renderImage(final Font font, final int x, final int y, final GuiGraphics g) {
        GuideHoldBar.draw(g, x, y + ABOVE, GuideClient.holdProgress(), kind);
    }
}
