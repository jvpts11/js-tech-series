/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui.flex;

/**
 * A width and a height, in pixels.
 *
 * @param width  how wide
 * @param height how tall
 */
public record Size(int width, int height) {

    /** Nothing at all. */
    public static final Size ZERO = new Size(0, 0);

    public Size {
        width = Math.max(0, width);
        height = Math.max(0, height);
    }
}
