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
 * The block kits of the rocket parts, by the part's id: those declared in code, by J's Space or an add-on, and those a
 * data pack's files say, {@code data/<namespace>/jsspace/rocket_kits/}, a file winning over a declaration of its id.
 * A part is built of its kit; the hangar's plan and its hologram place each block where the kit says.
 */
public final class RocketKits {

    private static final DeclaredData<RocketKit> KITS = new DeclaredData<>(
            ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, "rocket_kits"), "jsspace/rocket_kits",
            RocketKit.CODEC);

    private RocketKits() {
    }

    /** Declares the kit of the part {@code part}, as it is while no data pack says otherwise. */
    public static RocketKit declare(final ResourceLocation part, final RocketKit kit) {
        return KITS.declare(part, kit);
    }

    /** The kit of the part {@code part} on the side {@code level} is on, or none. */
    public static Optional<RocketKit> get(final Level level, final ResourceLocation part) {
        return KITS.get(level, part);
    }

    /** Every kit on the side {@code level} is on, by its part's id: a data pack's over the declared. */
    public static Map<ResourceLocation, RocketKit> all(final Level level) {
        return KITS.all(level);
    }

    /** Every kit declared in code, by its part's id. */
    public static Map<ResourceLocation, RocketKit> declared() {
        return KITS.declared();
    }
}
