/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.world.event;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/**
 * The world's settings for its events, as the server's settings file says: whether they happen at all, how often
 * each happens by chance, those that never happen by themselves, and those that leave the world as it is. An
 * operator's command starts any of them still.
 */
public final class WorldEventBalance {

    private static volatile boolean enabled = true;
    private static volatile Map<ResourceLocation, Double> frequency = Map.of();
    private static volatile Set<ResourceLocation> disabled = Set.of();
    private static volatile Set<ResourceLocation> harmless = Set.of();

    private WorldEventBalance() {
    }

    /** Whether world events happen by themselves. */
    public static boolean enabled() {
        return enabled;
    }

    public static void setEnabled(final boolean value) {
        enabled = value;
    }

    /** How many times as often the event {@code type} happens by chance; 1 unless the settings say otherwise. */
    public static double frequency(final ResourceLocation type) {
        return frequency.getOrDefault(type, 1.0);
    }

    public static void setFrequency(final Map<String, Double> byId) {
        final Map<ResourceLocation, Double> parsed = new HashMap<>();
        byId.forEach((id, factor) -> {
            final ResourceLocation key = ResourceLocation.tryParse(id);
            if (key != null) {
                parsed.put(key, factor);
            }
        });
        frequency = Map.copyOf(parsed);
    }

    /** Whether the event {@code type} never happens by itself. */
    public static boolean disabled(final ResourceLocation type) {
        return disabled.contains(type);
    }

    public static void setDisabled(final List<String> ids) {
        disabled = parse(ids);
    }

    /** Whether the event {@code type} leaves the world as it is, its effects and warnings still played. */
    public static boolean harmless(final ResourceLocation type) {
        return harmless.contains(type);
    }

    public static void setHarmless(final List<String> ids) {
        harmless = parse(ids);
    }

    private static Set<ResourceLocation> parse(final List<String> ids) {
        final Set<ResourceLocation> parsed = new HashSet<>();
        for (final String id : ids) {
            final ResourceLocation key = ResourceLocation.tryParse(id);
            if (key != null) {
                parsed.add(key);
            }
        }
        return Set.copyOf(parsed);
    }
}
