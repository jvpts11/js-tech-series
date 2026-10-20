/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Reads the world only where it is loaded.
 *
 * <p>On the server, a block entity asked for at a place whose chunk is not loaded makes the game load that chunk there
 * and then, on the server's thread: a disk read or a generation in the middle of a tick. Worse, a chunk read that way
 * while it is being unloaded takes a hold the unload waits on, and the unload, rescheduling itself in the very queue
 * the server is emptying, then eats every tick's spare time, which is the time the chunk's own loading needed to
 * finish and let go of that hold: the server keeps ticking but no chunk ever loads again.
 *
 * <p>So whatever is remembered by position and may lie anywhere (a server of the network, a linked peripheral, the
 * rack of a cluster node) is read through here, and a place outside the loaded world answers as empty.
 *
 * <p>Loaded here means loaded now. The game's own {@code Level.isLoaded} says only that the chunk is wanted loaded:
 * a chunk on its way still answers yes, and reading it then waits on the server's thread for it to finish.
 */
public final class Loaded {

    private Loaded() {
    }

    /** The block entity at {@code pos}, or null when there is none or its chunk is not loaded now. */
    @Nullable
    public static BlockEntity blockEntity(final Level level, final BlockPos pos) {
        return at(level, pos) ? level.getBlockEntity(pos) : null;
    }

    /**
     * Whether the chunk holding {@code pos} is loaded now, so it can be read at once; on the server, off its thread,
     * never.
     */
    public static boolean at(final Level level, final BlockPos pos) {
        return !level.isOutsideBuildHeight(pos) && chunk(level, pos.getX() >> 4, pos.getZ() >> 4);
    }

    /** Whether the chunk {@code (chunkX, chunkZ)} is loaded now; on the server, off its thread, never. */
    public static boolean chunk(final Level level, final int chunkX, final int chunkZ) {
        if (level instanceof ServerLevel server) {
            return server.getChunkSource().getChunkNow(chunkX, chunkZ) != null;
        }
        return level.hasChunk(chunkX, chunkZ);
    }

    /** Whether every chunk holding a place from {@code (fromX, fromZ)} to {@code (toX, toZ)} is loaded now. */
    public static boolean area(final Level level, final int fromX, final int fromZ, final int toX, final int toZ) {
        for (int x = fromX >> 4; x <= toX >> 4; x++) {
            for (int z = fromZ >> 4; z <= toZ >> 4; z++) {
                if (!chunk(level, x, z)) {
                    return false;
                }
            }
        }
        return true;
    }
}
