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
 * The starships' parts and modules, by id: those declared in code, by J's Space or an add-on, and those a data pack's
 * files say, {@code data/<namespace>/jsspace/ship_parts/}, a file winning over a declaration of its id.
 */
public final class ShipParts {

    private static final DeclaredData<ShipPart> PARTS = new DeclaredData<>(
            ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, "ship_parts"), "jsspace/ship_parts",
            ShipPart.CODEC);

    private ShipParts() {
    }

    /** Declares the part of {@code id}, as it is while no data pack says otherwise. */
    public static ShipPart declare(final ResourceLocation id, final ShipPart part) {
        return PARTS.declare(id, part);
    }

    /** The part of {@code id} on the side {@code level} is on, or none. */
    public static Optional<ShipPart> get(final Level level, final ResourceLocation id) {
        return PARTS.get(level, id);
    }

    /** Every part on the side {@code level} is on, by id: a data pack's over the declared. */
    public static Map<ResourceLocation, ShipPart> all(final Level level) {
        return PARTS.all(level);
    }

    /** Every part declared in code, by id. */
    public static Map<ResourceLocation, ShipPart> declared() {
        return PARTS.declared();
    }
}
