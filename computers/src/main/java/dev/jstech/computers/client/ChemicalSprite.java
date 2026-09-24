/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.storage.ChemicalBridges;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Objects;

/**
 * Draws a chemical in a 16x16 cell: a swatch in the chemical's own colour with a dark frame, the same footprint
 * an item or a fluid takes, so the three kinds of data sit side by side in every grid.
 *
 * <p>Its own frame and sheen are the palette {@code jsc:app/chemical_sprite}.
 */
@PaletteHolder
public final class ChemicalSprite {

    private static final Palette<Colours> PALETTE = Palettes.declare(JsComputers.MODID, "app/chemical_sprite",
            new Colours(0xFF202020, 0x40FFFFFF));

    private ChemicalSprite() {
    }

    public static void draw(final GuiGraphics g, final StorageKey key, final int x, final int y) {
        final int tint = ChemicalBridges.tint(Objects.requireNonNull(key.chemicalId()));
        final Colours c = PALETTE.get();
        g.fill(x + 1, y + 1, x + 15, y + 15, c.frame());
        g.fill(x + 2, y + 2, x + 14, y + 14, tint);
        // A lighter top edge gives the swatch the volume of a bottle rather than a flat patch.
        g.fill(x + 2, y + 2, x + 14, y + 4, c.sheen());
    }

    /** The swatch's dark frame, and the light sheen along its top edge. */
    private record Colours(int frame, int sheen) {
    }
}
