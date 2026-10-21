/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.tool;

import dev.jstech.core.gui.layout.ToolHudLayout;
import dev.jstech.core.item.ItemMode;
import dev.jstech.core.tool.MiningShape;
import dev.jstech.core.tool.ToolSpec;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphics;

/**
 * The small picture of a tool's mode on the ring of modes, drawn from what the mode mines: one block, an area as a
 * grid of its size, an area with depth as a tunnel, a vein as scattered ore. A mode that mines nothing of its own, of
 * a tool that mines, is the one block; a mode of anything else has no picture.
 */
final class ModeGlyphs {

    /* The scattered pieces of a vein, each two pixels square, within the picture. */
    private static final int[][] VEIN = {{2, 6}, {4, 5}, {5, 3}, {7, 4}, {8, 2}, {10, 3}, {6, 7}};
    /* The largest grid drawn: more cells would not show in the picture's ten pixels. */
    private static final int MOST_CELLS = 5;
    /* The span the grid of an area fills, in pixels. */
    private static final int GRID = 9;

    private ModeGlyphs() {
    }

    /** Whether the modes of a tool of {@code spec} have pictures: those of a tool that mines in any of them. */
    static boolean has(final ToolSpec spec) {
        return !spec.mining().isEmpty() || spec.minesAlways().isPresent();
    }

    /** Draws the picture of {@code mode} with its top left at ({@code x}, {@code y}). */
    static void draw(final GuiGraphics g, final ToolSpec spec, final ItemMode mode, final int x, final int y,
                     final int colour) {
        final Optional<MiningShape> shape = spec.mines(Optional.of(mode));
        if (shape.isEmpty()) {
            frame(g, x + 4, y + 2, x + 9, y + 7, colour);
            return;
        }
        switch (shape.get()) {
            case MiningShape.Area area when area.depth() > 1 -> {
                frame(g, x + 1, y + 1, x + ToolHudLayout.GLYPH_WIDTH - 1, y + ToolHudLayout.GLYPH_HEIGHT - 1, colour);
                frame(g, x + 3, y + 3, x + ToolHudLayout.GLYPH_WIDTH - 3, y + ToolHudLayout.GLYPH_HEIGHT - 3, colour);
            }
            case MiningShape.Area area -> grid(g, x, y, Math.min(MOST_CELLS, Math.max(area.width(), area.height())),
                    colour);
            case MiningShape.Vein vein -> {
                for (final int[] piece : VEIN) {
                    g.fill(x + piece[0], y + piece[1], x + piece[0] + 2, y + piece[1] + 2, colour);
                }
            }
        }
    }

    /* A grid of {@code cells} by {@code cells}, a pixel between them. */
    private static void grid(final GuiGraphics g, final int x, final int y, final int cells, final int colour) {
        final int pitch = Math.max(2, GRID / cells);
        final int cell = pitch - 1;
        for (int i = 0; i < cells; i++) {
            for (int j = 0; j < cells; j++) {
                g.fill(x + 1 + i * pitch, y + j * pitch, x + 1 + i * pitch + cell, y + j * pitch + cell, colour);
            }
        }
    }

    /* The outline of the box from ({@code x0}, {@code y0}) to ({@code x1}, {@code y1}), the far edges not in it. */
    private static void frame(final GuiGraphics g, final int x0, final int y0, final int x1, final int y1,
                              final int colour) {
        g.fill(x0, y0, x1, y0 + 1, colour);
        g.fill(x0, y1 - 1, x1, y1, colour);
        g.fill(x0, y0 + 1, x0 + 1, y1 - 1, colour);
        g.fill(x1 - 1, y0 + 1, x1, y1 - 1, colour);
    }
}
