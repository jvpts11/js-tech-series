/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.universe;

import dev.jstech.core.text.TextKey;
import dev.jstech.space.JsSpace;
import dev.jstech.space.api.SpaceWorld;
import dev.jstech.space.api.SpaceWorlds;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * The worlds of the Solar System the skeleton holds: the Overworld, whose dimension generates itself as the game's
 * does, the Moon, shaped by impacts, and Mars, shaped by wind. Their widths are the real ones. What each is like to
 * stand on is its dimension's rules file, {@code data/jsspace/dimension_rules/}; the Overworld keeps the game's own.
 */
public final class SolarSystemWorlds {

    /* The generators the Moon and Mars are shaped by. */
    private static final ResourceLocation CRATERED = id("cratered");
    private static final ResourceLocation ARID = id("arid");

    public static final ResourceLocation OVERWORLD = declare("overworld", UniverseTexts.OVERWORLD_WORLD,
            Level.OVERWORLD, null, SphereMap.EARTH_DIAMETER);
    public static final ResourceLocation MOON = declare("moon", UniverseTexts.MOON_WORLD, dimension("moon"), CRATERED,
            3_474.8);
    public static final ResourceLocation MARS = declare("mars", UniverseTexts.MARS_WORLD, dimension("mars"), ARID,
            6_779);

    private SolarSystemWorlds() {
    }

    /** Declares the worlds, before the game has loaded. */
    public static void declare() {
        // Loading the class declares them.
    }

    /** The body the world {@code id} is drawn as in the Solar System's space, if it is one of these. */
    public static Optional<SolarBody> bodyOf(final ResourceLocation id) {
        if (OVERWORLD.equals(id)) {
            return Optional.of(SolarBody.OVERWORLD);
        }
        if (MOON.equals(id)) {
            return Optional.of(SolarBody.MOON);
        }
        return MARS.equals(id) ? Optional.of(SolarBody.MARS) : Optional.empty();
    }

    /** The world drawn as {@code body} in the Solar System's space. */
    public static ResourceLocation worldOf(final SolarBody body) {
        return switch (body) {
            case OVERWORLD -> OVERWORLD;
            case MOON -> MOON;
            case MARS -> MARS;
        };
    }

    /* A world, {@code generator} null for one whose dimension generates itself. */
    private static ResourceLocation declare(final String path, final TextKey name, final ResourceKey<Level> dimension,
                                            @Nullable final ResourceLocation generator, final double diameter) {
        final ResourceLocation id = id(path);
        SpaceWorlds.declare(id, new SpaceWorld(name.key(), StarSystems.SOLAR_SYSTEM, dimension,
                Optional.ofNullable(generator), List.of(), diameter));
        return id;
    }

    private static ResourceKey<Level> dimension(final String path) {
        return ResourceKey.create(Registries.DIMENSION, id(path));
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, path);
    }
}
