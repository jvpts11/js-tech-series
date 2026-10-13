/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.computers.JsComputers;
import dev.jstech.core.persistence.ISaveUpgrade;
import dev.jstech.core.state.CoreState;
import dev.jstech.core.state.PlayerState;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
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
 *
 * <p>It is a state of each player, kept in the file it has always been kept in. A file from before the state had
 * versions held one list of players, each with their awards; the first step of the layout reads it.
 */
@EventBusSubscriber(modid = JsComputers.MODID)
public final class PendingAwards {

    /** The most awards kept for one player; far more than there are advancements an event can earn. */
    private static final int MOST_PER_PLAYER = 128;
    /** What a file from before versions held its players, their ids and their awards under. */
    private static final String OLD_PLAYERS = "Players";
    private static final String OLD_ID = "Id";
    private static final String OLD_AWARDS = "Awards";
    private static final String OLD_EVENT = "Event";
    private static final String OLD_DETAIL = "Detail";
    private static final String EVENT = "event";
    private static final String DETAIL = "detail";

    /** What each player has waiting, in the order it was earned. */
    public static final PlayerState<List<Award>> WAITING = CoreState.builder(
                    ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "pending_awards"), Award.CODEC.listOf(),
                    List.<Award>of())
            .upgrade(0, ISaveUpgrade.compound(PendingAwards::fromBeforeVersions))
            .player();

    private PendingAwards() {
    }

    /** Keeps an award for a player who is not here to be given it. */
    public static void keep(final MinecraftServer server, final UUID player, final String event,
                            final String detail) {
        final Award award = new Award(event, detail);
        WAITING.update(server, player, theirs -> {
            if (theirs.size() >= MOST_PER_PLAYER || theirs.contains(award)) {
                return theirs;
            }
            final List<Award> next = new ArrayList<>(theirs);
            next.add(award);
            return List.copyOf(next);
        });
    }

    /** What is waiting for that player, left in place. */
    public static List<Award> waitingFor(final MinecraftServer server, final UUID player) {
        return WAITING.get(server, player);
    }

    /** Hands a player everything waiting for them, and forgets it. */
    public static void deliver(final ServerPlayer player) {
        final List<Award> theirs = WAITING.get(player.server, player.getUUID());
        if (theirs.isEmpty()) {
            return;
        }
        WAITING.set(player.server, player.getUUID(), List.of());
        for (final Award award : theirs) {
            JscEvents.award(player, award.event(), award.detail());
        }
    }

    /** A player joining is given what they earned while they were away. */
    @SubscribeEvent
    public static void onLogin(final PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            deliver(player);
        }
    }

    /* A file from before versions, one list of players: each player's awards, under their id. */
    private static CompoundTag fromBeforeVersions(final CompoundTag old) {
        final CompoundTag players = new CompoundTag();
        for (final Tag entry : old.getList(OLD_PLAYERS, Tag.TAG_COMPOUND)) {
            final CompoundTag one = (CompoundTag) entry;
            if (!one.hasUUID(OLD_ID)) {
                continue;
            }
            final ListTag awards = new ListTag();
            for (final Tag each : one.getList(OLD_AWARDS, Tag.TAG_COMPOUND)) {
                final CompoundTag before = (CompoundTag) each;
                final CompoundTag award = new CompoundTag();
                award.putString(EVENT, before.getString(OLD_EVENT));
                award.putString(DETAIL, before.getString(OLD_DETAIL));
                awards.add(award);
            }
            if (!awards.isEmpty()) {
                players.put(one.getUUID(OLD_ID).toString(), awards);
            }
        }
        return players;
    }

    /**
     * One event earned, with the detail that tells apart the criteria of an advancement asking for all of them.
     *
     * @param event  the event
     * @param detail what tells its criteria apart, or empty
     */
    public record Award(String event, String detail) {

        static final Codec<Award> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf(EVENT).forGetter(Award::event),
                Codec.STRING.fieldOf(DETAIL).forGetter(Award::detail)
        ).apply(instance, Award::new));
    }
}
