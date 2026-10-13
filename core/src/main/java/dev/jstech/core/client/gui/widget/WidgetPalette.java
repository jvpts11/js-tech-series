/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.widget;

import dev.jstech.core.JsCore;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;

/**
 * The colours of the toolkit's vanilla widgets, the table, the graph and the radial menu:
 * {@code jscore:gui/widgets}.
 */
@PaletteHolder
final class WidgetPalette {

    static final Palette<Colours> PALETTE = Palettes.declare(JsCore.MODID, "gui/widgets",
            new Colours(0xFF2A2A2A, 0xFFFFFFFF, 0xFF1A1A1A, 0xFF222222, 0xFF101010, 0xFFFFFF00, 0xFFFFFFFF));

    private WidgetPalette() {
    }

    /** The colours as the loaded palette has them now. */
    static Colours get() {
        return PALETTE.get();
    }

    /**
     * The widgets' colours.
     *
     * @param tableHeader     a table's header row
     * @param tableHeaderText the text of a table's header
     * @param tableRowEven    every other row of a table, starting with the first
     * @param tableRowOdd     the rows between them
     * @param graphGround     a graph panel's ground
     * @param radialChosen    the label of a radial menu's chosen segment
     * @param radialLabel     the label of every other segment
     */
    record Colours(int tableHeader, int tableHeaderText, int tableRowEven, int tableRowOdd, int graphGround,
                   int radialChosen, int radialLabel) {
    }
}
