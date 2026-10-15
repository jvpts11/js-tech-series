/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui.flex;

/** Where the room nobody grows into goes, along a row or a column. */
public enum Justify {

    /** All of it after the boxes: they gather at the start. */
    START,
    /** Half before them and half after: they gather in the middle. */
    CENTER,
    /** All of it before them: they gather at the end. */
    END,
    /** Shared between them, none at the ends. */
    SPACE_BETWEEN,
    /** Shared around each of them, half as much at the ends as between two. */
    SPACE_AROUND
}
