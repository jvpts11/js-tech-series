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
 * The makers of starship parts, by id: those declared in code, by J's Space or an add-on, and those a data pack's files
 * say, {@code data/<namespace>/jsspace/ship_makers/}, a file winning over a declaration of its id.
 */
public final class ShipMakers {

    private static final DeclaredData<ShipMaker> MAKERS = new DeclaredData<>(
            ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, "ship_makers"), "jsspace/ship_makers",
            ShipMaker.CODEC);

    private ShipMakers() {
    }

    /** Declares the maker of {@code id}, as it is while no data pack says otherwise. */
    public static ShipMaker declare(final ResourceLocation id, final ShipMaker maker) {
        return MAKERS.declare(id, maker);
    }

    /** The maker of {@code id} on the side {@code level} is on, or none. */
    public static Optional<ShipMaker> get(final Level level, final ResourceLocation id) {
        return MAKERS.get(level, id);
    }

    /** Every maker on the side {@code level} is on, by id: a data pack's over the declared. */
    public static Map<ResourceLocation, ShipMaker> all(final Level level) {
        return MAKERS.all(level);
    }

    /** Every maker declared in code, by id. */
    public static Map<ResourceLocation, ShipMaker> declared() {
        return MAKERS.declared();
    }
}
