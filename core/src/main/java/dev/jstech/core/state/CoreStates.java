/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.state;

import dev.jstech.core.JsCore;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Every state the mods keep, registered once each while the game loads, and what the Core does for them as the game
 * runs: the changed values sent at the end of each tick, and each player sent what is theirs as they join, arrive in
 * another dimension or come back from death.
 *
 * <p>A state has to be registered before it is used, and on both sides, so the server and its players agree on what
 * each id names.
 */
@EventBusSubscriber(modid = JsCore.MODID)
public final class CoreStates {

    private static final Map<ResourceLocation, CoreState<?>> BY_ID = new ConcurrentHashMap<>();

    private static final Map<String, CoreState<?>> BY_FILE = new ConcurrentHashMap<>();

    private CoreStates() {
    }

    /** Registers {@code state}, from the constructor of the mod that declares it. */
    public static void register(final CoreState<?> state) {
        final CoreState<?> before = BY_ID.putIfAbsent(state.id(), state);
        if (before != null && before != state) {
            throw new IllegalStateException("two states are registered as " + state.id());
        }
        // The world's data storage hands back whatever it holds under a file name, so two states sharing one would
        // read each other's file as their own value.
        final CoreState<?> sharing = BY_FILE.putIfAbsent(state.fileName(), state);
        if (sharing != null && sharing != state) {
            throw new IllegalStateException("the states " + sharing.id() + " and " + state.id()
                    + " would share the file " + state.fileName());
        }
        state.markRegistered();
    }

    /** The state registered as {@code id}, or null when there is none. */
    public static @Nullable CoreState<?> byId(final ResourceLocation id) {
        return BY_ID.get(id);
    }

    /** Every state registered. */
    public static Collection<CoreState<?>> all() {
        return List.copyOf(BY_ID.values());
    }

    @SubscribeEvent
    public static void onServerTick(final ServerTickEvent.Post event) {
        StateSync.endOfTick(event.getServer());
    }

    @SubscribeEvent
    public static void onLoggedIn(final PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            StateSync.joined(player);
        }
    }

    @SubscribeEvent
    public static void onChangedDimension(final PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            StateSync.arrived(player);
        }
    }

    /* Coming back from the End lands a player in another dimension without a change of dimension being announced. */
    @SubscribeEvent
    public static void onRespawn(final PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            StateSync.arrived(player);
        }
    }

    @SubscribeEvent
    public static void onLoggedOut(final PlayerEvent.PlayerLoggedOutEvent event) {
        StateSync.left(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerStopped(final ServerStoppedEvent event) {
        StateSync.clear();
    }
}
