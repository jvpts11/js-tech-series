/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.progression;

import dev.jstech.core.util.DeclarationError;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Every axis a player advances along, by id, in the order registered: each registered once, from the constructor of
 * the mod that counts by it, while the game loads.
 */
public final class ProgressionAxes {

    private static final Map<ResourceLocation, ProgressionAxis<?>> BY_ID =
            Collections.synchronizedMap(new LinkedHashMap<>());

    private ProgressionAxes() {
    }

    /**
     * Registers {@code axis}, from the constructor of the mod that adds it.
     *
     * @throws IllegalStateException when an axis of the same id is registered already
     */
    public static void register(final ProgressionAxis<?> axis) {
        if (BY_ID.putIfAbsent(axis.id(), axis) != null) {
            throw DeclarationError.of(axis.id().getNamespace(), "the progression axis " + axis.id(),
                    "is registered twice", "register it once, from the constructor of the mod that adds it");
        }
    }

    /** The axis registered as {@code id}, or null when there is none. */
    public static @Nullable ProgressionAxis<?> byId(final ResourceLocation id) {
        return BY_ID.get(id);
    }

    /** Every axis registered, in the order registered. */
    public static Collection<ProgressionAxis<?>> all() {
        synchronized (BY_ID) {
            return List.copyOf(BY_ID.values());
        }
    }
}
