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
 * the block and each still be a part of its own run. An overhead line stands in places of its own: each insulator of a
 * block it is hung from ({@link #anchor}), and each wire hung, known by the block it is hung from and a number of its
 * own there ({@link #hung}).
 *
 * @param pos  the block's position, packed into one number
 * @param lane the lane's number, or {@link #WHOLE} for the whole block; from {@link #ANCHORS} an insulator, from
 *             {@link #HUNG} a hung wire
 */
public record GridPlace(long pos, int lane) {

    /** The lane of a place that is the whole block. */
    public static final int WHOLE = -1;
    /** The first number of a block's insulators, the first after every lane of a cable block. */
    public static final int ANCHORS = 64;
    /** The first number of the wires hung from a block, after its insulators. */
    public static final int HUNG = 128;

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

    /** The insulator numbered {@code index} of the block at {@code pos}, which an overhead wire is hung from. */
    public static GridPlace anchor(final long pos, final int index) {
        if (index < 0 || index >= HUNG - ANCHORS) {
            throw new IllegalArgumentException("a block has insulators 0 to " + (HUNG - ANCHORS - 1) + ", not "
                    + index);
        }
        return new GridPlace(pos, ANCHORS + index);
    }

    /** The wire numbered {@code slot} among those hung from the block at {@code pos}. */
    public static GridPlace hung(final long pos, final int slot) {
        if (slot < 0) {
            throw new IllegalArgumentException("a hung wire's number counts from 0, not " + slot);
        }
        return new GridPlace(pos, HUNG + slot);
    }

    /** Whether this is a whole block rather than one wire in it. */
    public boolean isWhole() {
        return this.lane == WHOLE;
    }

    /** Whether this is a wire in a lane of a cable block. */
    public boolean isLane() {
        return this.lane >= 0 && this.lane < ANCHORS;
    }

    /** Whether this is an insulator an overhead wire is hung from. */
    public boolean isAnchor() {
        return this.lane >= ANCHORS && this.lane < HUNG;
    }

    /** Whether this is a wire hung between insulators. */
    public boolean isHung() {
        return this.lane >= HUNG;
    }
}
