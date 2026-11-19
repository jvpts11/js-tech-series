/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.mixin;

import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.server.level.ChunkTaskPriorityQueue;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Gives the waiting tasks of chunks let go a turn of their own. The game serves each chunk's tasks by how much the
 * chunk is wanted, nearest first, and a chunk let go only once nothing else waits; as long as anything keeps moving
 * into new land there is always nearer work, so the tasks of the land left behind never ran. Those tasks are small,
 * most of them seeing they are no longer wanted and giving up, but until they run the generation waiting on them holds
 * every chunk round it: the chunks waiting to unload pile up without end, and each tick is slower for going through
 * them all. Every few turns of the queue goes to the land let go, while it has any, and the rest to the land wanted
 * as before, so neither waits on the other for good. A moving structure crossing new land fast (a ship over a flat
 * world, in space) keeps the queue from ever being idle.
 *
 * @param <T> what a task is
 */
@Mixin(ChunkTaskPriorityQueue.class)
public abstract class ChunkTaskQueueMixin<T> {

    @Shadow
    @Final
    private List<Long2ObjectLinkedOpenHashMap<List<Optional<T>>>> taskQueue;
    @Shadow
    private volatile int firstQueue;
    @Shadow
    @Final
    private LongSet acquired;
    @Shadow
    @Final
    private int maxTasks;
    /* The turns served since the land let go last had one. */
    @Unique
    private int jscore$turns;

    /* Where the queue keeps the tasks of chunks let go: its last place. */
    @Unique
    private static final int LET_GO = ChunkTaskPriorityQueue.PRIORITY_LEVEL_COUNT - 1;
    /* One turn in this many goes to the land let go. */
    @Unique
    private static final int TURNS = 4;

    @Shadow
    protected abstract Runnable acquire(long chunkPos);

    @Inject(method = "pop", at = @At("HEAD"), cancellable = true)
    private void jscore$serveLetGoInTurn(final CallbackInfoReturnable<Stream<Either<T, Runnable>>> info) {
        if (++this.jscore$turns < TURNS || this.acquired.size() >= this.maxTasks) {
            return;
        }
        final Long2ObjectLinkedOpenHashMap<List<Optional<T>>> letGo = this.taskQueue.get(LET_GO);
        if (letGo.isEmpty() || this.firstQueue == LET_GO) {
            // Nothing let go waits, or nothing else does: the game serves the queue as it is.
            return;
        }
        this.jscore$turns = 0;
        final long chunk = letGo.firstLongKey();
        final List<Optional<T>> tasks = letGo.removeFirst();
        info.setReturnValue(tasks.stream().map(task -> task.<Either<T, Runnable>>map(Either::left)
                .orElseGet(() -> Either.right(acquire(chunk)))));
    }
}
