/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.builder;

import dev.jstech.core.tool.BuildSources;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * How many of a block the place a Multiblock Builder takes from holds, as each side knows it. The server counts every
 * place itself, and the player's game counts the player's inventory itself; any other place, such as a network's
 * store, only the server can count, so the player's game keeps what the server last told it and asks again, every
 * time its display is read, for the blocks the display wanted. What the game knows is kept on its own thread; the
 * server keeps, on its thread, when each player last asked, and answers a player at most every few ticks.
 */
public final class SourceCounts {

    /* A game reads its display twice a second; an ask sooner than this after the last is not answered. */
    private static final long ASK_EVERY = 5L;
    private static final Map<Item, Long> KNOWN = new HashMap<>();
    private static final Set<Item> WANTED = new LinkedHashSet<>();
    private static final Map<ServerPlayer, Long> ASKED = new WeakHashMap<>();
    private static ResourceLocation knownOf = BuildSources.INVENTORY;

    private SourceCounts() {
    }

    /** How many of {@code item} the place {@code source} holds for {@code player}, as far as this side knows. */
    public static long count(final Player player, final ResourceLocation source, final Item item) {
        if (!player.level().isClientSide() || BuildSources.INVENTORY.equals(source)) {
            return BuildSources.get(source).count(player, item);
        }
        if (!source.equals(knownOf)) {
            KNOWN.clear();
            WANTED.clear();
            knownOf = source;
        }
        if (WANTED.size() < SourceAskPayload.MOST_ITEMS) {
            WANTED.add(item);
        }
        return KNOWN.getOrDefault(item, 0L);
    }

    /**
     * What the player's game asks the server for: the blocks its display wanted since it last asked, of the place it
     * takes from; none when it wanted none.
     */
    public static Optional<SourceAskPayload> ask() {
        if (WANTED.isEmpty()) {
            return Optional.empty();
        }
        final List<ResourceLocation> items = new ArrayList<>();
        for (final Item item : WANTED) {
            items.add(BuiltInRegistries.ITEM.getKey(item));
        }
        WANTED.clear();
        return Optional.of(new SourceAskPayload(knownOf, items));
    }

    /**
     * The server's answer to what {@code player} asked: how many of each block the place holds for them; none when
     * they asked again too soon.
     */
    public static Optional<SourceCountsPayload> answer(final ServerPlayer player, final SourceAskPayload ask) {
        final long now = player.serverLevel().getGameTime();
        final Long last = ASKED.get(player);
        if (last != null && now >= last && now - last < ASK_EVERY) {
            return Optional.empty();
        }
        ASKED.put(player, now);
        final Map<ResourceLocation, Long> counts = new HashMap<>();
        for (final ResourceLocation id : ask.items()) {
            final Item item = BuiltInRegistries.ITEM.get(id);
            if (item != Items.AIR) {
                counts.put(id, BuildSources.get(ask.source()).count(player, item));
            }
        }
        return Optional.of(new SourceCountsPayload(ask.source(), counts));
    }

    /** What the server said the place holds, kept while the display takes from that place. */
    public static void heard(final SourceCountsPayload told) {
        if (!told.source().equals(knownOf)) {
            return;
        }
        for (final Map.Entry<ResourceLocation, Long> count : told.counts().entrySet()) {
            KNOWN.put(BuiltInRegistries.ITEM.get(count.getKey()), count.getValue());
        }
    }

    /** Forgets everything heard, as the player leaves the game. */
    public static void forget() {
        KNOWN.clear();
        WANTED.clear();
        knownOf = BuildSources.INVENTORY;
    }
}
