/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.developer;

import dev.jstech.core.JsCore;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;

/** The colours of the developer's tools for screens: {@code jscore:gui/inspector}. */
@PaletteHolder
final class InspectorPalette {

    static final Palette<Colours> PALETTE = Palettes.declare(JsCore.MODID, "gui/inspector", new Colours(
            0xFF4F8DFF, 0xFF4FC85A, 0xFF8B8B8B, 0xFFE0483A, 0xFFF0C830, 0xFFFFFFFF, 0xFF000000, 0xFF404040,
            0xFF6E6E6E, 0xFFFFFFFF, 0xFFFF4FD8));

    private InspectorPalette() {
    }

    /** The colours as the loaded palette has them now. */
    static Colours get() {
        return PALETTE.get();
    }

    /**
     * The developer's tools' colours.
     *
     * @param layout      what only lays others out: its square in the tree and its dashed box on the screen
     * @param component   a component: its square and its dashed box
     * @param selected    behind the row chosen in the tree
     * @param error       an error's mark
     * @param warning     a warning's mark
     * @param errorText   the sign on an error's mark
     * @param warningText the sign on a warning's mark
     * @param text        the panel's words
     * @param dim         what follows a row's kind, and the key's name
     * @param handle      the squares round the component chosen in the editor
     * @param guide       the lines along which the chosen component lines up with the others
     */
    record Colours(int layout, int component, int selected, int error, int warning, int errorText,
                   int warningText, int text, int dim, int handle, int guide) {
    }
}
