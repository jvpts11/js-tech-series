/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.studio;

import dev.jstech.core.JsCore;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;

/** The colours of the studio: {@code jscore:gui/studio}. */
@PaletteHolder
final class StudioPalette {

    static final Palette<Colours> PALETTE = Palettes.declare(JsCore.MODID, "gui/studio", new Colours(
            0xE0304C7C, 0xC0181C22, 0xFF3A4452, 0xFFFFFFFF, 0xFFA8B0BC, 0xFF000000, 0xFFFFFFFF, 0xFF4FD46A,
            0xFFF0C830, 0xFFE0483A, 0xFF000000, 0xFFFFFFFF, 0xFF7AA2DC, 0xE0101318));

    private StudioPalette() {
    }

    /** The colours as the loaded palette has them now. */
    static Colours get() {
        return PALETTE.get();
    }

    /**
     * The studio's colours.
     *
     * @param bar         the bar along the top
     * @param panel       behind the tree, the checks and what a thing is made of
     * @param line        the line along a panel's top
     * @param text        the words
     * @param dim         a part's file, a row's count, a check's detail
     * @param selected    behind the thing chosen in the tree
     * @param outline     round the thing chosen
     * @param fine        the mark of a part that is right
     * @param warning     a warning's mark
     * @param error       an error's mark
     * @param warningText the sign on a warning's mark
     * @param errorText   the sign on an error's mark
     * @param sky         behind the bench
     * @param foot        the foot
     */
    record Colours(int bar, int panel, int line, int text, int dim, int selected, int outline, int fine,
                   int warning, int error, int warningText, int errorText, int sky, int foot) {
    }
}
