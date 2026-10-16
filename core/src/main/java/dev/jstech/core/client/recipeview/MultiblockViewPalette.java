/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.recipeview;

import dev.jstech.core.JsCore;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;

/** The colours a multiblock is drawn with inside a recipe viewer: {@code jscore:multiblock_view}. */
@PaletteHolder
public final class MultiblockViewPalette {

    /* The viewers' own greys: their dark text, and the game's sunken panel round the preview. */
    static final Palette<Colours> PALETTE = Palettes.declare(JsCore.MODID, "multiblock_view", new Colours(
            0xFF404040, 0xFF5A5A5A, 0xFF8B8B8B, 0xFF373737, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFA0A0A0));

    private MultiblockViewPalette() {
    }

    /** The colours as the loaded palette has them now. */
    public static Colours get() {
        return PALETTE.get();
    }

    /**
     * The colours.
     *
     * @param text       the words beside and under the preview
     * @param hint       the hint along the preview's foot
     * @param ground     the preview's ground
     * @param shade      the preview's top and left edges, in shadow
     * @param light      its bottom and right edges, in the light
     * @param button     a button's word
     * @param buttonOff  a button's word when it can do nothing
     */
    public record Colours(int text, int hint, int ground, int shade, int light, int button, int buttonOff) {
    }
}
