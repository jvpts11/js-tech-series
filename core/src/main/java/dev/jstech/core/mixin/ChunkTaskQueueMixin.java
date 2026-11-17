/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.mixin;

import net.minecraft.server.level.ChunkLevel;
import net.minecraft.server.level.ChunkTaskPriorityQueue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Serves the waiting tasks of a chunk no longer loaded first, not last. The game queues each chunk's tasks by how much
 * the chunk is wanted, nearest first, and a chunk let go goes to the back of the queue; as long as anything keeps
 * moving into new land, there is always nearer work, so the tasks of the land it left never run. Those tasks are
 * small, most of them seeing they are no longer wanted and giving up, but until they run, the generation waiting on
 * them holds every chunk round it: the chunks waiting to unload pile up without end, the land loaded with them, and
 * each tick is slower for going through them all. Served first, they run at once and let their land go, as they would
 * have the moment the queue was idle. A moving structure crossing new land fast (a ship over a flat world, in space)
 * keeps the queue from ever being idle.
 */
@Mixin(ChunkTaskPriorityQueue.class)
public abstract class ChunkTaskQueueMixin {

    /* The most wanted place in the queue, where the tasks of a chunk let go are served. */
    private static final int FIRST = 0;

    @ModifyVariable(method = "submit", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int jscore$serveLetGoFirst(final int chunkLevel) {
        return letGo(chunkLevel) ? FIRST : chunkLevel;
    }

    @ModifyVariable(method = "resortChunkTasks", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private int jscore$moveLetGoFirst(final int ticketLevel) {
        return letGo(ticketLevel) ? FIRST : ticketLevel;
    }

    /* Whether a chunk at {@code level} is no longer loaded. */
    private static boolean letGo(final int level) {
        return level > ChunkLevel.MAX_LEVEL;
    }
}
