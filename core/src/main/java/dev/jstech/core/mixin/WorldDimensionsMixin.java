/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.serialization.Lifecycle;
import dev.jstech.core.registry.StableNamespaces;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets a world whose extra dimensions are all of namespaces declared stable ({@link StableNamespaces}) open with no
 * warning about experimental settings. The game takes as stable only its own three dimensions, and any list of
 * dimensions that is not exactly those three as experimental. Two changes, for declared namespaces only: a dimension of
 * one is stable; and a list whose every extra dimension is of one stays stable. A dimension of any other namespace
 * still makes the world experimental, as it should.
 */
@Mixin(WorldDimensions.class)
public abstract class WorldDimensionsMixin {

    @Shadow
    @Final
    private Map<ResourceKey<LevelStem>, LevelStem> dimensions;

    @Inject(method = "checkStability", at = @At("HEAD"), cancellable = true)
    private static void jscore$stableNamespaceDimension(final ResourceKey<LevelStem> key, final LevelStem stem,
                                                        final CallbackInfoReturnable<Lifecycle> info) {
        if (StableNamespaces.isStable(key.location())) {
            info.setReturnValue(Lifecycle.stable());
        }
    }

    @ModifyArg(method = "bake", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/MappedRegistry;<init>("
            + "Lnet/minecraft/resources/ResourceKey;Lcom/mojang/serialization/Lifecycle;)V"), index = 1)
    private Lifecycle jscore$stableWithStableExtras(final Lifecycle vanilla,
                                                    @Local(argsOnly = true) final Registry<LevelStem> stems) {
        if (vanilla == Lifecycle.stable()) {
            return vanilla;
        }
        final Set<ResourceKey<LevelStem>> keys = new HashSet<>(stems.registryKeySet());
        keys.addAll(this.dimensions.keySet());
        for (final ResourceKey<LevelStem> key : keys) {
            if (key != LevelStem.OVERWORLD && key != LevelStem.NETHER && key != LevelStem.END
                    && !StableNamespaces.isStable(key.location())) {
                return vanilla;
            }
        }
        return Lifecycle.stable();
    }
}
