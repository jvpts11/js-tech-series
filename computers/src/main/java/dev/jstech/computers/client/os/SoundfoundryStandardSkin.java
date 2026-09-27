/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import java.util.Locale;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * The skin the Standard Soundfoundry wears on every desktop: Voidsoft grown up into a streaming service, flat
 * graphite with molten orange, and the little glyphs of its buttons drawn pixel by pixel. What it is drawn with, in
 * the colours of {@code jsc:app/soundfoundry_standard}.
 */
@PaletteHolder
final class SoundfoundryStandardSkin {

    /** The skin's colours, {@code jsc:app/soundfoundry_standard}. */
    static final Palette<Colours> PALETTE = Palettes.declare(JsComputers.MODID, "app/soundfoundry_standard",
            new Colours(0xFF141215, 0xFF0B0A0C, 0xFF0B0A0C, 0xFF0E0D10, 0xFF2A2630, 0xFF000000,
                    0xFFFF8A2A, 0xFFF4EFE8, 0xFFA39A8E, 0xFF5F574E, 0xFFC9C0B2,
                    0xFF18161A, 0xFF4ADE80, 0xFF6B6470, 0xFF2A2630, 0xFF221E24, 0xFF221A14,
                    0xFF3A3540, 0xFF221C1E, 0xFF1C181E, 0xFF4A4550,
                    0xFF3A2210, 0xFF5A1A14, 0xFF0B0A0C));

    /**
     * The pairs of colours a cover is made of when its recording brings none, {@code jsc:app/soundfoundry_covers}:
     * the pair is picked by the album's name.
     */
    static final Palette<Covers> COVERS = Palettes.declare(JsComputers.MODID, "app/soundfoundry_covers",
            new Covers(0xFF7A1F1A, 0xFFB8331F, 0xFF5A1010, 0xFFC0392B, 0xFF7C3A0A, 0xFFE8892B, 0xFF23304F,
                    0xFF3A6AE0, 0xFF1B2A3A, 0xFF2F5A8A, 0xFF1F4A4A, 0xFF3A8A7A, 0xFF3A1F4A, 0xFF7A3AA8,
                    0xFF2F3A14, 0xFF6A8A2A, 0x1F000000, 0xFFFFFFFF));
    private static final int COVER_PAIRS = 8;

    private SoundfoundryStandardSkin() {
    }

    /** What the buttons and the sidebar show, each drawn eight pixels across. */
    enum Glyph { HOME, SEARCH, LIBRARY, SHUFFLE, PREVIOUS, NEXT, REPEAT, SPEAKER, MONITOR }

    static Colours c() {
        return PALETTE.get();
    }

    static void fill(final GuiGraphics g, final int x, final int y, final int w, final int h, final int colour) {
        g.fill(x, y, x + w, y + h, colour);
    }

    /** Text in the game's font on that ground, with the shadow that suits it. */
    static void text(final GuiGraphics g, final Font font, final String text, final int x, final int y,
                     final int colour, final int ground) {
        Draw.text(g, font, text, x, y, colour, ground);
    }

    /** The same, ending at {@code right}. */
    static void right(final GuiGraphics g, final Font font, final String text, final int right, final int y,
                      final int colour, final int ground) {
        Draw.text(g, font, text, right - font.width(text) + 1, y, colour, ground);
    }

    /** The same, centred on {@code centre}. */
    static void centred(final GuiGraphics g, final Font font, final String text, final int centre, final int y,
                        final int colour, final int ground) {
        Draw.text(g, font, text, centre - font.width(text) / 2, y, colour, ground);
    }

    /** A heading: the same font, {@code scale} times as large. */
    static void big(final GuiGraphics g, final Font font, final String text, final int x, final int y,
                    final int scale, final int colour, final int ground) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1.0F);
        Draw.text(g, font, text, 0, 0, colour, ground);
        g.pose().popPose();
    }

    /** Voidsoft's anvil, fourteen pixels across at one to one, drawn at a half or three quarters of that. */
    static void anvil(final GuiGraphics g, final int x, final int y, final float scale, final int colour) {
        final int cut = c().titleBar();
        part(g, x, y, scale, 0, 2, 14, 3, colour);
        part(g, x, y, scale, 13, 3, 3, 1, colour);
        part(g, x, y, scale, 4, 5, 6, 3, colour);
        part(g, x, y, scale, 2, 8, 10, 2, colour);
        part(g, x, y, scale, 6, 3, 2, 2, cut);
    }

    /** The arrow saying a song is on the disk, orange, or can be downloaded, grey. */
    static void arrow(final GuiGraphics g, final int x, final int y, final int colour) {
        fill(g, x + 3, y, 2, 5, colour);
        fill(g, x + 1, y + 3, 6, 1, colour);
        fill(g, x + 2, y + 4, 4, 1, colour);
        fill(g, x, y + 7, 8, 1, colour);
    }

    /** A triangle pointing right, {@code h} tall: the play button's. */
    static void triangle(final GuiGraphics g, final int x, final int y, final int h, final int colour) {
        for (int k = 0; k < (h + 1) / 2; k++) {
            fill(g, x + k, y + k, 1, h - 2 * k, colour);
        }
    }

    static void glyph(final GuiGraphics g, final Glyph glyph, final int x, final int y, final int colour) {
        final int cut = c().side();
        switch (glyph) {
            case HOME -> {
                fill(g, x + 1, y + 3, 6, 5, colour);
                fill(g, x, y + 3, 8, 1, colour);
                fill(g, x + 1, y + 2, 6, 1, colour);
                fill(g, x + 2, y + 1, 4, 1, colour);
                fill(g, x + 3, y, 2, 1, colour);
                fill(g, x + 3, y + 5, 2, 3, cut);
            }
            case SEARCH -> {
                fill(g, x, y, 5, 1, colour);
                fill(g, x, y + 4, 5, 1, colour);
                fill(g, x, y, 1, 5, colour);
                fill(g, x + 4, y, 1, 5, colour);
                fill(g, x + 5, y + 5, 1, 1, colour);
                fill(g, x + 6, y + 6, 2, 2, colour);
            }
            case LIBRARY -> {
                fill(g, x, y, 2, 8, colour);
                fill(g, x + 3, y, 2, 8, colour);
                fill(g, x + 6, y + 1, 2, 7, colour);
            }
            case SHUFFLE -> {
                fill(g, x, y + 1, 3, 1, colour);
                fill(g, x + 3, y + 2, 2, 3, colour);
                fill(g, x + 5, y + 5, 3, 1, colour);
                fill(g, x, y + 5, 3, 1, colour);
                fill(g, x + 5, y + 1, 3, 1, colour);
            }
            case PREVIOUS -> {
                fill(g, x, y, 1, 7, colour);
                for (int k = 0; k < 4; k++) {
                    fill(g, x + 1 + k, y + 3 - k, 1, 1 + 2 * k, colour);
                }
            }
            case NEXT -> {
                for (int k = 0; k < 4; k++) {
                    fill(g, x + k, y + k, 1, 7 - 2 * k, colour);
                }
                fill(g, x + 4, y, 1, 7, colour);
            }
            case REPEAT -> {
                fill(g, x, y + 1, 7, 1, colour);
                fill(g, x, y + 1, 1, 3, colour);
                fill(g, x + 1, y + 6, 7, 1, colour);
                fill(g, x + 7, y + 4, 1, 3, colour);
                fill(g, x + 5, y, 1, 3, colour);
                fill(g, x + 2, y + 5, 1, 3, colour);
            }
            case SPEAKER -> {
                fill(g, x, y + 2, 2, 3, colour);
                fill(g, x + 2, y + 1, 1, 5, colour);
                fill(g, x + 3, y, 1, 7, colour);
                fill(g, x + 5, y + 2, 1, 3, colour);
                fill(g, x + 7, y + 1, 1, 5, colour);
            }
            case MONITOR -> {
                fill(g, x, y, 8, 5, colour);
                fill(g, x + 1, y + 1, 6, 3, cut);
                fill(g, x + 3, y + 6, 2, 1, colour);
                fill(g, x + 2, y + 7, 4, 1, colour);
            }
        }
    }

    /**
     * A cover made of the album's own colours and initials, which is what is shown when its recording brings none:
     * the two colours picked by its name, split on the diagonal, and its initials in the corner.
     */
    static void cover(final GuiGraphics g, final Font font, final int x, final int y, final int size,
                      final String name) {
        final Covers covers = COVERS.get();
        final int pair = Math.floorMod(name.toLowerCase(Locale.ROOT).hashCode(), COVER_PAIRS);
        final int dark = covers.dark(pair);
        fill(g, x, y, size, size, dark);
        for (int k = 0; k < size; k += 2) {
            fill(g, x + k, y + size - k - 2, size - k, 2, covers.light(pair));
        }
        fill(g, x, y, size, size, covers.shade());
        final String initials = initialsOf(name);
        if (size < 30 || initials.isEmpty()) {
            return;
        }
        final int scale = size >= 64 ? 3 : 2;
        big(g, font, initials, x + 4, y + size - 8 * scale - 4, scale, covers.ink(), dark);
    }

    /** The first letters of an album's first two words, as its made cover shows them. */
    static String initialsOf(final String name) {
        final StringBuilder out = new StringBuilder();
        for (final String word : name.strip().split("[\\s\\-_]+")) {
            if (!word.isEmpty() && out.length() < 2 && Character.isLetterOrDigit(word.charAt(0))) {
                out.append(Character.toUpperCase(word.charAt(0)));
            }
        }
        return out.toString();
    }

    private static void part(final GuiGraphics g, final int x, final int y, final float scale, final int px,
                             final int py, final int w, final int h, final int colour) {
        final int x0 = x + Math.round(px * scale);
        final int y0 = y + Math.round(py * scale);
        fill(g, x0, y0, Math.max(1, Math.round((px + w) * scale) - Math.round(px * scale)),
                Math.max(1, Math.round((py + h) * scale) - Math.round(py * scale)), colour);
    }

    /**
     * The skin's colours.
     *
     * @param base     the page
     * @param side     the sidebar
     * @param foot     the bar along the foot
     * @param titleBar the bar along the top
     * @param line     the rules between parts
     * @param frame    the edge round the window
     * @param orange   Voidsoft's molten orange: what is on, playing or on the disk
     * @param ink      the text
     * @param inkDim   the second line of a row, and what is off
     * @param inkOff   what cannot be used
     * @param anvil    the anvil and the window's own buttons
     * @param box      the box of the Soundfoundry Server
     * @param online   a server that is there
     * @param offline  a server that is not
     * @param swatch   a playlist's square, and a button's ground
     * @param buttonOff a button that cannot be used
     * @param banner   the ground of a notice
     * @param track    the empty part of a slider
     * @param playing  the ground of the row playing
     * @param picked   the ground of the row picked
     * @param arrowOff the arrow of a song only streamed
     * @param glowHome the glow at the top of the home page
     * @param glowAlbum the glow at the top of an album's page
     * @param knob     the dark mark on a light button
     */
    record Colours(int base, int side, int foot, int titleBar, int line, int frame,
                   int orange, int ink, int inkDim, int inkOff, int anvil,
                   int box, int online, int offline, int swatch, int buttonOff, int banner,
                   int track, int playing, int picked, int arrowOff,
                   int glowHome, int glowAlbum, int knob) {
    }

    /**
     * The colours of the covers made for albums that bring none: eight pairs, a dark one filling the square and a
     * light one on its diagonal, then the shade laid over it and the colour of the initials.
     */
    record Covers(int dark0, int light0, int dark1, int light1, int dark2, int light2, int dark3, int light3,
                  int dark4, int light4, int dark5, int light5, int dark6, int light6, int dark7, int light7,
                  int shade, int ink) {

        int dark(final int pair) {
            return switch (pair) {
                case 1 -> dark1;
                case 2 -> dark2;
                case 3 -> dark3;
                case 4 -> dark4;
                case 5 -> dark5;
                case 6 -> dark6;
                case 7 -> dark7;
                default -> dark0;
            };
        }

        int light(final int pair) {
            return switch (pair) {
                case 1 -> light1;
                case 2 -> light2;
                case 3 -> light3;
                case 4 -> light4;
                case 5 -> light5;
                case 6 -> light6;
                case 7 -> light7;
                default -> light0;
            };
        }
    }
}
