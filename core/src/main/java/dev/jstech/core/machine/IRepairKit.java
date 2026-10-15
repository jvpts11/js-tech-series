/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.machine;

import net.minecraft.world.item.ItemStack;

/**
 * An item that repairs damaged machines, as Factorio's repair packs do: held and used on a machine, it gives back its
 * health over a few seconds, spending itself as it goes. Which kits there are, and how fast each repairs, are each
 * mod's; {@link RepairKitItem} is one ready to register.
 */
public interface IRepairKit {

    /** How much health, of {@link MachineHealth#FULL}, the kit in {@code stack} gives back a second. */
    int repairsPerSecond(ItemStack stack);
}
