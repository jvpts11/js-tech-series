/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.universe;

import dev.jstech.core.dimension.RuntimeDimensions;
import dev.jstech.core.sky.Skies;
import dev.jstech.core.worldgen.WorldGen;
import dev.jstech.space.JsSpace;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;

/**
 * The space of each star system: a dimension of its own, where its worlds are spheres in their places and a world's
 * orbit is the space around its sphere, made the first time someone gets there, as a copy of one empty template, and
 * kept with the world from then on. It is vacuum with no gravity, by the template's rules
 * ({@code data/jsspace/dimension_rules/system_space.json}).
 */
public final class SystemSpaces {

    /**
     * What every system's space is a copy of: nothing at all, as tall as the Overworld, its sun always up since nothing
     * turns under it, and its black, starry sky drawn by J's Core's sky kit from
     * {@code data/jsspace/skies/system_space.json}.
     */
    public static final WorldGen.DimensionSpec TEMPLATE = WorldGen.dimension(id("system_space"))
            .layer(() -> Blocks.AIR, 1).biome(Biomes.THE_VOID).heights(-64, 384).fixedTime(6_000)
            .effects(Skies.EFFECTS).unnatural().runtimeOnly().declare();
    /* What a system's space is called after the system: jsspace:solar_system_space. */
    private static final String SPACE = "_space";

    private SystemSpaces() {
    }

    /** Declares the template, before the data generation writes the mod's files. */
    public static void declare() {
        // Loading the class declares it.
    }

    /** The space of {@code system}, made now as a copy of the template when no one has been there yet. */
    public static ServerLevel spaceOf(final MinecraftServer server, final ResourceLocation system) {
        return RuntimeDimensions.getOrCreate(server, keyOf(system), TEMPLATE.id());
    }

    /** The dimension that is, or will be, the space of {@code system}. */
    public static ResourceKey<Level> keyOf(final ResourceLocation system) {
        return ResourceKey.create(Registries.DIMENSION, system.withSuffix(SPACE));
    }

    /** The system whose space {@code dimension} is, when it is one. */
    public static Optional<ResourceLocation> systemOf(final ResourceKey<Level> dimension) {
        final ResourceLocation id = dimension.location();
        return RuntimeDimensions.templateOf(id).filter(TEMPLATE.id()::equals).map(template -> id.withPath(
                id.getPath().substring(0, id.getPath().length() - SPACE.length())));
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, path);
    }
}
