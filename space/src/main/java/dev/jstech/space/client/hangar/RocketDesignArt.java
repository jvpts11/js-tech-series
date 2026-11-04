/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.hangar;

import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.space.JsSpace;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import org.jetbrains.annotations.Nullable;

/**
 * The Rocket Design screen's things, drawn as the hangar's own, as the approved design has them: plates of painted
 * steel with their rivets, a band of hazard stripes along each plate's head with its name on a yellow plate, recesses
 * in the steel, lamps, the workshop's push buttons with their keys on tags, the tools' square buttons, a spec sheet of
 * ruled paper, bars of segments, the little globes of the worlds, and the green nodes a part snaps to.
 */
@PaletteHolder
public final class RocketDesignArt {

    public static final Palette<Colours> PALETTE = Palettes.declare(JsSpace.MODID, "rocket_design", new Colours(
            0xFF46505A, 0xFF646F78, 0xFF282E34, 0xFF1C2024, 0xFFE8E4D6, 0xFFE8B52A, 0xFF181818, 0xFFECE6D4,
            0xFFC4D2E0, 0xFFE6AAAA, 0xFF262A38, 0xFF6E7078, 0xFFBA2820, 0xFF50C864, 0xFFC83C2E, 0xFF4CFF5A,
            0xFFF06E60, 0xFFB0B8BE, 0xFFFFB03C, 0xFF78828A, 0xFF969CA2, 0xFF961E16, 0xFFDC503C, 0xFF5A100C,
            0xFFFFD6C8, 0xFF2A8038, 0xFF60C86E, 0xFF14461C, 0xFFDCFFE0, 0xFF46AA50, 0xFFFFFFFF, 0xFF000000,
            0x5A000000, 0x9663E6E2, 0xFF3A7AD0, 0xFFC8C8C8, 0xFFD05A30, 0xFF63E6E2, 0xFF282C30, 0xFF966E14,
            0xFFDCFFDC, 0xE614161A));
    /** The icons, by their cell in the icons' picture. */
    public static final int CAPSULE = 0;
    public static final int TANK = 1;
    public static final int ENGINE = 2;
    public static final int RING = 3;
    public static final int LEGS = 4;
    public static final int CHUTE = 5;
    public static final int PLACE = 6;
    public static final int OFFSET = 7;
    public static final int ROTATE = 8;
    public static final int ROOT = 9;
    public static final int MIRROR = 10;
    public static final int SNAP = 11;
    public static final int LOCK = 12;
    /** How many points square an icon is on a tool's button or a category's tab. */
    public static final int ICON_SMALL = 9;
    /* The icons as two masks a cell each, the ink on the top row and the accent under it, tinted as drawn. */
    private static final ResourceLocation ICONS = ResourceLocation.fromNamespaceAndPath(JsSpace.MODID,
            "textures/gui/hangar/rocket_design_icons.png");
    private static final ResourceLocation HAZARD = ResourceLocation.fromNamespaceAndPath(JsSpace.MODID,
            "textures/misc/hangar_hazard.png");
    private static final int CELL = 16;
    private static final int ICONS_WIDTH = 256;
    private static final int ICONS_HEIGHT = 32;
    /* How far apart rivets are; a tile of the hazard stripes; a lamp's glass; an inked bar's height; in points. */
    private static final int RIVET_STEP = 20;
    private static final int STRIPES = 8;
    private static final int LAMP = 5;
    private static final int INK_BAR = 5;
    /* The small letters' size against the game's; the height of the words by the mouse. */
    private static final float SMALL = 0.75F;
    private static final int CALLOUT_H = 20;
    private static final float LIGHTER = 0.45F;
    private static final float DARKER = 0.4F;
    private static final float CHANNEL = 255.0F;

    private RocketDesignArt() {
    }

    /**
     * A plate of painted steel: lit along its top and left, dark along its bottom and right, rivets along its top and
     * foot; its title on a yellow plate over a band of hazard stripes at its head.
     */
    public static void plate(final GuiGraphics g, final Font font, final int x0, final int y0, final int x1,
                             final int y1, @Nullable final String title) {
        final Colours c = PALETTE.get();
        g.fill(x0, y0, x1, y1, c.paint());
        hline(g, x0, x1, y0, c.paintHi());
        vline(g, x0, y0, y1, c.paintHi());
        hline(g, x0, x1, y1 - 1, c.paintDeep());
        vline(g, x1 - 1, y0, y1, c.paintDeep());
        for (int x = x0 + 3; x < x1 - 3; x += RIVET_STEP) {
            rivet(g, x, y0 + 2);
            rivet(g, x, y1 - 4);
        }
        if (title != null) {
            hazard(g, x0 + 4, y0 + 4, x1 - 4, y0 + 15);
            final int w = font.width(title) + 8;
            g.fill(x0 + 8, y0 + 4, x0 + 8 + w, y0 + 15, c.yellow());
            frame(g, x0 + 8, y0 + 4, x0 + 8 + w, y0 + 15, c.black());
            Draw.text(g, font, title, x0 + 12, y0 + 6, c.black());
        }
    }

    /** A band of hazard stripes, yellow and black on the slant. */
    public static void hazard(final GuiGraphics g, final int x0, final int y0, final int x1, final int y1) {
        for (int x = x0; x < x1; x += STRIPES) {
            for (int y = y0; y < y1; y += STRIPES) {
                final int w = Math.min(STRIPES, x1 - x);
                final int h = Math.min(STRIPES, y1 - y);
                g.blit(HAZARD, x, y, w, h, 0, 0, w * CELL / STRIPES, h * CELL / STRIPES, CELL, CELL);
            }
        }
    }

    /** A section's name on the plate, in small yellow letters, a painted rule after it to {@code x1}. */
    public static void heading(final GuiGraphics g, final Font font, final int x, final int y, final String name,
                               final int x1) {
        final Colours c = PALETTE.get();
        final int w = small(g, font, name, x, y, c.yellow());
        hline(g, x + w + 3, x1, y + 3, c.paintLo());
        hline(g, x + w + 3, x1, y + 4, c.paintHi());
    }

    /** {@code text} in the small letters; its width. */
    public static int small(final GuiGraphics g, final Font font, final String text, final int x, final int y,
                            final int colour) {
        Draw.textScaled(g, font, text, x, y, colour, SMALL);
        return smallWidth(font, text);
    }

    /** How wide {@code text} is in the small letters. */
    public static int smallWidth(final Font font, final String text) {
        return Math.round(font.width(text) * SMALL);
    }

    /**
     * A thing's name by the mouse, as the approved design writes the part under it: on a dark ground, a yellow line
     * along its top, the name in the steel's white and under it {@code line} in the small paler letters.
     */
    public static void namedCallout(final GuiGraphics g, final Font font, final String name, final String line,
                                    final int x0, final int y) {
        final Colours c = PALETTE.get();
        final int x1 = x0 + Math.max(font.width(name), smallWidth(font, line)) + 8;
        g.fill(x0, y, x1, y + CALLOUT_H, c.callout());
        hline(g, x0, x1, y, c.yellow());
        Draw.text(g, font, name, x0 + 4, y + 3, c.stencil());
        small(g, font, line, x0 + 4, y + 12, c.dimPaint());
    }

    /** How wide the words by the mouse are for {@code name} and {@code line}. */
    public static int namedCalloutWidth(final Font font, final String name, final String line) {
        return Math.max(font.width(name), smallWidth(font, line)) + 8;
    }

    /** A recess in the plate. */
    public static void well(final GuiGraphics g, final int x0, final int y0, final int x1, final int y1) {
        final Colours c = PALETTE.get();
        g.fill(x0, y0, x1, y1, c.paintDeep());
        hline(g, x0, x1, y0, c.paintLo());
        hline(g, x0, x1, y1 - 1, c.paintHi());
    }

    /** A lamp of {@code colour}, lit or dark, its glass catching the light at its top left. */
    public static void lamp(final GuiGraphics g, final int x, final int y, final int colour, final boolean lit) {
        final Colours c = PALETTE.get();
        g.fill(x - 1, y - 1, x + LAMP + 1, y + LAMP + 1, c.paintDeep());
        final int glass = lit ? colour : mix(colour, c.black(), 0.7F);
        g.fill(x, y, x + LAMP, y + LAMP, glass);
        g.fill(x + 1, y + 1, x + 3, y + 2, mix(glass, c.white(), 0.5F));
    }

    /**
     * A workshop push button: its dark collar, its cap of {@code colour} lit along its top, its label on it in the
     * small letters, and its key on a small tag at its left; the label in the middle for a button with no key.
     */
    public static void pushButton(final GuiGraphics g, final Font font, final int x, final int y, final int w,
                                  final int h, final String label, final int colour, final boolean lit,
                                  final String key) {
        final Colours c = PALETTE.get();
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, c.paintDeep());
        final int cap = lit ? colour : mix(colour, c.paintDeep(), 0.25F);
        g.fill(x, y, x + w, y + h, cap);
        hline(g, x, x + w, y, mix(cap, c.white(), LIGHTER));
        hline(g, x, x + w, y + h - 1, mix(cap, c.black(), DARKER));
        final int ink = bright(cap) ? c.black() : c.stencil();
        if (key.isEmpty()) {
            small(g, font, label, x + w / 2 - smallWidth(font, label) / 2, y + 3, ink);
            return;
        }
        g.fill(x + 2, y + 2, x + 10, y + h - 2, c.paintDeep());
        small(g, font, key, x + 6 - smallWidth(font, key) / 2, y + 3, c.yellow());
        small(g, font, label, x + 13, y + 3, ink);
    }

    /** A tool's button, {@code w} by {@code h}: its icon alone, lit yellow while it is in use. */
    public static void tool(final GuiGraphics g, final int x, final int y, final int w, final int h, final int icon,
                            final boolean on) {
        final Colours c = PALETTE.get();
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, c.paintDeep());
        final int face = on ? c.yellow() : c.paintHi();
        g.fill(x, y, x + w, y + h, face);
        hline(g, x, x + w, y, mix(face, c.white(), DARKER));
        icon(g, icon, x + 3, y + 2, ICON_SMALL, on ? c.black() : c.stencil(), on ? c.black() : c.yellow());
    }

    /** A card of ruled paper, its red margin, its shadow on the plate. */
    public static void paper(final GuiGraphics g, final int x0, final int y0, final int x1, final int y1) {
        final Colours c = PALETTE.get();
        g.fill(x0 + 1, y0 + 1, x1 + 1, y1 + 1, c.shadow());
        g.fill(x0, y0, x1, y1, c.paper());
        for (int y = y0 + 13; y < y1 - 2; y += 10) {
            hline(g, x0 + 2, x1 - 2, y, c.paperLine());
        }
        vline(g, x0 + 8, y0 + 1, y1 - 1, c.margin());
    }

    /** A bar inked on the spec sheet, hatched as far as {@code share}. */
    public static void inkBar(final GuiGraphics g, final int x, final int y, final int w, final double share) {
        final Colours c = PALETTE.get();
        frame(g, x, y, x + w, y + INK_BAR, c.ink());
        final int fill = (int) ((w - 2) * Math.clamp(share, 0, 1));
        for (int xx = x + 1; xx < x + 1 + fill; xx++) {
            if (xx % 2 == 0) {
                vline(g, xx, y + 1, y + INK_BAR - 1, c.ink());
            }
        }
    }

    /** A bar of {@code count} segments, lit {@code on} as far as {@code share}, the rest {@code off}. */
    public static void segments(final GuiGraphics g, final int x, final int y, final int w, final double share,
                                final int on, final int off, final int count) {
        final double each = w / (double) count;
        for (int i = 0; i < count; i++) {
            final int sx0 = (int) (x + i * each);
            final int sx1 = (int) (x + (i + 1) * each) - 1;
            g.fill(sx0, y, sx1, y + 4, (i + 0.5) / count <= share ? on : off);
        }
    }

    /** A world's little globe of {@code colour}, lit from the top left; with a ring round it for a low orbit. */
    public static void planet(final GuiGraphics g, final int cx, final int cy, final int r, final int colour,
                              final boolean ring) {
        final Colours c = PALETTE.get();
        for (int yy = -r; yy <= r; yy++) {
            for (int xx = -r; xx <= r; xx++) {
                if (xx * xx + yy * yy > r * r + r * 0.6) {
                    continue;
                }
                final double lit = (xx + yy) / (double) Math.max(1, r);
                final int dot = lit < -0.7 ? mix(colour, c.white(), 0.35F) : lit > 0.6 ? mix(colour, c.black(),
                        DARKER) : colour;
                g.fill(cx + xx, cy + yy, cx + xx + 1, cy + yy + 1, dot);
            }
        }
        if (ring) {
            final int dots = 28;
            for (int i = 0; i < dots; i++) {
                final double a = 2 * Math.PI * i / dots;
                final int x = (int) Math.round(cx + (r + 3) * Math.cos(a));
                final int y = (int) Math.round(cy + (r + 3) * Math.sin(a) * 0.45);
                if (!(Math.abs(x - cx) < r && y < cy)) {
                    g.fill(x, y, x + 1, y + 1, c.cyan());
                }
            }
        }
    }

    /** A green node a part snaps to, as Kerbal draws its spheres: a disc, a darker rim, a glint. */
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

    /** The icon in cell {@code icon}, {@code size} points square, its ink and its accent as given. */
    public static void icon(final GuiGraphics g, final int icon, final int x, final int y, final int size,
                            final int ink, final int accent) {
        tinted(g, ink, () -> g.blit(ICONS, x, y, size, size, icon * CELL, 0, CELL, CELL, ICONS_WIDTH,
                ICONS_HEIGHT));
        tinted(g, accent, () -> g.blit(ICONS, x, y, size, size, icon * CELL, CELL, CELL, CELL, ICONS_WIDTH,
                ICONS_HEIGHT));
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

    /* A rivet's head, its lit side up and left. */
    private static void rivet(final GuiGraphics g, final int x, final int y) {
        final Colours c = PALETTE.get();
        g.fill(x, y, x + 1, y + 1, c.paintHi());
        g.fill(x + 1, y + 1, x + 2, y + 2, c.paintDeep());
        g.fill(x + 1, y, x + 2, y + 1, c.rivet());
    }

    /* Whether ink on {@code colour} should be black: a light cap. */
    private static boolean bright(final int colour) {
        return FastColor.ARGB32.red(colour) + FastColor.ARGB32.green(colour) + FastColor.ARGB32.blue(colour) > 380;
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
     * @param paint      the plates' painted steel
     * @param paintHi    its lit edges
     * @param paintLo    its shaded edges
     * @param paintDeep  its recesses and collars
     * @param stencil    the white of letters painted on the steel
     * @param yellow     the hazard yellow: signs, lit tools, the save button, headings
     * @param black      the black of the stripes and of ink on yellow
     * @param paper      the spec sheet's paper
     * @param paperLine  its ruled lines
     * @param margin     its red margin
     * @param ink        the ink on the paper
     * @param inkDim     the paler ink
     * @param redInk     red ink
     * @param green      a green lamp, and words that say a thing is so
     * @param red        a red lamp, a short bar, the clear button
     * @param node       the nodes a part snaps to
     * @param lightRed   words that say a thing is short
     * @param dimPaint   the paler letters on the steel
     * @param led        the delta-v's display
     * @param rivet      a rivet's middle
     * @param holder     the holder of a bin's label card
     * @param signRed    the NOT READY sign
     * @param signRedHi  its lit edge
     * @param signRedLo  its shaded edge
     * @param signRedInk its letters
     * @param signGreen  the READY sign
     * @param signGreenHi its lit edge
     * @param signGreenLo its shaded edge
     * @param signGreenInk its letters
     * @param confirm    the confirm button
     * @param white      white
     * @param pure       black, for shading
     * @param shadow     the paper's shadow
     * @param holo       a part held, drawn see-through in the hangar
     * @param orbit      a low orbit's globe, and the Overworld's
     * @param moon       the Moon's globe
     * @param mars       Mars' globe
     * @param cyan       a low orbit's ring
     * @param bin        the ground a part is shown on in its bin
     * @param keyShade   the shade under a key's yellow cap
     * @param glint      a node's glint
     * @param callout    the ground under the words by the mouse, and under the view's keys
     */
    public record Colours(int paint, int paintHi, int paintLo, int paintDeep, int stencil, int yellow, int black,
                          int paper, int paperLine, int margin, int ink, int inkDim, int redInk, int green, int red,
                          int node, int lightRed, int dimPaint, int led, int rivet, int holder, int signRed,
                          int signRedHi, int signRedLo, int signRedInk, int signGreen, int signGreenHi,
                          int signGreenLo, int signGreenInk, int confirm, int white, int pure, int shadow, int holo,
                          int orbit, int moon, int mars, int cyan, int bin, int keyShade, int glint, int callout) {
    }
}
