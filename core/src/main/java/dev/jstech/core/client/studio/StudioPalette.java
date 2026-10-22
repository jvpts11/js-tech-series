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

/** The colours of the studio, the game's own menus' by default: {@code jscore:gui/studio}. */
@PaletteHolder
final class StudioPalette {

    static final Palette<Colours> PALETTE = Palettes.declare(JsCore.MODID, "gui/studio", new Colours(
            0xFFFFFFFF, 0xFFE0E0E0, 0xFFA0A0A0, 0xFF000000, 0xFFFFFFFF, 0x30FFFFFF, 0xFF55FF55, 0xFFFFFF55,
            0xFFFF5555, 0xFF000000, 0xFF000000, 0xFF608CDC, 0xFFAAC8F0, 0xFF000000, 0xFFFF5555, 0xFF55FFFF,
            0xFF55FFFF, 0x5A000000, 0x5AFFFFFF, 0xFF8B8B8B, 0x28FF5555, 0xFFFF5555, 0xC0000000, 0xFFFFFF55,
            0xC0141A1E, 0xFF74747E));

    private StudioPalette() {
    }

    /** The colours as the loaded palette has them now. */
    static Colours get() {
        return PALETTE.get();
    }

    /**
     * The studio's colours.
     *
     * @param text        a heading's words, and a chosen row's
     * @param label       a row's words
     * @param dim         a part's file, a row's count, a check's detail
     * @param selected    behind the thing chosen in the tree
     * @param outline     round the thing chosen, and the state a machine on the bench is in
     * @param pointed     behind a part the pointer is on, which a click opens
     * @param fine        the tick of a part that is right
     * @param warning     a warning's mark
     * @param error       an error's mark
     * @param warningText the sign on a warning's mark
     * @param errorText   the sign on an error's mark
     * @param skyTop      the sky behind the bench, at its top
     * @param skyLow      the sky at its foot
     * @param frame       the bench's frame
     * @param energy      energy in a block's gauges and its chart of what crossed its faces
     * @param items       items and fluid in that chart
     * @param bar         the bars of a chart of packets, and of how often a loot table drops a thing
     * @param track       behind a gauge and a chart
     * @param viewer      behind a recipe drawn as the recipe viewers draw it
     * @param arrow       the arrow from a recipe's inputs to what it makes
     * @param changed     behind a field that came back changed from the save
     * @param recording   the words of the recorder of a test while it records
     * @param overlay     behind the recorder's buttons, over the game
     * @param pathPoint   a camera path's keyframes seen from above, and the way between them; the one chosen is
     *                    drawn in the outline's colour
     * @param mapGround   behind a camera path seen from above
     * @param origin      the place a camera path is played from, seen from above
     */
    record Colours(int text, int label, int dim, int selected, int outline, int pointed, int fine, int warning,
                   int error, int warningText, int errorText, int skyTop, int skyLow, int frame, int energy, int items,
                   int bar, int track, int viewer, int arrow, int changed, int recording, int overlay, int pathPoint,
                   int mapGround, int origin) {
    }
}
