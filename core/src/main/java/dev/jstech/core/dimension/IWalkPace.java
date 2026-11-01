/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.dimension;

/**
 * The pace a creature's arms and legs swing at as it walks, the game's own times this: slower where the ground pulls
 * less, by the square root of its pull, as a pendulum swings, the stride as wide. On the Moon, a sixth of the
 * Overworld's pull, a walk swings at four tenths of the pace. Where nothing pulls at all, limbs still swing, as slowly
 * as the slowest pace, rather than stop mid stride. Every creature's walk takes its pace from the pull where it stands,
 * each time its walk is worked out ({@link DimensionRules#gravityAt}).
 */
public interface IWalkPace {

    /** The slowest pace a walk swings at, where nothing pulls. */
    float SLOWEST = 0.25F;

    /** The pace a walk swings at under {@code pull}, as a share of the Overworld's. */
    static float of(final double pull) {
        return (float) Math.sqrt(Math.max(SLOWEST * SLOWEST, pull));
    }

    /** Sets the pace this walk swings at from now on. */
    void jscore$pace(float pace);
}
