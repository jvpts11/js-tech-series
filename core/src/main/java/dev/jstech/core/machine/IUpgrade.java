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
 * An item that upgrades a machine when it sits in one of the machine's upgrade slots: every item of a stack counts, up
 * to how many of it fit in one machine.
 */
public interface IUpgrade {

    /** No limit on how many fit in one machine, but the machine's slots. */
    int ANY = Integer.MAX_VALUE;

    /** What one of {@code stack} does to a machine. */
    UpgradeEffect effect(ItemStack stack);

    /**
     * How many of {@code stack} fit in one machine, across all its upgrade slots, so a machine of few slots is fast or
     * thrifty, hardly both; {@link #ANY} unless the upgrade says.
     */
    default int most(final ItemStack stack) {
        return ANY;
    }
}
