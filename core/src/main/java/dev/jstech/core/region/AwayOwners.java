/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.region;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * The server's rule for chunks kept loaded while their owner is away ({@link ChunkLoadingBalance#letsGoAfter}): by
 * default they stay loaded, but a server may let them go as soon as the owner leaves, or once the owner has been away
 * so many hours of its running. An owner's chunks let go of are still noted as theirs, against their limit and the
 * server's budget, and are loaded again as soon as they come back; what a machine asks to load meanwhile waits for
 * them too. When each owner left is kept with the world, so the hours go on counting across restarts.
 */
public final class AwayOwners {

    /* How often the owners away are looked at, in ticks. */
    private static final int LOOK_EVERY = 400;
    private static final String FILE = "jstech_owners_away";
    /* The owners whose chunks are let go of now; worked out again as each server starts. */
    private static final Set<UUID> LET_GO = ConcurrentHashMap.newKeySet();

    private AwayOwners() {
    }

    /** Hands the game's bus the players coming and going and the server's ticks, once, from the Core's constructor. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                back(player.server, player.getUUID());
            }
        });
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedOutEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                left(player.server, player.getUUID());
            }
        });
        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, event -> {
            if (event.getServer().getTickCount() % LOOK_EVERY == 0) {
                letGo(event.getServer());
            }
        });
        NeoForge.EVENT_BUS.addListener(ServerStoppedEvent.class, event -> LET_GO.clear());
    }

    /** Notes that {@code owner} left the server now: from now their time away counts. */
    public static void left(final MinecraftServer server, final UUID owner) {
        final Ledger ledger = ledger(server);
        ledger.since.put(owner, now(server));
        ledger.setDirty();
    }

    /** {@code owner} is back: the chunks let go of while they were away are loaded again. */
    public static void back(final MinecraftServer server, final UUID owner) {
        final Ledger ledger = ledger(server);
        if (ledger.since.remove(owner) != null) {
            ledger.setDirty();
        }
        if (LET_GO.remove(owner)) {
            ChunkLoaders.forceAllOf(server, owner, true);
        }
    }

    /**
     * Lets go of the chunks of every owner away past what the server allows, as it does every twenty seconds; how
     * many owners. An owner with chunks never seen leaving, as after the rule was set, counts from now.
     */
    public static int letGo(final MinecraftServer server) {
        int letGo = 0;
        for (final UUID owner : ChunkLoaders.loadedPerOwner(server).keySet()) {
            letGo += letGo(server, owner) ? 1 : 0;
        }
        return letGo;
    }

    /** Lets go of {@code owner}'s chunks when they are away past what the server allows; whether it did now. */
    public static boolean letGo(final MinecraftServer server, final UUID owner) {
        if (LET_GO.contains(owner) || server.getPlayerList().getPlayer(owner) != null) {
            return false;
        }
        final Ledger ledger = ledger(server);
        final long now = now(server);
        final Long since = ledger.since.get(owner);
        if (since == null) {
            ledger.since.put(owner, now);
            ledger.setDirty();
        }
        if (!ChunkLoadingBalance.letsGoAfter(since == null ? 0 : now - since)) {
            return false;
        }
        ChunkLoaders.forceAllOf(server, owner, false);
        LET_GO.add(owner);
        return true;
    }

    /** Whether {@code owner}'s chunks are let go of now, while they are away. */
    public static boolean letGoOf(final UUID owner) {
        return LET_GO.contains(owner);
    }

    /* The server's running time, in ticks, as its first world keeps it. */
    private static long now(final MinecraftServer server) {
        return server.overworld().getGameTime();
    }

    private static Ledger ledger(final MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(Ledger::new,
                Ledger::load), FILE);
    }

    /** When each owner away left, as the first world's saved data. */
    private static final class Ledger extends SavedData {

        private final Map<UUID, Long> since = new LinkedHashMap<>();

        static Ledger load(final CompoundTag tag, final HolderLookup.Provider registries) {
            final Ledger ledger = new Ledger();
            for (final Tag one : tag.getList("away", Tag.TAG_COMPOUND)) {
                final CompoundTag owner = (CompoundTag) one;
                ledger.since.put(owner.getUUID("owner"), owner.getLong("since"));
            }
            return ledger;
        }

        @Override
        public CompoundTag save(final CompoundTag tag, final HolderLookup.Provider registries) {
            final ListTag list = new ListTag();
            since.forEach((owner, tick) -> {
                final CompoundTag one = new CompoundTag();
                one.putUUID("owner", owner);
                one.putLong("since", tick);
                list.add(one);
            });
            tag.put("away", list);
            return tag;
        }
    }
}
