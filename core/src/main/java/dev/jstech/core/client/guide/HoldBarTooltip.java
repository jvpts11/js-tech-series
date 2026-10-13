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
 * is held over the item, in the look of the manual the item's page is in, and runs from one side of the tooltip to the
 * other.
 *
 * <p>A tooltip line is only told where it starts, never how wide the tooltip came out, and that width is only known once
 * the game has wrapped the text lines. So the bar asks for no more room than its own least width, and is stretched to
 * the widest of the lines around it just before the tooltip is drawn ({@link #spanTo}).
 */
final class HoldBarTooltip implements TooltipComponent, ClientTooltipComponent {

    private final GuideStyle.HoldBar kind;
    private int span;

    /* A pixel of room over the bar and two under it, so it sits in the rhythm of the text lines around it. */
    private static final int ABOVE = 1;
    private static final int BELOW = 2;

    /**
     * @param kind the look of the manual it opens
     */
    HoldBarTooltip(final GuideStyle.HoldBar kind) {
        this.kind = kind;
    }

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
        GuideHoldBar.draw(g, x, y + ABOVE, Math.max(GuideHoldBar.WIDTH, span), GuideClient.holdProgress(), kind);
    }

    /** Stretches the bar to {@code width}, the width the tooltip's lines came out at; never below its own. */
    void spanTo(final int width) {
        this.span = width;
    }
}
