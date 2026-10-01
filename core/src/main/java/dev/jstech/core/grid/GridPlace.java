/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.grid;

/**
 * Where something of a grid stands in a dimension: a wire in one lane of a block, or a whole block, for a device that
 * passes the grid through. A block of many wires holds a place for each, so wires of lines that never join can share
 * the block and each still be a part of its own run.
 *
 * @param pos  the block's position, packed into one number
 * @param lane the lane's number, or {@link #WHOLE} for the whole block
 */
public record GridPlace(long pos, int lane) {

    /** The lane of a place that is the whole block. */
    public static final int WHOLE = -1;

    public GridPlace {
        if (lane < WHOLE) {
            throw new IllegalArgumentException("a lane is numbered from 0, or WHOLE for the whole block: " + lane);
        }
    }

    /** The whole block at {@code pos}. */
    public static GridPlace whole(final long pos) {
        return new GridPlace(pos, WHOLE);
    }

    /** The wire in lane {@code lane} of the block at {@code pos}. */
    public static GridPlace wire(final long pos, final int lane) {
        if (lane == WHOLE) {
            throw new IllegalArgumentException("a wire runs in a lane, not the whole block");
        }
        return new GridPlace(pos, lane);
    }

    /** Whether this is a whole block rather than one wire in it. */
    public boolean isWhole() {
        return this.lane == WHOLE;
    }
}
