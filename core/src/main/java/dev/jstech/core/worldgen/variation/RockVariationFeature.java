/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.variation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

/**
 * Puts each variant of a rock where geology puts it, the feature {@code jscore:rock_variations}: every block of the
 * rock in the chunk it is placed in, from the top of the ground down as deep as any variant reaches, takes the first
 * variant whose place it lies in ({@link RockRule}), by how deep it lies, how steep the ground is round it, how it lies
 * open and in which biome; any other is left as it is. Placed once a chunk, with no placement of its own, after the
 * rock in layers and before the ores, which then lie in the variants.
 */
public final class RockVariationFeature extends Feature<RockVariationConfig> {

    private static final int UPDATE = 2;
    /* How far round a column, in blocks, the ground's steepness is read. */
    private static final int REACH = 2;

    public RockVariationFeature() {
        super(RockVariationConfig.CODEC);
    }

    @Override
    public boolean place(final FeaturePlaceContext<RockVariationConfig> context) {
        final RockVariationConfig config = context.config();
        if (config.variants().isEmpty()) {
            return false;
        }
        final ChunkPos chunk = new ChunkPos(context.origin());
        vary(context.level(), config, chunk.getMinBlockX(), chunk.getMinBlockZ(), chunk.getMaxBlockX(),
                chunk.getMaxBlockZ(), Heightmap.Types.OCEAN_FLOOR_WG);
        return true;
    }

    /**
     * Varies the rock over the columns from {@code x0, z0} to {@code x1, z1} of {@code level}, the ground's top read
     * from {@code ground}'s height: the world's generation varies it a chunk at a time, anything else where it wants.
     */
    public static void vary(final WorldGenLevel level, final RockVariationConfig config, final int x0, final int z0,
                            final int x1, final int z1, final Heightmap.Types ground) {
        final int deepest = config.deepest();
        final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        final BlockPos.MutableBlockPos near = new BlockPos.MutableBlockPos();
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                final int top = level.getHeight(ground, x, z) - 1;
                final int slope = slope(level, ground, x, z, top);
                final Holder<Biome> biome = level.getBiome(at.set(x, top, z));
                for (int y = top; y > level.getMinBuildHeight() && (long) top - y <= deepest; y--) {
                    at.set(x, y, z);
                    if (!level.getBlockState(at).is(config.rock())) {
                        continue;
                    }
                    final RockPlace place = new RockPlace(top - y, slope, open(level, near.setWithOffset(at,
                            Direction.UP)), openSide(level, at, near), y);
                    for (final RockVariationConfig.Variant variant : config.variants()) {
                        if (variant.where().fits(place) && variant.biomes().map(set -> set.contains(biome))
                                .orElse(true)) {
                            level.setBlock(at, variant.block(), UPDATE);
                            break;
                        }
                    }
                }
            }
        }
    }

    /* How many blocks the ground rises or falls within {@link #REACH} of the column, the steepest way. */
    private static int slope(final WorldGenLevel level, final Heightmap.Types ground, final int x, final int z,
                             final int top) {
        int steepest = 0;
        for (final Direction side : Direction.Plane.HORIZONTAL) {
            final int there = level.getHeight(ground, x + side.getStepX() * REACH, z + side.getStepZ() * REACH) - 1;
            steepest = Math.max(steepest, Math.abs(there - top));
        }
        return steepest;
    }

    /* Whether nothing that fills a block lies at {@code pos}: air, a liquid, a plant. */
    private static boolean open(final WorldGenLevel level, final BlockPos pos) {
        return !level.getBlockState(pos).canOcclude();
    }

    private static boolean openSide(final WorldGenLevel level, final BlockPos at, final BlockPos.MutableBlockPos near) {
        for (final Direction side : Direction.Plane.HORIZONTAL) {
            if (open(level, near.setWithOffset(at, side))) {
                return true;
            }
        }
        return false;
    }
}
