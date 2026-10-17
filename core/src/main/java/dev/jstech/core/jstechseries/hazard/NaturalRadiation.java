/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.hazard;

import dev.jstech.core.hazard.Radiation;
import dev.jstech.core.hazard.RadiationKind;
import dev.jstech.core.jstechseries.NaturalRadioactivity;
import dev.jstech.core.material.MaterialThing;
import dev.jstech.core.material.Materials;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * The radiation the rock gives off where a mineral with uranium or thorium in it lies, a little for each block of it:
 * enough for a Geiger counter to click over a vein, never to harm by itself. The blocks of each section of a chunk are
 * counted once and kept for a minute, or until a block there is placed or broken, so the count costs nothing between.
 */
public final class NaturalRadiation {

    /** What a block of such a mineral gives off, at a block from it, in millisieverts an hour. */
    public static final double PER_BLOCK = 0.002;
    /* How long a section's count is kept, in ticks, and how many counts a dimension keeps. */
    private static final long KEPT = 1_200;
    private static final int MOST_KEPT = 20_000;
    /* Whether each block is of a mineral with uranium or thorium in it, found once. */
    private static final Map<Block, Boolean> BEARS = new ConcurrentHashMap<>();
    /* Each dimension's counted sections, by the section's place packed. */
    private static final Map<ServerLevel, Map<Long, Counted>> COUNTED = new WeakHashMap<>();

    private NaturalRadiation() {
    }

    /** Hands the game's bus what forgets a section's count as a block there changes, once, from the Core. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(BlockEvent.BreakEvent.class, event -> forget(event.getLevel(),
                event.getPos()));
        NeoForge.EVENT_BUS.addListener(BlockEvent.EntityPlaceEvent.class, event -> forget(event.getLevel(),
                event.getPos()));
    }

    /** What the rock round {@code at} of {@code level} gives off there, through the blocks between. */
    public static double at(final ServerLevel level, final Vec3 at, final RadiationKind kind) {
        if (!NaturalRadioactivity.on()) {
            return 0;
        }
        final int sx = SectionPos.blockToSectionCoord(at.x);
        final int sy = SectionPos.blockToSectionCoord(at.y);
        final int sz = SectionPos.blockToSectionCoord(at.z);
        double rate = 0;
        for (int x = sx - 1; x <= sx + 1; x++) {
            for (int y = sy - 1; y <= sy + 1; y++) {
                for (int z = sz - 1; z <= sz + 1; z++) {
                    final Counted counted = counted(level, x, y, z);
                    if (counted.blocks() == 0) {
                        continue;
                    }
                    final double distance = counted.centre().distanceTo(at);
                    rate += counted.blocks() * PER_BLOCK / Math.max(1, distance * distance)
                            * Radiation.transmission(level, counted.centre(), at, kind);
                }
            }
        }
        return rate;
    }

    /** Whether {@code state} is a block of a mineral with uranium or thorium in it. */
    public static boolean bears(final BlockState state) {
        return BEARS.computeIfAbsent(state.getBlock(), block -> Materials.of(block.asItem())
                .map(MaterialThing::material).map(NaturalRadioactivity::bearsUraniumOrThorium).orElse(false));
    }

    /**
     * Forgets the count of the section {@code pos} is in, to be counted again: for a mod that sets blocks without the
     * game's events, as a structure placed whole.
     */
    public static void forget(final LevelAccessor level, final BlockPos pos) {
        if (level instanceof ServerLevel server) {
            synchronized (COUNTED) {
                final Map<Long, Counted> sections = COUNTED.get(server);
                if (sections != null) {
                    sections.remove(SectionPos.asLong(pos));
                }
            }
        }
    }

    private static Counted counted(final ServerLevel level, final int x, final int y, final int z) {
        final long key = SectionPos.asLong(x, y, z);
        final Map<Long, Counted> sections;
        synchronized (COUNTED) {
            sections = COUNTED.computeIfAbsent(level, found -> new ConcurrentHashMap<>());
        }
        final Counted kept = sections.get(key);
        final long now = level.getGameTime();
        if (kept != null && now - kept.time() < KEPT) {
            return kept;
        }
        final Counted counted = count(level, x, y, z, now);
        // Counts outlive the places walked away from; past so many, they all go and are counted again as needed.
        if (sections.size() > MOST_KEPT) {
            sections.clear();
        }
        sections.put(key, counted);
        return counted;
    }

    /* Counts the blocks of such minerals in the section, and where their middle is; none in a chunk not loaded. */
    private static Counted count(final ServerLevel level, final int x, final int y, final int z, final long now) {
        final LevelChunk chunk = level.getChunkSource().getChunkNow(x, z);
        if (chunk == null || y < level.getMinSection() || y >= level.getMaxSection()) {
            return new Counted(0, Vec3.ZERO, now);
        }
        final LevelChunkSection section = chunk.getSection(chunk.getSectionIndexFromSectionY(y));
        if (section.hasOnlyAir() || !section.maybeHas(NaturalRadiation::bears)) {
            return new Counted(0, Vec3.ZERO, now);
        }
        int blocks = 0;
        double cx = 0;
        double cy = 0;
        double cz = 0;
        for (int by = 0; by < 16; by++) {
            for (int bz = 0; bz < 16; bz++) {
                for (int bx = 0; bx < 16; bx++) {
                    if (bears(section.getBlockState(bx, by, bz))) {
                        blocks++;
                        cx += bx;
                        cy += by;
                        cz += bz;
                    }
                }
            }
        }
        if (blocks == 0) {
            return new Counted(0, Vec3.ZERO, now);
        }
        final BlockPos origin = SectionPos.of(x, y, z).origin();
        return new Counted(blocks, new Vec3(origin.getX() + cx / blocks + 0.5, origin.getY() + cy / blocks + 0.5,
                origin.getZ() + cz / blocks + 0.5), now);
    }

    /* A section's count: how many blocks, where their middle is, and when it was counted. */
    private record Counted(int blocks, Vec3 centre, long time) {
    }
}
