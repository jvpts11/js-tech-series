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

/** The colours the Core draws with inside a recipe viewer: {@code jscore:recipe_view}. */
@PaletteHolder
public final class RecipeViewPalette {

    /*
     * The dark grey both viewers write their own recipe text in, so the Core's line reads as theirs do; and a dark
     * red for the knowledge a locked recipe needs.
     */
    static final Palette<Colours> PALETTE = Palettes.declare(JsCore.MODID, "recipe_view",
            new Colours(0xFF404040, 0xFFA02020));

    private RecipeViewPalette() {
    }

    /** The colours as the loaded palette has them now. */
    public static Colours get() {
        return PALETTE.get();
    }

    /**
     * The colours.
     *
     * @param text   the line of time and energy under a recipe
     * @param locked the line naming the knowledge a recipe needs, while the player's team does not have it
     */
    public record Colours(int text, int locked) {
    }
}
