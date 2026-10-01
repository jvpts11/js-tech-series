/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.network;

import dev.jstech.core.JsCore;
import dev.jstech.core.connect.Connection;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.resources.ResourceLocation;

/**
 * The data cables as lines a device's ports take: one line for each tier of data cable, in its first generation,
 * named {@code jscore:data/<tier>}.
 */
public final class DataLines {

    private static final Map<DataTier, Connection> BY_TIER = new EnumMap<>(DataTier.class);
    /** Every data line, so a block that takes them all says so in one word. */
    private static final Set<ResourceLocation> ALL;

    static {
        for (final DataTier tier : DataTier.values()) {
            BY_TIER.put(tier, Connection.of(ResourceLocation.fromNamespaceAndPath(JsCore.MODID,
                    "data/" + tier.serializedName())));
        }
        ALL = BY_TIER.values().stream().map(Connection::line).collect(Collectors.toUnmodifiableSet());
    }

    private DataLines() {
    }

    /** What a data cable of {@code tier} offers the face it touches. */
    public static Connection of(final DataTier tier) {
        return BY_TIER.get(tier);
    }

    /** What a data cable of each of {@code tiers} offers, for a port that takes them all. */
    public static Connection[] of(final DataTier... tiers) {
        final Connection[] out = new Connection[tiers.length];
        for (int i = 0; i < tiers.length; i++) {
            out[i] = of(tiers[i]);
        }
        return out;
    }

    /** Every tier's line, for a port that takes any data cable. */
    public static Connection[] every() {
        return of(DataTier.values());
    }

    /** Whether {@code line} is a data line. */
    public static boolean isData(final ResourceLocation line) {
        return ALL.contains(line);
    }
}
