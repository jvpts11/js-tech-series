/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.energy;

/**
 * How full a store looks on its own model, as bars or a level: a store holding anything shows at least one step, and
 * only a full one shows them all. A storage block keeps its block state's charge at this, so its model shows it with
 * no screen opened:
 *
 * <pre>{@code
 * fields().mirror(CHARGE, () -> EnergyCharge.level(store.stored(), store.capacity(), 4));
 * }</pre>
 *
 * Pure: no game types.
 */
public final class EnergyCharge {

    private EnergyCharge() {
    }

    /** The step, from 0 to {@code steps}, that {@code stored} of {@code capacity} shows. */
    public static int level(final long stored, final long capacity, final int steps) {
        if (steps <= 0) {
            throw new IllegalArgumentException("a store shows its charge in one step or more, not " + steps);
        }
        if (stored <= 0 || capacity <= 0) {
            return 0;
        }
        if (stored >= capacity) {
            return steps;
        }
        final int shown = (int) Math.ceil((double) stored / capacity * steps);
        return Math.max(1, Math.min(steps - 1, shown));
    }
}
