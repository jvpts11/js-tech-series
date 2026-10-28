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
 * What can be known of a world, by id: the series' own (its orbit and position, its size and gravity, its atmosphere,
 * its temperature, its magnetic field and radiation, its relief, what lies on its ground, its interior, its structures
 * and samples) and an add-on's. Each is registered once, while the game loads, from the mod's constructor.
 */
public final class WorldProperties {

    private static final Map<ResourceLocation, WorldProperty> PROPERTIES = new LinkedHashMap<>();

    private WorldProperties() {
    }

    /**
     * Registers {@code property} as {@code id}.
     *
     * @throws IllegalStateException when another property is registered as {@code id}
     */
    public static synchronized void register(final ResourceLocation id, final WorldProperty property) {
        Objects.requireNonNull(property, "property");
        if (PROPERTIES.putIfAbsent(Objects.requireNonNull(id, "id"), property) != null) {
            throw new IllegalStateException("the world property " + id + " is registered twice");
        }
    }

    /** The property registered as {@code id}, if any. */
    public static synchronized Optional<WorldProperty> get(final ResourceLocation id) {
        return Optional.ofNullable(PROPERTIES.get(id));
    }

    /** Every property, by id, in the order they were registered. */
    public static synchronized Map<ResourceLocation, WorldProperty> all() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(PROPERTIES));
    }
}
