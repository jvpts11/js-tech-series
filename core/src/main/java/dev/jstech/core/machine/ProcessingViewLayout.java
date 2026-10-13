/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.machine;

import java.math.BigDecimal;

/**
 * Where a processing recipe's parts go when a recipe viewer shows it: the inputs on the left, items first and then
 * fluids, in rows of up to three; an arrow; the outputs on the right in rows of up to two; and under them a line with
 * the time and the energy. Both viewers draw from this one layout, so a recipe looks the same in either.
 *
 * <p>Pure: no game types, so the arithmetic is checked without the game.
 *
 * @param inputs  how many inputs, items and fluids together
 * @param outputs how many outputs, items and fluids together
 */
public record ProcessingViewLayout(int inputs, int outputs) {

    /** A slot's side, border included. */
    public static final int SLOT = 18;
    public static final int ARROW_WIDTH = 24;
    public static final int ARROW_HEIGHT = 17;
    /** The room under the slots for the line of time and energy: the font's height and a pixel above it. */
    public static final int TEXT_LINE = 10;
    private static final int INPUT_COLUMNS = 3;
    private static final int OUTPUT_COLUMNS = 2;
    private static final int GAP = 4;
    private static final int TICKS_PER_SECOND = 20;
    /** Wide enough for the line of time and energy under the narrowest recipe. */
    private static final int MIN_WIDTH = 104;

    public ProcessingViewLayout {
        if (inputs < 0 || outputs < 0) {
            throw new IllegalArgumentException("a recipe has no fewer than no inputs and outputs");
        }
    }

    /**
     * A slot's top-left corner.
     *
     * @param x across from the recipe's left edge
     * @param y down from its top edge
     */
    public record Point(int x, int y) {
    }

    public int width() {
        return Math.max(MIN_WIDTH, outputLeft() + columns(outputs, OUTPUT_COLUMNS) * SLOT);
    }

    public int height() {
        return slotsHeight() + TEXT_LINE;
    }

    /** Where input {@code index} goes, counting the items first and the fluids after them. */
    public Point input(final int index) {
        return slot(index, inputs, INPUT_COLUMNS, 0);
    }

    /** Where output {@code index} goes, counting the items first and the fluids after them. */
    public Point output(final int index) {
        return slot(index, outputs, OUTPUT_COLUMNS, outputLeft());
    }

    /** Where the arrow goes, halfway down the slots. */
    public Point arrow() {
        return new Point(columns(inputs, INPUT_COLUMNS) * SLOT + GAP, (slotsHeight() - ARROW_HEIGHT) / 2);
    }

    /** Where the line of time and energy starts. */
    public Point textLine() {
        return new Point(0, slotsHeight() + 1);
    }

    /**
     * A recipe's time in seconds, as few digits as say it exactly: 200 ticks is "10", 10 ticks "0.5", 1 tick "0.05".
     */
    public static String seconds(final int ticks) {
        return BigDecimal.valueOf(ticks).divide(BigDecimal.valueOf(TICKS_PER_SECOND)).stripTrailingZeros()
                .toPlainString();
    }

    private int outputLeft() {
        return arrow().x() + ARROW_WIDTH + GAP;
    }

    private int slotsHeight() {
        return Math.max(rows(inputs, INPUT_COLUMNS), rows(outputs, OUTPUT_COLUMNS)) * SLOT;
    }

    /* A slot in a grid of {@code count} slots, the grid centred down the slots' height. */
    private Point slot(final int index, final int count, final int maxColumns, final int left) {
        if (index < 0 || index >= count) {
            throw new IndexOutOfBoundsException("slot " + index + " of " + count);
        }
        final int columns = columns(count, maxColumns);
        final int top = (slotsHeight() - rows(count, maxColumns) * SLOT) / 2;
        return new Point(left + index % columns * SLOT, top + index / columns * SLOT);
    }

    /* At least one column, so an empty side still keeps its place. */
    private static int columns(final int count, final int maxColumns) {
        return Math.max(1, Math.min(maxColumns, count));
    }

    private static int rows(final int count, final int maxColumns) {
        return Math.max(1, (count + maxColumns - 1) / maxColumns);
    }
}
