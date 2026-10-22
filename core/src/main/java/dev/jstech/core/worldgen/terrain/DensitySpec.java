/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.terrain;

import java.util.Objects;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;

/**
 * A density function declared in code ({@link Terrains#density}): written by the data generation as a file of its own,
 * which a terrain's ground, another density function or a datapack refers to by its name.
 *
 * @param id     what its file is named
 * @param recipe how it is made
 */
public record DensitySpec(ResourceLocation id, IDensityRecipe recipe) {

    public DensitySpec {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(recipe, "recipe");
    }

    /** The key it is registered under. */
    public ResourceKey<DensityFunction> key() {
        return ResourceKey.create(Registries.DENSITY_FUNCTION, this.id);
    }

    /** It by its name, as another density function or a terrain's ground samples it, from {@code densities}. */
    public DensityFunction reference(final HolderGetter<DensityFunction> densities) {
        return new DensityFunctions.HolderHolder(densities.getOrThrow(key()));
    }
}
