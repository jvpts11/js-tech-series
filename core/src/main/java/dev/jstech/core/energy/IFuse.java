/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.energy;

import net.minecraft.world.item.ItemStack;

/**
 * An item that is a fuse, for a machine's protection slot ({@link EnergyProtection}): on overvoltage it blows, cutting
 * the energy and shutting the machine down with no damage. A fuse has a rated voltage: one facing more than it can break
 * lets the arc jump over it. The fuses themselves are a mod's items.
 */
public interface IFuse {

    /** It breaks a line any number of classes above its port. */
    int ANY = Integer.MAX_VALUE;

    /** How many classes above its port a line may be for it to break it: one, or {@link #ANY}. */
    int breaks(ItemStack fuse);

    /** What is left of it once it blows: a fuse to rewire, a blown fuse to throw away. */
    ItemStack blown(ItemStack fuse);
}
