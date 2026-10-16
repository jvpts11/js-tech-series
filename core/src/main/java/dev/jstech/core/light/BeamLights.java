/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.light;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;

/**
 * The light spotlights and beams lay on the world past the game's fifteen levels: each light holds the places it
 * lights, at the level it brings each, and light stands there ({@link LightBlock}) at the brightest any light brings,
 * real light the game's own rules count, so mobs do not spawn where a floodlight shines and crops grow by it. A place
 * no light holds any more goes dark; one holding something else is passed by.
 */
public final class BeamLights {

    /* For each level, for each place, what each light by its owner's key brings it. */
    private static final Map<ResourceKey<Level>, Map<Long, Map<Long, Lit>>> HELD = new ConcurrentHashMap<>();
    /* For each level, the places each light holds. */
    private static final Map<ResourceKey<Level>, Map<Long, Set<Long>>> BY_OWNER = new ConcurrentHashMap<>();

    private BeamLights() {
    }

    /** Hands the game's bus what this answers, once, from the Core's constructor. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(LevelEvent.Unload.class, event -> {
            if (event.getLevel() instanceof ServerLevel level) {
                HELD.remove(level.dimension());
                BY_OWNER.remove(level.dimension());
            }
        });
    }

    /**
     * Lights {@code places} for the light known by {@code owner} (a lamp's place, packed), in place of what it lit
     * before: each place at the level and in the colour given.
     */
    public static synchronized void light(final ServerLevel level, final long owner, final Map<BlockPos, Lit> places) {
        final Map<Long, Map<Long, Lit>> held = HELD.computeIfAbsent(level.dimension(), key -> new HashMap<>());
        final Map<Long, Set<Long>> owners = BY_OWNER.computeIfAbsent(level.dimension(), key -> new HashMap<>());
        final Set<Long> before = owners.getOrDefault(owner, Set.of());
        final Set<Long> now = new HashSet<>();
        for (final Map.Entry<BlockPos, Lit> place : places.entrySet()) {
            final long at = place.getKey().asLong();
            now.add(at);
            held.computeIfAbsent(at, key -> new HashMap<>()).put(owner, place.getValue());
        }
        for (final long gone : before) {
            if (!now.contains(gone)) {
                final Map<Long, Lit> lights = held.get(gone);
                if (lights != null) {
                    lights.remove(owner);
                    if (lights.isEmpty()) {
                        held.remove(gone);
                    }
                }
            }
        }
        owners.put(owner, now);
        final Set<Long> changed = new HashSet<>(before);
        changed.addAll(now);
        for (final long at : changed) {
            settle(level, BlockPos.of(at), held.get(at));
        }
    }

    /** Takes away all the light {@code owner} laid. */
    public static synchronized void release(final ServerLevel level, final long owner) {
        light(level, owner, Map.of());
        final Map<Long, Set<Long>> owners = BY_OWNER.get(level.dimension());
        if (owners != null) {
            owners.remove(owner);
        }
    }

    /** Whether any light holds the place {@code pos}. */
    public static synchronized boolean holds(final ServerLevel level, final BlockPos pos) {
        final Map<Long, Map<Long, Lit>> held = HELD.get(level.dimension());
        return held != null && held.containsKey(pos.asLong());
    }

    /** The brightest light held at {@code pos}, with its colour, if any is. */
    public static synchronized Optional<Lit> at(final ServerLevel level, final BlockPos pos) {
        final Map<Long, Map<Long, Lit>> held = HELD.get(level.dimension());
        final Map<Long, Lit> lights = held == null ? null : held.get(pos.asLong());
        if (lights == null || lights.isEmpty()) {
            return Optional.empty();
        }
        return lights.values().stream().max((a, b) -> Integer.compare(a.level(), b.level()));
    }

    /* Light stands at the place at the brightest any light brings, or goes; a place holding a block is passed by. */
    private static void settle(final ServerLevel level, final BlockPos pos, final Map<Long, Lit> lights) {
        final BlockState there = level.getBlockState(pos);
        int brightest = 0;
        if (lights != null) {
            for (final Lit lit : lights.values()) {
                brightest = Math.max(brightest, lit.level());
            }
        }
        if (brightest <= 0) {
            if (there.getBlock() instanceof LightBlock) {
                level.setBlock(pos, LightBlock.under(there), Block.UPDATE_ALL);
            }
            return;
        }
        if (!LightBlock.fits(there)) {
            return;
        }
        final boolean wet = there.is(Blocks.WATER) || there.getBlock() instanceof LightBlock
                && there.getValue(LightBlock.WATERLOGGED);
        final BlockState lit = CoreLights.LIGHT.get().at(brightest, wet);
        if (there != lit) {
            level.setBlock(pos, lit, Block.UPDATE_ALL);
        }
    }

    /**
     * What a light brings a place.
     *
     * @param level  its level there, from 1 to 15
     * @param colour its colour, {@code 0xRRGGBB}
     */
    public record Lit(int level, int colour) {
    }
}
