/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.matter;

import dev.jstech.core.material.MatterState;
import java.util.Objects;
import java.util.Optional;

/**
 * A tank that holds its matter only while it is powered: a magnetic containment of plasma, a containment of exotic
 * matter. It spends its upkeep each tick for as long as it holds something, and, when it is empty, whether it could
 * pay it decides whether it takes anything in.
 *
 * <p>Without energy a containment takes nothing more. Plasma then dissipates: it was only ever held by the field.
 * Exotic matter is never let out by a containment that loses its energy; it stays sealed in, given to no one, until the
 * energy comes back, so there are no accidents with exotic matter.
 *
 * <p>Its block entity calls {@link #tick} once a tick with what pays the upkeep: its energy store, in whatever energy
 * its mod uses.
 */
public class ContainmentTank extends MatterTank {

    private final long upkeep;
    private boolean powered;

    /**
     * A containment of {@code state}, plasma or exotic matter, of {@code volume} millibuckets at {@code pressure} times
     * the pressure of the open air, that spends {@code upkeep} each tick while it holds something.
     */
    public ContainmentTank(final MatterState state, final long volume, final long pressure, final long upkeep,
                           final Runnable onChange) {
        super(state, volume, pressure, onChange);
        if (state != MatterState.PLASMA && state != MatterState.EXOTIC) {
            throw new IllegalArgumentException("a containment holds plasma or exotic matter, not "
                    + state.serializedName());
        }
        if (upkeep < 0) {
            throw new IllegalArgumentException("a containment spends no less than nothing, not " + upkeep);
        }
        this.upkeep = upkeep;
    }

    /** What it spends each tick while it holds something. */
    public long upkeep() {
        return this.upkeep;
    }

    /** Whether it had its energy at the last tick. */
    public boolean powered() {
        return this.powered;
    }

    /**
     * Pays the tick's upkeep from {@code energy} while it holds something, or, empty, asks whether it could: with the
     * upkeep paid it keeps holding and taking; without, it takes nothing more, and plasma dissipates.
     */
    public void tick(final IUpkeep energy) {
        Objects.requireNonNull(energy, "energy");
        final boolean could = energy.pay(this.upkeep, true) >= this.upkeep;
        this.powered = held().isEmpty() ? could : could && energy.pay(this.upkeep, false) >= this.upkeep;
        if (!this.powered && state() == MatterState.PLASMA) {
            lose();
        }
    }

    /**
     * Plasma escapes a broken containment and is gone; exotic matter is not let out at all, and stays held for the
     * containment's block to keep with itself (in the item it drops).
     */
    @Override
    public Optional<MatterStack> release() {
        return state() == MatterState.EXOTIC ? Optional.empty() : super.release();
    }

    @Override
    protected boolean takes() {
        return this.powered;
    }

    @Override
    protected boolean gives() {
        return this.powered;
    }

    /** What pays a containment's upkeep: its block's energy store. */
    @FunctionalInterface
    public interface IUpkeep {

        /** Spends up to {@code amount}, or says how much it would when simulated; how much it spent. */
        long pay(long amount, boolean simulate);
    }
}
