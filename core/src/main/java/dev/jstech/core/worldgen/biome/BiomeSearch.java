/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.biome;

import com.mojang.datafixers.util.Pair;
import dev.jstech.core.dimension.DimensionBorders;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import org.jetbrains.annotations.Nullable;

/**
 * The search for a biome across the whole of a dimension that keeps bounds of its own ({@link DimensionBorders}), as
 * a round world does: the game's own search reads 6,400 blocks round where it starts, and a planet millions of blocks
 * across holds its caps and its canyons farther. It reads the dimension's biome source on the {@link WorldSearch}'s
 * grid, at the height it starts from, so it finds a biome that spreads over a few hundred blocks or more anywhere in
 * the world, and its nearest place to within a step of that grid.
 */
public final class BiomeSearch {

    private BiomeSearch() {
    }

    /**
     * The place nearest {@code from} holding a biome {@code biome} takes, at {@code from}'s height, with its biome.
     *
     * @return the place and its biome, or null when the dimension keeps no bounds of its own or holds no such biome
     */
    public static @Nullable Pair<BlockPos, Holder<Biome>> wholeWorld(final ServerLevel level,
                                                                     final Predicate<Holder<Biome>> biome,
                                                                     final BlockPos from) {
        final Optional<DimensionBorders.Bounds> bounds = DimensionBorders.of(level);
        final BiomeSource source = level.getChunkSource().getGenerator().getBiomeSource();
        if (bounds.isEmpty() || source.possibleBiomes().stream().noneMatch(biome)) {
            return null;
        }
        final Climate.Sampler sampler = level.getChunkSource().randomState().sampler();
        final int quartY = QuartPos.fromBlock(from.getY());
        final WorldSearch.IProbe probe = (x, z) -> biome.test(source.getNoiseBiome(QuartPos.fromBlock(x), quartY,
                QuartPos.fromBlock(z), sampler));
        final DimensionBorders.Bounds inside = bounds.get();
        return WorldSearch.nearest(probe, from.getX(), from.getZ(), (int) Math.ceil(inside.minX()),
                        (int) Math.floor(inside.maxX()) - 1, (int) Math.ceil(inside.minZ()),
                        (int) Math.floor(inside.maxZ()) - 1)
                .map(found -> Pair.of(new BlockPos(found.x(), from.getY(), found.z()), source.getNoiseBiome(
                        QuartPos.fromBlock(found.x()), quartY, QuartPos.fromBlock(found.z()), sampler)))
                .orElse(null);
    }
}
