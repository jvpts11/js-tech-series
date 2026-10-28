/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.exploration;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.space.JsSpace;
import dev.jstech.space.api.WorldProperties;
import dev.jstech.space.api.WorldProperty;
import net.minecraft.resources.ResourceLocation;

/**
 * What can be known of a world, as the series' instruments reveal it: the ten properties of a world, five of them the
 * essentials a crew needs known to land, since they are what kills there.
 */
@TextHolder
public final class SpaceProperties {

    public static final TextKey ORBIT_NAME = TextKey.of("jsspace.property.orbit", "Orbit and position");
    public static final TextKey GRAVITY_NAME = TextKey.of("jsspace.property.gravity", "Size, mass and gravity");
    public static final TextKey ATMOSPHERE_NAME = TextKey.of("jsspace.property.atmosphere", "Atmosphere");
    public static final TextKey TEMPERATURE_NAME = TextKey.of("jsspace.property.temperature", "Temperature");
    public static final TextKey RADIATION_NAME = TextKey.of("jsspace.property.radiation",
            "Magnetic field and radiation");
    public static final TextKey RELIEF_NAME = TextKey.of("jsspace.property.relief", "Relief");
    public static final TextKey GROUND_NAME = TextKey.of("jsspace.property.ground", "What lies on the ground");
    public static final TextKey INTERIOR_NAME = TextKey.of("jsspace.property.interior", "Interior");
    public static final TextKey STRUCTURES_NAME = TextKey.of("jsspace.property.structures", "Structures");
    public static final TextKey SAMPLES_NAME = TextKey.of("jsspace.property.samples", "Samples");

    public static final ResourceLocation ORBIT = register("orbit", ORBIT_NAME, false);
    public static final ResourceLocation GRAVITY = register("gravity", GRAVITY_NAME, true);
    public static final ResourceLocation ATMOSPHERE = register("atmosphere", ATMOSPHERE_NAME, true);
    public static final ResourceLocation TEMPERATURE = register("temperature", TEMPERATURE_NAME, true);
    public static final ResourceLocation RADIATION = register("radiation", RADIATION_NAME, true);
    public static final ResourceLocation RELIEF = register("relief", RELIEF_NAME, true);
    public static final ResourceLocation GROUND = register("ground", GROUND_NAME, false);
    public static final ResourceLocation INTERIOR = register("interior", INTERIOR_NAME, false);
    public static final ResourceLocation STRUCTURES = register("structures", STRUCTURES_NAME, false);
    public static final ResourceLocation SAMPLES = register("samples", SAMPLES_NAME, false);

    private SpaceProperties() {
    }

    /** Registers the properties, before the game has loaded. */
    public static void declare() {
        // Loading the class registers them.
    }

    private static ResourceLocation register(final String path, final TextKey name, final boolean essential) {
        final ResourceLocation id = ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, path);
        WorldProperties.register(id, new WorldProperty(name.key(), essential));
        return id;
    }
}
