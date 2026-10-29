/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.hazard;

import net.minecraft.world.item.ItemStack;

/**
 * What a worn stack stops of a kind of harm besides what its item stops by the data map: what is fitted to that very
 * stack, a suit's module, a lining sewn in, which the data of its item cannot know ({@link Resistances#source}).
 */
@FunctionalInterface
public interface IStackResistance {

    /** What share of {@code kind} what is fitted to {@code stack} stops, worn: 0 for none. */
    double share(ItemStack stack, DamageKind kind);
}
