/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.hazard;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * The pollution kit at work: every kind any mod declares, given off by blocks onto the chunk they stand in. Each kind
 * spreads and fades as its field does; every few seconds a few columns of each polluted chunk the game has loaded are
 * looked at, the plants on them taking their share up and each of the kind's effects done there; a body breathing it
 * builds up its hazard from the chunk's amount; and where a kind says so, the land withers under enough of it.
 *
 * <pre>{@code
 * Pollution.emit(level, chimneyPos, MyPollution.SOOT, 40);           // each second the chimney smokes
 * double here = Pollution.at(level, new ChunkPos(pos), MyPollution.SOOT);
 * }</pre>
 */
public final class Pollution {

    private static final Map<ResourceLocation, PollutionKind> DECLARED = new LinkedHashMap<>();
    /* How often polluted chunks are looked at, in ticks; how many columns of each, and how many chunks a time. */
    private static final int SAMPLE_EVERY = 100;
    private static final int COLUMNS = 4;
    private static final int CHUNKS_A_TIME = 64;
    /* Where each dimension's look at each kind's chunks stopped, so the next goes on from there. */
    private static final Map<ResourceKey<Level>, Map<PollutionKind, Integer>> CURSORS = new ConcurrentHashMap<>();

    private Pollution() {
    }

    /** Declares {@code kind}, from a mod's constructor: its hazard and its withering hooked up. */
    public static PollutionKind declare(final PollutionKind kind) {
        synchronized (DECLARED) {
            if (DECLARED.putIfAbsent(kind.id(), kind) != null) {
                throw new IllegalStateException("the pollution " + kind.id() + " is declared twice");
            }
        }
        kind.felt().ifPresent(hazard -> hazard.addSource((body, level) -> kind.enabled()
                ? kind.field().get(level, body.chunkPosition()) : 0));
        kind.withersAt().ifPresent(amount -> Withering.declare((level, pos) -> kind.enabled()
                ? kind.field().get(level, new ChunkPos(pos)) / amount : 0));
        return kind;
    }

    /** The kind {@code id}, if one is declared. */
    public static Optional<PollutionKind> find(final ResourceLocation id) {
        synchronized (DECLARED) {
            return Optional.ofNullable(DECLARED.get(id));
        }
    }

    /** Every kind declared, in the order they were. */
    public static Collection<PollutionKind> kinds() {
        synchronized (DECLARED) {
            return List.copyOf(DECLARED.values());
        }
    }

    /** Hands the game's bus the looks at the polluted chunks, once, from the Core's constructor. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(LevelTickEvent.Post.class, event -> {
            if (event.getLevel() instanceof ServerLevel level && level.getGameTime() % SAMPLE_EVERY == 0) {
                for (final PollutionKind kind : kinds()) {
                    if (kind.enabled() && (kind.plantsTakeUp() > 0 || !kind.effects().isEmpty())) {
                        sample(level, kind);
                    }
                }
            }
        });
    }

    /** Puts {@code amount} of {@code kind}, in its unit, on the chunk {@code pos} is in: what a block gives off. */
    public static void emit(final ServerLevel level, final BlockPos pos, final PollutionKind kind,
                            final double amount) {
        if (kind.enabled()) {
            kind.field().add(level, new ChunkPos(pos), (float) amount);
        }
    }

    /** How much of {@code kind} lies on {@code chunk} of {@code level}, in its unit. */
    public static double at(final ServerLevel level, final ChunkPos chunk, final PollutionKind kind) {
        return kind.field().get(level, chunk);
    }

    /**
     * Takes up to {@code most} of {@code kind}, in its unit, from the chunk {@code pos} is in, as a scrubber or a
     * filter does ({@link PollutionCleaner}); how much it took, never more than lay there.
     */
    public static double takeUp(final ServerLevel level, final BlockPos pos, final PollutionKind kind,
                                final double most) {
        if (most <= 0) {
            return 0;
        }
        final ChunkPos chunk = new ChunkPos(pos);
        final double taken = Math.min(most, kind.field().get(level, chunk));
        if (taken > 0) {
            kind.field().add(level, chunk, (float) -taken);
        }
        return Math.max(0, taken);
    }

    /**
     * Looks at {@code chunk} of {@code level} for {@code kind} now: its plants take their share up and its effects are
     * done at a few columns. What the regular look does; for a test or a command.
     */
    public static void lookAt(final ServerLevel level, final ChunkPos chunk, final PollutionKind kind) {
        if (level.getChunkSource().getChunkNow(chunk.x, chunk.z) == null) {
            return;
        }
        final double amount = kind.field().get(level, chunk);
        if (amount <= 0) {
            return;
        }
        final RandomSource random = level.getRandom();
        int plants = 0;
        for (int i = 0; i < COLUMNS; i++) {
            final int x = chunk.getMinBlockX() + random.nextInt(16);
            final int z = chunk.getMinBlockZ() + random.nextInt(16);
            final BlockPos top = new BlockPos(x, level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1, z);
            if (growsPlants(level.getBlockState(top))) {
                plants++;
            }
            for (final IPollutionEffect effect : kind.effects()) {
                effect.at(level, top, amount);
            }
        }
        if (plants > 0 && kind.plantsTakeUp() > 0) {
            kind.field().add(level, chunk, (float) (-amount * kind.plantsTakeUp() * plants / COLUMNS));
        }
    }

    /** Whether {@code state} is something growing that takes pollution up: leaves, grass, flowers, crops. */
    public static boolean growsPlants(final BlockState state) {
        return state.is(BlockTags.LEAVES) || state.is(BlockTags.FLOWERS) || state.is(BlockTags.SAPLINGS)
                || state.is(BlockTags.CROPS) || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.SHORT_GRASS)
                || state.is(Blocks.TALL_GRASS) || state.is(Blocks.FERN) || state.is(Blocks.MOSS_BLOCK);
    }

    /* A look at the next of {@code kind}'s chunks of {@code level}, a few at a time, round and round. */
    private static void sample(final ServerLevel level, final PollutionKind kind) {
        final long[] chunks = kind.field().chunks(level);
        if (chunks.length == 0) {
            return;
        }
        final Map<PollutionKind, Integer> cursors = CURSORS.computeIfAbsent(level.dimension(),
                dimension -> new ConcurrentHashMap<>());
        int at = cursors.getOrDefault(kind, 0);
        for (int i = 0; i < Math.min(CHUNKS_A_TIME, chunks.length); i++) {
            at = (at + 1) % chunks.length;
            lookAt(level, new ChunkPos(chunks[at]), kind);
        }
        cursors.put(kind, at);
    }
}
