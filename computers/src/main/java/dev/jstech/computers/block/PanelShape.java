/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.block;

import java.util.Set;
import org.jetbrains.annotations.Nullable;

/**
 * The rectangle a group of flat monitors side by side makes, when it makes one.
 *
 * <p>Flat panels of one kind standing in one wall, facing one way and touching, join into one big screen, the way
 * ComputerCraft's and OpenComputers' monitors do, and with their limit: eight wide and six tall. They join only when
 * together they fill a whole rectangle; a group with a gap or a step in it, or one too big, stays a row of separate
 * monitors, so what a player builds is either clearly one screen or clearly many.
 *
 * <p>Cells are counted across the wall as its front is seen, {@code u} to the right and {@code v} up, from any cell of
 * the group. Pure maths, so a unit test can try the shapes.
 *
 * @param left   the leftmost column, as an offset from the cell counted from
 * @param bottom the lowest row, as an offset from that cell
 */
public record PanelShape(int left, int bottom, int width, int height) {

    public static final int MAX_WIDTH = 8;
    public static final int MAX_HEIGHT = 6;
    /** The most monitors one screen is made of. */
    public static final int MAX_CELLS = MAX_WIDTH * MAX_HEIGHT;

    private static final int HALF = 1 << 15;

    /**
     * The rectangle the cells fill, or null when they are one cell alone, leave a gap, or make more than the limit
     * allows either way.
     */
    @Nullable
    public static PanelShape of(final Set<Long> cells) {
        if (cells.size() < 2 || cells.size() > MAX_CELLS) {
            return null;
        }
        int minU = Integer.MAX_VALUE;
        int minV = Integer.MAX_VALUE;
        int maxU = Integer.MIN_VALUE;
        int maxV = Integer.MIN_VALUE;
        for (final long cell : cells) {
            minU = Math.min(minU, u(cell));
            maxU = Math.max(maxU, u(cell));
            minV = Math.min(minV, v(cell));
            maxV = Math.max(maxV, v(cell));
        }
        final int width = maxU - minU + 1;
        final int height = maxV - minV + 1;
        if (width > MAX_WIDTH || height > MAX_HEIGHT || width * height != cells.size()) {
            return null;
        }
        return new PanelShape(minU, minV, width, height);
    }

    /** One cell, {@code u} across and {@code v} up, packed for a set. */
    public static long cell(final int u, final int v) {
        return ((long) (u + HALF) << 32) | (v + HALF);
    }

    public static int u(final long cell) {
        return (int) (cell >>> 32) - HALF;
    }

    public static int v(final long cell) {
        return (int) (cell & 0xFFFFFFFFL) - HALF;
    }
}
