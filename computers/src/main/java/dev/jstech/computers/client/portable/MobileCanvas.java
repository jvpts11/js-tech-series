/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.portable;

import dev.jstech.core.client.gui.component.Draw;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * What a portable's screen is drawn with, in the GUI's pixels of the screen from its top left, half pixels allowed:
 * the systems were drawn at the window's resolution at the GUI's scale 2, so every shape is laid on a grid of half
 * pixels, and the game's font comes at its own sizes: {@link #NORMAL}, the mod's small one at three quarters, and the
 * clocks at whole multiples. Rounded shapes are cut row by row on that grid, as the pictures cut them.
 *
 * <p>What can be pressed is marked as it is drawn ({@link #press}), so a click finds the same places the picture shows.
 */
public final class MobileCanvas {

    private final GuiGraphics graphics;
    private final Font font;
    private final int width;
    private final int height;
    private final List<Press> presses = new ArrayList<>();

    /** The font's sizes, in half pixels to a pixel of the font: as the GUI draws it, three quarters, double, triple. */
    public static final float NORMAL = 2.0F;
    public static final float SMALL = 1.5F;
    public static final float BIG = 4.0F;
    public static final float HUGE = 6.0F;
    /* Half pixels to a GUI pixel. */
    private static final int HALF = 2;
    /* The pixels the game draws an item's icon at. */
    private static final float ITEM_PIXELS = 16.0F;
    private static final String ELLIPSIS = "...";

    /** A canvas over a screen {@code width} by {@code height} GUI pixels, drawn at the origin of {@code graphics}. */
    public MobileCanvas(final GuiGraphics graphics, final Font font, final int width, final int height) {
        this.graphics = graphics;
        this.font = font;
        this.width = width;
        this.height = height;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    /** The places marked to be pressed, the last drawn first: what is drawn over another is pressed first. */
    public List<Press> presses() {
        return List.copyOf(presses.reversed());
    }

    /** Marks the rectangle at {@code (x, y)} as what pressing it does. */
    public void press(final float x, final float y, final float w, final float h, final MobileTap tap) {
        presses.add(new Press(x, y, w, h, tap));
    }

    /** A rectangle in one colour. */
    public void rect(final float x, final float y, final float w, final float h, final int argb) {
        if (w <= 0 || h <= 0) {
            return;
        }
        half(() -> graphics.fill(h2(x), h2(y), h2(x) + h2(w), h2(y) + h2(h), argb));
    }

    /** A rectangle fading from {@code top} at its top to {@code bottom} at its foot. */
    public void gradient(final float x, final float y, final float w, final float h, final int top,
                         final int bottom) {
        half(() -> graphics.fillGradient(h2(x), h2(y), h2(x) + h2(w), h2(y) + h2(h), top, bottom));
    }

    /** A rectangle with corners rounded to {@code r}, filled, outlined a pixel wide, or both. */
    public void rounded(final float x, final float y, final float w, final float h, final float r,
                        @Nullable final Integer fill, @Nullable final Integer outline) {
        final int x0 = h2(x);
        final int y0 = h2(y);
        final int wide = h2(w);
        final int tall = h2(h);
        final int radius = h2(r);
        half(() -> {
            for (int row = 0; row < tall; row++) {
                final int in = inset(row, tall, radius);
                if (outline == null) {
                    graphics.fill(x0 + in, y0 + row, x0 + wide - in, y0 + row + 1, fill);
                    continue;
                }
                // The inside, a pixel in all round, filled; the ring between it and the edge, the outline.
                final boolean edgeRow = row < HALF || row >= tall - HALF;
                final int inner = edgeRow ? wide : Math.max(inset(row - HALF, tall - 2 * HALF, radius - HALF),
                        0) + HALF;
                if (edgeRow) {
                    graphics.fill(x0 + in, y0 + row, x0 + wide - in, y0 + row + 1, outline);
                    continue;
                }
                if (fill != null) {
                    graphics.fill(x0 + inner, y0 + row, x0 + wide - inner, y0 + row + 1, fill);
                }
                graphics.fill(x0 + in, y0 + row, x0 + inner, y0 + row + 1, outline);
                graphics.fill(x0 + wide - inner, y0 + row, x0 + wide - in, y0 + row + 1, outline);
            }
        });
    }

    /** A disc of radius {@code r} round {@code (cx, cy)}. */
    public void disc(final float cx, final float cy, final float r, final int argb) {
        ring(cx, cy, r, r, argb);
    }

    /** A ring {@code t} pixels thick inside radius {@code r} round {@code (cx, cy)}. */
    public void ring(final float cx, final float cy, final float r, final float t, final int argb) {
        final float outer = r * HALF;
        final float inner = Math.max(0.0F, (r - t) * HALF);
        final float x = cx * HALF;
        final float y = cy * HALF;
        half(() -> {
            for (int row = (int) Math.floor(y - outer); row < (int) Math.ceil(y + outer); row++) {
                final float dy = row + 0.5F - y;
                if (Math.abs(dy) >= outer) {
                    continue;
                }
                final float out = (float) Math.sqrt(outer * outer - dy * dy);
                final int left = Math.round(x - out);
                final int right = Math.round(x + out);
                if (Math.abs(dy) >= inner) {
                    graphics.fill(left, row, right, row + 1, argb);
                    continue;
                }
                final float in = (float) Math.sqrt(inner * inner - dy * dy);
                graphics.fill(left, row, Math.round(x - in), row + 1, argb);
                graphics.fill(Math.round(x + in), row, right, row + 1, argb);
            }
        });
    }

    /** A pixel glyph drawn from rows of {@code X}, each mark {@code k} pixels square. */
    public void glyph(final String[] rows, final float x, final float y, final int argb, final float k) {
        for (int j = 0; j < rows.length; j++) {
            for (int i = 0; i < rows[j].length(); i++) {
                if (rows[j].charAt(i) == 'X') {
                    rect(x + i * k, y + j * k, k, k, argb);
                }
            }
        }
    }

    /** A picture of {@code size} texels by {@code tall}, drawn {@code w} by {@code h} pixels at {@code (x, y)}. */
    public void picture(final ResourceLocation texture, final int size, final int tall, final float x, final float y,
                        final float w, final float h) {
        half(() -> graphics.blit(texture, h2(x), h2(y), h2(w), h2(h), 0.0F, 0.0F, size, tall, size, tall));
    }

    /**
     * The part of a picture {@code texW} by {@code texH} texels from {@code (u, v)}, {@code uw} by {@code vh} of them,
     * drawn {@code w} by {@code h} pixels at {@code (x, y)}; a negative {@code vh} reads it upside down, as a frame the
     * game rendered is stored.
     */
    public void picture(final ResourceLocation texture, final int texW, final int texH, final int u, final int v,
                        final int uw, final int vh, final float x, final float y, final float w, final float h) {
        half(() -> graphics.blit(texture, h2(x), h2(y), h2(w), h2(h), u, v, uw, vh, texW, texH));
    }

    /** The icon of {@code stack}, {@code size} pixels square, its top left at {@code (x, y)}. */
    public void item(final ItemStack stack, final float x, final float y, final float size) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(size / ITEM_PIXELS, size / ITEM_PIXELS, 1.0F);
        graphics.renderItem(stack, 0, 0);
        graphics.pose().popPose();
    }

    /** A line {@code t} pixels thick from {@code (x0, y0)} to {@code (x1, y1)}, laid on the grid of half pixels. */
    public void line(final float x0, final float y0, final float x1, final float y1, final float t, final int argb) {
        final float length = (float) Math.hypot(x1 - x0, y1 - y0);
        final int steps = Math.max(1, Math.round(length * HALF));
        for (int n = 0; n <= steps; n++) {
            final float along = (float) n / steps;
            rect(x0 + (x1 - x0) * along - t / 2.0F, y0 + (y1 - y0) * along - t / 2.0F, t, t, argb);
        }
    }

    /**
     * Writes {@code text} with its top at {@code y}, from {@code x} as {@code align} says, at {@code size}; bold
     * writes it twice half a font pixel apart, as the pictures did. Returns its width.
     */
    public float text(final String text, final float x, final float y, final int argb, final float size,
                      final Align align, final boolean bold) {
        final float w = width(text, size);
        final float left = switch (align) {
            case LEFT -> x;
            case CENTRE -> x - w / 2.0F;
            case RIGHT -> x - w;
        };
        half(() -> {
            write(text, left * HALF, y * HALF, argb, size);
            if (bold) {
                write(text, left * HALF + size / 2.0F, y * HALF, argb, size);
            }
        });
        return w;
    }

    /** Writes {@code text} at the normal size, from {@code x}. */
    public float text(final String text, final float x, final float y, final int argb, final float size) {
        return text(text, x, y, argb, size, Align.LEFT, false);
    }

    /** How wide {@code text} runs at {@code size}, in GUI pixels. */
    public float width(final String text, final float size) {
        return font.width(text) * size / HALF;
    }

    /** {@code text} cut to {@code wide}, an ellipsis at its end, as the systems cut a long name. */
    public String fit(final String text, final float wide, final float size) {
        if (width(text, size) <= wide) {
            return text;
        }
        String cut = text;
        while (!cut.isEmpty() && width(cut + ELLIPSIS, size) > wide) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut.strip() + ELLIPSIS;
    }

    /* Draws {@code drawing} on the grid of half pixels. */
    private void half(final Runnable drawing) {
        graphics.pose().pushPose();
        graphics.pose().scale(1.0F / HALF, 1.0F / HALF, 1.0F);
        drawing.run();
        graphics.pose().popPose();
    }

    /* The game's font at {@code size}, its top left at {@code (x, y)} in half pixels. */
    private void write(final String text, final float x, final float y, final int argb, final float size) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(size, size, 1.0F);
        Draw.text(graphics, font, text, 0, 0, argb);
        graphics.pose().popPose();
    }

    /* A length in GUI pixels, on the grid of half pixels. */
    private static int h2(final float value) {
        return Math.round(value * HALF);
    }

    /* How far in a rounded rectangle {@code tall} rows high with corners of {@code radius} starts at {@code row}. */
    private static int inset(final int row, final int tall, final int radius) {
        if (radius <= 0) {
            return 0;
        }
        final int fromEdge = Math.min(row, tall - 1 - row);
        if (fromEdge >= radius) {
            return 0;
        }
        final double dy = radius - fromEdge - 0.5;
        return (int) Math.round(radius - Math.sqrt(Math.max(0.0, (double) radius * radius - dy * dy)));
    }

    /** Where a line of text stands on the point it is written from. */
    public enum Align {
        LEFT,
        CENTRE,
        RIGHT
    }

    /** A place on the screen and what pressing it does. */
    public record Press(float x, float y, float w, float h, MobileTap tap) {

        /** Whether {@code (px, py)} is on it. */
        public boolean has(final double px, final double py) {
            return px >= x && px < x + w && py >= y && py < y + h;
        }
    }
}
