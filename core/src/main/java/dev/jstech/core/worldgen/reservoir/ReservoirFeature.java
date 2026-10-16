/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.reservoir;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Lays the reservoirs on the chunk it is placed in, and only there, the feature {@code jscore:reservoirs}: every
 * reservoir found where it lies that reaches the chunk ({@link Reservoirs#reaching}), laid over the chunk's columns
 * ({@link ReservoirPainter}). Placed once a chunk in every biome, before the ores.
 */
public final class ReservoirFeature extends Feature<NoneFeatureConfiguration> {

    private static final int CHUNK = 16;

    public ReservoirFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(final FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (Reservoirs.all().isEmpty()) {
            return false;
        }
        final WorldGenLevel level = context.level();
        final ChunkPos chunk = new ChunkPos(context.origin());
        final int x0 = chunk.getMinBlockX();
        final int z0 = chunk.getMinBlockZ();
        int laid = 0;
        for (final Reservoirs.Reservoir reservoir : Reservoirs.reaching(level.getSeed(), context.chunkGenerator(),
                level.getLevel().getChunkSource().randomState(), x0, z0, x0 + CHUNK - 1, z0 + CHUNK - 1)) {
            laid += ReservoirPainter.paint(level, reservoir, x0, z0, x0 + CHUNK - 1, z0 + CHUNK - 1);
        }
        return laid > 0;
    }
}
