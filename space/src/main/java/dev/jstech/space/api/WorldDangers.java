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
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * The dangers of every world, by the world's id ({@link SpaceWorlds}): those declared in code, by J's Space or an
 * add-on, and those a data pack's files say, {@code data/<namespace>/jsspace/dangers/}, a list of the world's
 * {@link WorldDanger}s, a file winning over a declaration of its id. Air, heat, cold and radiation are J's Core's
 * mechanics; this says which of them, and of the rest, each world holds.
 *
 * <pre>{@code
 * ["vacuum", "cold", "cosmic_radiation", "solar_storms", "dust", "gravity"]
 * }</pre>
 */
public final class WorldDangers {

    private static final DeclaredData<List<WorldDanger>> DANGERS = new DeclaredData<>(
            ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, "dangers"), "jsspace/dangers",
            WorldDanger.CODEC.listOf());

    private WorldDangers() {
    }

    /** Declares the dangers of the world {@code world}, while no data pack says otherwise, while the game loads. */
    public static List<WorldDanger> declare(final ResourceLocation world, final List<WorldDanger> dangers) {
        return DANGERS.declare(world, List.copyOf(dangers));
    }

    /** The dangers of the world {@code world} on the side {@code level} is on: none where none are said. */
    public static List<WorldDanger> of(final Level level, final ResourceLocation world) {
        return DANGERS.get(level, world).orElse(List.of());
    }

    /** Every world's dangers on the side {@code level} is on, by the world's id. */
    public static Map<ResourceLocation, List<WorldDanger>> all(final Level level) {
        return DANGERS.all(level);
    }

    /** Every world's dangers declared in code, by the world's id. */
    public static Map<ResourceLocation, List<WorldDanger>> declared() {
        return DANGERS.declared();
    }
}
