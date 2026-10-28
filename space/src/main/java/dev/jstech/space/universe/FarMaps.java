/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.universe;

import dev.jstech.space.api.SpaceWorlds;
import dev.jstech.space.network.FarMapPayload;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The far map of the ground round a player on the way up from a world to its space: the ground out to the horizon as
 * the world's own generator makes it, each pixel's biome and the height of its ground, worked out away from the
 * server's thread from the generator's noise alone, so no land is generated for it, in the rings {@link FarRings}
 * lays out. A player nearing the build limit is sent every ring round where they are, and a ring again once they have
 * gone a quarter of its width from its middle.
 */
public final class FarMaps {

    private static final int SIZE = FarRings.SIZE;
    private static final List<Integer> RING_BLOCKS = FarRings.BLOCKS;
    /* How far under the build limit a climber is sent their maps, so they are there as they rise above it. */
    private static final int AHEAD = 96;
    /* How often a player is looked at, in ticks. */
    private static final int EVERY = 20;
    /* The heights are worked out on every fourth pixel and spread between, as the generator's own noise is smooth. */
    private static final int HEIGHT_EVERY = 4;
    /* How strong the generator's first guess at its ground must be for there to be ground, and how far apart in height
     * it guesses, as the generator's own first look at a column does. */
    private static final double GROUND = 0.390625;
    private static final int GUESS_STEP = 8;
    /* Where each player's last rings stand, by dimension and middle; one being drawn is there already. */
    private static final Map<UUID, Sent[]> SENT = new ConcurrentHashMap<>();

    private FarMaps() {
    }

    /** Hands the game's bus what sends the far maps, once, from the mod's constructor. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(PlayerTickEvent.Post.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player && player.tickCount % EVERY == 0) {
                look(player);
            }
        });
    }

    /** Draws ring {@code ring} of the far map round ({@code x}, {@code z}) of {@code level}, on its pixels' grid. */
    public static FarMapPayload draw(final ServerLevel level, final int ring, final int x, final int z) {
        final int blocks = RING_BLOCKS.get(ring);
        final int centreX = Math.floorDiv(x, blocks) * blocks;
        final int centreZ = Math.floorDiv(z, blocks) * blocks;
        final double[] heights = heights(level, centreX, centreZ, blocks);
        final int sea = seaOf(level);
        final BiomeSource source = level.getChunkSource().getGenerator().getBiomeSource();
        final Climate.Sampler sampler = level.getChunkSource().randomState().sampler();
        final Map<Holder<Biome>, Integer> indices = new HashMap<>();
        final List<ResourceLocation> biomes = new ArrayList<>();
        final byte[] pixels = new byte[SIZE * SIZE];
        final byte[] packed = new byte[SIZE * SIZE];
        for (int row = 0; row < SIZE; row++) {
            final int bz = centreZ + (row - SIZE / 2) * blocks + blocks / 2;
            for (int column = 0; column < SIZE; column++) {
                final int bx = centreX + (column - SIZE / 2) * blocks + blocks / 2;
                final double ground = heights[row * SIZE + column];
                // The biome where the ground or the sea's top is, not of the caves under it.
                final int y = QuartPos.fromBlock((int) Math.max(ground, sea));
                final Holder<Biome> biome = source.getNoiseBiome(QuartPos.fromBlock(bx), y, QuartPos.fromBlock(bz),
                        sampler);
                final int index = indices.computeIfAbsent(biome, unused -> {
                    biomes.add(biome.unwrapKey().map(ResourceKey::location).orElse(Biomes.PLAINS.location()));
                    return biomes.size() - 1;
                });
                pixels[row * SIZE + column] = (byte) index;
                packed[row * SIZE + column] = FarMapPayload.height(ground);
            }
        }
        return new FarMapPayload(ring, centreX, centreZ, blocks, SIZE, sea, biomes, pixels, packed);
    }

    /**
     * The height of {@code level}'s sea, as its generator lays it: the level itself says the game's usual height for
     * any world, the flat one's too.
     */
    public static int seaOf(final ServerLevel level) {
        return level.getChunkSource().getGenerator().getSeaLevel();
    }

    /*
     * The ground's height at every pixel of a ring, worked out on every fourth pixel and spread smoothly between: for
     * the game's own noise generator, from its first guess at its terrain, the highest point of each column its density
     * first holds ground; for any other, the height it says its ground is.
     */
    private static double[] heights(final ServerLevel level, final int centreX, final int centreZ, final int blocks) {
        final ChunkGenerator generator = level.getChunkSource().getGenerator();
        final RandomState noise = level.getChunkSource().randomState();
        final DensityFunction guess = noise.router().initialDensityWithoutJaggedness();
        final boolean guessed = generator instanceof NoiseBasedChunkGenerator;
        final int bottom = level.getMinBuildHeight();
        final int top = level.getMaxBuildHeight();
        final int grid = SIZE / HEIGHT_EVERY + 1;
        final double[] coarse = new double[grid * grid];
        for (int gz = 0; gz < grid; gz++) {
            final int bz = centreZ + (gz * HEIGHT_EVERY - SIZE / 2) * blocks + blocks / 2;
            for (int gx = 0; gx < grid; gx++) {
                final int bx = centreX + (gx * HEIGHT_EVERY - SIZE / 2) * blocks + blocks / 2;
                coarse[gz * grid + gx] = guessed ? surface(guess, bx, bz, bottom, top)
                        : generator.getBaseHeight(bx, bz, Heightmap.Types.WORLD_SURFACE_WG, level, noise) - 1;
            }
        }
        final double[] out = new double[SIZE * SIZE];
        for (int row = 0; row < SIZE; row++) {
            final int gz = row / HEIGHT_EVERY;
            final double tz = (double) (row % HEIGHT_EVERY) / HEIGHT_EVERY;
            for (int column = 0; column < SIZE; column++) {
                final int gx = column / HEIGHT_EVERY;
                final double tx = (double) (column % HEIGHT_EVERY) / HEIGHT_EVERY;
                final double north = coarse[gz * grid + gx] * (1 - tx) + coarse[gz * grid + gx + 1] * tx;
                final double south = coarse[(gz + 1) * grid + gx] * (1 - tx) + coarse[(gz + 1) * grid + gx + 1] * tx;
                out[row * SIZE + column] = north * (1 - tz) + south * tz;
            }
        }
        return out;
    }

    /* The highest block of the column at ({@code x}, {@code z}) the generator's first guess holds ground in. */
    private static double surface(final DensityFunction guess, final int x, final int z, final int bottom,
                                  final int top) {
        int above = top;
        for (int y = top - GUESS_STEP; y >= bottom; y -= GUESS_STEP) {
            if (guess.compute(new DensityFunction.SinglePointContext(x, y, z)) > GROUND) {
                // Halved down to the block between the last height without ground and this one with it.
                int low = y;
                int high = above;
                while (high - low > 1) {
                    final int middle = (low + high) / 2;
                    if (guess.compute(new DensityFunction.SinglePointContext(x, middle, z)) > GROUND) {
                        low = middle;
                    } else {
                        high = middle;
                    }
                }
                return low;
            }
            above = y;
        }
        return bottom;
    }

    /* A player nearing the build limit of a world drawn in its system's space is sent each ring round them if due. */
    private static void look(final ServerPlayer player) {
        final ServerLevel level = player.serverLevel();
        if (player.getY() < level.getMaxBuildHeight() - AHEAD || !drawnInSpace(level)) {
            return;
        }
        final Sent[] sent = SENT.computeIfAbsent(player.getUUID(), unused -> new Sent[RING_BLOCKS.size()]);
        for (int ring = 0; ring < RING_BLOCKS.size(); ring++) {
            final Sent last = sent[ring];
            final int moved = SIZE * RING_BLOCKS.get(ring) / 4;
            if (last != null && last.dimension().equals(level.dimension())
                    && Math.abs(player.getX() - last.x()) < moved && Math.abs(player.getZ() - last.z()) < moved) {
                continue;
            }
            final int x = player.getBlockX();
            final int z = player.getBlockZ();
            final int which = ring;
            sent[ring] = new Sent(level.dimension(), x, z);
            CompletableFuture.supplyAsync(() -> draw(level, which, x, z), Util.backgroundExecutor())
                    .thenAcceptAsync(map -> {
                        if (!player.hasDisconnected() && player.level() == level) {
                            PacketDistributor.sendToPlayer(player, map);
                        }
                    }, level.getServer());
        }
    }

    /* Whether {@code level} is a world drawn as a sphere in the Solar System's space. */
    private static boolean drawnInSpace(final ServerLevel level) {
        for (final SolarBody body : SolarBody.values()) {
            final boolean here = SpaceWorlds.get(level, SolarSystemWorlds.worldOf(body))
                    .filter(world -> world.dimension().equals(level.dimension())).isPresent();
            if (here) {
                return true;
            }
        }
        return false;
    }

    /* Where a player's last ring stands. */
    private record Sent(ResourceKey<Level> dimension, int x, int z) {
    }
}
