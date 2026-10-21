/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.vehicle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;

/**
 * Something an engine burns: a liquid, by a tag of fluids, or a gas, by the materials it is; and what a millibucket of
 * it gives. An engine says by id which fuels it burns, so a pack adds a fuel and the engines that take it with no code.
 *
 * <pre>{@code
 * {"name": "fuel.mymod.diesel", "liquid": "c:diesel", "joules_per_mb": 36000}
 * }</pre>
 *
 * @param name        the translation key of its name
 * @param liquid      the fluids it is, by tag, for a liquid
 * @param gases       the materials it is, by id, for a gas
 * @param joulesPerMb what a millibucket of it gives burnt, in joules
 */
public record Fuel(String name, Optional<TagKey<Fluid>> liquid, List<ResourceLocation> gases, double joulesPerMb) {

    public static final Codec<Fuel> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(Fuel::name),
            TagKey.codec(Registries.FLUID).optionalFieldOf("liquid").forGetter(Fuel::liquid),
            ResourceLocation.CODEC.listOf().optionalFieldOf("gases", List.of()).forGetter(Fuel::gases),
            Codec.doubleRange(1.0e-6, Double.MAX_VALUE).fieldOf("joules_per_mb").forGetter(Fuel::joulesPerMb))
            .apply(instance, Fuel::new));

    public Fuel {
        gases = List.copyOf(gases);
    }

    /** A liquid fuel: the fluids of {@code tag}. */
    public static Fuel liquid(final String name, final TagKey<Fluid> tag, final double joulesPerMb) {
        return new Fuel(name, Optional.of(tag), List.of(), joulesPerMb);
    }

    /** A gas fuel: the materials {@code gases}. */
    public static Fuel gas(final String name, final List<ResourceLocation> gases, final double joulesPerMb) {
        return new Fuel(name, Optional.empty(), gases, joulesPerMb);
    }
}
