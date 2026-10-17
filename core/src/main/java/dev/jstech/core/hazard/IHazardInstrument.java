/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.hazard;

import java.util.Collection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * An item that measures hazards where it is carried, as a Geiger counter measures radiation: carried in the hotbar or
 * the off hand, each second it reads the rate of each of its hazards at the player's chunk onto their map of hazards
 * ({@link HazardMaps}), so the map knows the chunks that were measured and when.
 */
public interface IHazardInstrument {

    /** The ids of the hazards {@code stack} measures now; none when it measures nothing. */
    Collection<ResourceLocation> reads(ItemStack stack);
}
