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
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Registers the entries of a namespace declared stable ({@link StableNamespaces}) as stable. The game marks every
 * registry entry not from its own pack as experimental, every mod's jar being a pack of its own, and a world holding
 * one opens with a warning about experimental settings; no event lets a mod say its data is no experiment. Only the
 * entries of a declared namespace change; every other registers as the game says.
 */
@Mixin(MappedRegistry.class)
public abstract class MappedRegistryMixin {

    @ModifyVariable(method = "register(ILnet/minecraft/resources/ResourceKey;Ljava/lang/Object;"
            + "Lnet/minecraft/core/RegistrationInfo;)Lnet/minecraft/core/Holder$Reference;", at = @At("HEAD"),
            argsOnly = true)
    private RegistrationInfo jscore$stableNamespace(final RegistrationInfo info,
                                                    @Local(argsOnly = true) final ResourceKey<?> key) {
        if (info.lifecycle() == Lifecycle.stable() || !StableNamespaces.isStable(key.location())) {
            return info;
        }
        return new RegistrationInfo(info.knownPackInfo(), Lifecycle.stable());
    }
}
