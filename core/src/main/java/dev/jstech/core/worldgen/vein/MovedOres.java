/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.vein;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;

/**
 * Other mods' scattered ores, moved into veins of their own when the server tries: an ore laid as data, whose feature
 * places blocks of the common ores tag, gets a vein that replaces what its feature replaces with what its feature
 * places, so each block still takes the variant of its rock. Noted as the server's biomes are made, forgotten when it
 * stops.
 */
public final class MovedOres {

    /* A moved ore's vein: as common as a rare ore, an ordinary vein's size, from deep under the ground to the hills. */
    private static final DepositType MOVED = new DepositType(List.of(), -64, 128, 0.04, 14, 5, 0.45, Optional.empty(),
            0, List.of());
    private static final Map<ResourceLocation, Deposits.Entry> VEINS = new ConcurrentHashMap<>();
    private static final AtomicLong VERSION = new AtomicLong();

    private MovedOres() {
    }

    /** Whether {@code ore} places any block of {@code tagged}. */
    public static boolean places(final OreConfiguration ore, final TagKey<Block> tagged) {
        return ore.targetStates.stream().anyMatch(target -> target.state.is(tagged));
    }

    /** Notes the ore of the feature {@code feature} as moved into veins of its own. */
    public static void note(final ResourceLocation feature, final OreConfiguration ore) {
        if (VEINS.putIfAbsent(feature, new Deposits.Entry(feature.withPrefix("moved/"), MOVED, ore.targetStates))
                == null) {
            VERSION.incrementAndGet();
        }
    }

    /** The veins of the moved ores. */
    public static List<Deposits.Entry> deposits() {
        return List.copyOf(VEINS.values());
    }

    /** How many times the moved ores have changed. */
    public static long version() {
        return VERSION.get();
    }

    /** Forgets every moved ore, as the server stops and its biomes go. */
    public static void forget() {
        VEINS.clear();
        VERSION.incrementAndGet();
    }
}
