/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock;

/**
 * A shape given by a rule, with no pattern drawn: any box between a least and a most size, with a frame on its edges
 * and ports or glass on its faces, as a dynamic tank; a floor of any outline with walls round it, as a smeltery; any
 * filled cuboid of units, as a crafting processor. Its size is whatever the player built, within its limits. Pure.
 */
public sealed interface RuleShape extends IStructureShape permits BoxShape, WallsShape, CuboidShape {

    /** The most blocks a side of a shape by rule may be. */
    int MOST_SIDE = 64;
    /** The most places a floor of a floor and walls may have. */
    int MOST_AREA = 4096;

    /** Refuses a limit of a size beyond what a shape by rule may be. */
    static void requireWithin(final Limit size, final int most, final String what) {
        if (size.least() < 1 || size.most() > most) {
            throw new IllegalArgumentException("the " + what + " of a shape by rule runs from one to " + most
                    + ", not " + size.least() + " to " + size.most());
        }
    }
}
