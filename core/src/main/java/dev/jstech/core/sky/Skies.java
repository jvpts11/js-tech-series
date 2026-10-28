/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.sky;

import dev.jstech.core.JsCore;
import dev.jstech.core.data.DataRegistry;
import dev.jstech.core.dimension.RuntimeDimensions;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * Every dimension's sky, as the datapacks give it: a file named after the dimension,
 * {@code data/<namespace>/skies/<path>.json} for the dimension {@code <namespace>:<path>}, read whenever the server
 * loads its data and sent to the players. A dimension's sky is drawn from it when its kind of place is drawn as
 * {@link #EFFECTS} (a dimension type's {@code "effects"}, or {@code WorldGen.dimension(...).effects(Skies.EFFECTS)});
 * one with no file of its own has the overworld's sun and stars.
 */
public final class Skies {

    /** How a kind of place is drawn for the Core to draw its sky. */
    public static final ResourceLocation EFFECTS = ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "sky");

    private static final DataRegistry<SkySpec> FILES = DataRegistry.builder(
                    ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "skies"), "skies", SkySpec.CODEC)
            .synced().alsoSending(Skies::copiedSkies).register();

    private Skies() {
    }

    /** Declares the registry, from the Core's constructor. */
    public static void register() {
        // Loading the class declares it.
    }

    /**
     * The sky of {@code dimension} on the server: its own, or for a dimension made while the game runs that of the
     * dimension it copies, or the overworld's when there is none.
     */
    public static SkySpec of(final ResourceKey<Level> dimension) {
        return FILES.get(dimension.location())
                .or(() -> RuntimeDimensions.templateOf(dimension.location()).flatMap(FILES::get))
                .orElse(SkySpec.DEFAULT);
    }

    /** The sky of {@code level}: the server's on the server, the one it sent on a player's game, copies included. */
    public static SkySpec of(final Level level) {
        return of(level, level.dimension().location());
    }

    /**
     * The sky of the dimension {@code dimension} as {@code level}'s side knows it, whether or not it is the one
     * {@code level} is: for a sky shown from another, as on the way up into space.
     */
    public static SkySpec of(final Level level, final ResourceLocation dimension) {
        if (!level.isClientSide()) {
            return of(ResourceKey.create(Registries.DIMENSION, dimension));
        }
        return FILES.entries(level).getOrDefault(dimension, SkySpec.DEFAULT);
    }

    /* The sky each dimension made while the game runs takes from the dimension it copies, sent to the players. */
    private static Map<ResourceLocation, SkySpec> copiedSkies() {
        final Map<ResourceLocation, SkySpec> copied = new HashMap<>();
        RuntimeDimensions.templates().forEach((dimension, template) -> FILES.get(template)
                .ifPresent(sky -> copied.put(dimension, sky)));
        return copied;
    }
}
