/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.machine;

import java.util.List;

/**
 * What upgrades do to a machine: how much faster it works, and how much more energy a tick it spends for it. Several
 * upgrades multiply, so two speed upgrades of 1.5 make a machine 2.25 times as fast; an upgrade that saves energy
 * spends less than once.
 *
 * @param speed  how many times as fast it works; 1 as it was built
 * @param energy how many times the energy a tick it spends; 1 as it was built
 */
public record UpgradeEffect(double speed, double energy) {

    /** No upgrade at all. */
    public static final UpgradeEffect NONE = new UpgradeEffect(1.0, 1.0);

    public UpgradeEffect {
        if (!(speed > 0.0) || !(energy >= 0.0) || Double.isInfinite(speed) || Double.isInfinite(energy)) {
            throw new IllegalArgumentException("an upgrade has a speed above 0 and an energy of 0 or more; got "
                    + speed + " and " + energy);
        }
    }

    /** What every upgrade in {@code each} does together, each counted as many times as there are of it. */
    public static UpgradeEffect combine(final List<Counted> each) {
        double speed = 1.0;
        double energy = 1.0;
        for (final Counted counted : each) {
            speed *= Math.pow(counted.effect().speed(), counted.count());
            energy *= Math.pow(counted.effect().energy(), counted.count());
        }
        // A machine stacked with a great many upgrades must still have an effect: past what a double holds the
        // product stays at the largest (or, for slowing ones, the smallest) value it can.
        return new UpgradeEffect(Math.max(Double.MIN_VALUE, Math.min(Double.MAX_VALUE, speed)),
                Math.min(Double.MAX_VALUE, energy));
    }

    /** The ticks a recipe of {@code ticks} takes with this effect: never under one, never past an int. */
    public int ticks(final int ticks) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(1L, (long) Math.ceil(ticks / speed)));
    }

    /** The energy a tick a recipe of {@code perTick} spends with this effect, rounded up. */
    public long energyPerTick(final long perTick) {
        return (long) Math.ceil(perTick * energy);
    }

    /**
     * One upgrade and how many of it a machine holds.
     *
     * @param effect what one does
     * @param count  how many there are
     */
    public record Counted(UpgradeEffect effect, int count) {
    }
}
