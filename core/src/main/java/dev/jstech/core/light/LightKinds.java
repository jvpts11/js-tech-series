/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.light;

import dev.jstech.core.JsCore;
import dev.jstech.core.data.DataRegistry;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * The kinds of light, by id: each declared in code by its mod with what it is when nothing else says, and each a file
 * under {@code data/<namespace>/jstech/lights/} a data pack can write over, sent to every player. The data generation
 * writes a mod's declared kinds as its files ({@code LightKindProvider}).
 *
 * <pre>{@code
 * public static final ResourceLocation ALARM = LightKinds.declare(rl("alarm"),
 *         new LightKind(0xFF2010, 15, 15, LightKind.Shape.POINT, 0, Flicker.PULSE, LightKind.Control.REDSTONE, 0,
 *                 false));
 * }</pre>
 */
public final class LightKinds {

    /** The kinds the data packs say, by id. */
    public static final DataRegistry<LightKind> FILES = DataRegistry.builder(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "lights"), "jstech/lights", LightKind.CODEC)
            .synced().register();

    private static final Map<ResourceLocation, LightKind> DECLARED = new ConcurrentHashMap<>();

    private LightKinds() {
    }

    /** Declares the kind {@code id} as {@code kind}, what it is while no data pack says otherwise; and gives the id. */
    public static ResourceLocation declare(final ResourceLocation id, final LightKind kind) {
        DECLARED.put(Objects.requireNonNull(id, "id"), Objects.requireNonNull(kind, "kind"));
        return id;
    }

    /** The kind {@code id} on the side {@code level} is on: its file's, else as declared, else a plain white lamp. */
    public static LightKind get(final Level level, final ResourceLocation id) {
        final LightKind file = FILES.entries(level).get(id);
        if (file != null) {
            return file;
        }
        return DECLARED.getOrDefault(id, LightKind.PLAIN);
    }

    /** Every kind declared in code, by id. */
    public static Map<ResourceLocation, LightKind> declared() {
        return Map.copyOf(DECLARED);
    }
}
