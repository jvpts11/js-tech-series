/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worn;

import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Where the Core learns what an entity wears beyond its armour: the slots a mod of rings, belts and charms adds. The
 * Core asks Curios and Accessories itself when either is installed; a mod with slots of its own hands the Core a
 * source of its own through {@link WornItems#addSource}.
 */
@FunctionalInterface
public interface IWornSource {

    /** Adds what {@code entity} wears in this source's slots to {@code out}, leaving out the empty slots. */
    void collect(LivingEntity entity, List<ItemStack> out);
}
