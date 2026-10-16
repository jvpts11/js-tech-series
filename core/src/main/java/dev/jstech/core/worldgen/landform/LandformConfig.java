/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.landform;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.id.StableCodecs;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * What a kind of landform is, as its feature's file says: its kind, where in the world they lie
 * ({@link LandformField}), how tall or deep each is for its radius, and the blocks it lays.
 *
 * <pre>{@code
 * { "kind": "crater", "cell": 192, "chance": 0.5, "smallest": 8, "largest": 40, "salt": 7,
 *   "height": 0.3, "surface": {"Name": "minecraft:gravel"}, "fill": {"Name": "minecraft:stone"},
 *   "scatter": {"Name": "minecraft:cobblestone"} }
 * }</pre>
 *
 * @param kind      the kind
 * @param cell      a cell's side, in blocks, at most one in each
 * @param chance    how many cells hold one
 * @param smallest  the least radius
 * @param largest   the most radius
 * @param salt      what keeps them apart from another kind's on the same seed
 * @param height    how tall or deep it is, a share of its radius: a crater's depth, a mesa's height, a lava tube's
 *                  bore, a river's depth, a geyser's vent
 * @param surface   its top: a crater's floor, a volcano's slopes, a mesa's top
 * @param fill      what it is built of where it raises the ground
 * @param special   its own block: lava, ice, salt, a liquid
 * @param scatter   what it scatters or lines: ejecta, cracks, magma, what its liquid freezes to
 * @param bands     the bands of a mesa's body, from its foot up, again and again
 * @param freezes   below how many kelvin its liquid freezes to the scatter
 * @param boils     above how many kelvin its liquid boils away
 * @param biomes    the biomes one lies in, by the biome at its middle; empty for any biome it is placed in
 */
public record LandformConfig(LandformKind kind, int cell, double chance, int smallest, int largest, long salt,
                             double height, BlockState surface, BlockState fill, BlockState special,
                             BlockState scatter, List<BlockState> bands, double freezes, double boils,
                             Optional<HolderSet<Biome>> biomes)
        implements FeatureConfiguration {

    private static final int MOST_CELL = 4096;
    private static final int MOST_RADIUS = 512;
    private static final double MOST_HEIGHT = 4.0;
    private static final double WATER_FREEZES = 273.15;
    private static final double WATER_BOILS = 373.15;
    private static final double HOTTEST = 100_000;
    public static final Codec<LandformConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            StableCodecs.byName(LandformKind.class).fieldOf("kind").forGetter(LandformConfig::kind),
            Codec.intRange(16, MOST_CELL).fieldOf("cell").forGetter(LandformConfig::cell),
            Codec.doubleRange(0, 1).fieldOf("chance").forGetter(LandformConfig::chance),
            Codec.intRange(1, MOST_RADIUS).fieldOf("smallest").forGetter(LandformConfig::smallest),
            Codec.intRange(1, MOST_RADIUS).fieldOf("largest").forGetter(LandformConfig::largest),
            Codec.LONG.optionalFieldOf("salt", 0L).forGetter(LandformConfig::salt),
            Codec.doubleRange(0, MOST_HEIGHT).optionalFieldOf("height", 0.25).forGetter(LandformConfig::height),
            BlockState.CODEC.optionalFieldOf("surface", Blocks.STONE.defaultBlockState())
                    .forGetter(LandformConfig::surface),
            BlockState.CODEC.optionalFieldOf("fill", Blocks.STONE.defaultBlockState()).forGetter(LandformConfig::fill),
            BlockState.CODEC.optionalFieldOf("special", Blocks.WATER.defaultBlockState())
                    .forGetter(LandformConfig::special),
            BlockState.CODEC.optionalFieldOf("scatter", Blocks.COBBLESTONE.defaultBlockState())
                    .forGetter(LandformConfig::scatter),
            BlockState.CODEC.listOf().optionalFieldOf("bands", List.of()).forGetter(LandformConfig::bands),
            Codec.doubleRange(0, HOTTEST).optionalFieldOf("freezes", WATER_FREEZES).forGetter(LandformConfig::freezes),
            Codec.doubleRange(0, HOTTEST).optionalFieldOf("boils", WATER_BOILS).forGetter(LandformConfig::boils),
            RegistryCodecs.homogeneousList(Registries.BIOME).optionalFieldOf("biomes")
                    .forGetter(LandformConfig::biomes))
            .apply(instance, LandformConfig::new));

    public LandformConfig {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(surface, "surface");
        Objects.requireNonNull(fill, "fill");
        Objects.requireNonNull(special, "special");
        Objects.requireNonNull(scatter, "scatter");
        Objects.requireNonNull(biomes, "biomes");
        bands = List.copyOf(bands);
    }

    /** Where in the world they lie. */
    public LandformField field() {
        return new LandformField(this.cell, this.chance, this.smallest, Math.max(this.smallest, this.largest),
                this.salt);
    }
}
