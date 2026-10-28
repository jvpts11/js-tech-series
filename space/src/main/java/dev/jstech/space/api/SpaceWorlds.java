/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.api;

import dev.jstech.core.data.DeclaredData;
import dev.jstech.space.JsSpace;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * The worlds J's Space takes the player to, by id: those declared in code, by J's Space or an add-on, and those a
 * data pack's files say, {@code data/<namespace>/jsspace/worlds/}, a file winning over a declaration of its id, so a
 * pack removes, changes or adds a world with no code. Both sides read the same.
 */
public final class SpaceWorlds {

    private static final DeclaredData<SpaceWorld> WORLDS = new DeclaredData<>(
            ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, "worlds"), "jsspace/worlds", SpaceWorld.CODEC);

    private SpaceWorlds() {
    }

    /** Declares the world of {@code id}, as it is while no data pack says otherwise, while the game loads. */
    public static SpaceWorld declare(final ResourceLocation id, final SpaceWorld world) {
        return WORLDS.declare(id, world);
    }

    /** The world of {@code id} on the side {@code level} is on, or none. */
    public static Optional<SpaceWorld> get(final Level level, final ResourceLocation id) {
        return WORLDS.get(level, id);
    }

    /** The world {@code dimension} is, on the side {@code level} is on, or none. */
    public static Optional<SpaceWorld> ofDimension(final Level level, final ResourceKey<Level> dimension) {
        return WORLDS.all(level).values().stream().filter(world -> world.dimension().equals(dimension)).findFirst();
    }

    /** Every world on the side {@code level} is on, by id: a data pack's over the declared. */
    public static Map<ResourceLocation, SpaceWorld> all(final Level level) {
        return WORLDS.all(level);
    }

    /** Every world declared in code, by id. */
    public static Map<ResourceLocation, SpaceWorld> declared() {
        return WORLDS.declared();
    }
}
