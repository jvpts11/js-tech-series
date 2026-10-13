/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program;

import dev.jstech.computers.JsComputers;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import java.util.List;

/**
 * The accents a desktop offers in its Settings, and the ones its theme presets wear. Declared once as the palette
 * {@code jsc:accents}, so the presets name one of these rather than keeping a second copy of the colour.
 *
 * <p>An accent is a colour a machine keeps once it is chosen. A swatch in Settings offers the colour a resource
 * pack gives it, since that is the one the player sees and picks; a preset is put on by the machine, which knows no
 * resource pack, so it keeps the colour declared here.
 *
 * @param blue   the first offered, the modern Frames blue
 * @param green  a sea green
 * @param orange a burnt orange
 * @param violet a deep violet
 * @param rose   a rose red
 * @param gold   an old gold
 */
@PaletteHolder
public record Accents(int blue, int green, int orange, int violet, int rose, int gold) {

    public static final Palette<Accents> PALETTE = Palettes.declare(JsComputers.MODID, "accents", new Accents(
            0xFF3A6AE0, 0xFF12A26F, 0xFFD1633F, 0xFF7B52C9, 0xFFC93D6A, 0xFFC98320));

    /** Every accent, in the order Settings offers them. */
    public List<Integer> all() {
        return List.of(this.blue, this.green, this.orange, this.violet, this.rose, this.gold);
    }
}
