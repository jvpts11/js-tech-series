/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.matter;

import dev.jstech.core.material.Material;
import dev.jstech.core.material.MatterState;
import java.util.Optional;

/**
 * A block's or an item's store of one state of matter that is neither solid nor liquid, as the Core's capabilities of
 * each state give it ({@link MatterCapabilities}): its tanks, what each holds and can hold, what it takes and gives.
 * Amounts are in millibuckets at the pressure of the open air. Every move is offered simulated first, then done.
 */
public interface IMatterHandler {

    /** The state of matter it holds. */
    MatterState state();

    /** How many tanks it has. */
    int tanks();

    /** What tank {@code tank} holds, or none when it is empty. */
    Optional<MatterStack> stack(int tank);

    /** How much tank {@code tank} holds at most. */
    long capacity(int tank);

    /** Puts in up to {@code stack}'s amount; says how much it took, or would take when simulated. */
    long insert(MatterStack stack, boolean simulate);

    /** Takes out up to {@code amount} of {@code material}; says how much came out, or would. */
    long extract(Material material, long amount, boolean simulate);

    /** Takes out up to {@code amount} of whatever it gives first; says what came out, or would. */
    Optional<MatterStack> extractAny(long amount, boolean simulate);
}
