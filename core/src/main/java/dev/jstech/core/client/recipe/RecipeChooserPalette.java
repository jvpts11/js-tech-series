/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.recipe;

import dev.jstech.core.JsCore;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;

/** The colours of the chooser of recipes beside a result: {@code jscore:recipe_chooser}, the game's own greys. */
@PaletteHolder
public final class RecipeChooserPalette {

    static final Palette<Colours> PALETTE = Palettes.declare(JsCore.MODID, "recipe_chooser", new Colours(
            0xFF000000, 0xFFFFFFFF, 0xFFC6C6C6, 0xFFFFFFFF, 0xFF555555, 0xFF404040, 0xFFFFFF55));

    private RecipeChooserPalette() {
    }

    /** The colours as the loaded palette has them now. */
    public static Colours get() {
        return PALETTE.get();
    }

    /**
     * The colours.
     *
     * @param edge    the outline, as the game's buttons have
     * @param hovered the outline while the mouse is on it
     * @param face    its face, the game's window grey
     * @param light   its top and left edges, in the light
     * @param shade   its bottom and right edges, in shadow
     * @param ink     the arrows and the count
     * @param chosen  the mark beside the chosen recipe in its tooltip
     */
    public record Colours(int edge, int hovered, int face, int light, int shade, int ink, int chosen) {
    }
}
