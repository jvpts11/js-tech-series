/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.advancement;

import dev.jstech.computers.JsComputers;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * What players have earned while they were away.
 *
 * <p>Much of what a machine does finishes long after it was asked for: an Operation settles minutes later, a job
 * fires overnight. The player it is credited to may have logged off by then, and an advancement can only be given
 * to a player who is there. So what they earned waits here, with the world, and is handed over the next time they
 * join. Each award is kept once, since an advancement is earned once, and a player's list is held to a size, so
 * somebody who never comes back costs the world a few lines and no more.
 */
@EventBusSubscriber(modid = JsComputers.MODID)
public final class PendingAwards extends SavedData {

    /** Who has something waiting, and what, in the order it was earned. */
    private final Map<UUID, Set<Award>> waiting = new LinkedHashMap<>();

    public static final String DATA_NAME = "jsc_pending_awards";

    /** The most awards kept for one player; far more than there are advancements an event can earn. */
    private static final int MOST_PER_PLAYER = 128;

    private static final String PLAYERS = "Players";
    private static final String ID = "Id";
    private static final String AWARDS = "Awards";
    private static final String EVENT = "Event";
    private static final String DETAIL = "Detail";

    public PendingAwards() {
    }

    /** One event earned, with the detail that tells apart the criteria of an advancement asking for all of them. */
    public record Award(String event, String detail) {
    }

    /** The world's list. */
    public static PendingAwards of(final MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(PendingAwards::new, PendingAwards::load), DATA_NAME);
    }

    /** Keeps an award for a player who is not here to be given it. */
    public void keep(final UUID player, final String event, final String detail) {
        final Set<Award> theirs = this.waiting.computeIfAbsent(player, id -> new LinkedHashSet<>());
        if (theirs.size() < MOST_PER_PLAYER && theirs.add(new Award(event, detail))) {
            this.setDirty();
        }
    }

    /** What is waiting for that player, left in place. */
    public List<Award> waitingFor(final UUID player) {
        return List.copyOf(this.waiting.getOrDefault(player, Set.of()));
    }

    /** Hands a player everything waiting for them, and forgets it. */
    public void deliver(final ServerPlayer player) {
        final Set<Award> theirs = this.waiting.remove(player.getUUID());
        if (theirs == null) {
            return;
        }
        this.setDirty();
        for (final Award award : theirs) {
            JscEvents.award(player, award.event(), award.detail());
        }
    }

    /** A player joining is given what they earned while they were away. */
    @SubscribeEvent
    public static void onLogin(final PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            of(player.server).deliver(player);
        }
    }

    @Override
    public CompoundTag save(final CompoundTag tag, final HolderLookup.Provider registries) {
        final ListTag players = new ListTag();
        this.waiting.forEach((player, awards) -> {
            final CompoundTag one = new CompoundTag();
            one.putUUID(ID, player);
            final ListTag list = new ListTag();
            for (final Award award : awards) {
                final CompoundTag each = new CompoundTag();
                each.putString(EVENT, award.event());
                each.putString(DETAIL, award.detail());
                list.add(each);
            }
            one.put(AWARDS, list);
            players.add(one);
        });
        tag.put(PLAYERS, players);
        return tag;
    }

    /** Reads the list back from a save. */
    public static PendingAwards load(final CompoundTag tag, final HolderLookup.Provider registries) {
        final PendingAwards awards = new PendingAwards();
        for (final Tag entry : tag.getList(PLAYERS, Tag.TAG_COMPOUND)) {
            final CompoundTag one = (CompoundTag) entry;
            if (!one.hasUUID(ID)) {
                continue;
            }
            final List<Award> list = new ArrayList<>();
            for (final Tag each : one.getList(AWARDS, Tag.TAG_COMPOUND)) {
                final CompoundTag award = (CompoundTag) each;
                list.add(new Award(award.getString(EVENT), award.getString(DETAIL)));
            }
            if (!list.isEmpty()) {
                awards.waiting.put(one.getUUID(ID), new LinkedHashSet<>(list));
            }
        }
        return awards;
    }
}
