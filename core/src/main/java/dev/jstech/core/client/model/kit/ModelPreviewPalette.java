/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.model.kit;

import dev.jstech.core.JsCore;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;

/** The colours of the preview of a model: {@code jscore:model_preview}. */
@PaletteHolder
final class ModelPreviewPalette {

    static final Palette<Colours> PALETTE = Palettes.declare(JsCore.MODID, "model_preview", new Colours(
            0xFF4A6CA8, 0xFFFFFFFF, 0xFFB4BED0, 0xE0222C3C, 0xD0181C24, 0xFFFFFFFF, 0xFFD8DCE4, 0xFFFFE040,
            0xFFFF40FF, 0x40FFFFFF, 0xFFFFFFFF, 0xC0E0E0E0, 0xFFFFFFFF));

    private ModelPreviewPalette() {
    }

    /** The colours as the loaded palette has them now. */
    static Colours get() {
        return PALETTE.get();
    }

    /**
     * The preview's colours.
     *
     * @param titleBar the bar across the top
     * @param title    the title on it
     * @param faint    when the models were read, and the hint under the view
     * @param view     the ground of the view of the model
     * @param bar      the bar along the bottom
     * @param text     the column's labels and the animation's name
     * @param tree     a bone's name in the tree
     * @param chosen   the bone chosen, in the tree and in the view, with those it hangs from
     * @param locator  a locator's mark and its name
     * @param grid     the floor's lines
     * @param wire     the faces' edges, drawn as wire
     * @param boneLine the line from a bone to the one it hangs from
     * @param frame    the frame round the button of the animation that plays
     */
    record Colours(int titleBar, int title, int faint, int view, int bar, int text, int tree, int chosen, int locator,
                   int grid, int wire, int boneLine, int frame) {
    }
}
