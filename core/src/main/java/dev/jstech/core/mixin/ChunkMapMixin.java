/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.mixin;

import dev.jstech.core.moving.StructureSync;
import dev.jstech.core.moving.YardLayout;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets the players who see a moving structure follow the chunks of the yard its blocks are in, far as they are from
 * where the players stand: the game sends a chunk's changed blocks, its block entities and its light to the players
 * whose view holds it, and a structure's blocks are out in the yard; so a player seeing the structure counts as
 * seeing its chunks, and a chunk of its plot that finishes loading is sent to them. The game sends a chunk's changed
 * light only to the players at whose view's edge it is, as the others' games work it out from the blocks they are told
 * changed; a structure's blocks come into the yard as they may never be told, and light the server had not worked out
 * when a chunk was sent would stay wrong on their game for good, so the yard's chunks count as at the edge of the view
 * of those who see them. Every other chunk passes as before.
 */
@Mixin(ChunkMap.class)
public abstract class ChunkMapMixin {

    @Shadow
    @Final
    ServerLevel level;

    @Inject(method = "isChunkTracked", at = @At("HEAD"), cancellable = true)
    private void jscore$followYardChunks(final ServerPlayer player, final int x, final int z,
                                         final CallbackInfoReturnable<Boolean> info) {
        if (YardLayout.isYardChunk(x)) {
            info.setReturnValue(jscore$follows(player, x, z));
        }
    }

    @Inject(method = "isChunkOnTrackedBorder", at = @At("HEAD"), cancellable = true)
    private void jscore$sendYardLight(final ServerPlayer player, final int x, final int z,
                                      final CallbackInfoReturnable<Boolean> info) {
        if (YardLayout.isYardChunk(x)) {
            info.setReturnValue(jscore$follows(player, x, z));
        }
    }

    @Inject(method = "onChunkReadyToSend", at = @At("TAIL"))
    private void jscore$sendReadyYardChunks(final LevelChunk chunk, final CallbackInfo info) {
        if (YardLayout.isYardChunk(chunk.getPos().x)) {
            StructureSync.chunkReady(level, chunk);
        }
    }

    /* Whether {@code player} sees the structure of the yard's chunk at {@code (x, z)} and has the chunk already. */
    private static boolean jscore$follows(final ServerPlayer player, final int x, final int z) {
        return StructureSync.watches(player, x, z) && player.connection != null
                && !player.connection.chunkSender.isPending(ChunkPos.asLong(x, z));
    }
}
