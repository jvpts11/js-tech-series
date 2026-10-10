/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.dimension;

import com.mojang.logging.LogUtils;
import dev.jstech.core.JsCore;
import dev.jstech.core.data.DataRegistries;
import dev.jstech.core.worldgen.WorldGen;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.border.BorderChangeListener;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.WorldData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * Dimensions made while the game runs: a mod asks for a new dimension like one it declared, the player's own pocket
 * world, an instance of a dungeon, the surface of a planet just found, and the Core makes it at once, beside every
 * other, and makes it again each time the server starts until the mod takes it away.
 *
 * <p>A dimension made here is a copy of a declared one, its template: of the same kind of place, made the same way,
 * under rules of its own name. What it holds is saved with the world as any dimension's is.
 */
@EventBusSubscriber(modid = JsCore.MODID)
public final class RuntimeDimensions {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String FILE = "jscore_runtime_dimensions";

    /** The template of each dimension made while this server runs, for the rules a copy takes from it. */
    private static final Map<ResourceLocation, ResourceLocation> TEMPLATES = new ConcurrentHashMap<>();

    /* A dimension made while the game runs is not shown being prepared, as the world's own are when it starts. */
    private static final ChunkProgressListener QUIET = new ChunkProgressListener() {
        @Override
        public void updateSpawnPos(final ChunkPos center) {
        }

        @Override
        public void onStatusChange(final ChunkPos chunk, @Nullable final ChunkStatus status) {
        }

        @Override
        public void start() {
        }

        @Override
        public void stop() {
        }
    };

    private RuntimeDimensions() {
    }

    /**
     * The dimension {@code key}, made now as a copy of the declared dimension {@code template} when there is none of
     * that name yet. It is made again each time the server starts, until {@link #remove} takes it away.
     *
     * @throws IllegalArgumentException when no dimension is declared as {@code template}
     */
    public static ServerLevel getOrCreate(final MinecraftServer server, final ResourceKey<Level> key,
                                          final ResourceLocation template) {
        final ServerLevel existing = server.getLevel(key);
        if (existing != null) {
            return existing;
        }
        final LevelStem stem = stemOf(server, template)
                .orElseThrow(() -> new IllegalArgumentException("no dimension is declared as " + template));
        final ServerLevel level = make(server, key, stem, template);
        final Ledger ledger = ledger(server);
        ledger.made.put(key.location(), template);
        ledger.setDirty();
        DataRegistries.resync(server);
        return level;
    }

    /**
     * Takes away the dimension {@code key} the Core made: its players are sent to the overworld's spawn, what it holds
     * is saved, and it is not made again. A dimension the Core did not make is left alone.
     *
     * <p>Call it between the levels' ticks, as a command, a payload or a task handed to the server runs: a level taken
     * away while the levels tick would still be ticked once more after it was closed.
     *
     * @return whether a dimension was taken away
     */
    public static boolean remove(final MinecraftServer server, final ResourceKey<Level> key) {
        final Ledger ledger = ledger(server);
        if (!ledger.made.containsKey(key.location())) {
            return false;
        }
        ledger.made.remove(key.location());
        ledger.setDirty();
        TEMPLATES.remove(key.location());
        DataRegistries.resync(server);
        final ServerLevel level = server.getLevel(key);
        if (level == null) {
            return true;
        }
        final ServerLevel overworld = server.overworld();
        final BlockPos spawn = overworld.getSharedSpawnPos();
        for (final ServerPlayer player : List.copyOf(level.players())) {
            player.teleportTo(overworld, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, player.getYRot(),
                    player.getXRot());
        }
        level.save(null, true, false);
        NeoForge.EVENT_BUS.post(new LevelEvent.Unload(level));
        server.forgeGetWorldMap().remove(key);
        server.markWorldsDirty();
        try {
            level.close();
        } catch (final IOException failed) {
            LOGGER.warn("The dimension {} did not close cleanly: {}", key.location(), failed.toString());
        }
        return true;
    }

    /** The dimensions the Core made and makes again, each with the declared dimension it copies. */
    public static Map<ResourceLocation, ResourceLocation> made(final MinecraftServer server) {
        return Map.copyOf(ledger(server).made);
    }

    /**
     * How the dimension {@code template} is made: as the world holds it, or, for a world that leaves the datapacks'
     * dimensions out (a test server's flat world does), as its mod declared it, its kind of place read from the
     * registry every player's game shares.
     */
    public static Optional<LevelStem> stemOf(final MinecraftServer server, final ResourceLocation template) {
        final Optional<LevelStem> held = server.registryAccess().registryOrThrow(Registries.LEVEL_STEM)
                .getOptional(template);
        if (held.isPresent()) {
            return held;
        }
        final Optional<Holder.Reference<DimensionType>> type = server.registryAccess()
                .registryOrThrow(Registries.DIMENSION_TYPE).getHolder(template);
        return WorldGen.declaredDimension(template).flatMap(spec -> type.map(holder -> new LevelStem(holder,
                spec.generator(server.registryAccess().lookupOrThrow(Registries.BIOME),
                        server.registryAccess().lookupOrThrow(Registries.NOISE_SETTINGS)))));
    }

    /** The declared dimension {@code dimension} copies, when the Core made it while this server runs. */
    public static Optional<ResourceLocation> templateOf(final ResourceLocation dimension) {
        return Optional.ofNullable(TEMPLATES.get(dimension));
    }

    /** Every dimension made while this server runs, with the declared dimension it copies. */
    public static Map<ResourceLocation, ResourceLocation> templates() {
        return Map.copyOf(TEMPLATES);
    }

    /** Makes again, as the server starts, every dimension the Core made before. */
    @SubscribeEvent
    public static void onServerStarted(final ServerStartedEvent event) {
        final MinecraftServer server = event.getServer();
        TEMPLATES.clear();
        for (final Map.Entry<ResourceLocation, ResourceLocation> made : ledger(server).made.entrySet()) {
            final ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, made.getKey());
            final Optional<LevelStem> stem = stemOf(server, made.getValue());
            if (stem.isEmpty()) {
                LOGGER.warn("The dimension {} is not made again: its template {} is no longer declared",
                        made.getKey(), made.getValue());
            } else if (server.getLevel(key) == null) {
                make(server, key, stem.get(), made.getValue());
            }
        }
    }

    /*
     * A level beside the world's own, made as the server makes them when it starts: on the server's own executor
     * and storage, which only the server holds, so they are read from it; its weather its own, under its rules.
     */
    private static ServerLevel make(final MinecraftServer server, final ResourceKey<Level> key,
                                    final LevelStem stem, final ResourceLocation template) {
        TEMPLATES.put(key.location(), template);
        final Executor executor = ObfuscationReflectionHelper.getPrivateValue(MinecraftServer.class, server,
                "executor");
        final LevelStorageSource.LevelStorageAccess storage = ObfuscationReflectionHelper.getPrivateValue(
                MinecraftServer.class, server, "storageSource");
        final WorldData world = server.getWorldData();
        final ServerLevel overworld = server.overworld();
        final ServerLevel level = new ServerLevel(server, executor, storage, new RuledLevelData(world, key), key,
                stem, QUIET, world.isDebugWorld(), BiomeManager.obfuscateSeed(world.worldGenOptions().seed()),
                List.of(), false, overworld.getRandomSequences());
        overworld.getWorldBorder().addListener(new BorderChangeListener.DelegateBorderChangeListener(
                level.getWorldBorder()));
        server.forgeGetWorldMap().put(key, level);
        server.markWorldsDirty();
        NeoForge.EVENT_BUS.post(new LevelEvent.Load(level));
        return level;
    }

    private static Ledger ledger(final MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(Ledger::new, Ledger::load),
                FILE);
    }

    /** The dimensions the Core made, and the declared dimension each copies, kept with the world. */
    private static final class Ledger extends SavedData {

        private final Map<ResourceLocation, ResourceLocation> made = new LinkedHashMap<>();

        static Ledger load(final CompoundTag tag, final HolderLookup.Provider registries) {
            final Ledger ledger = new Ledger();
            for (final String key : tag.getAllKeys()) {
                final ResourceLocation dimension = ResourceLocation.tryParse(key);
                final ResourceLocation template = ResourceLocation.tryParse(tag.getString(key));
                if (dimension != null && template != null) {
                    ledger.made.put(dimension, template);
                }
            }
            return ledger;
        }

        @Override
        public CompoundTag save(final CompoundTag tag, final HolderLookup.Provider registries) {
            made.forEach((dimension, template) -> tag.putString(dimension.toString(), template.toString()));
            return tag;
        }
    }
}
