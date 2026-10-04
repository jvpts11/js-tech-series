/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.font;

/**
 * One thing to draw on a grid's row, where across the row it starts, in pixels of the cell.
 *
 * @param <S> the caller's style
 */
public sealed interface IGridPiece<S> permits IGridPiece.Text, IGridPiece.Whole {

    int x();

    S style();

    /**
     * Characters drawn as a string, from {@code x}.
     *
     * @param cellFont whether they are drawn in the grid's own font, or in the game's font for characters the grid's
     *                 font does not have
     */
    record Text<S>(String text, int x, S style, boolean cellFont) implements IGridPiece<S> {
    }

    /** A box line or a block, drawn by the grid to fill the cell that starts at {@code x}. */
    record Whole<S>(int codePoint, int x, S style) implements IGridPiece<S> {
    }
}
