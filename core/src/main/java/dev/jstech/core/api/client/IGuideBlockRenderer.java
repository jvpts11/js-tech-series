/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.api.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.ApiStatus;

/**
 * What draws a special block on a manual's page: a block of a kind the Core's ready blocks (text, figures, tables,
 * recipes, steps, warnings) do not cover, such as a multiblock turning in three dimensions.
 *
 * <p>An entry names the kind, how tall the block is, and the data the renderer is handed, written in the entry's file
 * as JSON and handed over as a tag:
 *
 * <pre>{@code
 * {"type": "custom", "kind": "myaddon:structure", "height": 80, "data": {"structure": "myaddon:furnace"}}
 * }</pre>
 *
 * <p>The Core keeps the room on the page and draws nothing in it but what the renderer draws. A kind no renderer is
 * registered for is left blank, so a manual still opens on a game without the mod that draws it.
 */
@ApiStatus.Experimental
public interface IGuideBlockRenderer {

    /**
     * Draws the block in the rectangle the page keeps for it.
     *
     * @param data   what the entry hands the block
     * @param mouseX where the pointer is, in the screen's pixels, for a block that answers to it
     */
    void draw(GuiGraphics graphics, Font font, int x, int y, int width, int height, CompoundTag data, int mouseX,
              int mouseY);
}
