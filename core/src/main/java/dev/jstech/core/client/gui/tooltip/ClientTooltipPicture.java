/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.tooltip;

import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.gui.tooltip.TooltipPicture;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;

/** A picture of a tooltip drawn whole, in the middle of the tooltip, with a little room under it. */
final class ClientTooltipPicture implements ClientTooltipComponent {

    private final TooltipPicture picture;

    /** The room under a picture, before the next line. */
    private static final int UNDER = 3;

    ClientTooltipPicture(final TooltipPicture picture) {
        this.picture = picture;
    }

    @Override
    public int getHeight() {
        return picture.height() + UNDER;
    }

    @Override
    public int getWidth(final Font font) {
        return picture.width();
    }

    @Override
    public void renderImage(final Font font, final int x, final int y, final GuiGraphics g) {
        final int left = TooltipPieces.centred(x, picture.width());
        Draw.blended(() -> g.blit(picture.texture(), left, y, 0.0F, 0.0F, picture.width(), picture.height(),
                picture.width(), picture.height()));
    }
}
