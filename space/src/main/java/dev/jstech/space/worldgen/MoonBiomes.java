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
 * The Moon's six biomes, its own, borrowed from no other world: the maria of dark basalt where its map has them, the
 * bright highlands everywhere else, the craters of eternal shadow round its poles, and, in the maria, the rilles lava
 * cut and the skylights of lava tubes; the young craters with their bright ejecta and rays, where the map has those.
 * Every biome is as cold as the Moon is, without air, but the polar craters, colder by far.
 */
public final class MoonBiomes {

    /* How cold the Moon's ground is on the whole, and the floors of its polar craters, in degrees Celsius. */
    private static final double MOON = -20;
    private static final double ETERNAL_SHADOW = -230;
    /* The black of a sky with no air, and the faint grey its far ground fades into. */
    private static final int BLACK = 0x000000;
    private static final int FAR = 0x101014;

    /** The plains of old lava. */
    public static final WorldBiomes.BiomeSpec MARE = biome("moon_mare", MOON).name("Mare").declare();
    /** The bright cratered land of anorthosite. */
    public static final WorldBiomes.BiomeSpec HIGHLANDS = biome("moon_highlands", MOON).name("Highlands").declare();
    /** The craters round the poles whose floors never see the Sun, holding ice. */
    public static final WorldBiomes.BiomeSpec POLAR_CRATERS = biome("moon_polar_craters", ETERNAL_SHADOW)
            .name("Polar Craters").declare();
    /** A mare cut by winding valleys of lava, as Hadley Rille. */
    public static final WorldBiomes.BiomeSpec RILLE = biome("moon_rille", MOON).name("Rille").declare();
    /** A fresh crater's ejecta and rays, bright over what they fell on. */
    public static final WorldBiomes.BiomeSpec YOUNG_CRATER = biome("moon_young_crater", MOON).name("Young Crater")
            .declare();
    /** A mare over lava tubes, open to the sky where their roofs fell in, as the pit of the Marius Hills. */
    public static final WorldBiomes.BiomeSpec LAVA_TUBE_SKYLIGHT = biome("moon_lava_tube_skylight", MOON)
            .name("Lava Tube Skylight").declare();
    /** Every one of them. */
    public static final List<WorldBiomes.BiomeSpec> ALL = List.of(MARE, HIGHLANDS, POLAR_CRATERS, RILLE,
            YOUNG_CRATER, LAVA_TUBE_SKYLIGHT);

    /** The biomes of mare basalt: the maria, their rilles and their skylights. */
    public static final TagKey<Biome> MARIA = tag("moon_maria");
    /** The biomes of anorthosite: the highlands, the young craters and the polar craters. */
    public static final TagKey<Biome> ANORTHOSITE = tag("moon_anorthosite");
    /** The biomes the maria's craters lie in. */
    public static final TagKey<Biome> MARIA_CRATERS = tag("moon_maria_craters");
    /** The biomes the highlands' craters lie in. */
    public static final TagKey<Biome> HIGHLAND_CRATERS = tag("moon_highland_craters");
    /** The biomes the fresh craters lie in. */
    public static final TagKey<Biome> FRESH_CRATERS = tag("moon_fresh_craters");
    /** The biomes the craters of eternal shadow lie in. */
    public static final TagKey<Biome> SHADOWED_CRATERS = tag("moon_shadowed_craters");
    /** The biomes rilles wind through. */
    public static final TagKey<Biome> RILLES = tag("moon_rilles");
    /** The biomes lava tubes run under. */
    public static final TagKey<Biome> LAVA_TUBES = tag("moon_lava_tubes");

    private MoonBiomes() {
    }

    /** Declares the biomes, once, from the mod's constructor. */
    public static void declare() {
        // Loading the class declares them.
    }

    /* A biome of the Moon, {@code celsius} cold, with no rain, under a black sky. */
    private static WorldBiomes.BiomeBuilder biome(final String id, final double celsius) {
        return WorldBiomes.biome(ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, id)).climate(celsius, 0).dry()
                .sky(BLACK).fog(FAR);
    }

    private static TagKey<Biome> tag(final String id) {
        return TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, id));
    }
}
