/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.tooltip;

import dev.jstech.core.gui.tooltip.TooltipBar;
import dev.jstech.core.gui.tooltip.TooltipContents;
import dev.jstech.core.gui.tooltip.TooltipPicture;
import dev.jstech.core.gui.tooltip.TooltipPreview;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.ApiStatus;

/**
 * What draws the Core's pieces of a tooltip, bars, pictures, 3D previews and what an item holds, handed to the game;
 * and how wide the tooltip being drawn is, so that a picture stands in its middle.
 */
public final class TooltipPieces {

    /* How wide the text area of the tooltip being drawn is, worked out before it is drawn. */
    private static volatile int width;

    private TooltipPieces() {
    }

    /** Hands the game what draws each piece, and listens for each tooltip drawn, once, from the Core's client. */
    @ApiStatus.Internal
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterClientTooltipComponentFactoriesEvent.class, event -> {
            event.register(TooltipBar.class, ClientTooltipBar::new);
            event.register(TooltipPicture.class, ClientTooltipPicture::new);
            event.register(TooltipPreview.class, ClientTooltipPreview::new);
            event.register(TooltipContents.class, ClientTooltipContents::new);
            event.register(TooltipGap.class, gap -> gap);
        });
        NeoForge.EVENT_BUS.addListener(RenderTooltipEvent.Pre.class, event -> {
            int wide = 0;
            for (final ClientTooltipComponent component : event.getComponents()) {
                wide = Math.max(wide, component.getWidth(event.getFont()));
            }
            width = wide;
        });
    }

    /** Where a piece {@code wide} pixels across starts to stand in the middle of a tooltip whose text starts at x. */
    static int centred(final int x, final int wide) {
        return x + Math.max(0, (width - wide) / 2);
    }
}
