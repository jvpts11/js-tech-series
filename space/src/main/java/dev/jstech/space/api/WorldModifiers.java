/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.api;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/**
 * The modifiers of worlds, by id, which a world lists in its data: the series' own (polar caps, a giant basin, lava
 * tubes, craters of eternal shadow and the others) and an add-on's. Each is registered once, while the game loads,
 * from the mod's constructor.
 */
public final class WorldModifiers {

    private static final Map<ResourceLocation, IWorldModifier> MODIFIERS = new LinkedHashMap<>();

    private WorldModifiers() {
    }

    /**
     * Registers {@code modifier} as {@code id}.
     *
     * @throws IllegalStateException when another modifier is registered as {@code id}
     */
    public static synchronized void register(final ResourceLocation id, final IWorldModifier modifier) {
        Objects.requireNonNull(modifier, "modifier");
        if (MODIFIERS.putIfAbsent(Objects.requireNonNull(id, "id"), modifier) != null) {
            throw new IllegalStateException("the world modifier " + id + " is registered twice");
        }
    }

    /** The modifier registered as {@code id}, if any. */
    public static synchronized Optional<IWorldModifier> get(final ResourceLocation id) {
        return Optional.ofNullable(MODIFIERS.get(id));
    }

    /** Every modifier, by id. */
    public static synchronized Map<ResourceLocation, IWorldModifier> all() {
        return Map.copyOf(MODIFIERS);
    }
}
