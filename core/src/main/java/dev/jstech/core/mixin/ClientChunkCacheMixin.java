/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.mixin;

import dev.jstech.core.moving.YardLayout;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.function.Consumer;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ChunkEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Keeps the chunks of the yard a player's game is sent, where the moving structures it sees keep their blocks: the
 * game holds only the chunks round the player and throws away any other it is sent, so a structure's blocks, and
 * its block entities, would never reach the player's world. The yard's chunks are held apart, found as any chunk is,
 * and dropped when the server says; every other chunk is held as before.
 */
@Mixin(ClientChunkCache.class)
public abstract class ClientChunkCacheMixin {

    @Shadow
    @Final
    ClientLevel level;

    @Unique
    private final Long2ObjectMap<LevelChunk> jscore$yard = new Long2ObjectOpenHashMap<>();

    @Inject(method = "getChunk(IILnet/minecraft/world/level/chunk/status/ChunkStatus;Z)"
            + "Lnet/minecraft/world/level/chunk/LevelChunk;", at = @At("HEAD"), cancellable = true)
    private void jscore$findYardChunks(final int x, final int z, final ChunkStatus status, final boolean require,
                                       final CallbackInfoReturnable<LevelChunk> info) {
        if (YardLayout.isYardChunk(x)) {
            final LevelChunk held = jscore$yard.get(ChunkPos.asLong(x, z));
            if (held != null || !require) {
                info.setReturnValue(held);
            }
        }
    }

    @Inject(method = "replaceWithPacketData", at = @At("HEAD"), cancellable = true)
    private void jscore$holdYardChunks(final int x, final int z, final FriendlyByteBuf buffer, final CompoundTag tag,
                                       final Consumer<ClientboundLevelChunkPacketData.BlockEntityTagOutput> output,
                                       final CallbackInfoReturnable<LevelChunk> info) {
        if (!YardLayout.isYardChunk(x)) {
            return;
        }
        final long key = ChunkPos.asLong(x, z);
        LevelChunk chunk = jscore$yard.get(key);
        if (chunk == null) {
            chunk = new LevelChunk(level, new ChunkPos(x, z));
            chunk.replaceWithPacketData(buffer, tag, output);
            jscore$yard.put(key, chunk);
        } else {
            chunk.replaceWithPacketData(buffer, tag, output);
        }
        level.onChunkLoaded(new ChunkPos(x, z));
        NeoForge.EVENT_BUS.post(new ChunkEvent.Load(chunk, false));
        info.setReturnValue(chunk);
    }

    @Inject(method = "replaceBiomes", at = @At("HEAD"), cancellable = true)
    private void jscore$yardBiomes(final int x, final int z, final FriendlyByteBuf buffer, final CallbackInfo info) {
        if (YardLayout.isYardChunk(x)) {
            final LevelChunk chunk = jscore$yard.get(ChunkPos.asLong(x, z));
            if (chunk != null) {
                chunk.replaceBiomes(buffer);
            }
            info.cancel();
        }
    }

    @Inject(method = "drop", at = @At("HEAD"), cancellable = true)
    private void jscore$dropYardChunks(final ChunkPos pos, final CallbackInfo info) {
        if (YardLayout.isYardChunk(pos.x)) {
            final LevelChunk chunk = jscore$yard.remove(pos.toLong());
            if (chunk != null) {
                NeoForge.EVENT_BUS.post(new ChunkEvent.Unload(chunk));
                level.unload(chunk);
            }
            info.cancel();
        }
    }
}
