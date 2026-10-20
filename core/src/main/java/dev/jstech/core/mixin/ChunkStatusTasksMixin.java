/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.mixin;

import dev.jstech.core.moving.YardLayout;
import java.util.EnumSet;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatusTasks;
import net.minecraft.world.level.chunk.status.ChunkStep;
import net.minecraft.world.level.chunk.status.WorldGenContext;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Keeps the yard's chunks empty: where moving structures keep their blocks the world generates no land, no carved
 * caves, no structure, no feature and no creature, every chunk of it air over the void's biome, so a structure's plot
 * holds only its own blocks. Every chunk outside the yard is generated as before.
 */
@Mixin(ChunkStatusTasks.class)
public abstract class ChunkStatusTasksMixin {

    @Inject(method = {"generateStructureStarts", "generateStructureReferences", "generateNoise", "generateSurface",
            "generateCarvers", "generateSpawn"}, at = @At("HEAD"), cancellable = true)
    private static void jscore$leaveTheYardEmpty(final WorldGenContext context, final ChunkStep step,
                                                 final StaticCache2D<GenerationChunkHolder> cache,
                                                 final ChunkAccess chunk,
                                                 final CallbackInfoReturnable<CompletableFuture<ChunkAccess>> info) {
        if (YardLayout.isYardChunk(chunk.getPos().x)) {
            info.setReturnValue(CompletableFuture.completedFuture(chunk));
        }
    }

    @Inject(method = "generateBiomes", at = @At("HEAD"), cancellable = true)
    private static void jscore$giveTheYardTheVoid(final WorldGenContext context, final ChunkStep step,
                                                  final StaticCache2D<GenerationChunkHolder> cache,
                                                  final ChunkAccess chunk,
                                                  final CallbackInfoReturnable<CompletableFuture<ChunkAccess>> info) {
        if (YardLayout.isYardChunk(chunk.getPos().x)) {
            final Holder<Biome> nothing = context.level().registryAccess().registryOrThrow(Registries.BIOME)
                    .getHolderOrThrow(Biomes.THE_VOID);
            chunk.fillBiomesFromNoise((x, y, z, sampler) -> nothing,
                    context.level().getChunkSource().randomState().sampler());
            info.setReturnValue(CompletableFuture.completedFuture(chunk));
        }
    }

    @Inject(method = "generateFeatures", at = @At("HEAD"), cancellable = true)
    private static void jscore$decorateNothingInTheYard(final WorldGenContext context, final ChunkStep step,
                                                        final StaticCache2D<GenerationChunkHolder> cache,
                                                        final ChunkAccess chunk,
                                                        final CallbackInfoReturnable<CompletableFuture<ChunkAccess>>
                                                                info) {
        if (YardLayout.isYardChunk(chunk.getPos().x)) {
            Heightmap.primeHeightmaps(chunk, EnumSet.of(Heightmap.Types.MOTION_BLOCKING,
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Heightmap.Types.OCEAN_FLOOR,
                    Heightmap.Types.WORLD_SURFACE));
            info.setReturnValue(CompletableFuture.completedFuture(chunk));
        }
    }
}
