/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.dimension;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.minecraft.world.level.storage.WorldData;

/**
 * What a dimension other than the overworld reads its weather from: the overworld's, as the game's own dimensions do,
 * unless the dimension's rules hold it clear, raining or storming. Asked each tick by the dimension's weather, so a
 * rule changed by a reload takes hold at once.
 */
final class RuledLevelData extends DerivedLevelData {

    private final ResourceKey<Level> dimension;

    RuledLevelData(final WorldData world, final ResourceKey<Level> dimension) {
        super(world, world.overworldData());
        this.dimension = dimension;
    }

    @Override
    public boolean isRaining() {
        return switch (DimensionRulesData.of(dimension).weather()) {
            case NATURAL -> super.isRaining();
            case CLEAR -> false;
            case RAIN, THUNDER -> true;
        };
    }

    @Override
    public boolean isThundering() {
        return switch (DimensionRulesData.of(dimension).weather()) {
            case NATURAL -> super.isThundering();
            case CLEAR, RAIN -> false;
            case THUNDER -> true;
        };
    }
}
