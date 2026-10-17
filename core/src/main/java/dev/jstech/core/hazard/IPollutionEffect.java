/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.hazard;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * What a pollution does to the land it lies on: acid rain eating at leaves and copper, metals in the soil. Asked for a
 * few columns of each polluted chunk now and then, at the top of each.
 */
@FunctionalInterface
public interface IPollutionEffect {

    /** What {@code amount} of it, in its unit, does at {@code top}, the top block of a column of {@code level}. */
    void at(ServerLevel level, BlockPos top, double amount);
}
