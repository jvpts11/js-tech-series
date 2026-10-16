/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.vein;

import dev.jstech.core.material.HostRock;
import dev.jstech.core.material.HostRocks;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.Materials;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

/**
 * Which rock a block is, and which block is a material's ore in a rock: the game's own where it has one (iron in
 * stone), the one an installed mod uses, or none, so a vein lays only the ores some mod uses. Looked up while the
 * world generates, on any thread; worked out once each.
 */
public final class OreTable {

    private static final Map<Pair, Optional<Block>> ORES = new ConcurrentHashMap<>();
    private static volatile Map<Block, HostRock> rocks;

    private OreTable() {
    }

    /** The rock ores sit in that {@code block} is, or none. */
    public static @Nullable HostRock rockOf(final Block block) {
        Map<Block, HostRock> known = rocks;
        if (known == null) {
            final Map<Block, HostRock> made = new HashMap<>();
            for (final HostRock rock : HostRocks.all()) {
                made.putIfAbsent(rock.rock(), rock);
            }
            known = Map.copyOf(made);
            rocks = known;
        }
        return known.get(block);
    }

    /** {@code material}'s ore in {@code rock}, or none when no installed mod uses it there. */
    public static @Nullable Block oreOf(final ResourceLocation material, final HostRock rock) {
        return ORES.computeIfAbsent(new Pair(material, rock), OreTable::find).orElse(null);
    }

    private static Optional<Block> find(final Pair pair) {
        final Optional<Material> material = Materials.byId(pair.material());
        if (material.isEmpty()) {
            return Optional.empty();
        }
        final Optional<Supplier<? extends Block>> provided = material.get().providedOre(pair.rock());
        if (provided.isPresent()) {
            return Optional.of(provided.get().get());
        }
        return BuiltInRegistries.BLOCK.getOptional(pair.rock().oreId(material.get()));
    }

    private record Pair(ResourceLocation material, HostRock rock) {
    }
}
