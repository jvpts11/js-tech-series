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
 * The finishes a starship may be painted in, by id: those declared in code, by J's Space or an add-on, and those a data
 * pack's files say, {@code data/<namespace>/jsspace/ship_finishes/}, a file winning over a declaration of its id.
 */
public final class ShipFinishes {

    private static final DeclaredData<ShipFinish> FINISHES = new DeclaredData<>(
            ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, "ship_finishes"), "jsspace/ship_finishes",
            ShipFinish.CODEC);

    private ShipFinishes() {
    }

    /** Declares the finish of {@code id}, as it is while no data pack says otherwise. */
    public static ShipFinish declare(final ResourceLocation id, final ShipFinish finish) {
        return FINISHES.declare(id, finish);
    }

    /** The finish of {@code id} on the side {@code level} is on, or none. */
    public static Optional<ShipFinish> get(final Level level, final ResourceLocation id) {
        return FINISHES.get(level, id);
    }

    /** Every finish on the side {@code level} is on, by id: a data pack's over the declared. */
    public static Map<ResourceLocation, ShipFinish> all(final Level level) {
        return FINISHES.all(level);
    }

    /** Every finish declared in code, by id. */
    public static Map<ResourceLocation, ShipFinish> declared() {
        return FINISHES.declared();
    }
}
