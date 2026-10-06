/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.guide;

import dev.jstech.core.JsCore;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.guide.GuideStyle;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import net.minecraft.client.gui.GuiGraphics;

/**
 * The little bar that fills while the manual key is held over an item, in the look of the manual it opens
 * ({@link GuideStyle.HoldBar}): plain in the series' accent; blue blocks lighting one after another in a dark, rimmed
 * track, as a desktop of the 2000s did while it loaded; or black and yellow stripes running in, as a machine's guard
 * is marked. Its colours are {@code jscore:guide/hold_bar}.
 */
@PaletteHolder
final class GuideHoldBar {

    /** How big the bar is, in the screen's units. */
    static final int WIDTH = 26;
    static final int HEIGHT = 7;

    private static final Palette<Colours> PALETTE = Palettes.declare(JsCore.MODID, "guide/hold_bar", new Colours(
            0xFF101418, 0xFF3A4452, 0xFF39D6C4, 0xFF000000, 0xFFB4B8C0, 0xFF8CBBFF, 0xFF2F6FE8, 0xFF173FA6,
            0xFFF2C200, 0xFF161616, 0xFF3A3A3A));
    /* A lit block of the blocks bar, and the room between two. */
    private static final int BLOCK = 3;
    private static final int BLOCK_GAP = 1;
    /*
     * How many columns a stripe of the guard's marking takes before the other colour. Two, shifted a column a row,
     * still read as slanted stripes on a bar three rows tall; wider ones read as steps.
     */
    private static final int STRIPE = 2;

    private GuideHoldBar() {
    }

    /** Draws the bar with its top left at ({@code x}, {@code y}), filled to {@code progress}, from 0 to 1. */
    static void draw(final GuiGraphics g, final int x, final int y, final float progress,
                     final GuideStyle.HoldBar kind) {
        final float done = Math.max(0.0F, Math.min(1.0F, progress));
        switch (kind) {
            case PLAIN -> plain(g, x, y, done);
            case BLOCKS -> blocks(g, x, y, done);
            case HAZARD -> hazard(g, x, y, done);
        }
    }

    private static void plain(final GuiGraphics g, final int x, final int y, final float done) {
        final Colours c = PALETTE.get();
        g.fill(x, y + 1, x + WIDTH, y + HEIGHT - 1, c.plainTrack());
        Draw.outline(g, x, y + 1, WIDTH, HEIGHT - 2, c.plainEdge());
        g.fill(x + 1, y + 2, x + 1 + Math.round(done * (WIDTH - 2)), y + HEIGHT - 2, c.plainFill());
    }

    /* A dark track with a light rim and its corners cut round, and the blocks in it lit from the left. */
    private static void blocks(final GuiGraphics g, final int x, final int y, final float done) {
        final Colours c = PALETTE.get();
        g.fill(x + 1, y, x + WIDTH - 1, y + HEIGHT, c.blocksEdge());
        g.fill(x, y + 1, x + WIDTH, y + HEIGHT - 1, c.blocksEdge());
        g.fill(x + 1, y + 1, x + WIDTH - 1, y + HEIGHT - 1, c.blocksTrack());
        final int count = (WIDTH - 4 + BLOCK_GAP) / (BLOCK + BLOCK_GAP);
        final int lit = (int) Math.ceil(done * count);
        for (int i = 0; i < lit; i++) {
            final int bx = x + 2 + i * (BLOCK + BLOCK_GAP);
            g.fill(bx, y + 2, bx + BLOCK, y + 3, c.blocksTop());
            g.fill(bx, y + 3, bx + BLOCK, y + HEIGHT - 3, c.blocksMiddle());
            g.fill(bx, y + HEIGHT - 3, bx + BLOCK, y + HEIGHT - 2, c.blocksBottom());
        }
    }

    /* Diagonal stripes of black and yellow running in from the left, over a dark track. */
    private static void hazard(final GuiGraphics g, final int x, final int y, final float done) {
        final Colours c = PALETTE.get();
        g.fill(x, y + 1, x + WIDTH, y + HEIGHT - 1, c.hazardBlack());
        g.fill(x + 1, y + 2, x + WIDTH - 1, y + HEIGHT - 2, c.hazardEmpty());
        final int filled = Math.round(done * (WIDTH - 2));
        for (int column = 0; column < filled; column++) {
            for (int row = 0; row < HEIGHT - 4; row++) {
                final boolean yellow = Math.floorMod(column + row, 2 * STRIPE) < STRIPE;
                g.fill(x + 1 + column, y + 2 + row, x + 2 + column, y + 3 + row,
                        yellow ? c.hazardYellow() : c.hazardBlack());
            }
        }
    }

    /**
     * The bar's colours: the plain one's track, rim and fill; the blocks' track, rim, and a block's top, middle and
     * foot; the guard's yellow and black, and its track not yet filled.
     */
    private record Colours(int plainTrack, int plainEdge, int plainFill, int blocksTrack, int blocksEdge,
                           int blocksTop, int blocksMiddle, int blocksBottom, int hazardYellow, int hazardBlack,
                           int hazardEmpty) {
    }
}
