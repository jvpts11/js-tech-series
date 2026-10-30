/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.hangar;

import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.space.JsSpace;
import dev.jstech.space.client.skin.PaintedSkin;
import dev.jstech.space.client.skin.Picture;

/**
 * The skins of a formed Rocket Hangar's crane and work platforms, painted to the size of their faces as the approved
 * hangar has them: the crane's yellow box girders, their flanges lit and shaded, a stiffener every block and a stencil
 * on the long ones; the hoist's steel plates with their rivets; a platform's grated deck, its underframe of I-beams
 * across every block and along its sides, and the hazard stripes round its edge. Their measures are the approved ones,
 * in texels.
 */
@PaletteHolder
public final class HangarSkins {

    /** The hangar's colours. */
    public static final Palette<Colours> PALETTE = Palettes.declare(JsSpace.MODID, "hangar_pieces", new Colours(
            0xFFD6A828, 0xFF967014, 0xFFF0C85A, 0xFF1E1E20, 0xFF686D74, 0xFF8C929A, 0xFF464A50, 0xFF2E3136,
            0xFF969AA0, 0xFF6E7278, 0xFFBABEC4, 0xFF60646A, 0xFFB0B4BA, 0xFF141416));
    /* A girder's stiffeners, a block apart and half a block in; the shortest girder that carries a stencil. */
    private static final int STIFFENERS = 16;
    private static final int STIFFENER_FROM = 8;
    private static final int STENCILLED = 60;
    private static final int UNDERFRAME_BEAM = 4;
    private static final int UNDERFRAME_ROW = 16;
    private static final int GRATING_BARS = 4;
    private static final int EDGE_STRIPE = 3;
    private static final float GRAIN = 0.02F;
    private static final float PLATE_GRAIN = 0.03F;

    private HangarSkins() {
    }

    /** A side of a box girder of the crane, {@code across} by {@code down} blocks. */
    static Picture girder(final double across, final double down) {
        final Colours c = PALETTE.get();
        final PaintedSkin s = new PaintedSkin(across, down, c.yellow(), GRAIN, 51);
        final int w = s.width();
        final int h = s.height();
        s.bevel(0, 0, w, h, c.yellow(), c.yellowLit(), c.yellowDark(), true);
        for (int x = STIFFENER_FROM; x < w; x += STIFFENERS) {
            s.vline(x, c.yellowDark(), 2, h - 2);
            s.vline(x + 1, c.yellowLit(), 2, h - 2);
        }
        if (w > STENCILLED) {
            s.text(w / 2 - 12, h / 2 - 1, 5, c.black());
        }
        return s.finish();
    }

    /** A steel plate of the hoist, {@code across} by {@code down} blocks, a rivet at each corner. */
    static Picture plate(final double across, final double down) {
        final Colours c = PALETTE.get();
        final PaintedSkin s = new PaintedSkin(across, down, c.steelDark(), PLATE_GRAIN, 6);
        final int w = s.width();
        final int h = s.height();
        s.bevel(0, 0, w, h, c.steelDark(), c.steelLit(), c.steelDeep(), true);
        for (final int[] bolt : new int[][] {{2, 2}, {w - 4, 2}, {2, h - 4}, {w - 4, h - 4}}) {
            s.rivet(bolt[0], bolt[1], c.steelLit(), c.steelDeep());
        }
        return s.finish();
    }

    /** A work platform's deck from above, {@code across} by {@code down} blocks: its grating. */
    static Picture grating(final double across, final double down) {
        final Colours c = PALETTE.get();
        final PaintedSkin s = new PaintedSkin(across, down, c.grating(), 0, 4);
        for (int y = 0; y < s.height(); y += GRATING_BARS) {
            s.hline(y, c.steelDark());
        }
        for (int x = 0; x < s.width(); x += GRATING_BARS) {
            s.vline(x, c.steel());
        }
        return s.finish();
    }

    /**
     * A work platform from below, {@code across} by {@code down} blocks: light steel, its I-beams across every block
     * and along each side and its middle, their flanges lit.
     */
    static Picture underframe(final double across, final double down) {
        final Colours c = PALETTE.get();
        final PaintedSkin s = new PaintedSkin(across, down, c.underside(), GRAIN, 54);
        final int w = s.width();
        for (int y = 0; y < s.height(); y += UNDERFRAME_ROW) {
            s.rect(0, y, w, y + 3, c.underBeam());
            s.hline(y, c.underBeamLit());
        }
        for (final int x : new int[] {0, w / 2 - 2, w - UNDERFRAME_BEAM}) {
            s.rect(x, 0, x + UNDERFRAME_BEAM, s.height(), c.beam());
            s.vline(x, c.beamLit());
        }
        return s.finish();
    }

    /** A work platform's edge, {@code across} by {@code down} blocks: fine hazard stripes. */
    static Picture edge(final double across, final double down) {
        final Colours c = PALETTE.get();
        final PaintedSkin s = new PaintedSkin(across, down, c.yellow(), 0, 52);
        s.hazard(0, 0, s.width(), s.height(), c.yellow(), c.black(), EDGE_STRIPE);
        return s.finish();
    }

    /**
     * The hangar's colours: the crane's yellow, its flanges dark and lit, the stencils' and the stripes' black; steel,
     * lit, dark and deepest; a platform's underside, its cross beams and their lit flange, its side beams and theirs,
     * its grating.
     */
    public record Colours(int yellow, int yellowDark, int yellowLit, int black, int steel, int steelLit,
                          int steelDark, int steelDeep, int underside, int underBeam, int underBeamLit, int beam,
                          int beamLit, int grating) {
    }
}
