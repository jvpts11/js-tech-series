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
 * The rockets' parts, by id: those declared in code, by J's Space or an add-on, and those a data pack's files say,
 * {@code data/<namespace>/jsspace/rocket_parts/}, a file winning over a declaration of its id. Any of the four standard
 * diameters may be declared; J's Space builds its parts in the first two.
 */
public final class RocketParts {

    private static final DeclaredData<RocketPart> PARTS = new DeclaredData<>(
            ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, "rocket_parts"), "jsspace/rocket_parts",
            RocketPart.CODEC);

    private RocketParts() {
    }

    /** Declares the part of {@code id}, as it is while no data pack says otherwise. */
    public static RocketPart declare(final ResourceLocation id, final RocketPart part) {
        return PARTS.declare(id, part);
    }

    /** The part of {@code id} on the side {@code level} is on, or none. */
    public static Optional<RocketPart> get(final Level level, final ResourceLocation id) {
        return PARTS.get(level, id);
    }

    /** Every part on the side {@code level} is on, by id: a data pack's over the declared. */
    public static Map<ResourceLocation, RocketPart> all(final Level level) {
        return PARTS.all(level);
    }

    /** Every part declared in code, by id. */
    public static Map<ResourceLocation, RocketPart> declared() {
        return PARTS.declared();
    }
}
