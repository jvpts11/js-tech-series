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
 * The block kits of the starship parts, by the part's id: those declared in code, by J's Space or an add-on, and those
 * a data pack's files say, {@code data/<namespace>/jsspace/ship_kits/}, a file winning over a declaration of its id. A
 * part is built of its kit; the shipyard's plan and its hologram place each block where the kit says.
 */
public final class ShipKits {

    private static final DeclaredData<ShipKit> KITS = new DeclaredData<>(
            ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, "ship_kits"), "jsspace/ship_kits", ShipKit.CODEC);

    private ShipKits() {
    }

    /** Declares the kit of the part {@code part}, as it is while no data pack says otherwise. */
    public static ShipKit declare(final ResourceLocation part, final ShipKit kit) {
        return KITS.declare(part, kit);
    }

    /** The kit of the part {@code part} on the side {@code level} is on, or none. */
    public static Optional<ShipKit> get(final Level level, final ResourceLocation part) {
        return KITS.get(level, part);
    }

    /** Every kit on the side {@code level} is on, by its part's id: a data pack's over the declared. */
    public static Map<ResourceLocation, ShipKit> all(final Level level) {
        return KITS.all(level);
    }

    /** Every kit declared in code, by its part's id. */
    public static Map<ResourceLocation, ShipKit> declared() {
        return KITS.declared();
    }
}
