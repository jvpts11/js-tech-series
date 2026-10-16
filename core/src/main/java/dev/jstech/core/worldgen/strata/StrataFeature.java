/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.strata;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

/**
 * Lays the rock of a land in layers, the feature {@code jscore:strata}: each column of the chunk it is placed in takes
 * the sequence of its region ({@link StrataMath#region}) and, from the ground's top down, puts each layer's rock in
 * the place of the rock it replaces, the boundaries rising and falling gently across the land. Placed once a chunk,
 * with no placement of its own, before the ores, which then lie in the layers.
 */
public final class StrataFeature extends Feature<StrataConfig> {

    private static final int CHUNK = 16;
    private static final int UPDATE = 2;
    /* How wide the rises and falls of the layers' boundaries are, against the regions. */
    private static final double WOBBLE_SCALE = 48;

    public StrataFeature() {
        super(StrataConfig.CODEC);
    }

    @Override
    public boolean place(final FeaturePlaceContext<StrataConfig> context) {
        final StrataConfig config = context.config();
        if (config.sequences().isEmpty()) {
            return false;
        }
        final ChunkPos chunk = new ChunkPos(context.origin());
        lay(context.level(), config, chunk.getMinBlockX(), chunk.getMinBlockZ(), chunk.getMaxBlockX(),
                chunk.getMaxBlockZ(), Heightmap.Types.OCEAN_FLOOR_WG);
        return true;
    }

    /**
     * Lays the layers over the columns from {@code x0, z0} to {@code x1, z1} of {@code level}, the ground's top read
     * from {@code ground}'s height: the world's generation lays them a chunk at a time, anything else where it wants.
     */
    public static void lay(final WorldGenLevel level, final StrataConfig config, final int x0, final int z0,
                           final int x1, final int z1, final Heightmap.Types ground) {
        final long seed = level.getSeed() ^ config.salt();
        final List<List<Integer>> thicknesses = config.sequences().stream()
                .map(sequence -> sequence.stream().map(StrataConfig.Layer::thickness).toList()).toList();
        final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                final int which = StrataMath.region(seed, x, z, config.region(), config.sequences().size());
                final List<StrataConfig.Layer> layers = config.sequences().get(which);
                final double shift = StrataMath.noise(seed, x, z, WOBBLE_SCALE) * config.wobble();
                final int top = level.getHeight(ground, x, z) - 1;
                for (int y = top; y > level.getMinBuildHeight(); y--) {
                    final int layer = StrataMath.layerAt(Math.max(0, top - y + shift), thicknesses.get(which));
                    if (layer < 0) {
                        break;
                    }
                    final BlockState there = level.getBlockState(at.set(x, y, z));
                    if (there.is(config.replaces())) {
                        level.setBlock(at, layers.get(layer).block(), UPDATE);
                    }
                }
            }
        }
    }
}
