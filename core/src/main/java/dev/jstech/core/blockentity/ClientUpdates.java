/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Sends the players the fields that changed, once per block entity at the end of each server tick, however many
 * changed during it, and checks the worked-out values the players see for a change first. The work is in proportion
 * to what changed and to the block entities that show worked-out values, never to every block entity loaded.
 */
public final class ClientUpdates {

    private static final Map<ServerLevel, Pending> PENDING = new IdentityHashMap<>();

    private ClientUpdates() {
    }

    /** Hands the game's bus what this answers, once, from the mod's constructor. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(LevelTickEvent.Post.class, ClientUpdates::afterTick);
        NeoForge.EVENT_BUS.addListener(LevelEvent.Unload.class, ClientUpdates::unload);
    }

    public static void afterTick(final LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            final Pending pending = PENDING.get(level);
            if (pending != null) {
                pending.flush(level);
            }
        }
    }

    public static void unload(final LevelEvent.Unload event) {
        PENDING.remove(event.getLevel());
    }

    /**
     * Sends the players watching {@code pos} the block there and its block entity at once, rather than at the end of
     * the tick: for what a player's own hand just did, so the change shows the moment it is made, as a block the player
     * places does. The end of the tick still sends what it would have, so nothing is left out; it costs a packet more,
     * which is why the batched sending stays the rule for everything else.
     */
    public static void sendNow(final ServerLevel level, final BlockPos pos) {
        final List<ServerPlayer> watching = level.getChunkSource().chunkMap.getPlayers(new ChunkPos(pos), false);
        if (watching.isEmpty()) {
            return;
        }
        final ClientboundBlockUpdatePacket block = new ClientboundBlockUpdatePacket(level, pos);
        final BlockEntity entity = level.getBlockEntity(pos);
        final Packet<ClientGamePacketListener> data = entity == null ? null : entity.getUpdatePacket();
        for (final ServerPlayer player : watching) {
            player.connection.send(block);
            if (data != null) {
                player.connection.send(data);
            }
        }
    }

    static void dirty(final ServerLevel level, final SyncedBlockEntity blockEntity) {
        PENDING.computeIfAbsent(level, created -> new Pending()).dirty.add(blockEntity);
    }

    static void watch(final ServerLevel level, final SyncedBlockEntity blockEntity) {
        PENDING.computeIfAbsent(level, created -> new Pending()).watched.add(blockEntity);
    }

    static void forget(final ServerLevel level, final SyncedBlockEntity blockEntity) {
        final Pending pending = PENDING.get(level);
        if (pending != null) {
            pending.watched.remove(blockEntity);
            pending.dirty.remove(blockEntity);
        }
    }

    /** What one level has to send at the end of its tick, and what it has to check. */
    private static final class Pending {

        private final Set<SyncedBlockEntity> dirty = new LinkedHashSet<>();
        private final Set<SyncedBlockEntity> watched = new LinkedHashSet<>();

        void flush(final ServerLevel level) {
            final Iterator<SyncedBlockEntity> polled = watched.iterator();
            while (polled.hasNext()) {
                final SyncedBlockEntity blockEntity = polled.next();
                // One whose chunk has gone is not ticked either; it is watched again when the chunk comes back.
                if (blockEntity.isRemoved() || !level.isLoaded(blockEntity.getBlockPos())) {
                    polled.remove();
                    continue;
                }
                blockEntity.fields().poll();
            }
            if (dirty.isEmpty()) {
                return;
            }
            final SyncedBlockEntity[] toSend = dirty.toArray(SyncedBlockEntity[]::new);
            dirty.clear();
            for (final SyncedBlockEntity blockEntity : toSend) {
                final BlockPos pos = blockEntity.getBlockPos();
                /*
                 * A block entity broken this tick, replaced by another, or whose chunk has gone has nothing left to
                 * send; asked only while its chunk is loaded, so a send never loads a chunk back.
                 */
                if (blockEntity.isRemoved() || !level.isLoaded(pos) || level.getBlockEntity(pos) != blockEntity) {
                    continue;
                }
                final BlockState state = level.getBlockState(pos);
                final BlockState wanted = blockEntity.fields().mirrored(state);
                if (wanted != state) {
                    // Setting the new state sends it, and the block entity's update with it.
                    level.setBlock(pos, wanted, Block.UPDATE_CLIENTS);
                } else {
                    level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
                }
            }
        }
    }
}
