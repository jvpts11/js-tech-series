/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.launcher;

import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.space.JsSpace;

/**
 * The skins of a formed mobile launcher's deck, painted to the size of their faces as the approved launcher has them:
 * the deck's top of dark steel plates two blocks square with their seams and bolts, the yellow walkway line round its
 * edge, the hazard band round the exhaust opening and the tie-down cleats at its corners; its sides of steel panels
 * under a dark trim band, a stiffener every two blocks, access doors, vents, stencils and lamps between them, hazard
 * stripes at their ends; the opening's scorched lining; the grating across the opening's foot; and the plates and
 * panels of the hold-down arms' housings and the tail service masts. Their measures are the approved ones, in texels.
 */
@PaletteHolder
public final class LauncherSkins {

    /** The launcher's colours. */
    public static final Palette<Colours> PALETTE = Palettes.declare(JsSpace.MODID, "launcher", new Colours(
            0xFF686D74, 0xFF8C929A, 0xFF464A50, 0xFF2E3136, 0xFFAAB0B8, 0xFF464A50, 0xFF62676E, 0xFF303338,
            0xFFD6A828, 0xFF1E1E20, 0xFFE6E6DE, 0xFFFF9632, 0xFF242426, 0xFF18181A, 0xFF3C342E, 0xFF28221E,
            0xFF141416, 0xFF282A2E, 0xFF3A3D42, 0xFF1E1E20, 0xFF787C82, 0xFFD2D6DC, 0xFF60646A, 0xFF285AB4,
            0xFFDC7828, 0xFFDCDEE0, 0xFFA22C20, 0xFFC84E3A, 0xFF6C1C14, 0xFF28282A, 0xFFFF3C28, 0xFF6E7278,
            0xFF969AA0, 0xFF585C62, 0xFF8C9096, 0xFF5A5E64, 0xFFE2E4E6, 0xFFF6F7F8, 0xFFAAAEB4, 0xFFBEC2C8,
            0xFF1E5AAA));
    /* Two blocks: a deck plate's side, the spacing of the side panels' stiffeners. */
    private static final int PLATE = 32;
    private static final int DECK_TRIM = 5;
    private static final int DECK_FOOT = 4;
    private static final int WALKWAY = 6;
    private static final int HAZARD_WIDE = 8;
    private static final int CLEAT_OUT = 20;
    private static final int CLEAT_IN = 14;
    private static final int CLEAT = 6;
    private static final int PANEL_KINDS = 4;
    private static final int LINING_SEAMS = 6;
    private static final int GRATING_BARS = 4;
    private static final float DECK_GRAIN = 0.03F;
    private static final float SIDE_GRAIN = 0.025F;
    private static final float LINING_GRAIN = 0.06F;
    private static final float PLATE_GRAIN = 0.03F;
    private static final float RED_GRAIN = 0.02F;
    private static final float WHITE_GRAIN = 0.015F;
    /* A swing arm's stiffeners, ten texels apart; the narrowest face of the white room that still shows its door. */
    private static final int ARM_STIFFENERS = 10;
    private static final int ROOM_DOOR = 14;
    /* The boom's web, a member every half block, a diagonal in every other panel. */
    private static final int BOOM_PANEL = 8;

    private LauncherSkins() {
    }

    /**
     * The deck seen from above, {@code across} by {@code down} blocks, with the opening {@code hole}: its left, top,
     * right and bottom in blocks from the deck's corner.
     */
    static Picture deckTop(final double across, final double down, final double[] hole) {
        final Colours c = PALETTE.get();
        final LauncherSkin s = new LauncherSkin(across, down, c.deck(), DECK_GRAIN, 1);
        final int w = s.width();
        final int h = s.height();
        for (int y = 0; y < h; y += PLATE) {
            s.hline(y, c.deckDark());
            s.hline(y + 1, c.deckLit());
        }
        for (int x = 0; x < w; x += PLATE) {
            s.vline(x, c.deckDark());
            s.vline(x + 1, c.deckLit());
        }
        for (int y = 4; y < h; y += PLATE) {
            for (int x = 4; x < w; x += PLATE) {
                for (final int[] bolt : new int[][] {{0, 0}, {24, 0}, {0, 24}, {24, 24}}) {
                    s.rivet(x + bolt[0], y + bolt[1], c.deckLit(), c.deckDark());
                }
            }
        }
        s.rect(WALKWAY, WALKWAY, w - WALKWAY, WALKWAY + 2, c.yellow());
        s.rect(WALKWAY, h - WALKWAY - 2, w - WALKWAY, h - WALKWAY, c.yellow());
        s.rect(WALKWAY, WALKWAY, WALKWAY + 2, h - WALKWAY, c.yellow());
        s.rect(w - WALKWAY - 2, WALKWAY, w - WALKWAY, h - WALKWAY, c.yellow());
        final int hx0 = (int) (hole[0] * LauncherSkin.TEXELS);
        final int hz0 = (int) (hole[1] * LauncherSkin.TEXELS);
        final int hx1 = (int) (hole[2] * LauncherSkin.TEXELS);
        final int hz1 = (int) (hole[3] * LauncherSkin.TEXELS);
        s.hazard(hx0 - HAZARD_WIDE, hz0 - HAZARD_WIDE, hx1 + HAZARD_WIDE, hz1 + HAZARD_WIDE, c.yellow(), c.black());
        s.rect(hx0, hz0, hx1, hz1, c.opening());
        for (final int[] cleat : new int[][] {{hx0 - CLEAT_OUT, hz0 - CLEAT_OUT}, {hx1 + CLEAT_IN, hz0 - CLEAT_OUT},
                {hx0 - CLEAT_OUT, hz1 + CLEAT_IN}, {hx1 + CLEAT_IN, hz1 + CLEAT_IN}}) {
            s.bevel(cleat[0], cleat[1], cleat[0] + CLEAT, cleat[1] + CLEAT, c.steel(), c.steelLit(), c.steelDeep(),
                    true);
        }
        return new Picture(w, h, s.finish());
    }

    /** A side of the deck, {@code across} by {@code down} blocks, its panels laid by {@code seed}. */
    static Picture deckSide(final double across, final double down, final int seed) {
        final Colours c = PALETTE.get();
        final LauncherSkin s = new LauncherSkin(across, down, c.steel(), SIDE_GRAIN, seed);
        final int w = s.width();
        final int h = s.height();
        s.rect(0, 0, w, DECK_TRIM, c.steelDeep());
        s.hline(DECK_TRIM, c.steelLit());
        s.rect(0, h - DECK_FOOT, w, h, c.steelDeep());
        for (int x = 0; x < w; x += PLATE) {
            s.bevel(x, DECK_TRIM + 1, x + 4, h - DECK_FOOT, c.steelLit(), c.stiffener(), c.steelDark(), true);
        }
        int k = 0;
        for (int x = 8; x < w - 24; x += PLATE) {
            switch ((k + seed) % PANEL_KINDS) {
                case 0 -> {
                    // An access door, sunk, and its handle.
                    s.bevel(x + 4, 14, x + 20, h - 6, c.steelDark(), c.steel(), c.steelDeep(), false);
                    s.rect(x + 16, 26, x + 18, 30, c.steelLit());
                }
                case 1 -> {
                    for (int y = 14; y < 30; y += 3) {
                        s.rect(x + 4, y, x + 22, y + 1, c.steelDeep());
                        s.rect(x + 4, y + 1, x + 22, y + 2, c.steelLit());
                    }
                }
                case 2 -> {
                    s.text(x + 4, 16, 5, c.stencil());
                    s.lamp(x + 18, 30, c.lamp(), c.lampHousing());
                }
                default -> {
                    s.bevel(x + 6, 18, x + 18, 34, c.steel(), c.steelLit(), c.steelDark(), true);
                    for (final int[] bolt : new int[][] {{8, 20}, {14, 20}, {8, 30}, {14, 30}}) {
                        s.rivet(x + bolt[0], bolt[1], c.steelLit(), c.steelDeep());
                    }
                }
            }
            k++;
        }
        s.hazard(0, h - 12, 12, h - DECK_FOOT, c.yellow(), c.black());
        s.hazard(w - 12, h - 12, w, h - DECK_FOOT, c.yellow(), c.black());
        return new Picture(w, h, s.finish());
    }

    /** The exhaust opening's scorched lining, {@code across} by {@code down} blocks. */
    static Picture lining(final double across, final double down) {
        final Colours c = PALETTE.get();
        final LauncherSkin s = new LauncherSkin(across, down, c.lining(), LINING_GRAIN, 3);
        for (int y = 0; y < s.height(); y += LINING_SEAMS) {
            s.hline(y, c.liningSeam());
        }
        s.rect(0, 0, s.width(), 3, c.steelLit());
        return new Picture(s.width(), s.height(), s.finish());
    }

    /** The grating across the opening's foot, {@code across} by {@code down} blocks. */
    static Picture grating(final double across, final double down) {
        final Colours c = PALETTE.get();
        final LauncherSkin s = new LauncherSkin(across, down, c.grating(), 0, 4);
        for (int y = 0; y < s.height(); y += GRATING_BARS) {
            s.hline(y, c.steelDark());
        }
        for (int x = 0; x < s.width(); x += GRATING_BARS) {
            s.vline(x, c.steel());
        }
        return new Picture(s.width(), s.height(), s.finish());
    }

    /** A panel of a housing or a column, {@code across} by {@code down} blocks: its hazard band, a seam, its rivets. */
    static Picture panel(final double across, final double down) {
        final Colours c = PALETTE.get();
        final LauncherSkin s = new LauncherSkin(across, down, c.steel(), PLATE_GRAIN, 5);
        final int w = s.width();
        final int h = s.height();
        s.bevel(0, 0, w, h, c.steel(), c.steelLit(), c.steelDark(), true);
        s.hazard(0, 3, w, 9, c.yellow(), c.black());
        s.rect(3, 14, w - 3, 15, c.steelDeep());
        for (final int y : new int[] {18, h - 6}) {
            s.rivet(3, y, c.steelLit(), c.steelDeep());
            s.rivet(w - 5, y, c.steelLit(), c.steelDeep());
        }
        return new Picture(w, h, s.finish());
    }

    /** A plate on a housing's or a column's top, {@code across} by {@code down} blocks, a rivet at each corner. */
    static Picture plate(final double across, final double down) {
        return plate(across, down, PALETTE.get().steelDark());
    }

    /** A plate of {@code colour}, {@code across} by {@code down} blocks, a rivet at each corner. */
    static Picture plate(final double across, final double down, final int colour) {
        final Colours c = PALETTE.get();
        final LauncherSkin s = new LauncherSkin(across, down, colour, PLATE_GRAIN, 6);
        final int w = s.width();
        final int h = s.height();
        s.bevel(0, 0, w, h, colour, c.steelLit(), c.steelDeep(), true);
        for (final int[] bolt : new int[][] {{2, 2}, {w - 4, 2}, {2, h - 4}, {w - 4, h - 4}}) {
            s.rivet(bolt[0], bolt[1], c.steelLit(), c.steelDeep());
        }
        return new Picture(w, h, s.finish());
    }

    /** A face of the crane's house on the tower's top, {@code across} by {@code down} blocks: red, its louvres. */
    static Picture craneHouse(final double across, final double down) {
        final Colours c = PALETTE.get();
        final LauncherSkin s = new LauncherSkin(across, down, c.red(), RED_GRAIN, 32);
        final int w = s.width();
        final int h = s.height();
        s.bevel(0, 0, w, h, c.red(), c.redLit(), c.redDark(), true);
        for (int x = 6; x < w - 6; x += 10) {
            for (int y = 5; y < h - 5; y += 3) {
                s.rect(x, y, x + 6, y + 1, c.redDark());
            }
        }
        return new Picture(w, h, s.finish());
    }

    /** A face of the crane's boom, {@code across} by {@code down} blocks: its red girder, its web's members. */
    static Picture boom(final double across, final double down) {
        final Colours c = PALETTE.get();
        final LauncherSkin s = new LauncherSkin(across, down, c.red(), RED_GRAIN, 33);
        final int w = s.width();
        final int h = s.height();
        s.bevel(0, 0, w, h, c.red(), c.redLit(), c.redDark(), true);
        final int web = Math.max(1, h - 6);
        for (int x = 4; x < w - 4; x += BOOM_PANEL) {
            s.rect(x, 3, x + 1, h - 3, c.redDark());
            if (x / BOOM_PANEL % 2 == 0) {
                for (int y = 3; y < h - 3; y++) {
                    final int at = x + (y - 3) * 7 / web;
                    s.rect(at, y, at + 1, y + 1, c.redDark());
                }
            }
        }
        return new Picture(w, h, s.finish());
    }

    /**
     * A side of a swing arm's beam, {@code across} by {@code down} blocks: grey panels with their stiffeners, the
     * hazard band near the tower, and along its foot the colours of the {@code lines} it carries.
     */
    static Picture armSide(final double across, final double down, final int[] lines) {
        final Colours c = PALETTE.get();
        final LauncherSkin s = new LauncherSkin(across, down, c.armGrey(), SIDE_GRAIN, 8);
        final int w = s.width();
        final int h = s.height();
        s.bevel(0, 0, w, h, c.armGrey(), c.armLit(), c.steelDark(), true);
        for (int x = 8; x < w - 4; x += ARM_STIFFENERS) {
            s.vline(x, c.armStiffener());
            s.vline(x + 1, c.armStiffenerLit());
        }
        s.hazard(w - 10, 2, w - 2, h - 2, c.yellow(), c.black());
        for (int k = 0; k < lines.length; k++) {
            s.rect(2, h - 3 - k * 2, w - 12, h - 2 - k * 2, lines[k]);
        }
        return new Picture(w, h, s.finish());
    }

    /** A face of the white room at the crew access arm's end, {@code across} by {@code down} blocks: its door. */
    static Picture whiteRoom(final double across, final double down) {
        final Colours c = PALETTE.get();
        final LauncherSkin s = new LauncherSkin(across, down, c.white(), WHITE_GRAIN, 34);
        final int w = s.width();
        final int h = s.height();
        s.bevel(0, 0, w, h, c.white(), c.whiteLit(), c.whiteDark(), true);
        if (w > ROOM_DOOR) {
            s.bevel(w / 2 - 6, 6, w / 2 + 6, h - 2, c.door(), c.white(), c.armLit(), false);
        }
        s.rect(3, 3, w - 3, 5, c.roomBand());
        return new Picture(w, h, s.finish());
    }

    /** A painted skin: its size in texels and its colours, row by row. */
    record Picture(int width, int height, int[] argb) {
    }

    /**
     * The launcher's colours: its steel, lit, dark and deepest, and its stiffeners' light edge; the deck's plates, lit
     * and dark; the hazard stripes' yellow and black; the stencils, a lamp and its housing; the opening's darkness, its
     * lining and the lining's seams, the grating; the deck's underside and its trim lip, the grating's frame; a
     * hold-down arm's jaw and its cylinder's chrome; a mast's hood and its blue, orange and white lines; the tower's
     * red, lit and dark, the crane's cable and the aviation lamps on the tower's top; a swing arm's grey, lit, its
     * stiffeners dark and lit, its hinge's column; the white room's white, lit and dark, its door and its blue band.
     */
    public record Colours(int steel, int steelLit, int steelDark, int steelDeep, int stiffener, int deck, int deckLit,
                          int deckDark, int yellow, int black, int stencil, int lamp, int lampHousing, int opening,
                          int lining, int liningSeam, int grating, int underside, int lip, int gratingFrame, int jaw,
                          int chrome, int hood, int lineBlue, int lineOrange, int lineWhite, int red, int redLit,
                          int redDark, int cable, int aviation, int armGrey, int armLit, int armStiffener,
                          int armStiffenerLit, int hinge, int white, int whiteLit, int whiteDark, int door,
                          int roomBand) {
    }
}
