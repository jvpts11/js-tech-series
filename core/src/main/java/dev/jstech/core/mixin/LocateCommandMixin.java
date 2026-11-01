/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.datafixers.util.Pair;
import dev.jstech.core.worldgen.biome.BiomeSearch;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.commands.LocateCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Lets {@code /locate biome} find a biome anywhere in a dimension that keeps bounds of its own, as a round world does:
 * the game searches 6,400 blocks round where the command is run and says the biome is nowhere past that, and a planet
 * millions of blocks across holds its caps and its canyons farther away. Only when the game's own search finds nothing
 * is the whole world searched ({@link BiomeSearch}); every other dimension answers as the game answers.
 */
@Mixin(LocateCommand.class)
public abstract class LocateCommandMixin {

    @WrapOperation(method = "locateBiome", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/"
            + "ServerLevel;findClosestBiome3d(Ljava/util/function/Predicate;Lnet/minecraft/core/BlockPos;III)"
            + "Lcom/mojang/datafixers/util/Pair;"))
    private static Pair<BlockPos, Holder<Biome>> jscore$searchTheWholeWorld(
            final ServerLevel level, final Predicate<Holder<Biome>> biome, final BlockPos from, final int radius,
            final int horizontalStep, final int verticalStep,
            final Operation<Pair<BlockPos, Holder<Biome>>> original) {
        final Pair<BlockPos, Holder<Biome>> near = original.call(level, biome, from, radius, horizontalStep,
                verticalStep);
        return near != null ? near : BiomeSearch.wholeWorld(level, biome, from);
    }
}
