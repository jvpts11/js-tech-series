/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.vein;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

/**
 * A kind of vein, a deposit type, as its file under {@code data/<namespace>/jstech/deposits/} says: the materials it
 * brings together and how much of each, the heights its middle lies between, how many zones hold one, its size and
 * density, the biomes it is found in and the rocks it crosses. Each block of a vein turns into the ore of one of its
 * materials in the rock it replaced, the rock's own variant; a material whose ore no installed mod uses in that rock
 * leaves the block as it was.
 *
 * <pre>{@code
 * { "minerals": [ {"material": "jscore:iron", "weight": 6}, {"material": "jscore:titanium", "weight": 1} ],
 *   "min_y": -48, "max_y": 96, "frequency": 0.12, "radius": 20, "thickness": 6, "density": 0.55,
 *   "biomes": "minecraft:is_overworld", "rocks": ["jscore:stone", "jscore:deepslate", "jscore:granite"] }
 * }</pre>
 *
 * @param minerals  the materials it brings together, each with its weight
 * @param minY      the lowest its middle lies
 * @param maxY      the highest
 * @param frequency how many zones hold one, 0 to 1
 * @param radius    how far across it reaches from its middle, in blocks
 * @param thickness how far up and down
 * @param density   how much of it is ore at its middle, 0 to 1, thinning to none at its edge
 * @param biomes    the biomes it is found in, by the biome at its middle; empty for any
 * @param elsewhere how many as often it is found outside those biomes, 0 for never
 * @param rocks     the rocks it crosses, by their ids; empty for every rock ores sit in
 */
public record DepositType(List<Mineral> minerals, int minY, int maxY, double frequency, double radius,
                          double thickness, double density, Optional<TagKey<Biome>> biomes, double elsewhere,
                          List<ResourceLocation> rocks) {

    private static final int LOWEST = -2048;
    private static final int HIGHEST = 2048;
    private static final double WIDEST = 48;
    private static final double RADIUS = 20;
    private static final double THICKNESS = 6;
    private static final double DENSITY = 0.5;
    public static final Codec<DepositType> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Mineral.CODEC.listOf().fieldOf("minerals").forGetter(DepositType::minerals),
            Codec.intRange(LOWEST, HIGHEST).fieldOf("min_y").forGetter(DepositType::minY),
            Codec.intRange(LOWEST, HIGHEST).fieldOf("max_y").forGetter(DepositType::maxY),
            Codec.doubleRange(0, 1).fieldOf("frequency").forGetter(DepositType::frequency),
            Codec.doubleRange(1, WIDEST).optionalFieldOf("radius", RADIUS).forGetter(DepositType::radius),
            Codec.doubleRange(1, WIDEST).optionalFieldOf("thickness", THICKNESS).forGetter(DepositType::thickness),
            Codec.doubleRange(0, 1).optionalFieldOf("density", DENSITY).forGetter(DepositType::density),
            TagKey.codec(Registries.BIOME).optionalFieldOf("biomes").forGetter(DepositType::biomes),
            Codec.doubleRange(0, 1).optionalFieldOf("elsewhere", 0.0).forGetter(DepositType::elsewhere),
            ResourceLocation.CODEC.listOf().optionalFieldOf("rocks", List.of()).forGetter(DepositType::rocks))
            .apply(instance, DepositType::new));

    public DepositType {
        minerals = List.copyOf(minerals);
        rocks = List.copyOf(rocks);
        Objects.requireNonNull(biomes, "biomes");
        if (maxY < minY) {
            final int low = maxY;
            maxY = minY;
            minY = low;
        }
    }

    /** As the vein field places it, its frequency, size and density set by the server's settings. */
    public VeinField.Kind kind(final double rarity, final double spread) {
        return new VeinField.Kind(Math.clamp(this.frequency * rarity, 0, 1), this.minY, this.maxY,
                Math.min(WIDEST, this.radius * spread), Math.min(WIDEST, this.thickness * spread));
    }

    /**
     * A material a vein brings together.
     *
     * @param material the material, by its id: {@code jscore:iron}
     * @param weight   how much of the vein is its ore, against the others'
     */
    public record Mineral(ResourceLocation material, double weight) {

        public static final Codec<Mineral> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("material").forGetter(Mineral::material),
                Codec.doubleRange(0, Double.MAX_VALUE).optionalFieldOf("weight", 1.0).forGetter(Mineral::weight))
                .apply(instance, Mineral::new));
    }
}
