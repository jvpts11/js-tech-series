/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.containment;

import dev.jstech.core.jstechseries.Antimatter;
import dev.jstech.core.material.MatterState;
import dev.jstech.core.matter.ContainmentTank;
import dev.jstech.core.matter.MatterStack;
import org.jetbrains.annotations.Nullable;

/**
 * The containment of the series' shared blocks: exotic matter, held only while powered, never let out when the energy
 * fails. The containment of exotic matter keeps every exotic form but antimatter; the Antimatter Trap, a Penning trap,
 * keeps only antimatter.
 */
public final class ExoticContainment extends ContainmentTank {

    private final boolean antimatter;

    /**
     * A containment of {@code volume} millibuckets that spends {@code upkeep} each tick while it holds something: of
     * antimatter alone, or of the other exotic forms.
     */
    public ExoticContainment(final boolean antimatter, final long volume, final long upkeep,
                             final Runnable onChange) {
        super(MatterState.EXOTIC, volume, 1L, upkeep, onChange);
        this.antimatter = antimatter;
    }

    /** Whether it keeps antimatter alone, as a Penning trap; the other exotic forms otherwise. */
    public boolean antimatter() {
        return this.antimatter;
    }

    @Override
    public long insert(final MatterStack stack, final boolean simulate) {
        return accepts(stack) ? super.insert(stack, simulate) : 0L;
    }

    /* Pushed in past its pressure, as a pump does, it still takes only its own kind of matter. */
    @Override
    public long push(final MatterStack stack, final long pressure, final boolean simulate) {
        return accepts(stack) ? super.push(stack, pressure, simulate) : 0L;
    }

    @Override
    public boolean set(final @Nullable MatterStack stack) {
        return (stack == null || accepts(stack)) && super.set(stack);
    }

    /* Whether {@code stack} is the kind it keeps: antimatter in a Penning trap, the other exotic forms elsewhere. */
    private boolean accepts(final MatterStack stack) {
        return Antimatter.is(stack.material()) == this.antimatter;
    }
}
