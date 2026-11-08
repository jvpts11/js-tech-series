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
 * Immutable specification of a power supply unit.
 *
 * <p>An auto-scaling PSU is one whose output is not a fixed wattage but dimensions itself to whatever
 * the rest of the build draws, so it is treated as always able to satisfy the power draw. The
 * {@code wattage} of an auto-scaling unit is only a nominal/display value and is not used to gate the
 * build's power check.
 *
 * <p>A supply wears out while it is on and in the end fails ({@link PsuWear}): {@code lifeDays} is how long it lasts
 * at full load, in game days, and lives longer the less of its rating it gives. A redundant supply has more than one
 * {@code units}, each wearing on its own and sharing the load; it fails only when every one has.
 *
 * @param wattage           what it gives at most, in watts
 * @param efficiencyPercent the share of what it takes from the line that reaches the parts
 * @param autoScaling       whether it sizes itself to the build and never refuses one
 * @param lifeDays          how long it lasts at full load, in game days
 * @param units             how many units it has, each with its own wear; more than one is a redundant supply
 */
@ApiStatus.Experimental
public record PsuSpec(int wattage, int efficiencyPercent, boolean autoScaling, int lifeDays, int units) {

    /** The life of a supply that names none: the MF PowerGold line's, a good supply's. */
    public static final int DEFAULT_LIFE_DAYS = 100;

    public PsuSpec {
        if (wattage <= 0) {
            throw new IllegalArgumentException("wattage must be > 0; got " + wattage);
        }
        if (efficiencyPercent < 1 || efficiencyPercent > 100) {
            throw new IllegalArgumentException(
                    "efficiencyPercent must be in 1..100; got " + efficiencyPercent);
        }
        if (lifeDays <= 0) {
            throw new IllegalArgumentException("lifeDays must be > 0; got " + lifeDays);
        }
        if (units < 1) {
            throw new IllegalArgumentException("units must be >= 1; got " + units);
        }
    }

    /** A supply of one unit with a good supply's life. */
    public PsuSpec(final int wattage, final int efficiencyPercent, final boolean autoScaling) {
        this(wattage, efficiencyPercent, autoScaling, DEFAULT_LIFE_DAYS, 1);
    }

    /**
     * Convenience constructor for a conventional, fixed-wattage PSU (not auto-scaling).
     */
    public PsuSpec(final int wattage, final int efficiencyPercent) {
        this(wattage, efficiencyPercent, false);
    }

    /** A conventional, fixed-wattage supply of one unit that lasts {@code lifeDays} at full load. */
    public PsuSpec(final int wattage, final int efficiencyPercent, final int lifeDays) {
        this(wattage, efficiencyPercent, false, lifeDays, 1);
    }

    /** Whether it has more than one unit, so one can fail and the others carry the load. */
    public boolean redundant() {
        return units > 1;
    }
}
