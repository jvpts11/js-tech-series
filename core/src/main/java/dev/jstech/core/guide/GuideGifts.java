/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide;

import com.mojang.serialization.Codec;
import dev.jstech.core.JsCore;
import dev.jstech.core.state.CoreState;
import dev.jstech.core.state.CoreStates;
import dev.jstech.core.state.PlayerState;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * The manuals a player is handed the first time they join a world, each once: a player who drops theirs or puts it in
 * a chest is not given another when they come back, and one who joins a new world is given it there.
 *
 * <p>A manual goes into the first free place of the inventory above the hotbar, so it never takes the place of what
 * the player holds; only a full inventory puts it in the hotbar, and a full one drops it at their feet.
 *
 * <p>A mod names the manual and its item while the game loads, and the Core keeps, for each player of a world, the
 * manuals they were given.
 */
@EventBusSubscriber(modid = JsCore.MODID)
public final class GuideGifts {

    /** The manuals each player was given, by the manual's id. */
    private static final PlayerState<List<String>> GIVEN = CoreState.builder(
                    ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "manuals_given"), Codec.STRING.listOf(),
                    List.<String>of())
            .player();
    private static final Map<String, Supplier<? extends ItemLike>> GIFTS = new LinkedHashMap<>();
    /** The inventory above the hotbar: its first and last slot. */
    private static final int FIRST_STORED = 9;
    private static final int LAST_STORED = 35;

    private GuideGifts() {
    }

    /** Registers the state the gifts are kept in, from the Core's constructor. */
    public static void register() {
        CoreStates.register(GIVEN);
    }

    /** Hands every player the manual's item, once, the first time they join a world. */
    public static void giveOnFirstJoin(final String manual, final Supplier<? extends ItemLike> item) {
        Objects.requireNonNull(item, "item");
        if (GIFTS.putIfAbsent(Objects.requireNonNull(manual, "manual"), item) != null) {
            throw new IllegalStateException("the manual " + manual + " is given twice");
        }
    }

    /** Gives the player each manual they were never given, and keeps that they have it now. */
    public static void welcome(final ServerPlayer player) {
        final List<String> given = GIVEN.get(player.server, player.getUUID());
        final List<String> now = new ArrayList<>(given);
        GIFTS.forEach((manual, item) -> {
            if (!given.contains(manual)) {
                hand(player, new ItemStack(item.get()));
                now.add(manual);
            }
        });
        if (now.size() > given.size()) {
            GIVEN.set(player.server, player.getUUID(), List.copyOf(now));
        }
    }

    /** Whether the player was given that manual in this world. */
    public static boolean wasGiven(final ServerPlayer player, final String manual) {
        return GIVEN.get(player.server, player.getUUID()).contains(manual);
    }

    @SubscribeEvent
    public static void onLoggedIn(final PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            welcome(player);
        }
    }

    private static void hand(final ServerPlayer player, final ItemStack stack) {
        for (int slot = FIRST_STORED; slot <= LAST_STORED; slot++) {
            if (player.getInventory().getItem(slot).isEmpty()) {
                player.getInventory().setItem(slot, stack);
                return;
            }
        }
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }
}
