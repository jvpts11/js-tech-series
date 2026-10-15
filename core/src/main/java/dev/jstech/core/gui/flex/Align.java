/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui.flex;

/** Where a box stands across its row or column, or inside its grid cell. */
public enum Align {

    /** At the start: the top of a row, the left of a column. */
    START,
    /** In the middle. */
    CENTER,
    /** At the end: the bottom of a row, the right of a column. */
    END,
    /** Stretched over the whole of it, within the box's own largest size. */
    STRETCH
}
