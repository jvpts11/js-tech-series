/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.vein;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

/**
 * The placement {@code jscore:scattered_ores}, put first on a scattered ore's feature that veins stand in for: it lets
 * the ore be laid only while the server's ores do not lie in veins, or, for another mod's ore that is {@code moved},
 * while the server does not try moving it. Asked as each chunk generates, so the server's settings in force decide.
 */
public final class ScatteredOres extends PlacementFilter {

    private final boolean moved;

    public static final MapCodec<ScatteredOres> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("moved", false).forGetter(filter -> filter.moved))
            .apply(instance, ScatteredOres::new));

    public ScatteredOres(final boolean moved) {
        this.moved = moved;
    }

    /** Whether this ore is laid scattered now. */
    public boolean laid() {
        return !VeinBalance.enabled() || this.moved && !VeinBalance.moveOthers();
    }

    @Override
    public PlacementModifierType<?> type() {
        return Veins.SCATTERED_ORES.get();
    }

    @Override
    protected boolean shouldPlace(final PlacementContext context, final RandomSource random, final BlockPos pos) {
        return laid();
    }
}
