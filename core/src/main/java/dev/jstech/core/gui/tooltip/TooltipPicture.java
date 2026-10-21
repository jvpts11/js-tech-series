/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui.tooltip;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

/**
 * A picture in a tooltip, a whole texture drawn at a size and in the middle of the tooltip: the photo J's Computers
 * prints shows the photo, a record its cover. The texture may be one a mod draws while the game runs, as a
 * {@code DynamicTexture} registered under its name.
 *
 * @param texture the texture
 * @param width   how wide it is drawn, in the GUI's pixels
 * @param height  how tall it is drawn
 */
public record TooltipPicture(ResourceLocation texture, int width, int height) implements TooltipComponent {

    /** The largest a picture is drawn, so that one never fills the screen. */
    public static final int MOST = 256;

    public TooltipPicture {
        if (width < 1 || height < 1 || width > MOST || height > MOST) {
            throw new IllegalArgumentException("A tooltip's picture is 1 to " + MOST + " pixels a side, not "
                    + width + " by " + height);
        }
    }
}
