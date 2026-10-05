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
 * An item that upgrades a machine when it sits in one of the machine's upgrade slots: every item of a stack counts.
 */
public interface IUpgrade {

    /** What one of {@code stack} does to a machine. */
    UpgradeEffect effect(ItemStack stack);
}
