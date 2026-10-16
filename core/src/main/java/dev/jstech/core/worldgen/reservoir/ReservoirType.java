/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.reservoir;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.worldgen.vein.VeinField;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;

/**
 * A kind of reservoir, as its file under {@code data/<namespace>/jstech/reservoirs/} says: the liquid or gas in it, the
 * porous rock soaked in it, the plain rock a block of that turns back into once drained, the cap rock that holds it in,
 * how much each block holds, and where, how often and how large one lies. A reservoir is a lens of the soaked rock in
 * the rock it replaces, under a dome of the cap rock; it holds no liquid as a liquid, so digging into one floods
 * nothing, and shows it.
 *
 * <pre>{@code
 * { "fluid": "mymod:crude_oil", "soaked": "mymod:oil_sand", "drained": "minecraft:sandstone",
 *   "cap": "minecraft:smooth_basalt", "per_block": 1000, "min_y": -40, "max_y": 20, "frequency": 0.25,
 *   "radius": 28, "thickness": 5, "biomes": "c:is_desert", "elsewhere": 0.3 }
 * }</pre>
 *
 * @param fluid     the liquid or gas it holds, by its id
 * @param soaked    the porous rock soaked in it, by its id
 * @param drained   the plain rock a block of the soaked rock turns into once it holds no more, by its id
 * @param cap       the cap rock over it, by its id
 * @param perBlock  how much each block of it holds, in millibuckets
 * @param minY      the lowest its middle lies
 * @param maxY      the highest
 * @param frequency how many zones of eight chunks a side hold one, 0 to 1
 * @param radius    how far across it reaches from its middle, in blocks
 * @param thickness how far up and down its lens reaches from its middle
 * @param capping   how thick its cap is, in blocks
 * @param biomes    the biomes it lies under, by the biome at its middle; empty for any
 * @param elsewhere how many as often it lies outside those biomes, 0 for never
 * @param replaces  the rock it takes the place of; it never replaces air, a liquid or what is not that rock
 */
public record ReservoirType(ResourceLocation fluid, ResourceLocation soaked, ResourceLocation drained,
                            ResourceLocation cap, int perBlock, int minY, int maxY, double frequency, double radius,
                            double thickness, int capping, Optional<TagKey<Biome>> biomes, double elsewhere,
                            TagKey<Block> replaces) {

    private static final int LOWEST = -2048;
    private static final int HIGHEST = 2048;
    private static final int MOST_PER_BLOCK = 1_000_000;
    private static final double WIDEST = 128;
    private static final int THICKEST_CAP = 16;
    public static final Codec<ReservoirType> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("fluid").forGetter(ReservoirType::fluid),
            ResourceLocation.CODEC.fieldOf("soaked").forGetter(ReservoirType::soaked),
            ResourceLocation.CODEC.fieldOf("drained").forGetter(ReservoirType::drained),
            ResourceLocation.CODEC.fieldOf("cap").forGetter(ReservoirType::cap),
            Codec.intRange(1, MOST_PER_BLOCK).optionalFieldOf("per_block", 1000).forGetter(ReservoirType::perBlock),
            Codec.intRange(LOWEST, HIGHEST).fieldOf("min_y").forGetter(ReservoirType::minY),
            Codec.intRange(LOWEST, HIGHEST).fieldOf("max_y").forGetter(ReservoirType::maxY),
            Codec.doubleRange(0, 1).fieldOf("frequency").forGetter(ReservoirType::frequency),
            Codec.doubleRange(2, WIDEST).optionalFieldOf("radius", 24.0).forGetter(ReservoirType::radius),
            Codec.doubleRange(1, WIDEST).optionalFieldOf("thickness", 4.0).forGetter(ReservoirType::thickness),
            Codec.intRange(0, THICKEST_CAP).optionalFieldOf("capping", 2).forGetter(ReservoirType::capping),
            TagKey.codec(Registries.BIOME).optionalFieldOf("biomes").forGetter(ReservoirType::biomes),
            Codec.doubleRange(0, 1).optionalFieldOf("elsewhere", 0.0).forGetter(ReservoirType::elsewhere),
            TagKey.codec(Registries.BLOCK).optionalFieldOf("replaces", BlockTags.BASE_STONE_OVERWORLD)
                    .forGetter(ReservoirType::replaces))
            .apply(instance, ReservoirType::new));

    public ReservoirType {
        Objects.requireNonNull(fluid, "fluid");
        Objects.requireNonNull(soaked, "soaked");
        Objects.requireNonNull(drained, "drained");
        Objects.requireNonNull(cap, "cap");
        Objects.requireNonNull(biomes, "biomes");
        Objects.requireNonNull(replaces, "replaces");
        if (maxY < minY) {
            final int low = maxY;
            maxY = minY;
            minY = low;
        }
    }

    /** As the field places it. */
    public VeinField.Kind kind() {
        return new VeinField.Kind(this.frequency, this.minY, this.maxY, this.radius, this.thickness);
    }
}
