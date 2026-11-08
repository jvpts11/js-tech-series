/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.material.Material;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;

/**
 * Draws a material of J's Core in one of its states of matter in a 16x16 cell, each state its own way so a grid tells
 * them apart at a glance (computers/docs/design/storage.md): a gas a translucent cloud of its colour, a plasma glowing
 * from its centre, a supercritical fluid half liquid and half gas with no line between them, a slurry grainy, exotic
 * matter changing colour.
 *
 * <p>Its own colours are the palette {@code jsc:app/matter_sprite}: the frame, the sheen, and the glow's white.
 */
@PaletteHolder
public final class MatterSprite {

    private static final Palette<Colours> PALETTE = Palettes.declare(JsComputers.MODID, "app/matter_sprite",
            new Colours(0xFF202020, 0x40FFFFFF, 0xFFFFFFFF, 0xFF808080));

    /* The cloud a gas is drawn as, a row of the cell to a string: '#' its body, '+' its lighter puffs. */
    private static final String[] CLOUD = {
            "                ",
            "                ",
            "      ++        ",
            "     ++++  ++   ",
            "   ++####+++++  ",
            "  +##########+  ",
            " +############+ ",
            " ############## ",
            " ############## ",
            "  ############  ",
            "   ##########   ",
            "                ",
    };
    /* How opaque a gas's body and its puffs are, and a supercritical fluid's gas half. */
    private static final int CLOUD_ALPHA = 0xA0;
    private static final int PUFF_ALPHA = 0x60;
    private static final int HALF_GAS_ALPHA = 0x70;
    /* How long exotic matter takes to go round every colour, in milliseconds. */
    private static final long EXOTIC_CYCLE_MS = 6_000L;

    private MatterSprite() {
    }

    public static void draw(final GuiGraphics g, final StorageKey key, final int x, final int y) {
        final Colours c = PALETTE.get();
        final int colour = key.material().map(Material::colour).map(FastColor.ARGB32::opaque).orElse(c.unknown());
        switch (key.kind()) {
            case GAS -> cloud(g, x, y, colour);
            case PLASMA -> glow(g, x, y, colour, c);
            case SUPERCRITICAL -> halves(g, x, y, colour, c);
            case SLURRY -> grain(g, x, y, colour, c);
            default -> exotic(g, x, y, colour, c);
        }
    }

    /* A gas: a cloud of its colour you can see through, its top puffs lighter still. */
    private static void cloud(final GuiGraphics g, final int x, final int y, final int colour) {
        for (int row = 0; row < CLOUD.length; row++) {
            for (int col = 0; col < CLOUD[row].length(); col++) {
                final char at = CLOUD[row].charAt(col);
                if (at != ' ') {
                    final int alpha = at == '#' ? CLOUD_ALPHA : PUFF_ALPHA;
                    g.fill(x + col, y + 2 + row, x + col + 1, y + 3 + row, alpha << 24 | colour & 0xFFFFFF);
                }
            }
        }
    }

    /* A plasma: its colour at the edge of the swatch, brighter towards a white-hot centre. */
    private static void glow(final GuiGraphics g, final int x, final int y, final int colour, final Colours c) {
        g.fill(x + 1, y + 1, x + 15, y + 15, c.frame());
        for (int ring = 0; ring < 6; ring++) {
            final int shade = mix(colour, c.white(), ring / 6.0F);
            g.fill(x + 2 + ring, y + 2 + ring, x + 14 - ring, y + 14 - ring, shade);
        }
    }

    /* A supercritical fluid: liquid on the left, gas on the right, one fading into the other with no line. */
    private static void halves(final GuiGraphics g, final int x, final int y, final int colour, final Colours c) {
        g.fill(x + 1, y + 1, x + 15, y + 15, c.frame());
        for (int col = 0; col < 12; col++) {
            final float gas = Mth.clamp((col - 4) / 4.0F, 0.0F, 1.0F);
            final int alpha = Math.round(0xFF - (0xFF - HALF_GAS_ALPHA) * gas);
            g.fill(x + 2 + col, y + 2, x + 3 + col, y + 14, alpha << 24 | colour & 0xFFFFFF);
        }
        g.fill(x + 2, y + 2, x + 8, y + 4, c.sheen());
    }

    /* A slurry: its colour with grains darker and lighter through it. */
    private static void grain(final GuiGraphics g, final int x, final int y, final int colour, final Colours c) {
        g.fill(x + 1, y + 1, x + 15, y + 15, c.frame());
        g.fill(x + 2, y + 2, x + 14, y + 14, colour);
        for (int row = 0; row < 12; row++) {
            for (int col = 0; col < 12; col++) {
                final int seed = col * 7 + row * 13;
                if (seed % 5 == 0) {
                    g.fill(x + 2 + col, y + 2 + row, x + 3 + col, y + 3 + row, mix(colour, c.frame(), 0.45F));
                } else if (seed % 11 == 3) {
                    g.fill(x + 2 + col, y + 2 + row, x + 3 + col, y + 3 + row, mix(colour, c.white(), 0.35F));
                }
            }
        }
    }

    /* Exotic matter: its colour turned round every hue in turn, as nothing ordinary is. */
    private static void exotic(final GuiGraphics g, final int x, final int y, final int colour, final Colours c) {
        g.fill(x + 1, y + 1, x + 15, y + 15, c.frame());
        final float turn = (Util.getMillis() % EXOTIC_CYCLE_MS) / (float) EXOTIC_CYCLE_MS;
        for (int row = 0; row < 12; row++) {
            final int shade = FastColor.ARGB32.opaque(Mth.hsvToRgb((turn + row / 24.0F) % 1.0F, 0.7F, 0.95F));
            g.fill(x + 2, y + 2 + row, x + 14, y + 3 + row, mix(shade, colour, 0.25F));
        }
        g.fill(x + 2, y + 2, x + 14, y + 4, c.sheen());
    }

    /* Two opaque colours mixed, {@code share} of the second. */
    private static int mix(final int a, final int b, final float share) {
        final int r = Math.round(((a >> 16) & 0xFF) * (1 - share) + ((b >> 16) & 0xFF) * share);
        final int gr = Math.round(((a >> 8) & 0xFF) * (1 - share) + ((b >> 8) & 0xFF) * share);
        final int bl = Math.round((a & 0xFF) * (1 - share) + (b & 0xFF) * share);
        return FastColor.ARGB32.opaque(r << 16 | gr << 8 | bl);
    }

    /** The swatch's frame, the sheen along its top, the white a plasma glows to, and an unknown material's grey. */
    private record Colours(int frame, int sheen, int white, int unknown) {
    }
}
