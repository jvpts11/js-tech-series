/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * A world J's Space takes the player to, made of three layers: the generator that shaped its ground, the modifiers
 * laid over it, and its data. Two worlds of the same generator never look alike, because everything else is the
 * world's own. What it is like to stand on (its gravity, its air, its heat, its day and how they change with height)
 * is J's Core's dimension rules file of its dimension, {@code data/<namespace>/dimension_rules/<path>.json}.
 *
 * <pre>{@code
 * {"name": "mymod.world.dune", "system": "jsspace:tau_ceti", "dimension": "mymod:dune",
 *  "generator": "jsspace:arid", "modifiers": ["mymod:dust_storms"], "diameter": 9800}
 * }</pre>
 *
 * @param name      the key of its name in the language files
 * @param system    the star system it is in
 * @param dimension the dimension it is
 * @param generator the generator that shaped its ground; none for a world whose dimension generates itself, as the
 *                  Overworld
 * @param modifiers what is laid over its generator, in order
 * @param diameter  how wide it is, in kilometres
 */
public record SpaceWorld(String name, ResourceLocation system, ResourceKey<Level> dimension,
                         Optional<ResourceLocation> generator, List<ResourceLocation> modifiers, double diameter) {

    /** As a data pack's file writes one. */
    public static final Codec<SpaceWorld> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(SpaceWorld::name),
            ResourceLocation.CODEC.fieldOf("system").forGetter(SpaceWorld::system),
            ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(SpaceWorld::dimension),
            ResourceLocation.CODEC.optionalFieldOf("generator").forGetter(SpaceWorld::generator),
            ResourceLocation.CODEC.listOf().optionalFieldOf("modifiers", List.of()).forGetter(SpaceWorld::modifiers),
            Codec.doubleRange(1.0, 1_000_000.0).fieldOf("diameter").forGetter(SpaceWorld::diameter)
    ).apply(instance, SpaceWorld::new));

    public SpaceWorld {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(system, "system");
        Objects.requireNonNull(dimension, "dimension");
        generator = generator == null ? Optional.empty() : generator;
        modifiers = List.copyOf(modifiers);
    }
}
