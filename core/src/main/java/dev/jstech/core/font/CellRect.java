/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.font;

/**
 * A rectangle of a cell to fill, in the cell's pixels from its top left corner, with how much of the colour to put
 * there: 255 for all of it, less for the shades.
 */
public record CellRect(int x, int y, int width, int height, int alpha) {

    /** All of the colour. */
    public static final int SOLID = 255;
}
