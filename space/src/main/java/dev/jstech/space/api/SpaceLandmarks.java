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
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * The landmarks of the worlds, by id: those declared in code, by J's Space or an add-on, and those a data pack's
 * files say, {@code data/<namespace>/jsspace/landmarks/}, a file winning over a declaration of its id. The first
 * declared was the Moon's Tycho. A world's generator lays the landmarks declared for it as the data generation writes
 * its ground.
 */
public final class SpaceLandmarks {

    private static final DeclaredData<SpaceLandmark> LANDMARKS = new DeclaredData<>(
            ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, "landmarks"), "jsspace/landmarks",
            SpaceLandmark.CODEC);

    private SpaceLandmarks() {
    }

    /** Declares the landmark of {@code id}, as it is while no data pack says otherwise, while the game loads. */
    public static SpaceLandmark declare(final ResourceLocation id, final SpaceLandmark landmark) {
        return LANDMARKS.declare(id, landmark);
    }

    /** Every landmark on the side {@code level} is on, by id: a data pack's over the declared. */
    public static Map<ResourceLocation, SpaceLandmark> all(final Level level) {
        return LANDMARKS.all(level);
    }

    /** Every landmark declared in code, by id. */
    public static Map<ResourceLocation, SpaceLandmark> declared() {
        return LANDMARKS.declared();
    }

    /** The landmarks declared in code on the world {@code world}. */
    public static List<SpaceLandmark> declaredOn(final ResourceLocation world) {
        return LANDMARKS.declared().values().stream().filter(landmark -> landmark.world().equals(world)).toList();
    }
}
