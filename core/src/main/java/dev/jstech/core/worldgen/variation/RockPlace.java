/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.variation;

/**
 * Where a block of rock lies, as geology tells its variants apart: how deep under the top of the ground, how steep the
 * ground is round it, whether it lies open to the sky or open on a side, and how high. Pure.
 *
 * @param depth     how many blocks under the top of the ground it lies, 0 at the top
 * @param slope     how many blocks the ground rises or falls within two blocks of it, the steepest way: 0 on a
 *                  plain, 2 or more on a scarp
 * @param openAbove whether nothing solid lies on it: the top of the ground
 * @param openSide  whether nothing solid lies beside it on some side: a face of a scarp, a wall or a cut
 * @param y         its height
 */
public record RockPlace(int depth, int slope, boolean openAbove, boolean openSide, int y) {

    /** Whether nothing open lies round it at all: rock buried in rock. */
    public boolean buried() {
        return !this.openAbove && !this.openSide;
    }
}
