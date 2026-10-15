/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.style;

import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.gui.style.Style;
import dev.jstech.core.gui.style.StyleRole;
import dev.jstech.core.gui.style.StyleSprite;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Draws a style's pieces: a sprite cut as the game cuts it, a texture's region in nine slices, a texture laid side by
 * side, or flat colours with their edge and bevel. A flat raised piece of a style whose finish has the double bevel
 * gets it too, in the style's bevel colours.
 */
public final class StylePainter {

    private StylePainter() {
    }

    /** Draws {@code sprite} over {@code (x, y)} sized {@code w} by {@code h}, in {@code style}'s colours. */
    public static void draw(final GuiGraphics g, final Style style, final StyleSprite sprite, final int x, final int y,
                            final int w, final int h) {
        if (w <= 0 || h <= 0) {
            return;
        }
        switch (sprite) {
            case StyleSprite.Sprite plain -> Draw.blended(() -> g.blitSprite(plain.id(), x, y, w, h));
            case StyleSprite.Slice slice -> Draw.blended(() -> nineSlice(g, slice, x, y, w, h));
            case StyleSprite.Tile tile -> Draw.blended(() -> tile(g, tile.texture(), tile.width(), tile.height(), x,
                    y, w, h));
            case StyleSprite.Flat flat -> flat(g, style, flat, x, y, w, h);
            case StyleSprite.Nothing ignored -> {
            }
            // The game's menu ground is the screen's to draw; as a piece of something else it is nothing.
            case StyleSprite.GameMenu ignored -> {
            }
        }
    }

    /** How tall a piece is drawn when it has a height of its own: a tile's; one pixel otherwise. */
    public static int naturalHeight(final StyleSprite sprite) {
        return sprite instanceof StyleSprite.Tile tile ? tile.height() : 1;
    }

    private static void nineSlice(final GuiGraphics g, final StyleSprite.Slice s, final int x, final int y,
                                  final int w, final int h) {
        final int b = Math.max(0, Math.min(s.border(), Math.min(Math.min(w, h) / 2, Math.min(s.width(),
                s.height()) / 2)));
        final int midW = w - 2 * b;
        final int midH = h - 2 * b;
        final int srcMidW = s.width() - 2 * s.border();
        final int srcMidH = s.height() - 2 * s.border();
        final int sb = s.border();
        final ResourceLocation t = s.texture();
        final int tw = s.textureWidth();
        final int th = s.textureHeight();
        // The corners as they are.
        g.blit(t, x, y, b, b, s.u(), s.v(), b, b, tw, th);
        g.blit(t, x + w - b, y, b, b, s.u() + s.width() - b, s.v(), b, b, tw, th);
        g.blit(t, x, y + h - b, b, b, s.u(), s.v() + s.height() - b, b, b, tw, th);
        g.blit(t, x + w - b, y + h - b, b, b, s.u() + s.width() - b, s.v() + s.height() - b, b, b, tw, th);
        if (midW > 0) {
            g.blit(t, x + b, y, midW, b, s.u() + sb, s.v(), srcMidW, b, tw, th);
            g.blit(t, x + b, y + h - b, midW, b, s.u() + sb, s.v() + s.height() - b, srcMidW, b, tw, th);
        }
        if (midH > 0) {
            g.blit(t, x, y + b, b, midH, s.u(), s.v() + sb, b, srcMidH, tw, th);
            g.blit(t, x + w - b, y + b, b, midH, s.u() + s.width() - b, s.v() + sb, b, srcMidH, tw, th);
        }
        if (midW > 0 && midH > 0) {
            g.blit(t, x + b, y + b, midW, midH, s.u() + sb, s.v() + sb, srcMidW, srcMidH, tw, th);
        }
    }

    private static void tile(final GuiGraphics g, final ResourceLocation texture, final int tw, final int th,
                             final int x, final int y, final int w, final int h) {
        for (int ty = 0; ty < h; ty += th) {
            final int rowH = Math.min(th, h - ty);
            for (int tx = 0; tx < w; tx += tw) {
                final int colW = Math.min(tw, w - tx);
                g.blit(texture, x + tx, y + ty, colW, rowH, 0, 0, colW, rowH, tw, th);
            }
        }
    }

    private static void flat(final GuiGraphics g, final Style style, final StyleSprite.Flat flat, final int x,
                             final int y, final int w, final int h) {
        flat.fill().ifPresent(fill -> g.fill(x, y, x + w, y + h, style.colour(fill)));
        flat.edge().ifPresent(edge -> Draw.outline(g, x, y, w, h, style.colour(edge)));
        final int light = flat.light().map(style::colour).orElseGet(() -> style.finish().doubleBevel()
                ? style.colour(StyleRole.BEVEL_LIGHT) : 0);
        final int dark = flat.dark().map(style::colour).orElseGet(() -> style.finish().doubleBevel()
                ? style.colour(StyleRole.BEVEL_DARK) : 0);
        final int topLeft = flat.sunken() ? dark : light;
        final int bottomRight = flat.sunken() ? light : dark;
        if ((topLeft >>> 24) != 0) {
            g.fill(x, y, x + w, y + 1, topLeft);
            g.fill(x, y, x + 1, y + h, topLeft);
        }
        if ((bottomRight >>> 24) != 0) {
            g.fill(x, y + h - 1, x + w, y + h, bottomRight);
            g.fill(x + w - 1, y, x + w, y + h, bottomRight);
        }
    }
}
