/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui.style;

import dev.jstech.core.JsCore;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;

/** The colours of the styles' own, apart from any one style's. */
@PaletteHolder
public final class StylePalette {

    /** What a role no style in a chain sets is drawn in. */
    public static final Palette<Colours> COLOURS = Palettes.declare(JsCore.MODID, "gui/style", new Colours(0xFFFF00FF));

    private StylePalette() {
    }

    /**
     * The colours.
     *
     * @param unset what a role no style in a chain sets is drawn in: a colour nobody would choose, so the slip shows
     */
    public record Colours(int unset) {
    }
}
