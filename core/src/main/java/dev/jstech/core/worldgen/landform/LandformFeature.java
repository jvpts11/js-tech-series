/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.landform;

import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

/**
 * Lays the landforms of a kind on the chunk it is placed in, and only there, the feature {@code jscore:landform}: it
 * finds every landform of the kind that reaches the chunk ({@link LandformField}), keeps those whose middle lies in
 * the kind's biomes, and paints each over the chunk's columns ({@link LandformPainter}) from the ground as the noise
 * made it, before anything grew on it. A landform larger than a chunk is laid a chunk at a time, the same on any
 * thread for the same seed. Placed once a chunk, with no placement of its own.
 */
public final class LandformFeature extends Feature<LandformConfig> {

    private static final int CHUNK = 16;

    public LandformFeature() {
        super(LandformConfig.CODEC);
    }

    @Override
    public boolean place(final FeaturePlaceContext<LandformConfig> context) {
        final LandformConfig config = context.config();
        final WorldGenLevel level = context.level();
        final ChunkPos chunk = new ChunkPos(context.origin());
        final int x0 = chunk.getMinBlockX();
        final int z0 = chunk.getMinBlockZ();
        final List<LandformField.Instance> reaching = config.field().reaching(level.getSeed(), x0, z0, x0 + CHUNK - 1,
                z0 + CHUNK - 1, config.kind().reach());
        final ChunkGenerator generator = context.chunkGenerator();
        final RandomState random = level.getLevel().getChunkSource().randomState();
        boolean painted = false;
        for (final LandformField.Instance one : reaching) {
            final int base = generator.getBaseHeight(one.x(), one.z(), Heightmap.Types.OCEAN_FLOOR_WG, level,
                    random) - 1;
            if (config.biomes().isPresent() && !config.biomes().get().contains(middle(generator, random, one, base))) {
                continue;
            }
            LandformPainter.paint(level, config, one, base, Heightmap.Types.OCEAN_FLOOR_WG, x0, z0, x0 + CHUNK - 1,
                    z0 + CHUNK - 1);
            painted = true;
        }
        return painted;
    }

    /* The biome at a landform's middle, as the world's biomes are laid out, whether or not its chunk is made yet. */
    private static Holder<Biome> middle(final ChunkGenerator generator, final RandomState random,
                                        final LandformField.Instance one, final int base) {
        return generator.getBiomeSource().getNoiseBiome(QuartPos.fromBlock(one.x()), QuartPos.fromBlock(base),
                QuartPos.fromBlock(one.z()), random.sampler());
    }
}
