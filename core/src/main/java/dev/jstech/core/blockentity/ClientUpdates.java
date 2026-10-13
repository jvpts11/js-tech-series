/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.blockentity;

import dev.jstech.core.JsCore;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Sends the players the fields that changed, once per block entity at the end of each server tick, however many
 * changed during it, and checks the worked-out values the players see for a change first. The work is in proportion
 * to what changed and to the block entities that show worked-out values, never to every block entity loaded.
 */
@EventBusSubscriber(modid = JsCore.MODID)
public final class ClientUpdates {

    private static final Map<ServerLevel, Pending> PENDING = new IdentityHashMap<>();

    private ClientUpdates() {
    }

    @SubscribeEvent
    public static void afterTick(final LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            final Pending pending = PENDING.get(level);
            if (pending != null) {
                pending.flush(level);
            }
        }
    }

    @SubscribeEvent
    public static void unload(final LevelEvent.Unload event) {
        PENDING.remove(event.getLevel());
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
