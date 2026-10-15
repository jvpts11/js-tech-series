/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui.flex;

/** How a box lays out what it holds. */
public enum Flow {

    /** Nothing: a box with no children, sized by what it shows. */
    LEAF,
    /** One after the other, left to right. */
    ROW,
    /** One under the other, top to bottom. */
    COLUMN,
    /** In the cells of a grid, row after row, its columns declared. */
    GRID,
    /** All over each other, as a ground under what stands on it. */
    STACK
}
