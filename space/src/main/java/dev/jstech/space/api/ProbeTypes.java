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
 * The kinds of probe, by id: those declared in code, by J's Space or an add-on, and those a data pack's files say,
 * {@code data/<namespace>/jsspace/probe_types/}, a file winning over a declaration of its id.
 */
public final class ProbeTypes {

    private static final DeclaredData<ProbeType> TYPES = new DeclaredData<>(
            ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, "probe_types"), "jsspace/probe_types",
            ProbeType.CODEC);

    private ProbeTypes() {
    }

    /** Declares the kind of probe of {@code id}, as it is while no data pack says otherwise. */
    public static ProbeType declare(final ResourceLocation id, final ProbeType type) {
        return TYPES.declare(id, type);
    }

    /** The kind of probe of {@code id} on the side {@code level} is on, or none. */
    public static Optional<ProbeType> get(final Level level, final ResourceLocation id) {
        return TYPES.get(level, id);
    }

    /** Every kind of probe on the side {@code level} is on, by id: a data pack's over the declared. */
    public static Map<ResourceLocation, ProbeType> all(final Level level) {
        return TYPES.all(level);
    }

    /** Every kind of probe declared in code, by id. */
    public static Map<ResourceLocation, ProbeType> declared() {
        return TYPES.declared();
    }
}
