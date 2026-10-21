/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.mixin;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import dev.jstech.core.unify.Unification;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Unifies the recipes' files just before the game reads them, with the item tags as they are loading, which the game
 * has read already by then: what reaches the recipes this way reaches recipes of every kind, the Core's or not.
 */
@Mixin(RecipeManager.class)
public abstract class RecipeManagerMixin extends SimpleJsonResourceReloadListener {

    private RecipeManagerMixin(final Gson gson, final String directory) {
        super(gson, directory);
    }

    @Inject(method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;"
            + "Lnet/minecraft/util/profiling/ProfilerFiller;)V", at = @At("HEAD"))
    private void jscore$unify(final Map<ResourceLocation, JsonElement> recipes, final ResourceManager resources,
                              final ProfilerFiller profiler, final CallbackInfo info) {
        Unification.unifyRecipes(recipes, getContext());
    }
}
