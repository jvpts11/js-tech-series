/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.crawler;

import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.space.JsSpace;
import dev.jstech.space.client.skin.PaintedSkin;
import dev.jstech.space.client.skin.Picture;

/**
 * The skins of a formed crawler-transporter, painted as the approved crawler's are: the hull's sides with their trim,
 * panel seams, louvres, doors, stencils, lamps and hazard band; the deck with its plates, its yellow line and the
 * hazard squares round the jack pins; the cabs' glass and lamps; the trucks' frames and the plates on their tops.
 */
@PaletteHolder
public final class CrawlerSkins {

    /** The crawler's colours. */
    public static final Palette<Colours> PALETTE = Palettes.declare(JsSpace.MODID, "crawler", new Colours(
            0xFF48423C, 0xFF6C645A, 0xFF96928C, 0xFF969896, 0xFFBABCBA, 0xFF686A68, 0xFF424448, 0xFF606268,
            0xFF282A2E, 0xFF787A80, 0xFF1E2024, 0xFF46484A, 0xFF828482, 0xFF505250, 0xFF28282A, 0xFF3C3C3E,
            0xFF8C8E8C, 0xFFFF9632, 0xFF242426, 0xFFD6A828, 0xFF1E1E20, 0xFF848684, 0xFF646664, 0xFFA0A2A0,
            0xFF1E2E42, 0xFF96B4D2, 0xFFFFF0C8, 0xFF8C929A, 0xFF2E3136, 0xFF707276, 0xFF54565A, 0xFF96989C,
            0xFF5A5C60, 0xFF808286, 0xFF3C3E42, 0xFF606264, 0xFF56585C, 0xFF46484C, 0xFFDEE2E6, 0xFF606266,
            0xFF505256, 0xFFFF9628, 0xFF343230, 0xFF141210, 0xFF28282A, 0xFF323438));
    /* Two blocks, the hull's panels and the deck's plates; the spacing of the deck's dots and the frames' rivets. */
    private static final int PANEL = 32;
    private static final int DOTS = 8;
    private static final int RIVETS = 12;
    private static final int PANEL_KINDS = 4;
    private static final int STRIPE = 3;
    private static final float HULL_GRAIN = 0.025F;
    private static final float DECK_GRAIN = 0.03F;
    private static final float CAB_GRAIN = 0.02F;
    private static final float FRAME_GRAIN = 0.03F;
    private static final float PLATE_GRAIN = 0.03F;

    private CrawlerSkins() {
    }

    /** A side of the hull, {@code across} by {@code down} blocks, its panels laid by {@code seed}. */
    static Picture hullSide(final double across, final double down, final int seed) {
        final Colours c = PALETTE.get();
        final PaintedSkin s = new PaintedSkin(across, down, c.grey(), HULL_GRAIN, seed);
        final int w = s.width();
        final int h = s.height();
        s.rect(0, 0, w, 3, c.trim());
        s.hline(3, c.greyLit());
        for (int x = 0; x < w; x += PANEL) {
            s.vline(x, c.greyDark(), 4, h);
            s.vline(x + 1, c.greyLit(), 4, h);
        }
        int k = seed;
        for (int x = 4; x < w - 28; x += PANEL) {
            switch (k % PANEL_KINDS) {
                case 0 -> {
                    for (int y = 6; y < h - 5; y += 2) {
                        s.rect(x + 3, y, x + 25, y + 1, c.greyDark());
                    }
                }
                case 1 -> {
                    s.bevel(x + 8, 5, x + 20, h - 4, c.door(), c.greyLit(), c.doorShade(), false);
                    s.rect(x + 17, h / 2, x + 19, h / 2 + 1, c.handle());
                    s.rect(x + 9, 7, x + 10, 9, c.hinge());
                }
                case 2 -> {
                    s.text(x + 4, 6, 6, c.handle());
                    s.lamp(x + 22, 7, c.lamp(), c.housing());
                }
                default -> {
                    s.bevel(x + 4, 5, x + 26, h - 5, c.panel(), c.greyLit(), c.greyDark(), true);
                    for (final int[] bolt : new int[][] {{6, 7}, {23, 7}, {6, h - 8}, {23, h - 8}}) {
                        s.rivet(x + bolt[0], bolt[1], c.greyLit(), c.trim());
                    }
                }
            }
            k++;
        }
        s.hazard(0, h - 3, w, h, c.yellow(), c.black(), STRIPE);
        return s.finish();
    }

    /**
     * The deck seen from above, {@code across} by {@code down} blocks: its plates, the dots of their tread, the yellow
     * line round it, and a hazard square round each of {@code pins}, in blocks from its corner.
     */
    static Picture deckTop(final double across, final double down, final double[][] pins) {
        final Colours c = PALETTE.get();
        final PaintedSkin s = new PaintedSkin(across, down, c.deck(), DECK_GRAIN, 72);
        final int w = s.width();
        final int h = s.height();
        for (int y = 0; y < h; y += PANEL) {
            s.hline(y, c.deckSeam());
            s.hline(y + 1, c.deckSeamLit());
        }
        for (int x = 0; x < w; x += PANEL) {
            s.vline(x, c.deckSeam());
            s.vline(x + 1, c.deckSeamLit());
        }
        for (int y = 6; y < h; y += DOTS) {
            for (int x = 6 + y / DOTS % 2 * 4; x < w; x += DOTS) {
                s.rect(x, y, x + 2, y + 1, c.grey());
            }
        }
        s.rect(10, 10, w - 10, 12, c.yellow());
        s.rect(10, h - 12, w - 10, h - 10, c.yellow());
        s.rect(10, 10, 12, h - 10, c.yellow());
        s.rect(w - 12, 10, w - 10, h - 10, c.yellow());
        for (final double[] pin : pins) {
            final int px = (int) (pin[0] * PaintedSkin.TEXELS);
            final int pz = (int) (pin[1] * PaintedSkin.TEXELS);
            s.hazard(px - 12, pz - 12, px + 12, pz + 12, c.yellow(), c.black(), STRIPE);
        }
        return s.finish();
    }

    /** A side of a cab, {@code across} by {@code down} blocks: its glass, its mullions, its lamps, its hazard band. */
    static Picture cabSide(final double across, final double down) {
        final Colours c = PALETTE.get();
        final PaintedSkin s = new PaintedSkin(across, down, c.grey(), CAB_GRAIN, 73);
        final int w = s.width();
        final int h = s.height();
        s.bevel(0, 0, w, h, c.grey(), c.greyLit(), c.greyDark(), true);
        final int top = 4;
        final int sill = (int) (h * 0.55);
        s.rect(2, top, w - 2, sill, c.glass());
        for (int x = 2; x < w - 2; x += DOTS) {
            s.vline(x, c.trim(), top, sill);
        }
        for (int k = 0; k < 4; k++) {
            s.rect(5 + k, top + 2 + k, 7 + k, top + 3 + k, c.glare());
        }
        s.hline(sill, c.trim());
        s.lamp(4, h - 5, c.headlamp(), c.housing());
        s.lamp(w - 5, h - 5, c.headlamp(), c.housing());
        s.hazard(0, h - 2, w, h, c.yellow(), c.black(), STRIPE);
        return s.finish();
    }

    /** A face of a truck's or a track's frame, {@code across} by {@code down} blocks, a row of rivets top and foot. */
    static Picture frame(final double across, final double down) {
        final Colours c = PALETTE.get();
        final PaintedSkin s = new PaintedSkin(across, down, c.frame(), FRAME_GRAIN, 71);
        final int w = s.width();
        final int h = s.height();
        s.bevel(0, 0, w, h, c.frame(), c.frameLit(), c.frameDark(), true);
        for (int x = 6; x < w - 4; x += RIVETS) {
            s.rivet(x, 2, c.rivet(), c.rivetDark());
            s.rivet(x, h - 4, c.rivet(), c.rivetDark());
        }
        return s.finish();
    }

    /** A plate of {@code colour}, {@code across} by {@code down} blocks, a rivet at each corner. */
    static Picture plate(final double across, final double down, final int colour) {
        final Colours c = PALETTE.get();
        final PaintedSkin s = new PaintedSkin(across, down, colour, PLATE_GRAIN, 6);
        final int w = s.width();
        final int h = s.height();
        s.bevel(0, 0, w, h, colour, c.steelLit(), c.steelDeep(), true);
        for (final int[] bolt : new int[][] {{2, 2}, {w - 4, 2}, {2, h - 4}, {w - 4, h - 4}}) {
            s.rivet(bolt[0], bolt[1], c.steelLit(), c.steelDeep());
        }
        return s.finish();
    }

    /**
     * The crawler's colours.
     *
     * @param shoe        a track shoe
     * @param shoeLit     a shoe's grouser bar
     * @param shoePin     the pins at a shoe's ends
     * @param grey        the hull and the cabs
     * @param greyLit     their lit edges
     * @param greyDark    their seams and louvres
     * @param frame       a truck's and a track's frame
     * @param frameLit    its lit edge
     * @param frameDark   its shaded edge
     * @param rivet       a frame's rivet
     * @param rivetDark   a frame rivet's shadow
     * @param trim        the hull's dark trim, the cabs' mullions
     * @param door        a door in the hull's side
     * @param doorShade   a door's shaded edge
     * @param handle      a door's handle, the stencils
     * @param hinge       a door's hinge
     * @param panel       a raised panel in the hull's side
     * @param lamp        the hull's amber lamps
     * @param housing     a lamp's housing
     * @param yellow      the yellow line, the hazard bands and the railing
     * @param black       the dark stripes of the hazard bands
     * @param deck        the deck's plates
     * @param deckSeam    the deck's seams
     * @param deckSeamLit their lit side
     * @param glass       a cab's glass
     * @param glare       the sky's glare on it
     * @param headlamp    a cab's lamps
     * @param steelLit    a plate's lit edge
     * @param steelDeep   a plate's shaded edge
     * @param wheel       a road wheel
     * @param wheelCap    its face
     * @param hub         its hub
     * @param idler       a sprocket or an idler
     * @param idlerCap    its face
     * @param axle        its axle
     * @param truckTop    the plate on a truck's top
     * @param tube        a truck's guide tube
     * @param jackFoot    a jack's cylinder
     * @param jackRod     a jack's rod
     * @param pin         a jack pin under the launcher
     * @param roof        a cab's roof
     * @param beacon      a cab's beacon
     * @param stack       an exhaust stack
     * @param stackMouth  its mouth
     * @param stackCap    its rain cap
     * @param belly       the hull's underside
     */
    public record Colours(int shoe, int shoeLit, int shoePin, int grey, int greyLit, int greyDark, int frame,
                          int frameLit, int frameDark, int rivet, int rivetDark, int trim, int door, int doorShade,
                          int handle, int hinge, int panel, int lamp, int housing, int yellow, int black, int deck,
                          int deckSeam, int deckSeamLit, int glass, int glare, int headlamp, int steelLit,
                          int steelDeep, int wheel, int wheelCap, int hub, int idler, int idlerCap, int axle,
                          int truckTop, int tube, int jackFoot, int jackRod, int pin, int roof, int beacon, int stack,
                          int stackMouth, int stackCap, int belly) {
    }
}
