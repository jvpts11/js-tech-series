/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.landform;

import dev.jstech.core.worldgen.biome.BiomeClimates;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

/**
 * Freezes the seas or boils them away by the temperature of the place, the feature {@code jscore:sea}: in each column
 * of the chunk it is placed in whose top is the seas' liquid, the top freezes where it is colder than the liquid
 * freezes, and the liquid is gone, down to the sea's floor, where it is hotter than it boils. Placed once a chunk,
 * with no placement of its own, once the seas are laid.
 */
public final class SeaFeature extends Feature<SeaConfig> {

    private static final int UPDATE = 2;

    public SeaFeature() {
        super(SeaConfig.CODEC);
    }

    @Override
    public boolean place(final FeaturePlaceContext<SeaConfig> context) {
        final ChunkPos chunk = new ChunkPos(context.origin());
        return settle(context.level(), context.config(), chunk.getMinBlockX(), chunk.getMinBlockZ(),
                chunk.getMaxBlockX(), chunk.getMaxBlockZ(), Heightmap.Types.WORLD_SURFACE_WG);
    }

    /**
     * Freezes or boils away the seas over the columns from {@code x0, z0} to {@code x1, z1}, each column's top read
     * from {@code surface}'s height; whether any column changed.
     */
    public static boolean settle(final WorldGenLevel level, final SeaConfig config, final int x0, final int z0,
                                 final int x1, final int z1, final Heightmap.Types surface) {
        final Block liquid = config.liquid().getBlock();
        final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        boolean changed = false;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                final int top = level.getHeight(surface, x, z) - 1;
                if (!level.getBlockState(at.set(x, top, z)).is(liquid)) {
                    continue;
                }
                final double kelvin = BiomeClimates.kelvinAsMade(level, at);
                if (kelvin >= config.boils()) {
                    int y = top;
                    while (y > level.getMinBuildHeight() && level.getBlockState(at.set(x, y, z)).is(liquid)) {
                        level.setBlock(at, Blocks.AIR.defaultBlockState(), UPDATE);
                        y--;
                    }
                    changed = true;
                } else if (kelvin < config.freezes()) {
                    level.setBlock(at, config.frozen(), UPDATE);
                    changed = true;
                }
            }
        }
        return changed;
    }
}
