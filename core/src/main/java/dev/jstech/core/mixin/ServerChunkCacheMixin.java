/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.mixin;

import dev.jstech.core.moving.physics.PhysicsLens;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Answers a thread working the physics of moving structures beside the server's thread from the chunks gathered for
 * it ({@link PhysicsLens}): off the server's thread the game would hand the question to the server's thread and wait,
 * and the server's thread waits on the physics. Every other question is answered as before, at the cost of one read.
 */
@Mixin(ServerChunkCache.class)
public abstract class ServerChunkCacheMixin {

    @Shadow
    @Final
    public ServerLevel level;

    @Inject(method = "getChunkNow", at = @At("HEAD"), cancellable = true)
    private void jscore$chunkNowThroughTheLens(final int x, final int z,
                                               final CallbackInfoReturnable<LevelChunk> info) {
        final PhysicsLens lens = PhysicsLens.on(this.level);
        if (lens != null) {
            info.setReturnValue(lens.chunkNow(x, z));
        }
    }

    @Inject(method = "getChunk(IILnet/minecraft/world/level/chunk/status/ChunkStatus;Z)"
            + "Lnet/minecraft/world/level/chunk/ChunkAccess;", at = @At("HEAD"), cancellable = true)
    private void jscore$chunkThroughTheLens(final int x, final int z, final ChunkStatus status, final boolean load,
                                            final CallbackInfoReturnable<ChunkAccess> info) {
        final PhysicsLens lens = PhysicsLens.on(this.level);
        if (lens != null) {
            info.setReturnValue(load ? lens.chunk(x, z) : lens.chunkNow(x, z));
        }
    }
}
