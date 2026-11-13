/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.landform;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.dimension.DimensionBorders;
import dev.jstech.core.id.StableCodecs;
import dev.jstech.core.worldgen.terrain.RoundMap;
import dev.jstech.core.worldgen.terrain.RoundMaps;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
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
 * @param within    the region of a round world's map one lies in, by its middle; empty for anywhere
 */
public record LandformConfig(LandformKind kind, int cell, double chance, int smallest, int largest, long salt,
                             double height, BlockState surface, BlockState fill, BlockState special,
                             BlockState scatter, List<BlockState> bands, double freezes, double boils,
                             Optional<HolderSet<Biome>> biomes, Optional<Region> within)
        implements FeatureConfiguration {

    private static final int MOST_CELL = 4096;
    private static final int MOST_RADIUS = 512;
    private static final double MOST_HEIGHT = 4.0;
    private static final double WATER_FREEZES = 273.15;
    private static final double WATER_BOILS = 373.15;
    private static final double HOTTEST = 100_000;
    /* How white a region's map must read where a landform's middle lies. */
    private static final double HALF = 0.5;
    public static final Codec<LandformConfig> CODEC = RecordCodecBuilder.<LandformConfig>create(instance -> instance
            .group(
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
                    .forGetter(LandformConfig::biomes),
            Region.CODEC.optionalFieldOf("within").forGetter(LandformConfig::within))
            .apply(instance, LandformConfig::new))
            // A file whose largest is below its smallest is told so, not made the smallest without a word.
            .validate(config -> config.largest() >= config.smallest() ? DataResult.success(config)
                    : DataResult.error(() -> "a landform's largest, " + config.largest() + ", is below its smallest, "
                            + config.smallest()));

    public LandformConfig {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(surface, "surface");
        Objects.requireNonNull(fill, "fill");
        Objects.requireNonNull(special, "special");
        Objects.requireNonNull(scatter, "scatter");
        Objects.requireNonNull(biomes, "biomes");
        Objects.requireNonNull(within, "within");
        bands = List.copyOf(bands);
    }

    /** A kind of landform that lies anywhere its biomes allow, in no region of a map. */
    public LandformConfig(final LandformKind kind, final int cell, final double chance, final int smallest,
                          final int largest, final long salt, final double height, final BlockState surface,
                          final BlockState fill, final BlockState special, final BlockState scatter,
                          final List<BlockState> bands, final double freezes, final double boils,
                          final Optional<HolderSet<Biome>> biomes) {
        this(kind, cell, chance, smallest, largest, salt, height, surface, fill, special, scatter, bands, freezes,
                boils, biomes, Optional.empty());
    }

    /** Where in the world they lie. */
    public LandformField field() {
        return new LandformField(this.cell, this.chance, this.smallest, Math.max(this.smallest, this.largest),
                this.salt);
    }

    /**
     * Whether a landform whose middle is at {@code x, z} lies where they may: anywhere when they keep to no region,
     * else where the region's map reads more than half, at the size its world has on the server; nowhere where that
     * map or that size is not known.
     */
    public boolean mayLieAt(final int x, final int z) {
        if (this.within.isEmpty()) {
            return true;
        }
        final Region region = this.within.get();
        final Optional<DimensionBorders.Bounds> bounds = DimensionBorders.inForce(ResourceKey.create(
                Registries.DIMENSION, region.world()));
        final Optional<RoundMap> map = RoundMaps.get(region.map());
        if (bounds.isEmpty() || map.isEmpty()) {
            return false;
        }
        return map.get().atBlock(x, z, bounds.get().maxX() - bounds.get().minX()) > HALF;
    }

    /**
     * The region of a round world's map a kind of landform keeps to: where the map is white.
     *
     * @param map   the map, of {@link RoundMaps}
     * @param world the round world it is the map of, its dimension's id
     */
    public record Region(ResourceLocation map, ResourceLocation world) {

        public static final Codec<Region> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("map").forGetter(Region::map),
                ResourceLocation.CODEC.fieldOf("world").forGetter(Region::world))
                .apply(instance, Region::new));
    }
}
