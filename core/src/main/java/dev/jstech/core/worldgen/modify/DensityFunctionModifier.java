/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.modify;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.JsCore;
import dev.jstech.core.worldgen.terrain.density.OriginalDensity;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * {@code jscore:density_function}: puts a new function in the place of one of the game's or any mod's, by its id, so
 * every terrain and every other function that names it draws the new one. Where the new function holds
 * {@code {"type": "jscore:original"}}, the old one stands there, so a change can wrap what was ("the game's continents,
 * raised a little") rather than write it all again:
 *
 * <pre>{@code
 * {"type": "jscore:density_function", "target": "minecraft:overworld/continents",
 *  "density": {"type": "minecraft:add", "argument1": {"type": "jscore:original"}, "argument2": 0.1}}
 * }</pre>
 *
 * <p>The functions the new one names are taken as they stand when it is made, so a later modifier of one of them
 * does not reach into this one.
 *
 * @param target  the function changed
 * @param density what stands in its place
 */
public record DensityFunctionModifier(ResourceKey<DensityFunction> target, DensityFunction density)
        implements IWorldgenModifier {

    /** The kind's id. */
    public static final ResourceLocation KIND = ResourceLocation.fromNamespaceAndPath(JsCore.MODID,
            "density_function");
    public static final MapCodec<DensityFunctionModifier> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceKey.codec(Registries.DENSITY_FUNCTION).fieldOf("target").forGetter(DensityFunctionModifier::target),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("density").forGetter(DensityFunctionModifier::density)
    ).apply(i, DensityFunctionModifier::new));

    @Override
    public ResourceLocation kind() {
        return KIND;
    }

    @Override
    public ResourceLocation changes() {
        return target.location();
    }

    @Override
    public boolean apply(final RegistryAccess registries, final List<String> problems) {
        final Optional<Holder.Reference<DensityFunction>> holder = registries.registryOrThrow(
                Registries.DENSITY_FUNCTION).getHolder(target);
        if (holder.isEmpty()) {
            problems.add("no density function is called " + target.location());
            return false;
        }
        final DensityFunction old = holder.get().value();
        WorldgenModifiers.rebind(holder.get(), density.mapAll(function -> function == OriginalDensity.INSTANCE ? old
                : function));
        return true;
    }
}
