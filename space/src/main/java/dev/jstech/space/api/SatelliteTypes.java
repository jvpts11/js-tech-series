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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * The kinds of satellite, by id: those declared in code, by J's Space or an add-on, as J's Warfare adds its defence
 * satellites, and those a data pack's files say, {@code data/<namespace>/jsspace/satellite_types/}, a file winning over
 * a declaration of its id.
 */
public final class SatelliteTypes {

    private static final DeclaredData<SatelliteType> TYPES = new DeclaredData<>(
            ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, "satellite_types"), "jsspace/satellite_types",
            SatelliteType.CODEC);

    private SatelliteTypes() {
    }

    /** Declares the kind of satellite of {@code id}, as it is while no data pack says otherwise. */
    public static SatelliteType declare(final ResourceLocation id, final SatelliteType type) {
        return TYPES.declare(id, type);
    }

    /** The kind of satellite of {@code id} on the side {@code level} is on, or none. */
    public static Optional<SatelliteType> get(final Level level, final ResourceLocation id) {
        return TYPES.get(level, id);
    }

    /** Every kind of satellite on the side {@code level} is on, by id: a data pack's over the declared. */
    public static Map<ResourceLocation, SatelliteType> all(final Level level) {
        return TYPES.all(level);
    }

    /** Every kind of satellite declared in code, by id. */
    public static Map<ResourceLocation, SatelliteType> declared() {
        return TYPES.declared();
    }
}
