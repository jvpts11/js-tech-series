/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.mixin;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Drops the unloading of a chunk that was wanted again before it ran. The game queues a chunk's unloading when nothing
 * wants it, to run when the server has time; if the chunk is wanted again first and then lent to the generation of a
 * chunk beside it, the queued unloading finds it busy and queues itself again at once, round and round, as long as
 * the server has time: it takes all of that time, the generation it waits for never gets any, and the server spins
 * there for good, every tick, and at its stop. A chunk the server keeps no longer waits to unload, so the unloading has
 * nothing to do; when nothing wants the chunk again, its unloading is queued anew. Anything that wants chunks a while
 * and lets them go makes this likelier: a moving structure's plot, a chunk loader, a player passing by.
 */
@Mixin(ChunkMap.class)
public abstract class ChunkUnloadMixin {

    @Shadow
    @Final
    private Long2ObjectLinkedOpenHashMap<ChunkHolder> pendingUnloads;

    @Inject(method = "scheduleUnload", at = @At("HEAD"), cancellable = true)
    private void jscore$dropUnloadOfChunkKept(final long chunkPos, final ChunkHolder holder, final CallbackInfo info) {
        if (pendingUnloads.get(chunkPos) != holder) {
            info.cancel();
        }
    }
}
