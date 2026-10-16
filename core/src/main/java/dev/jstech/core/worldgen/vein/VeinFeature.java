/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.vein;

import java.util.List;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Lays the veins on the chunk it is placed in, and only there, the feature {@code jscore:veins}: it finds every vein
 * that reaches the chunk ({@link VeinField}), keeps those found where they lie ({@link Veins#found}), and lays each
 * over the chunk's columns ({@link VeinPainter}). Nothing while the server's ores do not lie in veins. Placed once a
 * chunk in every biome, with the ores.
 */
public final class VeinFeature extends Feature<NoneFeatureConfiguration> {

    private static final int CHUNK = 16;

    public VeinFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(final FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (!VeinBalance.enabled()) {
            return false;
        }
        final List<Deposits.Entry> kinds = Deposits.all();
        if (kinds.isEmpty()) {
            return false;
        }
        final WorldGenLevel level = context.level();
        final ChunkPos chunk = new ChunkPos(context.origin());
        final int x0 = chunk.getMinBlockX();
        final int z0 = chunk.getMinBlockZ();
        final RandomState random = level.getLevel().getChunkSource().randomState();
        int laid = 0;
        for (final VeinField.Vein vein : Veins.field(kinds).reaching(level.getSeed(), x0, z0, x0 + CHUNK - 1,
                z0 + CHUNK - 1)) {
            final Deposits.Entry kind = kinds.get(vein.kind());
            if (Veins.found(context.chunkGenerator(), random, kind, vein)) {
                laid += VeinPainter.paint(level, kind, vein, Veins.density(kind), x0, z0, x0 + CHUNK - 1,
                        z0 + CHUNK - 1);
            }
        }
        return laid > 0;
    }
}
