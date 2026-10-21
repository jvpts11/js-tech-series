/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.hud;

import dev.jstech.core.JsCore;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;

/** The colours of the HUD editor: {@code jscore:gui/hud_editor}. */
@PaletteHolder
final class HudEditorPalette {

    static final Palette<Colours> PALETTE = Palettes.declare(JsCore.MODID, "gui/hud_editor", new Colours(
            0x78000000, 0x1EFFFFFF, 0xFFFFFFFF, 0xFFFFFF55, 0xFF8C8C8C, 0xA0000000, 0xFF505050, 0xFF404040,
            0xFF646464));

    private HudEditorPalette() {
    }

    /** The colours as the loaded palette has them now. */
    static Colours get() {
        return PALETTE.get();
    }

    /**
     * The HUD editor's colours.
     *
     * @param dim      laid over the game while the editor is open
     * @param grid     the dots of the grid
     * @param box      an element's box, its label and its eye
     * @param picked   the chosen element's box and its corners
     * @param hidden   a hidden element's box
     * @param eyeGround behind an eye
     * @param closedEye the middle of a hidden element's eye
     * @param title    the window's title and what to do
     * @param snaps    the window's second line
     */
    record Colours(int dim, int grid, int box, int picked, int hidden, int eyeGround, int closedEye, int title,
                   int snaps) {
    }
}
