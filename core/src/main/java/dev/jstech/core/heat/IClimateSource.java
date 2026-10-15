/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.heat;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * A place's own air, where it is not the dimension's: a sealed room's, a biome's climate, a cold store's. Declared to
 * {@link HeatClimate#source}; the first source that speaks of a place gives its air, and where none does the
 * dimension's rules give it.
 */
@FunctionalInterface
public interface IClimateSource {

    /** The air at {@code pos} of {@code level}, or nothing where this source says nothing of it. */
    @Nullable HeatClimate.Air at(Level level, BlockPos pos);
}
