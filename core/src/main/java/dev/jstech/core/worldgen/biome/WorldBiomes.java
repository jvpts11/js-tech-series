/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.biome;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.function.Supplier;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * The biomes a mod adds, declared in code with their climate in real values and written by the generator as the
 * datapack files the game reads, the biome and its climate both, so a datapack can change either. A biome lies in any
 * dimension whose biomes name it ({@code WorldGen.dimension(id).biome(key, temperature, humidity)}); the Overworld's
 * own regions are left to a mod made for the Overworld.
 *
 * <pre>{@code
 * WorldBiomes.biome(id("salt_steppe")).name("Salt Steppe").climate(9.5, 240).seasons(16)
 *         .sky(0x9DB8D9).fog(0xD8D2C4).water(0x5E9FA8).grass(0xB8B07A)
 *         .feature(GenerationStep.Decoration.VEGETAL_DECORATION, VegetationPlacements.PATCH_GRASS_PLAIN)
 *         .declare();
 * }</pre>
 */
public final class WorldBiomes {

    private static final List<BiomeSpec> BIOMES = Collections.synchronizedList(new ArrayList<>());

    private WorldBiomes() {
    }

    /** Starts declaring a biome named {@code id}. */
    public static BiomeBuilder biome(final ResourceLocation id) {
        return new BiomeBuilder(id);
    }

    /** The biomes the mod of that namespace declared. */
    public static List<BiomeSpec> biomesOf(final String namespace) {
        synchronized (BIOMES) {
            return BIOMES.stream().filter(biome -> biome.id().getNamespace().equals(namespace)).toList();
        }
    }

    /**
     * A biome.
     *
     * @param id            what it is named
     * @param name          its name in English
     * @param climate       its climate in real values
     * @param precipitation whether rain, or snow where it is cold, falls on it
     * @param sky           its sky's colour
     * @param fog           its fog's colour
     * @param water         its water's colour
     * @param waterFog      the colour seen under its water
     * @param grass         its grass's colour, or empty for the one its climate gives
     * @param foliage       its leaves' colour, or empty for the one its climate gives
     * @param features      what grows and lies in it, each with the step it is placed in
     * @param carvers       the caves cut through it
     * @param spawns        what lives in it
     */
    public record BiomeSpec(ResourceLocation id, String name, BiomeClimate climate, boolean precipitation, int sky,
                            int fog, int water, int waterFog, OptionalInt grass, OptionalInt foliage,
                            List<Placed> features, List<ResourceKey<ConfiguredWorldCarver<?>>> carvers,
                            List<Spawn> spawns) {

        public BiomeSpec {
            features = List.copyOf(features);
            carvers = List.copyOf(carvers);
            spawns = List.copyOf(spawns);
        }

        /** The key the game knows it by. */
        public ResourceKey<Biome> key() {
            return ResourceKey.create(Registries.BIOME, this.id);
        }

        /** The biome, as the game registers it. */
        public Biome biome(final HolderGetter<PlacedFeature> placed,
                           final HolderGetter<ConfiguredWorldCarver<?>> carved) {
            final BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(placed, carved);
            this.features.forEach(feature -> generation.addFeature(feature.step(), feature.feature()));
            this.carvers.forEach(carver -> generation.addCarver(GenerationStep.Carving.AIR, carver));
            final MobSpawnSettings.Builder living = new MobSpawnSettings.Builder();
            this.spawns.forEach(spawn -> living.addSpawn(spawn.category(), new MobSpawnSettings.SpawnerData(
                    spawn.type().get(), spawn.weight(), spawn.fewest(), spawn.most())));
            final BiomeSpecialEffects.Builder effects = new BiomeSpecialEffects.Builder().skyColor(this.sky)
                    .fogColor(this.fog).waterColor(this.water).waterFogColor(this.waterFog);
            this.grass.ifPresent(effects::grassColorOverride);
            this.foliage.ifPresent(effects::foliageColorOverride);
            return new Biome.BiomeBuilder().hasPrecipitation(this.precipitation)
                    .temperature(this.climate.gameTemperature()).downfall(this.climate.gameDownfall())
                    .specialEffects(effects.build()).mobSpawnSettings(living.build())
                    .generationSettings(generation.build()).build();
        }
    }

    /**
     * Something placed in a biome.
     *
     * @param step    when in the generation it is placed
     * @param feature what is placed
     */
    public record Placed(GenerationStep.Decoration step, ResourceKey<PlacedFeature> feature) {
    }

    /**
     * Something that lives in a biome.
     *
     * @param category what kind of living thing it is, which says how many of it the biome holds
     * @param type     what lives there
     * @param weight   how often it spawns beside the others of its category
     * @param fewest   the fewest in a group
     * @param most     the most in a group
     */
    public record Spawn(MobCategory category, Supplier<? extends EntityType<?>> type, int weight, int fewest,
                        int most) {
    }

    /** A biome being declared; it starts temperate, as mild and wet as plains, under the Overworld's colours. */
    public static final class BiomeBuilder {

        private final ResourceLocation id;
        private final List<Placed> features = new ArrayList<>();
        private final List<ResourceKey<ConfiguredWorldCarver<?>>> carvers = new ArrayList<>();
        private final List<Spawn> spawns = new ArrayList<>();
        private String name;
        private double temperature = ClimateMath.celsius(0.8);
        private double rain = ClimateMath.rain(0.4);
        private double seasons = Double.NaN;
        private boolean precipitation = true;
        private int sky = 0x78A7FF;
        private int fog = 0xC0D8FF;
        private int water = 0x3F76E4;
        private int waterFog = 0x050533;
        private OptionalInt grass = OptionalInt.empty();
        private OptionalInt foliage = OptionalInt.empty();

        private BiomeBuilder(final ResourceLocation id) {
            this.id = Objects.requireNonNull(id, "id");
            this.name = id.getPath();
        }

        /** Its name in English, which the generator writes to the mod's English. */
        public BiomeBuilder name(final String english) {
            this.name = Objects.requireNonNull(english, "english");
            return this;
        }

        /**
         * Its climate: a mean of {@code celsius} degrees over the year and {@code rainMillimetres} of rain a year. The
         * game's temperature and downfall for it, which say where it snows and how its grass is coloured, follow.
         */
        public BiomeBuilder climate(final double celsius, final double rainMillimetres) {
            this.temperature = celsius;
            this.rain = rainMillimetres;
            return this;
        }

        /** How far the seasons swing it either way, in degrees Celsius; otherwise as far as its climate says. */
        public BiomeBuilder seasons(final double swing) {
            this.seasons = swing;
            return this;
        }

        /** Nothing falls on it: no rain, no snow. */
        public BiomeBuilder dry() {
            this.precipitation = false;
            return this;
        }

        public BiomeBuilder sky(final int rgb) {
            this.sky = rgb;
            return this;
        }

        public BiomeBuilder fog(final int rgb) {
            this.fog = rgb;
            return this;
        }

        /** Its water's colour, and the colour seen under it. */
        public BiomeBuilder water(final int rgb, final int underRgb) {
            this.water = rgb;
            this.waterFog = underRgb;
            return this;
        }

        public BiomeBuilder grass(final int rgb) {
            this.grass = OptionalInt.of(rgb);
            return this;
        }

        public BiomeBuilder foliage(final int rgb) {
            this.foliage = OptionalInt.of(rgb);
            return this;
        }

        /** Places {@code feature} in it in {@code step} of the generation. */
        public BiomeBuilder feature(final GenerationStep.Decoration step, final ResourceKey<PlacedFeature> feature) {
            this.features.add(new Placed(step, feature));
            return this;
        }

        /** Cuts the caves of {@code carver} through it. */
        public BiomeBuilder carver(final ResourceKey<ConfiguredWorldCarver<?>> carver) {
            this.carvers.add(carver);
            return this;
        }

        /** Has {@code type} live in it, {@code fewest} to {@code most} in a group, as often as {@code weight}. */
        public BiomeBuilder spawn(final MobCategory category, final Supplier<? extends EntityType<?>> type,
                                  final int weight, final int fewest, final int most) {
            this.spawns.add(new Spawn(category, type, weight, fewest, most));
            return this;
        }

        /**
         * Declares the biome.
         *
         * @throws IllegalStateException when its climate is colder than anything can be, or it rains less than nothing
         */
        public BiomeSpec declare() {
            if (temperature < -273.15 || rain < 0) {
                throw new IllegalStateException("the biome " + id + " has a climate that can be: not "
                        + temperature + " °C and " + rain + " mm of rain");
            }
            final double game = ClimateMath.gameTemperature(temperature);
            final double swing = Double.isNaN(seasons) ? ClimateMath.seasons(game, ClimateMath.downfall(rain))
                    : seasons;
            final BiomeSpec spec = new BiomeSpec(id, name, new BiomeClimate(temperature, swing, rain), precipitation,
                    sky, fog, water, waterFog, grass, foliage, features, carvers, spawns);
            BIOMES.add(spec);
            return spec;
        }
    }
}
