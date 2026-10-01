/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.chemical;

import java.util.List;
import net.minecraft.resources.ResourceLocation;

/**
 * A block's chemical inventory, seen without knowing which mod provides it: substances identified by registry id and
 * measured in millibuckets. An {@link IChemicalBridge} supplies ports for the blocks of the mod it integrates, so a
 * network or a bus moves chemicals through them as it moves items and fluids.
 */
public interface IChemicalPort {

    /** Pushes up to {@code amount} mB of {@code chemical} into the block; returns how much it accepted. */
    long fill(ResourceLocation chemical, long amount, boolean simulate);

    /** Pulls up to {@code amount} mB of {@code chemical} out of the block; returns how much came out. */
    long drain(ResourceLocation chemical, long amount, boolean simulate);

    /** How much of {@code chemical} the block currently holds, in mB. */
    long count(ResourceLocation chemical);

    /** Every chemical the block currently holds, in tank order, without duplicates. */
    List<ResourceLocation> available();
}
