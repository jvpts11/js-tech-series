/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.heat;

import dev.jstech.core.heat.Blackbody;
import dev.jstech.core.heat.HeatBody;
import dev.jstech.core.heat.HeatClimate;
import dev.jstech.core.heat.IHeatHolder;
import dev.jstech.core.heat.ThermalScale;
import dev.jstech.core.heat.ThermalSight;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.OptionalDouble;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * What is hot or cold around the player, looked for every half second: the blocks that hold heat hot enough to glow,
 * near enough to be drawn glowing, and, while the player sees heat, every block seen apart from the air, a heat holder
 * or a liquid far from the air's temperature. Each list is made anew and swapped in whole, so the frames read a whole
 * one.
 */
final class HeatScan {

    /* How often it looks, in ticks; how far a glow is drawn and how far heat is seen, in chunks. */
    private static final int EVERY = 10;
    private static final int GLOW_CHUNKS = 4;
    private static final int SEEN_CHUNKS = 2;
    /* How far above and below the player liquids are looked for while heat is seen, in blocks. */
    private static final int SEEN_HEIGHT = 32;
    private static final int SECTION = 16;
    /* The most blocks seen at once: a sea of lava shows its nearest shore, not the whole of it. */
    private static final int MOST_SEEN = 8_192;
    /* The chunks around the player's, nearest first, so what is cut off by the most is what is farthest. */
    private static final int[][] NEAREST_FIRST = nearestFirst();

    /* What was found last, and the places of what is seen, to leave out the faces they share. */
    private static List<Found> glowing = List.of();
    private static List<Found> seen = List.of();
    private static Set<Long> seenPlaces = Set.of();
    private static Set<Long> glowingPlaces = Set.of();
    private static int ticks;

    private HeatScan() {
    }

    static void listen() {
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> tick());
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> clear());
    }

    /** The blocks hot enough to glow, as last looked for. */
    static List<Found> glowing() {
        return glowing;
    }

    /** Whether a block at {@code packed} glows, to leave out the face it shares with another. */
    static boolean glowsAt(final long packed) {
        return glowingPlaces.contains(packed);
    }

    /** The blocks seen apart from the air while heat is seen, as last looked for; none while it is not. */
    static List<Found> seen() {
        return seen;
    }

    /** Whether a block at {@code packed} is seen, to leave out the face it shares with another. */
    static boolean seenAt(final long packed) {
        return seenPlaces.contains(packed);
    }

    private static void tick() {
        if (++ticks % EVERY != 0) {
            return;
        }
        final Minecraft minecraft = Minecraft.getInstance();
        final ClientLevel level = minecraft.level;
        final LocalPlayer player = minecraft.player;
        if (level == null || player == null) {
            clear();
            return;
        }
        final boolean sees = ThermalSight.sees(player);
        final List<Found> glows = new ArrayList<>();
        final List<Found> sights = new ArrayList<>();
        final BlockPos at = player.blockPosition();
        final int cx = SectionPos.blockToSectionCoord(at.getX());
        final int cz = SectionPos.blockToSectionCoord(at.getZ());
        for (final int[] offset : NEAREST_FIRST) {
            final LevelChunk chunk = level.getChunkSource().getChunk(cx + offset[0], cz + offset[1],
                    ChunkStatus.FULL, false);
            if (chunk == null) {
                continue;
            }
            final boolean near = Math.abs(offset[0]) <= SEEN_CHUNKS && Math.abs(offset[1]) <= SEEN_CHUNKS;
            holders(level, chunk, sees && near, glows, sights);
            if (sees && near) {
                liquids(level, chunk, at.getY(), sights);
            }
        }
        glowing = List.copyOf(glows);
        glowingPlaces = places(glows);
        seen = List.copyOf(sights);
        seenPlaces = places(sights);
    }

    /* The chunk's blocks that hold heat: those that glow, and, when {@code seeing}, those seen apart from the air. */
    private static void holders(final ClientLevel level, final LevelChunk chunk, final boolean seeing,
                                final List<Found> glows, final List<Found> sights) {
        for (final BlockEntity entity : chunk.getBlockEntities().values()) {
            if (!(entity instanceof IHeatHolder holder)) {
                continue;
            }
            final HeatBody body = holder.heatBody();
            if (body == null || !body.known()) {
                continue;
            }
            final double kelvin = body.temperature();
            final BlockPos pos = entity.getBlockPos();
            if (Blackbody.glows(kelvin)) {
                glows.add(new Found(pos, kelvin));
            }
            if (seeing && ThermalScale.apart(kelvin, HeatClimate.ambient(level, pos))) {
                sights.add(new Found(pos, kelvin));
            }
        }
    }

    /* The chunk's liquids far from the air's temperature, near the player's height, section by section. */
    private static void liquids(final ClientLevel level, final LevelChunk chunk, final int height,
                                final List<Found> sights) {
        final LevelChunkSection[] sections = chunk.getSections();
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        final double air = HeatClimate.ambient(level, chunk.getPos().getMiddleBlockPosition(height));
        for (int i = 0; i < sections.length; i++) {
            final LevelChunkSection section = sections[i];
            final int bottom = SectionPos.sectionToBlockCoord(chunk.getMinSection() + i);
            if (section.hasOnlyAir() || bottom + SECTION < height - SEEN_HEIGHT || bottom > height + SEEN_HEIGHT
                    || !section.maybeHas(state -> farLiquid(state, air))) {
                continue;
            }
            for (int y = 0; y < SECTION; y++) {
                for (int z = 0; z < SECTION; z++) {
                    for (int x = 0; x < SECTION; x++) {
                        if (sights.size() >= MOST_SEEN || !farLiquid(section.getBlockState(x, y, z), air)) {
                            continue;
                        }
                        pos.set(chunk.getPos().getMinBlockX() + x, bottom + y, chunk.getPos().getMinBlockZ() + z);
                        if (enclosed(level, pos, air)) {
                            continue;
                        }
                        final OptionalDouble kelvin = ThermalSight.of(level, pos);
                        if (kelvin.isPresent()) {
                            sights.add(new Found(pos.immutable(), kelvin.getAsDouble()));
                        }
                    }
                }
            }
        }
    }

    /*
     * Whether a block holds a liquid whose type is far from the chunk's air, {@code air}: a quick look at the type,
     * the same for the whole section, before its own place is asked.
     */
    private static boolean farLiquid(final BlockState state, final double air) {
        return !state.getFluidState().isEmpty() && Math.abs(state.getFluidState().getFluidType().getTemperature()
                - air) >= ThermalSight.LIQUID_APART;
    }

    /* Whether the liquid at {@code pos} is wholly inside more of it, where none of it can be seen. */
    private static boolean enclosed(final ClientLevel level, final BlockPos pos, final double air) {
        for (final Direction way : Direction.values()) {
            if (!farLiquid(level.getBlockState(pos.relative(way)), air)) {
                return false;
            }
        }
        return true;
    }

    private static int[][] nearestFirst() {
        final List<int[]> offsets = new ArrayList<>();
        for (int x = -GLOW_CHUNKS; x <= GLOW_CHUNKS; x++) {
            for (int z = -GLOW_CHUNKS; z <= GLOW_CHUNKS; z++) {
                offsets.add(new int[] {x, z});
            }
        }
        offsets.sort(Comparator.comparingInt(offset -> offset[0] * offset[0] + offset[1] * offset[1]));
        return offsets.toArray(new int[0][]);
    }

    private static Set<Long> places(final List<Found> found) {
        final Set<Long> places = new HashSet<>();
        for (final Found each : found) {
            places.add(each.pos().asLong());
        }
        return places;
    }

    private static void clear() {
        glowing = List.of();
        seen = List.of();
        glowingPlaces = Set.of();
        seenPlaces = Set.of();
    }

    /**
     * A block found hot or cold.
     *
     * @param pos    where it is
     * @param kelvin how hot it is
     */
    record Found(BlockPos pos, double kelvin) {
    }
}
