/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.retro;

import dev.jstech.core.JsCore;
import dev.jstech.core.diagnostic.Section;
import dev.jstech.core.log.CoreLogs;
import dev.jstech.core.registry.CoreAttachments;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

/**
 * Generation for the chunks made before it was part of the world: a mod added after a world began, or a new kind of
 * vein, lays what it generates into the chunks players already explored. Each chunk keeps which of this work it has
 * had; one made while the work is part of the world's generation is marked as it is made. A chunk that lacks some is
 * noted as it loads and generated later, never inside its loading, a few chunks each tick within the work's budget,
 * once its eight neighbours are loaded too so the work may reach a little into them.
 *
 * <pre>{@code
 * Retrogen.feature(rl("tin_veins"), MyWorldGen.TIN_VEINS_PLACED);
 * Retrogen.declare(rl("meteor_craters"), (level, chunk, random) -> Craters.paint(level, chunk, random));
 * }</pre>
 */
public final class Retrogen {

    /** The work of generating old chunks, timed, watched and kept to its budget. */
    public static final Section SECTION = Section.builder(ResourceLocation.fromNamespaceAndPath(JsCore.MODID,
            "retrogen"), CoreLogs.WORLD).budget(Duration.ofMillis(2)).declare();

    private static final Logger LOGGER = CoreLogs.WORLD.logger();
    private static final Map<ResourceLocation, IRetroWork> WORK = new LinkedHashMap<>();
    private static final Map<ResourceKey<Level>, Set<Long>> WAITING = new ConcurrentHashMap<>();
    /* How long a tick's share of the work may run, in nanoseconds. */
    private static final long SLICE_NANOS = 1_000_000L;
    private static final long MIX = 0x9E3779B97F4A7C15L;

    private Retrogen() {
    }

    /** Hands the game's bus the chunks as they load and the end of each tick, once, from the Core's constructor. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(ChunkEvent.Load.class, Retrogen::onChunkLoad);
        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, event -> {
            for (final ServerLevel level : event.getServer().getAllLevels()) {
                final Set<Long> waiting = WAITING.get(level.dimension());
                if (waiting != null && !waiting.isEmpty() && SECTION.due(level.getGameTime())) {
                    SECTION.run(() -> work(level, Integer.MAX_VALUE, SLICE_NANOS * SECTION.stride()));
                }
            }
        });
    }

    /**
     * Declares the work {@code id}, generated into every old chunk that has not had it.
     *
     * @return {@code id}
     */
    public static synchronized ResourceLocation declare(final ResourceLocation id, final IRetroWork work) {
        WORK.put(Objects.requireNonNull(id, "id"), Objects.requireNonNull(work, "work"));
        return id;
    }

    /** Declares the work {@code id} that places the feature {@code feature} in each old chunk, as generation would. */
    public static ResourceLocation feature(final ResourceLocation id, final ResourceKey<PlacedFeature> feature) {
        return declare(id, (level, chunk, random) -> {
            final Holder<PlacedFeature> placed = level.registryAccess().registryOrThrow(Registries.PLACED_FEATURE)
                    .getHolder(feature).orElse(null);
            if (placed != null) {
                placed.value().placeWithBiomeCheck(level, level.getChunkSource().getGenerator(), random,
                        new BlockPos(chunk.getPos().getMinBlockX(), level.getMinBuildHeight(),
                                chunk.getPos().getMinBlockZ()));
            }
        });
    }

    /** The work declared, in the order it runs in a chunk. */
    public static synchronized List<ResourceLocation> declared() {
        return List.copyOf(WORK.keySet());
    }

    /** The work {@code chunk} has had. */
    public static Set<ResourceLocation> done(final LevelChunk chunk) {
        final Set<ResourceLocation> done = new LinkedHashSet<>();
        for (final String id : chunk.getData(CoreAttachments.RETROGEN)) {
            final ResourceLocation parsed = ResourceLocation.tryParse(id);
            if (parsed != null) {
                done.add(parsed);
            }
        }
        return done;
    }

    /** Has {@code chunk} forget it had {@code id}, so the work runs in it again the next time it is looked at. */
    public static void forget(final LevelChunk chunk, final ResourceLocation id) {
        final Set<ResourceLocation> done = done(chunk);
        if (done.remove(id)) {
            keep(chunk, done);
        }
    }

    /**
     * Notes {@code chunk} for its work later when it lacks some, as loading it does.
     *
     * @return whether it waits for work now
     */
    public static boolean look(final ServerLevel level, final LevelChunk chunk) {
        if (done(chunk).containsAll(declared())) {
            return false;
        }
        WAITING.computeIfAbsent(level.dimension(), key -> new LinkedHashSet<>()).add(chunk.getPos().toLong());
        return true;
    }

    /** How many chunks of {@code level} wait for their work. */
    public static int waiting(final ServerLevel level) {
        final Set<Long> waiting = WAITING.get(level.dimension());
        return waiting == null ? 0 : waiting.size();
    }

    /**
     * Generates the waiting chunks of {@code level} now, up to {@code most} of them: what the tick does a few at a
     * time. A chunk no longer loaded, or whose neighbours are not, is let go until it or one of them loads again.
     *
     * @return how many were generated
     */
    public static int work(final ServerLevel level, final int most) {
        return work(level, most, Long.MAX_VALUE);
    }

    private static int work(final ServerLevel level, final int most, final long budgetNanos) {
        final Set<Long> waiting = WAITING.get(level.dimension());
        if (waiting == null) {
            return 0;
        }
        final long start = System.nanoTime();
        int generated = 0;
        final Iterator<Long> next = waiting.iterator();
        while (generated < most && next.hasNext() && System.nanoTime() - start < budgetNanos) {
            final ChunkPos at = new ChunkPos(next.next());
            next.remove();
            final LevelChunk chunk = level.getChunkSource().getChunkNow(at.x, at.z);
            if (chunk != null && neighboursLoaded(level, at)) {
                generate(level, chunk);
                generated++;
            }
        }
        return generated;
    }

    /* Marks a chunk made with every work as having had it; notes an old one, and its neighbours, for theirs. */
    private static void onChunkLoad(final ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getChunk() instanceof LevelChunk chunk)) {
            return;
        }
        if (event.isNewChunk()) {
            keep(chunk, new LinkedHashSet<>(declared()));
            return;
        }
        look(level, chunk);
        // A neighbour let go while this chunk was missing can go on now.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                final LevelChunk near = level.getChunkSource().getChunkNow(chunk.getPos().x + dx,
                        chunk.getPos().z + dz);
                if (near != null && near != chunk) {
                    look(level, near);
                }
            }
        }
    }

    private static boolean neighboursLoaded(final ServerLevel level, final ChunkPos at) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (level.getChunkSource().getChunkNow(at.x + dx, at.z + dz) == null) {
                    return false;
                }
            }
        }
        return true;
    }

    /* Runs every work the chunk lacks, in the order declared, and marks each had, even one that failed. */
    private static void generate(final ServerLevel level, final LevelChunk chunk) {
        final Set<ResourceLocation> done = done(chunk);
        final Map<ResourceLocation, IRetroWork> work;
        synchronized (Retrogen.class) {
            work = new LinkedHashMap<>(WORK);
        }
        for (final Map.Entry<ResourceLocation, IRetroWork> one : work.entrySet()) {
            if (done.contains(one.getKey())) {
                continue;
            }
            final RandomSource random = RandomSource.create(level.getSeed() ^ chunk.getPos().toLong() * MIX
                    ^ one.getKey().hashCode());
            try {
                one.getValue().generate(level, chunk, random);
            } catch (final RuntimeException failure) {
                LOGGER.warn("The generation {} failed in the chunk {} of {}", one.getKey(), chunk.getPos(),
                        level.dimension().location(), failure);
            }
            done.add(one.getKey());
        }
        keep(chunk, done);
    }

    private static void keep(final LevelChunk chunk, final Set<ResourceLocation> done) {
        final List<String> ids = new ArrayList<>();
        done.forEach(id -> ids.add(id.toString()));
        chunk.setData(CoreAttachments.RETROGEN, List.copyOf(ids));
        chunk.setUnsaved(true);
    }
}
