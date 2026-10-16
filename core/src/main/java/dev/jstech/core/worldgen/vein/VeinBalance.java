/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.vein;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/**
 * How the server's world lays its ores, as its balance file says: in veins or scattered as the game lays them; how
 * often, how large and how dense each kind of vein is, by its id; the materials whose ore is never laid; and whether
 * other mods' scattered ores are moved into veins too. Read while the world generates, on any thread.
 */
public final class VeinBalance {

    private static volatile boolean enabled = true;
    private static volatile boolean moveOthers;
    private static volatile Map<ResourceLocation, Double> rarity = Map.of();
    private static volatile Map<ResourceLocation, Double> spread = Map.of();
    private static volatile Map<ResourceLocation, Double> density = Map.of();
    private static volatile Set<ResourceLocation> withoutOre = Set.of();

    private VeinBalance() {
    }

    /** Whether ores lie in veins; when not, the game's scattered ores come back and no vein is laid. */
    public static boolean enabled() {
        return enabled;
    }

    public static void setEnabled(final boolean value) {
        enabled = value;
    }

    /** Whether other mods' scattered ores are moved into veins of their own. */
    public static boolean moveOthers() {
        return moveOthers;
    }

    public static void setMoveOthers(final boolean value) {
        moveOthers = value;
    }

    /** How many times as often veins of {@code deposit} are found. */
    public static double rarity(final ResourceLocation deposit) {
        return rarity.getOrDefault(deposit, 1.0);
    }

    public static void setRarity(final Map<String, Double> byId) {
        rarity = parse(byId);
    }

    /** How many times as large veins of {@code deposit} are. */
    public static double spread(final ResourceLocation deposit) {
        return spread.getOrDefault(deposit, 1.0);
    }

    public static void setSpread(final Map<String, Double> byId) {
        spread = parse(byId);
    }

    /** How many times as dense veins of {@code deposit} are, never past wholly ore. */
    public static double density(final ResourceLocation deposit) {
        return density.getOrDefault(deposit, 1.0);
    }

    public static void setDensity(final Map<String, Double> byId) {
        density = parse(byId);
    }

    /** Whether {@code material}'s ore is never laid. */
    public static boolean withoutOre(final ResourceLocation material) {
        return withoutOre.contains(material);
    }

    public static void setWithoutOre(final List<String> materials) {
        final Set<ResourceLocation> out = new HashSet<>();
        for (final String id : materials) {
            final ResourceLocation parsed = ResourceLocation.tryParse(id);
            if (parsed != null) {
                out.add(parsed);
            }
        }
        withoutOre = Set.copyOf(out);
    }

    /* The settings' ids as the game's: a written id that reads as none is passed over. */
    private static Map<ResourceLocation, Double> parse(final Map<String, Double> byId) {
        final Map<ResourceLocation, Double> out = new HashMap<>();
        byId.forEach((id, value) -> {
            final ResourceLocation parsed = ResourceLocation.tryParse(id);
            if (parsed != null && value != null && value >= 0) {
                out.put(parsed, value);
            }
        });
        return Map.copyOf(out);
    }
}
