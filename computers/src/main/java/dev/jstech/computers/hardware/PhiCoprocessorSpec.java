/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import dev.jstech.core.tier.IndustrialTier;

/**
 * A crafting co-processor for the Supercomputer's six dedicated slots.
 */
public record PhiCoprocessorSpec(IndustrialTier tier, int maxSlot, int cores, int mhz, int tdpWatts)
        implements IExpansionCardSpec {

    public static final int SLOT_COUNT = 6;

    public static long craftsForSlot(final int slotIndex) {
        // A shift count wraps at 64, so a slot outside the six would quietly answer for another one.
        if (slotIndex < 0 || slotIndex >= SLOT_COUNT) {
            throw new IllegalArgumentException("slotIndex must be 0.." + (SLOT_COUNT - 1) + ", got " + slotIndex);
        }
        return 8L << slotIndex;
    }

    public PhiCoprocessorSpec {
        if (maxSlot < 1 || maxSlot > SLOT_COUNT) {
            throw new IllegalArgumentException("maxSlot must be 1.." + SLOT_COUNT + ", got " + maxSlot);
        }
        if (cores < 1) {
            throw new IllegalArgumentException("cores must be at least 1, got " + cores);
        }
        if (mhz < 1) {
            throw new IllegalArgumentException("mhz must be at least 1, got " + mhz);
        }
        if (tdpWatts < 0) {
            throw new IllegalArgumentException("tdpWatts must not be negative, got " + tdpWatts);
        }
    }

    public boolean fitsSlot(final int slotIndex) {
        return slotIndex >= 0 && slotIndex < maxSlot;
    }

    @Override
    public PcieGeneration bus() {
        return PcieGeneration.PCIE_3_0;
    }

    @Override
    public ExpansionCardKind kind() {
        return ExpansionCardKind.PHI;
    }
}
