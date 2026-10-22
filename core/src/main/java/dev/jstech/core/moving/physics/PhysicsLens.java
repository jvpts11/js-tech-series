/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving.physics;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.EmptyLevelChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * The world as the physics worked beside the server's thread reads it: the chunks loaded round a group of structures,
 * gathered on the server's thread before the work begins and handed to the thread that works them. While a thread
 * holds a lens, the game's chunk source and its block entities answer that thread from it, so nothing it reads waits
 * on the server's thread, which waits on it; a chunk the lens has not is empty, as a chunk not loaded is to the
 * physics. What the work would change on the server's thread it asks to be done there once it is over
 * ({@link #onServerThread}). Public only for the mixins that answer from it.
 */
@ApiStatus.Internal
public final class PhysicsLens {

    private final ServerLevel level;
    private final Long2ObjectMap<LevelChunk> chunks = new Long2ObjectOpenHashMap<>();
    /* What the work asked of the server's thread, in the order it asked. */
    private final Deque<Runnable> later = new ArrayDeque<>();

    /* The lens the thread works through now, if any, and how many threads hold one: the game asks only when some do. */
    private static final ThreadLocal<PhysicsLens> HELD = new ThreadLocal<>();
    private static final AtomicInteger OPEN = new AtomicInteger();

    PhysicsLens(final ServerLevel level) {
        this.level = level;
    }

    /**
     * The lens this thread reads {@code level} through, or none: always none on the server's own thread, and found at
     * the cost of one read while no physics is worked beside it.
     */
    public static @Nullable PhysicsLens on(final Object level) {
        if (OPEN.get() == 0) {
            return null;
        }
        final PhysicsLens held = HELD.get();
        return held != null && held.level == level ? held : null;
    }

    /**
     * Runs {@code work} on the server's thread: at once when on it, or, from a thread working physics through a lens,
     * once that work is over, before the tick goes on.
     */
    static void onServerThread(final Runnable work) {
        final PhysicsLens held = HELD.get();
        if (held == null) {
            work.run();
        } else {
            held.later.add(work);
        }
    }

    /** The chunk at {@code x, z} as loaded when the lens was made, or none. */
    public @Nullable LevelChunk chunkNow(final int x, final int z) {
        return this.chunks.get(ChunkPos.asLong(x, z));
    }

    /** The chunk at {@code x, z}, or an empty one where the lens holds none. */
    public ChunkAccess chunk(final int x, final int z) {
        final LevelChunk held = chunkNow(x, z);
        return held != null ? held : new EmptyLevelChunk(this.level, new ChunkPos(x, z), this.level.registryAccess()
                .registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.PLAINS));
    }

    /**
     * The block entity at {@code pos} in a chunk the lens holds, as it stands, read without making or taking in any;
     * none elsewhere.
     */
    public @Nullable BlockEntity blockEntity(final BlockPos pos) {
        final LevelChunk held = chunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        return held == null ? null : held.getBlockEntities().get(pos);
    }

    /* Takes in the chunks from {@code x0, z0} to {@code x1, z1} that are loaded now; on the server's thread. */
    void take(final int x0, final int z0, final int x1, final int z1) {
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                final long key = ChunkPos.asLong(x, z);
                if (!this.chunks.containsKey(key)) {
                    final LevelChunk loaded = this.level.getChunkSource().getChunkNow(x, z);
                    if (loaded != null) {
                        this.chunks.put(key, loaded);
                    }
                }
            }
        }
    }

    /* This thread reads the world through the lens from now on. */
    void hold() {
        HELD.set(this);
        OPEN.incrementAndGet();
    }

    /* This thread reads the world as it always does again. */
    void release() {
        HELD.remove();
        OPEN.decrementAndGet();
    }

    /* Runs on the server's thread what the work asked of it, in order. */
    void runLater() {
        while (!this.later.isEmpty()) {
            this.later.poll().run();
        }
    }
}
