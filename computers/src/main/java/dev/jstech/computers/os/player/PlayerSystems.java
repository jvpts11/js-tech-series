/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.player;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.os.OsDef;
import dev.jstech.core.persistence.SaveLayout;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The operating systems and desktops the players of a world have built (computers/docs/design/player-systems.md),
 * kept with the world.
 *
 * <p>The mod's own systems are registered while the game loads; a player's are made while it runs, so they are kept
 * here instead, and every lookup of a system falls back to them. Each side knows the systems as the installers and the
 * firmware see them: the server from the world's save, and each client from what the server tells it when it joins
 * and whenever a system is built.
 */
public final class PlayerSystems extends SavedData {

    private final Map<ResourceLocation, PlayerSystem> systems = new LinkedHashMap<>();
    private final Map<ResourceLocation, PlayerDesktop> desktops = new LinkedHashMap<>();

    public static final String DATA_NAME = "jsc_player_systems";
    /** The layout the file is saved in. */
    public static final SaveLayout LAYOUT = SaveLayout.of(JsComputers.MODID + ":player_systems");

    /* What this side knows: each system as the installers see it, and each desktop's look. */
    private static final Map<ResourceLocation, OsDef> KNOWN_SYSTEMS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, PlayerDesktop> KNOWN_DESKTOPS = new ConcurrentHashMap<>();
    /* The server's systems and desktops with their files, which an install lays on a disk; empty on a client. */
    private static final Map<ResourceLocation, PlayerSystem> SERVER_SYSTEMS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, PlayerDesktop> SERVER_DESKTOPS = new ConcurrentHashMap<>();

    public PlayerSystems() {
    }

    /** Hears the server starting and stopping, and players joining, on the game's bus. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(ServerStartedEvent.class, event -> get(event.getServer()));
        NeoForge.EVENT_BUS.addListener(ServerStoppedEvent.class, event -> forget());
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player && player.getServer() != null) {
                PacketDistributor.sendToPlayer(player, get(player.getServer()).told());
            }
        });
    }

    public static SavedData.Factory<PlayerSystems> factory() {
        return new SavedData.Factory<>(PlayerSystems::new, PlayerSystems::load);
    }

    /** The server's store, read from the save the first time it is asked for. */
    public static PlayerSystems get(final MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(factory(), DATA_NAME);
    }

    /** The player's system of that id as the installers see it, on whichever side asks; null for none. */
    @Nullable
    public static OsDef os(@Nullable final ResourceLocation id) {
        return id == null ? null : KNOWN_SYSTEMS.get(id);
    }

    /** Every player's system this side knows, as the installers see them. */
    public static Collection<OsDef> oses() {
        return List.copyOf(KNOWN_SYSTEMS.values());
    }

    /** The player's desktop of that id, its look and programs, on whichever side asks; null for none. */
    @Nullable
    public static PlayerDesktop knownDesktop(@Nullable final ResourceLocation id) {
        return id == null ? null : KNOWN_DESKTOPS.get(id);
    }

    /**
     * Takes what the server told this client, in place of what it knew. What is still there is written over rather
     * than cleared first: in a world played alone the server reads these very maps on its own thread, and must never
     * find a system it has missing for a moment.
     */
    public static void know(final PlayerSystemsPayload told) {
        final Map<ResourceLocation, OsDef> systems = new LinkedHashMap<>();
        for (final OsDef os : told.systems()) {
            systems.put(os.id(), os);
        }
        KNOWN_SYSTEMS.putAll(systems);
        KNOWN_SYSTEMS.keySet().retainAll(systems.keySet());
        final Map<ResourceLocation, PlayerDesktop> desktops = new LinkedHashMap<>();
        for (final PlayerDesktop desktop : told.desktops()) {
            desktops.put(desktop.id(), desktop);
        }
        KNOWN_DESKTOPS.putAll(desktops);
        KNOWN_DESKTOPS.keySet().retainAll(desktops.keySet());
    }

    /** Forgets every player's system, as a world closing takes them away. */
    public static void forget() {
        KNOWN_SYSTEMS.clear();
        KNOWN_DESKTOPS.clear();
        SERVER_SYSTEMS.clear();
        SERVER_DESKTOPS.clear();
    }

    /** The server's system of that id with its files, which its install lays on a disk; null on a client or none. */
    @Nullable
    public static PlayerSystem withFiles(@Nullable final ResourceLocation id) {
        return id == null ? null : SERVER_SYSTEMS.get(id);
    }

    /** The server's desktop of that id with its files, which its package lays on a disk; null on a client or none. */
    @Nullable
    public static PlayerDesktop desktopWithFiles(@Nullable final ResourceLocation id) {
        return id == null ? null : SERVER_DESKTOPS.get(id);
    }

    /** The system of that id, with its files, or null. */
    @Nullable
    public PlayerSystem system(final ResourceLocation id) {
        return this.systems.get(id);
    }

    /** The desktop of that id, with its files, or null. */
    @Nullable
    public PlayerDesktop desktop(final ResourceLocation id) {
        return this.desktops.get(id);
    }

    /**
     * Keeps a system built, in place of an earlier build of the same name and version, and tells every player. The
     * newest build is kept last, which is what {@link #lastBuiltFrom} finds.
     */
    public void put(final PlayerSystem system) {
        this.systems.remove(system.id());
        this.systems.put(system.id(), system);
        SERVER_SYSTEMS.put(system.id(), system);
        KNOWN_SYSTEMS.put(system.id(), system.os());
        this.changed();
    }

    /** Keeps a desktop built, in place of an earlier build of the same name, and tells every player. */
    public void put(final PlayerDesktop desktop) {
        this.desktops.remove(desktop.id());
        this.desktops.put(desktop.id(), desktop);
        SERVER_DESKTOPS.put(desktop.id(), desktop);
        KNOWN_DESKTOPS.put(desktop.id(), withoutFiles(desktop));
        this.changed();
    }

    /** The system last built from a folder of that name, or null when none was. */
    @Nullable
    public PlayerSystem lastBuiltFrom(final String folder) {
        PlayerSystem last = null;
        for (final PlayerSystem system : this.systems.values()) {
            if (system.folder().equals(folder)) {
                last = system;
            }
        }
        return last;
    }

    /** The desktop last built from a folder of that name, or null when none was. */
    @Nullable
    public PlayerDesktop lastDesktopFrom(final String folder) {
        PlayerDesktop last = null;
        for (final PlayerDesktop desktop : this.desktops.values()) {
            if (desktop.folder().equals(folder)) {
                last = desktop;
            }
        }
        return last;
    }

    @Override
    public CompoundTag save(final CompoundTag tag, final HolderLookup.Provider registries) {
        final ListTag systemList = new ListTag();
        for (final PlayerSystem system : this.systems.values()) {
            PlayerCodecs.SYSTEM.encodeStart(NbtOps.INSTANCE, system).result().ifPresent(systemList::add);
        }
        tag.put("Systems", systemList);
        final ListTag desktopList = new ListTag();
        for (final PlayerDesktop desktop : this.desktops.values()) {
            PlayerCodecs.DESKTOP.encodeStart(NbtOps.INSTANCE, desktop).result().ifPresent(desktopList::add);
        }
        tag.put("Desktops", desktopList);
        return LAYOUT.stamp(tag);
    }

    /* What a client is told: the systems as the installers see them, and the desktops without their files. */
    private PlayerSystemsPayload told() {
        final List<OsDef> oses = new ArrayList<>();
        for (final PlayerSystem system : this.systems.values()) {
            oses.add(system.os());
        }
        final List<PlayerDesktop> looks = new ArrayList<>();
        for (final PlayerDesktop desktop : this.desktops.values()) {
            looks.add(withoutFiles(desktop));
        }
        return new PlayerSystemsPayload(oses, looks);
    }

    private void changed() {
        this.setDirty();
        PacketDistributor.sendToAllPlayers(this.told());
    }

    private static PlayerDesktop withoutFiles(final PlayerDesktop desktop) {
        return new PlayerDesktop(desktop.id(), desktop.facts(), desktop.folder(), Map.of());
    }

    private static PlayerSystems load(final CompoundTag saved, final HolderLookup.Provider registries) {
        final PlayerSystems store = new PlayerSystems();
        final CompoundTag tag = LAYOUT.read(saved);
        for (final Tag element : tag.getList("Systems", Tag.TAG_COMPOUND)) {
            PlayerCodecs.SYSTEM.parse(NbtOps.INSTANCE, element).result().ifPresent(system -> {
                store.systems.put(system.id(), system);
                SERVER_SYSTEMS.put(system.id(), system);
                KNOWN_SYSTEMS.put(system.id(), system.os());
            });
        }
        for (final Tag element : tag.getList("Desktops", Tag.TAG_COMPOUND)) {
            PlayerCodecs.DESKTOP.parse(NbtOps.INSTANCE, element).result().ifPresent(desktop -> {
                store.desktops.put(desktop.id(), desktop);
                SERVER_DESKTOPS.put(desktop.id(), desktop);
                KNOWN_DESKTOPS.put(desktop.id(), withoutFiles(desktop));
            });
        }
        return store;
    }
}
