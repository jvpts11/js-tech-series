/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.depot;

import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.space.JsSpace;
import dev.jstech.space.client.skin.PaintedSkin;
import dev.jstech.space.client.skin.Picture;

/**
 * The skins of a formed propellant depot, painted as the approved one's are: each ring of the sphere's white plates
 * its own picture, a latitude weld at its foot, a meridian weld every thirty degrees, the rings at the equator darker
 * where the legs are welded on.
 */
@PaletteHolder
public final class DepotSkins {

    /** The depot's colours. */
    public static final Palette<Colours> PALETTE = Palettes.declare(JsSpace.MODID, "depot", new Colours(
            0xFFE4E6E8, 0xFFB0B4BA, 0xFFF4F6F8, 0xFFBABEC4, 0xFFAAAEB4, 0xFFC4C8CE, 0xFF686D74, 0xFF8C929A,
            0xFF2E3136));
    /* A meridian weld every thirty degrees; the share of an equator ring that is darker. */
    private static final int WELDS = 12;
    private static final int EQUATOR_SHARE = 3;
    private static final float PLATE_GRAIN = 0.02F;

    private DepotSkins() {
    }

    /**
     * A ring of the sphere's plates, {@code round} blocks round and {@code high} tall, laid by {@code seed}; an
     * {@code equator} ring has its darker band where the legs are welded on.
     */
    static Picture ring(final double round, final double high, final int seed, final boolean equator) {
        final Colours c = PALETTE.get();
        final PaintedSkin s = new PaintedSkin(round, Math.max(high, 2.0 / PaintedSkin.TEXELS), c.plate(),
                PLATE_GRAIN, seed);
        final int w = s.width();
        final int h = s.height();
        s.hline(0, c.weld());
        s.hline(1, c.plateLit());
        for (int j = 0; j < WELDS; j++) {
            s.vline(j * w / WELDS, c.meridian());
        }
        if (equator) {
            s.rect(0, 0, w, Math.max(1, h / EQUATOR_SHARE), c.equator());
        }
        return s.finish();
    }

    /**
     * The depot's colours.
     *
     * @param plate     the sphere's white plates
     * @param weld      a latitude weld
     * @param plateLit  the lit edge above it
     * @param meridian  a meridian weld
     * @param equator   the darker band at the equator
     * @param leg       a leg
     * @param brace     a brace between the legs
     * @param vent      the vent stack
     * @param ventMouth the vent stack's mouth
     */
    public record Colours(int plate, int weld, int plateLit, int meridian, int equator, int leg, int brace, int vent,
                          int ventMouth) {
    }
}
