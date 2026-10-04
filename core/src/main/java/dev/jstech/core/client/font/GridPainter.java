/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.font;

import dev.jstech.core.font.CellFont;
import dev.jstech.core.font.CellGlyphs;
import dev.jstech.core.font.CellRect;
import dev.jstech.core.font.GridLayout;
import dev.jstech.core.font.GridSpan;
import dev.jstech.core.font.IGridMetrics;
import dev.jstech.core.font.IGridPiece;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

/**
 * Draws text on a monospace grid: every character in a cell of its own, all the cells one width.
 *
 * <p>The grid draws in a {@link CellFont} when it is given one and in the game's font otherwise. A character the cell
 * font lacks is drawn in the game's font, in the middle of its cell, on the cell font's baseline. The box lines and
 * the blocks are drawn by the grid itself to fill the cell's width and the row's height ({@link CellGlyphs}), so a
 * frame or a bar joins from cell to cell and from row to row however far apart the rows are. A row is laid out once
 * into the fewest strings that land on the grid ({@link GridLayout}) and remembered for as long as the caller keeps
 * the object it was drawn for; the rows of a call go to the card in one batch.
 *
 * <p>The text is plain, with no shadow, as the series' screens are now; the colour of the ground the rows are on is
 * still taken, which is what a shadow worked out from it would need.
 *
 * @param <S> what the caller keeps a style as; the caller says the colour each style has where it draws
 */
public final class GridPainter<S> {

    private final @Nullable CellFont font;
    private final Style cellStyle;
    private final Map<Object, List<IGridPiece<S>>> worked = new WeakHashMap<>();
    private final Map<Integer, Integer> cellAdvances = new HashMap<>();
    private final Map<Integer, Integer> gameAdvances = new HashMap<>();
    private final Map<Long, List<CellRect>> wholes = new HashMap<>();
    private int generation = -1;

    /** How wide a cell is in the game's font: what nearly every letter of it takes. */
    public static final int GAME_CELL_WIDTH = 6;
    /** How tall a line of the game's font is. */
    public static final int GAME_CELL_HEIGHT = 9;
    /** How many rows of the game's letters are above their baseline. */
    private static final int GAME_BASELINE = 7;
    /** Where a colour's alpha starts, in its bits. */
    private static final int ALPHA_SHIFT = 24;

    /** @param font the font the grid draws in, or null for the game's own */
    public GridPainter(final @Nullable CellFont font) {
        this.font = font;
        this.cellStyle = font == null ? Style.EMPTY : Style.EMPTY.withFont(font.id());
    }

    /** The font the grid draws in, or null when it draws in the game's own. */
    public @Nullable CellFont font() {
        return font;
    }

    public int cellWidth() {
        return font == null ? GAME_CELL_WIDTH : font.cellWidth();
    }

    /** How tall a cell is: the row pitch at which the font's lines touch. */
    public int cellHeight() {
        return font == null ? GAME_CELL_HEIGHT : font.cellHeight();
    }

    /**
     * Draws rows downwards from a point, in the pose the caller has set up, each laid out once for as long as the
     * caller keeps the object it stands for.
     *
     * @param spansOf  the spans of a row, asked only the first time that row is drawn
     * @param pitch    how far apart the rows are, in the units of the pose; box lines and blocks fill it
     * @param colourOf the colour of a style here
     * @param ground   the colour of what the rows are drawn on
     */
    public <K> void draw(final GuiGraphics g, final Font gameFont, final List<K> rows,
                         final Function<K, List<GridSpan<S>>> spansOf, final int x, final int y, final int pitch,
                         final ToIntFunction<S> colourOf, final int ground) {
        forgetIfReloaded();
        final Matrix4f pose = g.pose().last().pose();
        g.drawManaged(() -> {
            int at = y;
            for (final K row : rows) {
                final List<IGridPiece<S>> pieces = worked.computeIfAbsent(row,
                        key -> layout(gameFont, spansOf.apply(row)));
                pieces(g, gameFont, pieces, x, at, pitch, colourOf, pose);
                at += pitch;
            }
        });
    }

    /** One row that changes too often to be worth remembering, such as the line being typed. */
    public void drawOnce(final GuiGraphics g, final Font gameFont, final List<GridSpan<S>> row, final int x,
                         final int y, final int pitch, final ToIntFunction<S> colourOf, final int ground) {
        forgetIfReloaded();
        final Matrix4f pose = g.pose().last().pose();
        g.drawManaged(() -> pieces(g, gameFont, layout(gameFont, row), x, y, pitch, colourOf, pose));
    }

    /** The pieces a row is drawn as, in order across it. */
    public List<IGridPiece<S>> layout(final Font gameFont, final List<GridSpan<S>> row) {
        return GridLayout.layout(row, metrics(gameFont));
    }

    private void pieces(final GuiGraphics g, final Font gameFont, final List<IGridPiece<S>> row, final int x,
                        final int y, final int pitch, final ToIntFunction<S> colourOf, final Matrix4f pose) {
        // The text sits on the cell font's baseline, so the top of the cell font's glyphs is the top of the row.
        final int textY = y + (font == null ? 0 : font.baseline() - GAME_BASELINE);
        for (final IGridPiece<S> piece : row) {
            final int colour = opaque(colourOf.applyAsInt(piece.style()));
            switch (piece) {
                case IGridPiece.Text<S> text -> gameFont.drawInBatch(FormattedCharSequence.forward(text.text(),
                                text.cellFont() ? cellStyle : Style.EMPTY), x + text.x(), textY, colour, false, pose,
                        g.bufferSource(), Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
                case IGridPiece.Whole<S> whole -> {
                    for (final CellRect rect : whole(whole.codePoint(), pitch)) {
                        final int alpha = (colour >>> ALPHA_SHIFT) * rect.alpha() / CellRect.SOLID;
                        g.fill(x + whole.x() + rect.x(), y + rect.y(), x + whole.x() + rect.x() + rect.width(),
                                y + rect.y() + rect.height(), (alpha << ALPHA_SHIFT) | (colour & 0xFFFFFF));
                    }
                }
            }
        }
    }

    private List<CellRect> whole(final int codePoint, final int pitch) {
        return wholes.computeIfAbsent(((long) codePoint << 32) | pitch,
                key -> CellGlyphs.of(codePoint, cellWidth(), pitch));
    }

    private IGridMetrics metrics(final Font gameFont) {
        return new IGridMetrics() {
            @Override
            public int cellWidth() {
                return GridPainter.this.cellWidth();
            }

            @Override
            public boolean inCellFont(final int codePoint) {
                return font != null && CellFonts.covers(font, codePoint);
            }

            @Override
            public int advance(final int codePoint, final boolean cellFont) {
                final Map<Integer, Integer> cache = cellFont ? cellAdvances : gameAdvances;
                return cache.computeIfAbsent(codePoint, key -> gameFont.width(FormattedText.of(
                        new String(Character.toChars(codePoint)), cellFont ? cellStyle : Style.EMPTY)));
            }
        };
    }

    /* The packs were loaded again: what the fonts draw, and how wide, may have changed with them. */
    private void forgetIfReloaded() {
        if (generation != CellFonts.generation()) {
            generation = CellFonts.generation();
            worked.clear();
            cellAdvances.clear();
            gameAdvances.clear();
        }
    }

    /**
     * The game's font draws a colour whose alpha is under four as opaque, so a colour written without an alpha still
     * shows; the blocks follow it, so both look the same.
     */
    private static int opaque(final int colour) {
        return colour >>> ALPHA_SHIFT < 4 ? colour | 0xFF << ALPHA_SHIFT : colour;
    }
}
