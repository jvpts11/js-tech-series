/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.dimension;

import dev.jstech.core.JsCore;
import dev.jstech.core.data.DataRegistry;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * Every dimension's rules, as the datapacks give them: read whenever the server loads its data, and sent to the
 * players, whose games draw the weather and work out the gravity the same way.
 */
public final class DimensionRulesData {

    private static final DataRegistry<DimensionRules> FILES = DataRegistry.builder(
                    ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "dimension_rules"), "dimension_rules",
                    DimensionRules.CODEC)
            .synced().alsoSending(DimensionRulesData::copiedRules)
            .onReload(rules -> DimensionEffects.rulesChanged()).register();

    private DimensionRulesData() {
    }

    /** Declares the registry, from the Core's constructor. */
    public static void register() {
        // Loading the class declares it.
    }

    /**
     * The rules of {@code dimension} on the server: its own, or for a dimension made while the game runs those of the
     * dimension it copies, or the overworld's when there are none.
     */
    public static DimensionRules of(final ResourceKey<Level> dimension) {
        return FILES.get(dimension.location())
                .or(() -> RuntimeDimensions.templateOf(dimension.location()).flatMap(FILES::get))
                .orElse(DimensionRules.OVERWORLD);
    }

    /** The rules of {@code level}: the server's on the server, those it sent on a player's game, copies included. */
    public static DimensionRules of(final Level level) {
        if (!level.isClientSide()) {
            return of(level.dimension());
        }
        return FILES.entries(level).getOrDefault(level.dimension().location(), DimensionRules.OVERWORLD);
    }

    /*
     * The rules each dimension made while the game runs takes from the dimension it copies, sent to the players
     * because only the server knows which dimension copies which; a copy with a file of its own is left to the file.
     */
    private static Map<ResourceLocation, DimensionRules> copiedRules() {
        final Map<ResourceLocation, DimensionRules> copied = new HashMap<>();
        RuntimeDimensions.templates().forEach((dimension, template) -> FILES.get(template)
                .ifPresent(rules -> copied.put(dimension, rules)));
        return copied;
    }
}
