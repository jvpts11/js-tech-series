/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import dev.jstech.core.JsCore;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Every declared {@link DataRegistry}: each is read whenever the server loads its data, and each synced one is sent to
 * the players as they join and after every reload.
 */
@EventBusSubscriber(modid = JsCore.MODID)
public final class DataRegistries {

    private static final String NETWORK_VERSION = "1";
    private static final List<DataRegistry<?>> ALL = new CopyOnWriteArrayList<>();
    private static final Map<ResourceLocation, DataRegistry<?>> BY_ID = new ConcurrentHashMap<>();

    private DataRegistries() {
    }

    /** Every declared registry, in the order they were declared. */
    public static List<DataRegistry<?>> all() {
        return List.copyOf(ALL);
    }

    @SubscribeEvent
    public static void onAddReloadListeners(final AddReloadListenerEvent event) {
        for (final DataRegistry<?> registry : ALL) {
            event.addListener(new Listener(registry, event.getRegistryAccess()));
        }
    }

    @SubscribeEvent
    public static void onDatapackSync(final OnDatapackSyncEvent event) {
        final MinecraftServer server = event.getPlayerList().getServer();
        for (final DataRegistry<?> registry : ALL) {
            if (registry.synced()) {
                final DataRegistryPayload payload = new DataRegistryPayload(registry.id(),
                        registry.encode(server.registryAccess()));
                /*
                 * Only to a game that took the payload when it joined: a player made by a test, or by another mod,
                 * stands on a connection that agreed on nothing, and sending it what it does not know is an error.
                 */
                event.getRelevantPlayers().filter(player -> player.connection.hasChannel(DataRegistryPayload.TYPE))
                        .forEach(player -> PacketDistributor.sendToPlayer(player, payload));
            }
        }
    }

    @SubscribeEvent
    public static void onRegisterPayloads(final RegisterPayloadHandlersEvent event) {
        event.registrar(NETWORK_VERSION).playToClient(DataRegistryPayload.TYPE, DataRegistryPayload.STREAM_CODEC,
                DataRegistries::onValues);
    }

    /** The player's game left the server: the values it sent are forgotten. */
    public static void forgetClientValues() {
        ALL.forEach(DataRegistry::forgetClientValues);
    }

    static void add(final DataRegistry<?> registry) {
        if (BY_ID.putIfAbsent(registry.id(), registry) != null) {
            throw new IllegalStateException("the data registry " + registry.id() + " is declared twice");
        }
        ALL.add(registry);
    }

    /* Runs on a player's game: the payload is only ever sent to one. */
    private static void onValues(final DataRegistryPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            final DataRegistry<?> registry = BY_ID.get(payload.registry());
            if (registry != null) {
                registry.acceptFromServer(payload.entries(), context.player().registryAccess());
            }
        });
    }

    /* Reads one registry's files on every data load. */
    private static final class Listener extends SimpleJsonResourceReloadListener {

        private final DataRegistry<?> registry;
        private final HolderLookup.Provider registries;

        private Listener(final DataRegistry<?> registry, final HolderLookup.Provider registries) {
            super(new Gson(), registry.folder());
            this.registry = registry;
            this.registries = registries;
        }

        @Override
        protected void apply(final Map<ResourceLocation, JsonElement> files, final ResourceManager manager,
                             final ProfilerFiller profiler) {
            this.registry.load(files, this.registries);
        }
    }
}
