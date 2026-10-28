/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.api;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/**
 * The generators of worlds, by id, which a world names in its data: the series' own (cratered, arid and the others)
 * and an add-on's. Each is registered once, while the game loads, from the mod's constructor.
 */
public final class WorldGenerators {

    private static final Map<ResourceLocation, IWorldGenerator> GENERATORS = new LinkedHashMap<>();

    private WorldGenerators() {
    }

    /**
     * Registers {@code generator} as {@code id}.
     *
     * @throws IllegalStateException when another generator is registered as {@code id}
     */
    public static synchronized void register(final ResourceLocation id, final IWorldGenerator generator) {
        Objects.requireNonNull(generator, "generator");
        if (GENERATORS.putIfAbsent(Objects.requireNonNull(id, "id"), generator) != null) {
            throw new IllegalStateException("the world generator " + id + " is registered twice");
        }
    }

    /** The generator registered as {@code id}, if any. */
    public static synchronized Optional<IWorldGenerator> get(final ResourceLocation id) {
        return Optional.ofNullable(GENERATORS.get(id));
    }

    /** Every generator, by id, in the order they were registered. */
    public static synchronized Map<ResourceLocation, IWorldGenerator> all() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(GENERATORS));
    }
}
