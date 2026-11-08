/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

/**
 * The line's side of a UPS: the store that holds what the line brought for the load to draw on, which the battery
 * makes up when the line falls short and charges from when it brings more. A rack's store is its own, a UPS block's
 * the one its inlet fills.
 */
interface IUpsLine {

    /** What the store holds, in joules. */
    long stored();

    /**
     * Whether the line is there: it brought something since the last tick, or the store was left full, with no room
     * for it to fill. A UPS goes to its battery as the line goes, not once the store has run down.
     */
    boolean fed();

    /** Whether the store holds a tick of {@code watts} for the load, or the draw is off. */
    boolean canRun(double watts);

    /** Puts up to {@code joules} from the battery into the store; answers what it took. */
    long fromBattery(long joules);

    /** Takes up to {@code most} joules of what the store holds beyond a tick of {@code watts}; answers what it took. */
    long spareForCharging(long most, double watts);
}
