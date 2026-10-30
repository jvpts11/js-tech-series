/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.skin;

/**
 * A painted skin: its size in texels and its colours, row by row.
 *
 * @param width  how many texels across
 * @param height how many down
 * @param argb   its colours, a texel each, row by row
 */
public record Picture(int width, int height, int[] argb) {
}
