/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.datagen;

import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.moving.BlockAboard;
import dev.jstech.core.moving.physics.BlockPhysics;
import dev.jstech.core.room.Seal;
import dev.jstech.core.room.Seals;
import dev.jstech.space.JsSpace;
import dev.jstech.space.api.RocketKit;
import dev.jstech.space.api.RocketKits;
import dev.jstech.space.api.RocketPart;
import dev.jstech.space.api.RocketParts;
import dev.jstech.space.rocket.RocketKitBlocks;
import dev.jstech.space.ship.ShipKitBlocks;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.DataMapProvider;

/**
 * What the rocket parts' blocks are in J's Core's rooms, physics and moving structures. The crew seat is a seat aboard.
 * The capsules' and the lander cabin's pressure shell holds a crew's air against the vacuum, its windows and hatch as
 * well as its hull, whatever a world's rules say of glass and doors; so does every block of a starship's kit, its
 * pressurised hull. Each block weighs its share of the parts it is built into: a part's mass empty spread over its
 * blocks, and a block used in several parts the mean of its shares, so a structure of them weighs what the rockets'
 * sums say.
 */
public final class RocketKitDataProvider extends DataMapProvider {

    /** What a pressure shell stands, in kilopascals: well beyond an atmosphere held against a vacuum. */
    public static final double SHELL_RATING = 300;
    /* How high over its block's bottom a crew seat's sitter sits. */
    private static final double CREW_SEAT_HEIGHT = 0.5;
    /* The blocks of the pressure shell round a crew. */
    private static final List<BlockEntry<Block>> SHELL = List.of(RocketKitBlocks.HEAT_SHIELD,
            RocketKitBlocks.CAPSULE_HULL, RocketKitBlocks.CAPSULE_WINDOW, RocketKitBlocks.CAPSULE_HATCH,
            RocketKitBlocks.FLIGHT_COMPUTER, RocketKitBlocks.DOCKING_PORT, RocketKitBlocks.CABIN_WALL,
            RocketKitBlocks.CABIN_WINDOW);

    public RocketKitDataProvider(final PackOutput output, final CompletableFuture<HolderLookup.Provider> lookup) {
        super(output, lookup);
    }

    @Override
    public String getName() {
        return JsSpace.MODID + ":rocket_kits";
    }

    /**
     * The mass of each block of the kits, in kilograms: for each part, its mass empty over its blocks; for a part
     * built in layers, a layer's mass over the blocks of each of its layers, its single layer and its foot among them;
     * a block in several parts, the mean over all the places it stands in them.
     */
    public static Map<ResourceLocation, Double> masses(final Map<ResourceLocation, RocketPart> parts,
                                                       final Map<ResourceLocation, RocketKit> kits) {
        final Map<ResourceLocation, double[]> summed = new LinkedHashMap<>();
        // In the kits' order by id, so the sums come out the same on every run.
        for (final Map.Entry<ResourceLocation, RocketKit> entry : new TreeMap<>(kits).entrySet()) {
            final RocketPart part = parts.get(entry.getKey());
            final RocketKit kit = entry.getValue();
            if (part == null) {
                continue;
            }
            if (kit.stretch().isEmpty()) {
                share(summed, kit, kit.layers(), part.mass());
                continue;
            }
            final List<List<List<String>>> each = new ArrayList<>();
            kit.layers().forEach(layer -> each.add(List.of(layer)));
            kit.alone().ifPresent(layer -> each.add(List.of(layer)));
            kit.foot().ifPresent(layer -> each.add(List.of(layer)));
            for (final List<List<String>> layer : each) {
                share(summed, kit, layer, part.mass());
            }
        }
        final Map<ResourceLocation, Double> masses = new LinkedHashMap<>();
        summed.forEach((block, sum) -> masses.put(block, sum[0] / sum[1]));
        return masses;
    }

    @Override
    protected void gather() {
        final Builder<Seal, Block> seals = builder(Seals.SEALS);
        for (final BlockEntry<Block> block : SHELL) {
            seals.add(block, Seal.wall(0.0, SHELL_RATING), false);
        }
        // A starship's kit is its pressurised hull, every block of it a wall that holds its crew's air.
        for (final BlockEntry<Block> block : ShipKitBlocks.all()) {
            seals.add(block, Seal.wall(0.0, SHELL_RATING), false);
        }
        // A crew seat is sat on upright, as in the lander's cabin; a capsule's model lays its crew down on it.
        builder(BlockAboard.ABOARD).add(RocketKitBlocks.CREW_SEAT, new BlockAboard(Optional.of(
                new BlockAboard.Seat(CREW_SEAT_HEIGHT, false)), Optional.empty(), Optional.empty(), Optional.empty()),
                false);
        final Builder<BlockPhysics, Block> physics = builder(BlockPhysics.PHYSICS);
        masses(RocketParts.declared(), RocketKits.declared()).forEach((id, mass) -> physics.add(
                BuiltInRegistries.BLOCK.wrapAsHolder(BuiltInRegistries.BLOCK.get(id)),
                new BlockPhysics(Optional.of(mass), Optional.empty(), Optional.empty(), Optional.empty(),
                        Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()),
                false));
    }

    /* Spreads {@code kilograms} over the blocks of {@code layers}, adding each block's share to its sums. */
    private static void share(final Map<ResourceLocation, double[]> summed, final RocketKit kit,
                              final List<List<String>> layers, final double kilograms) {
        final Map<ResourceLocation, Integer> counts = new LinkedHashMap<>();
        for (final List<String> layer : layers) {
            for (final String row : layer) {
                for (int x = 0; x < row.length(); x++) {
                    final ResourceLocation block = kit.key().get(String.valueOf(row.charAt(x)));
                    if (block != null) {
                        counts.merge(block, 1, Integer::sum);
                    }
                }
            }
        }
        final int blocks = counts.values().stream().mapToInt(Integer::intValue).sum();
        if (blocks == 0) {
            return;
        }
        counts.forEach((block, count) -> {
            final double[] sum = summed.computeIfAbsent(block, key -> new double[2]);
            sum[0] += kilograms / blocks * count;
            sum[1] += count;
        });
    }
}
