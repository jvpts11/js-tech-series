/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.integration.jei;

import dev.jstech.core.client.recipeview.RecipeViewPalette;
import mezz.jei.api.gui.drawable.IDrawable;
import net.minecraft.client.gui.GuiGraphics;

/**
 * The small ring drawn over the lower right corner of a slot of what a machine needs and never uses up, a mask or a
 * mould: the slot looked like any input, and a player took it for one used up by every job.
 */
final class LastingRing implements IDrawable {

    /** The one ring every page draws; its colour is read from the palette as it is drawn. */
    static final LastingRing INSTANCE = new LastingRing();

    /* The ring is five pixels across, its corners left out, in the item's own sixteen. */
    private static final int ITEM_SIZE = 16;
    private static final int FROM = 10;
    private static final int TO = 14;

    private LastingRing() {
    }

    @Override
    public int getWidth() {
        return ITEM_SIZE;
    }

    @Override
    public int getHeight() {
        return ITEM_SIZE;
    }

    @Override
    public void draw(final GuiGraphics graphics, final int xOffset, final int yOffset) {
        final int colour = RecipeViewPalette.get().lasting();
        final int left = xOffset + FROM;
        final int top = yOffset + FROM;
        final int right = xOffset + TO;
        final int bottom = yOffset + TO;
        graphics.fill(left + 1, top, right, top + 1, colour);
        graphics.fill(left + 1, bottom, right, bottom + 1, colour);
        graphics.fill(left, top + 1, left + 1, bottom, colour);
        graphics.fill(right, top + 1, right + 1, bottom, colour);
    }
}
