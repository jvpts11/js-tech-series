/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

import dev.jstech.core.region.Box;
import dev.jstech.core.region.ChunkLoaders;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;

/**
 * A moving structure that keeps its place in the world loaded: with a chunk loader aboard, the Space Persistor or any
 * block that keeps chunks through the Core, the structure keeps the world's chunks under it loaded and ticking
 * wherever it goes, so it goes on working with no player near. Without one, a structure no player is near freezes
 * with the chunk it is over and is saved with it.
 */
final class StructureKeeping {

    private final LongSet kept = new LongOpenHashSet();
    private int nextLook;
    private boolean keeperAboard;

    /* Every how many ticks the structure looks again for a chunk loader among its blocks. */
    private static final int LOOK_EVERY = 20;

    /** One tick: looks for a loader aboard now and then, and keeps the chunks under the structure while one is. */
    void tick(final ServerLevel level, final Box bounds, final AABB world) {
        if (--nextLook <= 0) {
            nextLook = LOOK_EVERY;
            keeperAboard = loaderAboard(level, bounds);
        }
        if (!keeperAboard) {
            letGo(level);
            return;
        }
        final LongSet wanted = new LongOpenHashSet();
        for (int x = ((int) Math.floor(world.minX)) >> 4; x <= ((int) Math.floor(world.maxX)) >> 4; x++) {
            for (int z = ((int) Math.floor(world.minZ)) >> 4; z <= ((int) Math.floor(world.maxZ)) >> 4; z++) {
                wanted.add(ChunkPos.asLong(x, z));
            }
        }
        final LongIterator old = kept.iterator();
        while (old.hasNext()) {
            final long chunk = old.nextLong();
            if (!wanted.contains(chunk)) {
                StructureYard.releaseChunk(level, new ChunkPos(chunk));
                old.remove();
            }
        }
        final LongIterator fresh = wanted.iterator();
        while (fresh.hasNext()) {
            final long chunk = fresh.nextLong();
            if (kept.add(chunk)) {
                StructureYard.holdChunk(level, new ChunkPos(chunk));
            }
        }
    }

    /** Whether the structure keeps any of the world's chunks loaded now. */
    boolean keeping() {
        return !kept.isEmpty();
    }

    /** Lets go of every chunk of the world it keeps. */
    void letGo(final ServerLevel level) {
        if (kept.isEmpty()) {
            return;
        }
        final LongIterator each = kept.iterator();
        while (each.hasNext()) {
            StructureYard.releaseChunk(level, new ChunkPos(each.nextLong()));
        }
        kept.clear();
    }

    private static boolean loaderAboard(final ServerLevel level, final Box bounds) {
        for (final BlockPos source : ChunkLoaders.sources(level).keySet()) {
            if (bounds.contains(source.getX(), source.getY(), source.getZ())) {
                return true;
            }
        }
        return false;
    }
}
