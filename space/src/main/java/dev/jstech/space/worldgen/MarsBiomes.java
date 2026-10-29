/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.worldgen;

import dev.jstech.core.worldgen.biome.WorldBiomes;
import dev.jstech.space.JsSpace;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

/**
 * Mars' seven biomes, its own: the low smooth northern plains and the old cratered southern highlands either side of
 * the dichotomy, the dark dunes of basalt sand where its map is dark, the chaos terrain where its canyons open, the
 * oxide plains strewn with hematite and the beds of old lakes in the highlands, and its polar caps. Every one is as
 * cold as Mars is, the caps colder by far.
 */
public final class MarsBiomes {

    /* How cold Mars' ground is on the whole, and its polar caps, in degrees Celsius. */
    private static final double MARS = -63;
    private static final double CAP = -125;
    /* The butterscotch of a dusty sky, and the haze its far ground fades into. */
    private static final int SKY = 0xC9946A;
    private static final int HAZE = 0xB98A68;

    /** The low smooth plains north of the dichotomy, as Utopia Planitia. */
    public static final WorldBiomes.BiomeSpec NORTHERN_PLAINS = biome("mars_northern_plains", MARS)
            .name("Northern Plains").declare();
    /** Dark red ground strewn with hematite's spherules, as Meridiani Planum. */
    public static final WorldBiomes.BiomeSpec OXIDE_PLAINS = biome("mars_oxide_plains", MARS).name("Oxide Plains")
            .declare();
    /** The old high ground south of the dichotomy, crater on crater. */
    public static final WorldBiomes.BiomeSpec SOUTHERN_HIGHLANDS = biome("mars_southern_highlands", MARS)
            .name("Southern Highlands").declare();
    /** Crescents of dark basalt sand walking over the red ground. */
    public static final WorldBiomes.BiomeSpec DARK_DUNES = biome("mars_dark_dunes", MARS).name("Dark Dunes")
            .declare();
    /** Ground broken into flat-topped blocks where the ice under it melted and flowed away. */
    public static final WorldBiomes.BiomeSpec CHAOS_TERRAIN = biome("mars_chaos_terrain", MARS)
            .name("Chaos Terrain").declare();
    /** The bed of an old lake in a crater, its mudstone laid by the water, as Gale and Jezero. */
    public static final WorldBiomes.BiomeSpec LAKEBED = biome("mars_lakebed", MARS).name("Lakebed").declare();
    /** Water ice in layers with dust between them, carbon dioxide frost over it, cut by spiral troughs. */
    public static final WorldBiomes.BiomeSpec POLAR_CAP = biome("mars_polar_cap", CAP).name("Polar Cap").declare();
    /** Every one of them. */
    public static final List<WorldBiomes.BiomeSpec> ALL = List.of(NORTHERN_PLAINS, OXIDE_PLAINS, SOUTHERN_HIGHLANDS,
            DARK_DUNES, CHAOS_TERRAIN, LAKEBED, POLAR_CAP);

    /** The biomes of Mars' stony ground, where its basalt's stones lie loose. */
    public static final TagKey<Biome> STONY = tag("mars_stony");
    /** The biomes of its old cratered ground. */
    public static final TagKey<Biome> CRATERED = tag("mars_cratered");
    /** The biomes its basalt lies under, varied by geology. */
    public static final TagKey<Biome> BASALT = tag("mars_basalt");
    /** The biomes its mudstone lies in, varied by geology. */
    public static final TagKey<Biome> MUDSTONE = tag("mars_mudstone");
    /** The biomes its dunes walk over. */
    public static final TagKey<Biome> DUNES = tag("mars_dunes");
    /** The biomes its chaos breaks into blocks. */
    public static final TagKey<Biome> CHAOS = tag("mars_chaos");
    /** The biomes its old lakes lie in. */
    public static final TagKey<Biome> LAKES = tag("mars_lakes");
    /** The biomes its caps' spiral troughs are cut in. */
    public static final TagKey<Biome> TROUGHS = tag("mars_troughs");
    /** The biomes its oxides lie in, hematite in the regolith and the mudstone. */
    public static final TagKey<Biome> OXIDES = tag("mars_oxides");

    private MarsBiomes() {
    }

    /** Declares the biomes, once, from the mod's constructor. */
    public static void declare() {
        // Loading the class declares them.
    }

    /* A biome of Mars, {@code celsius} cold, with no rain, under its dusty sky. */
    private static WorldBiomes.BiomeBuilder biome(final String id, final double celsius) {
        return WorldBiomes.biome(ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, id)).climate(celsius, 0).dry()
                .sky(SKY).fog(HAZE);
    }

    private static TagKey<Biome> tag(final String id) {
        return TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, id));
    }
}
