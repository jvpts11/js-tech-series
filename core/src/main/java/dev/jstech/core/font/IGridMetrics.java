/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.font;

/** What a grid needs to know of the fonts it draws in to lay a row out. */
public interface IGridMetrics {

    /** How wide a cell is, in pixels. */
    int cellWidth();

    /** Whether the grid's own font has that character; the rest are drawn in the game's font. */
    boolean inCellFont(int codePoint);

    /** How far drawing that character moves the pen, in the grid's font or the game's. */
    int advance(int codePoint, boolean cellFont);
}
