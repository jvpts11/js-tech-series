/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.ApiStatus;

/**
 * How worn one power supply is, kept on its item so it goes with the supply from machine to machine: its own life
 * against its line's, drawn once the first time it is turned on, and the wear of each of its units, from nothing to
 * failed. Read it against the supply's {@link PsuSpec}, which says how many units it has.
 *
 * @param life its life against its line's, between 0.8 and 1.2; 0 until it is first turned on
 * @param wear the wear of each unit, from 0 to 1, in order; a unit with no entry is new, and 1 is a failed unit
 */
@ApiStatus.Experimental
public record PsuCondition(float life, List<Float> wear) {

    /** A supply never turned on. */
    public static final PsuCondition NEW = new PsuCondition(0f, List.of());

    public PsuCondition {
        final List<Float> clamped = new ArrayList<>(wear.size());
        for (final Float unit : wear) {
            clamped.add(unit == null ? 0f : Math.max(0f, Math.min(1f, unit)));
        }
        wear = List.copyOf(clamped);
        life = Math.max(0f, life);
    }

    /** The wear of unit {@code unit}, from 0 to 1. */
    public float wearOf(final int unit) {
        return unit >= 0 && unit < wear.size() ? wear.get(unit) : 0f;
    }

    /** How many of {@code spec}'s units have failed. */
    public int failedUnits(final PsuSpec spec) {
        int failed = 0;
        for (int unit = 0; unit < spec.units(); unit++) {
            if (wearOf(unit) >= 1f) {
                failed++;
            }
        }
        return failed;
    }

    /** Whether every unit has failed: the supply gives nothing. */
    public boolean failed(final PsuSpec spec) {
        return failedUnits(spec) == spec.units();
    }

    /** Whether it still gives, but a unit has failed or one is worn past the warning. */
    public boolean degraded(final PsuSpec spec) {
        return !failed(spec) && (failedUnits(spec) > 0 || worst(spec) >= PsuWear.DEGRADED);
    }

    /** The wear of the most worn unit still working, or 1 when none is. */
    public float worst(final PsuSpec spec) {
        float worst = -1f;
        for (int unit = 0; unit < spec.units(); unit++) {
            if (wearOf(unit) < 1f) {
                worst = Math.max(worst, wearOf(unit));
            }
        }
        return worst < 0f ? 1f : worst;
    }

    /** The same supply with its life drawn, when it has none yet. */
    public PsuCondition withLife(final float drawn) {
        return life > 0f ? this : new PsuCondition(drawn, wear);
    }
}
