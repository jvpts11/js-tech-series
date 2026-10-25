/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import org.jetbrains.annotations.ApiStatus;

/**
 * How many parts of each kind a computer's case counts: the room it has, whatever board goes in it. The board then
 * says how many of those it can use; a case never seats more than it has room for, nor a board more than it has
 * sockets and slots for.
 *
 * @param processors the processor sockets the case has room for
 * @param memory     the memory modules
 * @param cards      the expansion cards; {@link #AS_THE_BOARD} leaves the number to the board alone
 * @param disks      the disks inside the case
 * @param bays       the 5.25-inch bays on its front
 * @param storage    the slots of the storage it keeps for the network beside its disks, none for a computer that
 *                   keeps none
 */
@ApiStatus.Experimental
public record CaseParts(int processors, int memory, int cards, int disks, int bays, int storage) {

    /** A count the case puts no limit on: the board's own decides. */
    public static final int AS_THE_BOARD = Integer.MAX_VALUE;

    public CaseParts {
        if (processors < 0 || memory < 0 || cards < 0 || disks < 0 || bays < 0 || storage < 0) {
            throw new IllegalArgumentException("a case counts no fewer than none of a part");
        }
    }
}
