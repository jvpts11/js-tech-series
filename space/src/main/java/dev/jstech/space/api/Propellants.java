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
 * The propellants, by id: those declared in code, by J's Space or an add-on, and those a data pack's files say,
 * {@code data/<namespace>/jsspace/propellants/}, a file winning over a declaration of its id. An engine names the one
 * it burns, and lights only on it.
 */
public final class Propellants {

    private static final DeclaredData<Propellant> PROPELLANTS = new DeclaredData<>(
            ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, "propellants"), "jsspace/propellants",
            Propellant.CODEC);

    private Propellants() {
    }

    /** Declares the propellant of {@code id}, as it is while no data pack says otherwise. */
    public static Propellant declare(final ResourceLocation id, final Propellant propellant) {
        return PROPELLANTS.declare(id, propellant);
    }

    /** The propellant of {@code id} on the side {@code level} is on, or none. */
    public static Optional<Propellant> get(final Level level, final ResourceLocation id) {
        return PROPELLANTS.get(level, id);
    }

    /** Every propellant on the side {@code level} is on, by id: a data pack's over the declared. */
    public static Map<ResourceLocation, Propellant> all(final Level level) {
        return PROPELLANTS.all(level);
    }

    /** Every propellant declared in code, by id. */
    public static Map<ResourceLocation, Propellant> declared() {
        return PROPELLANTS.declared();
    }
}
