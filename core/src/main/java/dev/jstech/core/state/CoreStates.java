/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.state;

import dev.jstech.core.util.DeclarationError;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
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
public final class CoreStates {

    private static final Map<ResourceLocation, CoreState<?>> BY_ID = new ConcurrentHashMap<>();

    private static final Map<String, CoreState<?>> BY_FILE = new ConcurrentHashMap<>();

    private CoreStates() {
    }

    /** Registers {@code state}, from the constructor of the mod that declares it. */
    public static void register(final CoreState<?> state) {
        final CoreState<?> before = BY_ID.putIfAbsent(state.id(), state);
        if (before != null && before != state) {
            throw DeclarationError.of(state.id().getNamespace(), "the state " + state.id(),
                    "is registered for two different states", "give the second state an id of its own");
        }
        // The world's data storage hands back whatever it holds under a file name, so two states sharing one would
        // read each other's file as their own value.
        final CoreState<?> sharing = BY_FILE.putIfAbsent(state.fileName(), state);
        if (sharing != null && sharing != state) {
            throw DeclarationError.of(state.id().getNamespace(), "the state " + state.id(),
                    "would be saved in " + state.fileName() + ", the file of " + sharing.id(),
                    "give one of the two another id, so each is saved in a file of its own");
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

    /** Hands the game's bus what this answers, once, from the mod's constructor. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, CoreStates::onServerTick);
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class, CoreStates::onLoggedIn);
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerChangedDimensionEvent.class, CoreStates::onChangedDimension);
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerRespawnEvent.class, CoreStates::onRespawn);
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedOutEvent.class, CoreStates::onLoggedOut);
        NeoForge.EVENT_BUS.addListener(ServerStoppedEvent.class, CoreStates::onServerStopped);
    }

    public static void onServerTick(final ServerTickEvent.Post event) {
        StateSync.endOfTick(event.getServer());
    }

    public static void onLoggedIn(final PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            StateSync.joined(player);
        }
    }

    public static void onChangedDimension(final PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            StateSync.arrived(player);
        }
    }

    /* Coming back from the End lands a player in another dimension without a change of dimension being announced. */
    public static void onRespawn(final PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            StateSync.arrived(player);
        }
    }

    public static void onLoggedOut(final PlayerEvent.PlayerLoggedOutEvent event) {
        StateSync.left(event.getEntity().getUUID());
    }

    public static void onServerStopped(final ServerStoppedEvent event) {
        StateSync.clear();
    }
}
