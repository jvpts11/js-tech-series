/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.shipyard;

import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.space.JsSpace;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;

/**
 * The Starship Design screen's things, drawn in the starship builders' way the approved design has: panels of dark
 * glass, a corner cut, a faint cyan edge brighter at its corners, their titles in spaced capitals; tags filled cyan
 * with a point, as the measures are; the tools' buttons, filled when chosen; key prompts in small caps; bars of
 * segments; the green nodes a section snaps to; and the icons of the parts' kinds and the tools.
 */
@PaletteHolder
public final class ShipDesignArt {

    public static final Palette<Colours> PALETTE = Palettes.declare(JsSpace.MODID, "ship_design", new Colours(
            0xFF63E6E2, 0x6E63E6E2, 0xFF1E5C62, 0xFFE8F0F2, 0xFF8AA0AA, 0xFF4E626E, 0xDE091017, 0xEB101C26,
            0xFF263C4A, 0xFFEF6A5A, 0xFFF0B23A, 0xFF5FE07A, 0xFF4CFF5A, 0xFF080C10, 0xF00C161E, 0xF50E282E,
            0xFF1C2A34, 0xEB04090D, 0xFF17212A, 0xFF0A0F13, 0xFF151E26, 0xFF5CDBD6, 0xFF63E5E0, 0xA04CCCFF,
            0xFFDCFFDC, 0xC8091017, 0xFF000000, 0xFFFFFFFF, 0xFFEAEAE4, 0xFF2A2B2E, 0xFFB22822, 0xFFA0A2A6,
            0xFF787C82, 0xFF962024, 0xFF961E24, 0xFFE2D8C0, 0xFFD4AA48));
    /** The icons, by their cell in the icons' picture: the parts' kinds, then the tools. */
    public static final int COCKPIT = 0;
    public static final int HULL = 1;
    public static final int MODULE = 2;
    public static final int REACTOR = 3;
    public static final int ENGINE = 4;
    public static final int WING = 5;
    public static final int GEAR = 6;
    public static final int PLACE = 7;
    public static final int REMOVE = 8;
    public static final int FLIP = 9;
    public static final int MIRROR = 10;
    public static final int FINISH = 11;
    public static final int SNAP = 12;
    public static final int SEARCH = 13;
    public static final int LOCK = 14;
    /* The icons as two masks a cell each, the ink on the top row and the accent under it, tinted as drawn. */
    private static final ResourceLocation ICONS = ResourceLocation.fromNamespaceAndPath(JsSpace.MODID,
            "textures/gui/shipyard/ship_design_icons.png");
    private static final int CELL = 16;
    private static final int ICONS_WIDTH = 256;
    private static final int ICONS_HEIGHT = 32;
    /* A panel's bright corners, and a button's cut corner. */
    private static final int CORNER = 12;
    private static final int CORNER_DOWN = 8;
    private static final int BUTTON_CUT = 4;
    private static final int TAG_H = 13;
    private static final float DARKER = 0.4F;
    private static final float CHANNEL = 255.0F;
    /* The small text's size against the game's letters, as the mock's small face against its labels. */
    private static final float SMALL = 0.75F;

    private ShipDesignArt() {
    }

    /**
     * A panel of dark glass from {@code x0, y0} to before {@code x1, y1}: its top left corner cut by {@code cut}, a
     * faint cyan edge, brighter cyan at its corners, and its title in spaced capitals with a rule after it.
     */
    public static void panel(final GuiGraphics g, final Font font, final int x0, final int y0, final int x1,
                             final int y1, final String title, final int cut) {
        final Colours c = PALETTE.get();
        chamfer(g, x0, y0, x1, y1, c.panel(), cut, true, false, false);
        for (int k = 0; k < cut; k++) {
            g.fill(x0 + cut - k, y0 + k, x0 + cut - k + 1, y0 + k + 1, c.accent());
        }
        hline(g, x0 + cut, x1, y0, c.accentDim());
        vline(g, x0, y0 + cut, y1, c.accentDim());
        hline(g, x0, x1, y1 - 1, c.accentDim());
        vline(g, x1 - 1, y0, y1, c.accentDim());
        hline(g, x1 - CORNER, x1, y0, c.accent());
        vline(g, x1 - 1, y0, y0 + CORNER_DOWN, c.accent());
        hline(g, x0, x0 + CORNER, y1 - 1, c.accent());
        vline(g, x0, y1 - CORNER_DOWN, y1, c.accent());
        if (!title.isEmpty()) {
            final int w = spaced(g, font, title, x0 + cut + 6, y0 + 5, c.accent());
            hline(g, x0 + cut + 12 + w, x1 - 8, y0 + 9, c.line());
        }
    }

    /** A fill from {@code x0, y0} to before {@code x1, y1} with its chosen corners cut by {@code cut}. */
    public static void chamfer(final GuiGraphics g, final int x0, final int y0, final int x1, final int y1,
                               final int colour, final int cut, final boolean topLeft, final boolean bottomLeft,
                               final boolean topRight) {
        for (int y = y0; y < y1; y++) {
            final int left = Math.max(topLeft ? cut - (y - y0) : 0, bottomLeft ? cut - (y1 - 1 - y) : 0);
            final int right = topRight ? cut - (y - y0) : 0;
            g.fill(x0 + Math.max(0, left), y, x1 - Math.max(0, right), y + 1, colour);
        }
    }

    /** A heading in small spaced capitals, a rule after it to {@code x1}. */
    public static void heading(final GuiGraphics g, final Font font, final int x, final int y, final String name,
                               final int x1) {
        final Colours c = PALETTE.get();
        final int w = small(g, font, name, x, y, c.dim());
        hline(g, x + w + 5, x1, y + 3, c.line());
    }

    /**
     * A tag as the measures are: filled cyan, a point on its left or its right, the name in dark capitals; a measure
     * under it in cyan. Its width.
     */
    public static int tag(final GuiGraphics g, final Font font, final int x, final int y, final String name,
                          final String measure, final boolean pointLeft) {
        final Colours c = PALETTE.get();
        final int w = font.width(name) + 12;
        for (int yy = 0; yy < TAG_H; yy++) {
            final int inset = Math.abs(yy - TAG_H / 2);
            if (pointLeft) {
                g.fill(x + inset, y + yy, x + w + 6, y + yy + 1, c.accent());
            } else {
                g.fill(x, y + yy, x + w + 6 - inset, y + yy + 1, c.accent());
            }
        }
        Draw.text(g, font, name, x + (pointLeft ? 9 : 4), y + 3, c.dark());
        if (!measure.isEmpty()) {
            Draw.text(g, font, measure, x + (w + 6) / 2 - font.width(measure) / 2, y + TAG_H + 3, c.accent());
        }
        return w + 6;
    }

    /** A tool's button: its icon and its name; filled when chosen. Its width. */
    public static int button(final GuiGraphics g, final Font font, final int x, final int y, final int h,
                             final int icon, final String label, final boolean on) {
        final Colours c = PALETTE.get();
        final int w = buttonWidth(font, label);
        final int ink;
        final int accent;
        if (on) {
            chamfer(g, x, y, x + w, y + h, c.accent(), BUTTON_CUT, true, false, false);
            ink = c.dark();
            accent = c.dark();
        } else {
            chamfer(g, x, y, x + w, y + h, c.panelHi(), BUTTON_CUT, true, false, false);
            hline(g, x + BUTTON_CUT, x + w, y, c.accentDim());
            hline(g, x, x + w, y + h - 1, c.line());
            ink = c.text();
            accent = c.accent();
        }
        icon(g, icon, x + 3, y + 1, ink, accent);
        small(g, font, label, x + 18, y + 6, ink);
        return w;
    }

    /** How wide a tool's button of {@code label} is. */
    public static int buttonWidth(final Font font, final String label) {
        return 32 + spacedWidth(font, label);
    }

    /** A key prompt: the key in a small cap, what it does beside it. Its width. */
    public static int keycap(final GuiGraphics g, final Font font, final int x, final int y, final String key,
                             final String label) {
        final Colours c = PALETTE.get();
        final int kw = Math.max(11, Math.round(font.width(key) * SMALL) + 6);
        g.fill(x, y, x + kw, y + 12, c.text());
        hline(g, x, x + kw, y + 11, c.dim());
        Draw.textScaled(g, font, key, x + kw / 2 - Math.round(font.width(key) * SMALL / 2), y + 3, c.dark(),
                SMALL);
        return kw + 5 + small(g, font, label, x + kw + 5, y + 3, c.text());
    }

    /** A bar of twenty segments, as full as {@code share}, from {@code x} {@code w} wide. */
    public static void bar(final GuiGraphics g, final int x, final int y, final int w, final double share,
                           final int colour) {
        final Colours c = PALETTE.get();
        final int segments = 20;
        final double seg = w / (double) segments;
        for (int i = 0; i < segments; i++) {
            final int a = (int) (x + i * seg);
            final int b = (int) (x + (i + 1) * seg) - 1;
            final boolean lit = (i + 0.5) / segments <= share;
            g.fill(a, y, b, y + 4, lit ? colour : c.barOff());
        }
    }

    /** A green node a section snaps to, as an editor's sphere: a disc, its darker rim, a glint. */
    public static void node(final GuiGraphics g, final int x, final int y, final boolean big) {
        final Colours c = PALETTE.get();
        final int s = big ? 4 : 3;
        for (int dy = -s; dy <= s; dy++) {
            for (int dx = -s; dx <= s; dx++) {
                if (dx * dx + dy * dy <= s * s) {
                    final boolean inner = dx * dx + dy * dy < (s - 1) * (s - 1);
                    g.fill(x + dx, y + dy, x + dx + 1, y + dy + 1, inner ? c.node() : mix(c.node(), c.black(),
                            DARKER));
                }
            }
        }
        g.fill(x - 1, y - 1, x, y, c.glint());
    }

    /** The icon in cell {@code icon}, its ink in {@code ink} and its accent in {@code accent}. */
    public static void icon(final GuiGraphics g, final int icon, final int x, final int y, final int ink,
                            final int accent) {
        tinted(g, ink, () -> g.blit(ICONS, x, y, icon * CELL, 0, CELL, CELL, ICONS_WIDTH, ICONS_HEIGHT));
        tinted(g, accent, () -> g.blit(ICONS, x, y, icon * CELL, CELL, CELL, CELL, ICONS_WIDTH, ICONS_HEIGHT));
    }

    /** {@code text} in the small letters, spaced a pixel; its width. */
    public static int small(final GuiGraphics g, final Font font, final String text, final int x, final int y,
                            final int colour) {
        Draw.textScaled(g, font, text, x, y, colour, SMALL);
        return Math.round(font.width(text) * SMALL);
    }

    /** How wide {@code text} is in the small letters. */
    public static int smallWidth(final Font font, final String text) {
        return Math.round(font.width(text) * SMALL);
    }

    /** {@code text} in the game's letters, its capitals spaced a pixel apart; its width. */
    public static int spaced(final GuiGraphics g, final Font font, final String text, final int x, final int y,
                             final int colour) {
        int at = x;
        for (int i = 0; i < text.length(); i++) {
            final String letter = String.valueOf(text.charAt(i));
            Draw.text(g, font, letter, at, y, colour);
            at += font.width(letter) + 1;
        }
        return at - x;
    }

    /** How wide {@code text} is spaced a pixel apart. */
    public static int spacedWidth(final Font font, final String text) {
        return font.width(text) + text.length();
    }

    /** {@code a} mixed toward {@code b} by {@code share}. */
    public static int mix(final int a, final int b, final float share) {
        return FastColor.ARGB32.lerp(share, a, b);
    }

    /** A line across, from {@code x0} to before {@code x1}. */
    public static void hline(final GuiGraphics g, final int x0, final int x1, final int y, final int colour) {
        g.fill(x0, y, x1, y + 1, colour);
    }

    /** A line down, from {@code y0} to before {@code y1}. */
    public static void vline(final GuiGraphics g, final int x, final int y0, final int y1, final int colour) {
        g.fill(x, y0, x + 1, y1, colour);
    }

    /** A frame a pixel wide round the box from {@code x0, y0} to before {@code x1, y1}. */
    public static void frame(final GuiGraphics g, final int x0, final int y0, final int x1, final int y1,
                             final int colour) {
        hline(g, x0, x1, y0, colour);
        hline(g, x0, x1, y1 - 1, colour);
        vline(g, x0, y0, y1, colour);
        vline(g, x1 - 1, y0, y1, colour);
    }

    /** A dotted line from {@code (ax, ay)} to {@code (bx, by)}, a dot every {@code step} pixels. */
    public static void dotted(final GuiGraphics g, final float ax, final float ay, final float bx, final float by,
                              final int colour, final int step) {
        final int n = (int) Math.hypot(bx - ax, by - ay);
        for (int i = 0; i < n; i += step) {
            final float f = i / (float) Math.max(1, n);
            final int x = Math.round(ax + (bx - ax) * f);
            final int y = Math.round(ay + (by - ay) * f);
            g.fill(x, y, x + 1, y + 1, colour);
        }
    }

    private static void tinted(final GuiGraphics g, final int colour, final Runnable drawing) {
        g.setColor(FastColor.ARGB32.red(colour) / CHANNEL, FastColor.ARGB32.green(colour) / CHANNEL,
                FastColor.ARGB32.blue(colour) / CHANNEL, FastColor.ARGB32.alpha(colour) / CHANNEL);
        drawing.run();
        g.setColor(1, 1, 1, 1);
    }

    /**
     * The screen's colours, from the approved design.
     *
     * @param accent     the cyan of the edges, the tags and the chosen
     * @param accentDim  the faint cyan of the panels' edges
     * @param accentDeep a chosen filter's and a slot's deep cyan
     * @param text       the letters
     * @param dim        the dim letters, the headings
     * @param faint      the faintest letters, a free slot's
     * @param panel      the panels' dark glass
     * @param panelHi    a button's glass
     * @param line       the rules
     * @param red        a check that keeps the ship down
     * @param amber      a check to mind, and the power and the dock's bars
     * @param green      a check that is met
     * @param node       a joint's node
     * @param dark       the letters on cyan
     * @param card       a card's glass
     * @param cardOn     the chosen card's glass
     * @param barOff     a bar's unlit segments
     * @param search     the search line's glass
     * @param skyLow     the void at its horizon
     * @param skyHigh    the void overhead
     * @param floor      the floor
     * @param floorMark  the floor's crosses
     * @param axis       the floor's lines along and across the ship's middle
     * @param holo       a part held, drawn see-through over the ship
     * @param glint      a node's glint
     * @param label      the ground under a label in the view
     * @param black      black, for shading
     * @param white      white
     * @param orbiterA   the Orbiter finish's first swatch, white
     * @param orbiterB   its second, the tiles' black
     * @param orbiterC   its third, the red band
     * @param metalA     the Bare Metal finish's first swatch
     * @param metalB     its second
     * @param metalC     its third, the crimson band
     * @param enamelA    the Enamel finish's first swatch, crimson
     * @param enamelB    its second, cream
     * @param enamelC    its third, the gold line
     */
    public record Colours(int accent, int accentDim, int accentDeep, int text, int dim, int faint, int panel,
                          int panelHi, int line, int red, int amber, int green, int node, int dark, int card,
                          int cardOn, int barOff, int search, int skyLow, int skyHigh, int floor, int floorMark,
                          int axis, int holo, int glint, int label, int black, int white, int orbiterA, int orbiterB,
                          int orbiterC, int metalA, int metalB, int metalC, int enamelA, int enamelB, int enamelC) {
    }
}
