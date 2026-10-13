/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.font;

/**
 * A stretch of a grid's row in one style: each of its characters takes one cell.
 *
 * @param <S> what the caller keeps a style as, which the grid hands back with the pieces it lays out
 */
public record GridSpan<S>(String text, S style) {
}
