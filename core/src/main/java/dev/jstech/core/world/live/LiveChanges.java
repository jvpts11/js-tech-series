/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.world.live;

import dev.jstech.core.JsCore;
import dev.jstech.core.data.DataRegistry;
import dev.jstech.core.diagnostic.Section;
import dev.jstech.core.heat.HeatClimate;
import dev.jstech.core.log.CoreLogs;
import dev.jstech.core.time.Season;
import dev.jstech.core.time.WorldCalendar;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * The world changing by itself as the climate and the weather say ({@link LiveChange}), and the biome of a place
 * changed by a mod, as terraforming or a climate shifting would. The changes are declared in code by their mods or
 * written by data packs under {@code data/<namespace>/jstech/live_changes/}; once a second, a column of each chunk near
 * each player is looked at, within the work's budget. The Core declares none itself: a world changes only as its mods
 * say.
 *
 * <pre>{@code
 * LiveChanges.change(rl("ice_thaws")).test(new BlockMatchTest(Blocks.ICE)).becomes(Blocks.WATER.defaultBlockState())
 *         .warmerThan(275).sky().chance(0.2).declare();
 * LiveChanges.setBiome(level, BoundingBox.fromCorners(from, to), level.registryAccess()
 *         .registryOrThrow(Registries.BIOME).getHolderOrThrow(MyBiomes.SCORCHED));
 * }</pre>
 */
public final class LiveChanges {

    /** The changes the data packs write, by id. */
    public static final DataRegistry<LiveChange> FILES = DataRegistry.builder(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "live_changes"), "jstech/live_changes",
            LiveChange.CODEC).register();
    /** The rounds of changes, timed, watched and kept to their budget. */
    public static final Section SECTION = Section.builder(ResourceLocation.fromNamespaceAndPath(JsCore.MODID,
            "live_changes"), CoreLogs.WORLD).budget(Duration.ofMillis(1)).declare();
    /** How far round each player the chunks are looked at, in chunks. */
    public static final int REACH = 6;

    private static final Map<ResourceLocation, LiveChange> DECLARED = new LinkedHashMap<>();
    /* How often a round looks at the chunks, in ticks. */
    private static final int EVERY = 20;
    private static final int ALL = Block.UPDATE_ALL;

    private LiveChanges() {
    }

    /** Hands the game's bus the rounds, once, from the Core's constructor. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(LevelTickEvent.Post.class, event -> {
            if (event.getLevel() instanceof ServerLevel level && level.getGameTime() % EVERY == 0
                    && !level.players().isEmpty() && SECTION.due(level.getGameTime() / EVERY)) {
                SECTION.run(() -> round(level));
            }
        });
    }

    /** Starts declaring the change {@code id}. */
    public static LiveChangeBuilder change(final ResourceLocation id) {
        return new LiveChangeBuilder(id);
    }

    /** Every change in {@code level}'s side, by id: the declared ones, then the files, which replace or add. */
    public static Map<ResourceLocation, LiveChange> all(final Level level) {
        final Map<ResourceLocation, LiveChange> byId;
        synchronized (DECLARED) {
            byId = new TreeMap<>(DECLARED);
        }
        byId.putAll(FILES.entries(level));
        return byId;
    }

    /**
     * Looks at the column {@code x, z} of {@code level}, its chunk loaded, and makes the first change that holds
     * there: what each round does to one column of each chunk.
     *
     * @return whether a change was made
     */
    public static boolean column(final ServerLevel level, final int x, final int z, final RandomSource random) {
        final BlockPos top = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1, z);
        double kelvin = Double.NaN;
        Season season = null;
        for (final LiveChange change : all(level).values()) {
            if (!change.dimensions().isEmpty() && !change.dimensions().contains(level.dimension())) {
                continue;
            }
            final BlockPos at = change.at() == LiveChange.Where.TOP ? top : top.above();
            if (!change.test().test(level.getBlockState(at), random)
                    || change.on().isPresent() && !change.on().get().test(level.getBlockState(at.below()), random)
                    || change.sky() && !level.canSeeSky(at.above())
                    || change.rain().isPresent() && change.rain().get() != level.isRainingAt(at.above())
                    || change.basin() && !basin(level, at, change.becomes())) {
                continue;
            }
            if (change.minKelvin().isPresent() || change.maxKelvin().isPresent()) {
                kelvin = Double.isNaN(kelvin) ? HeatClimate.ambient(level, at) : kelvin;
                if (change.minKelvin().isPresent() && kelvin < change.minKelvin().get()
                        || change.maxKelvin().isPresent() && kelvin > change.maxKelvin().get()) {
                    continue;
                }
            }
            if (!change.seasons().isEmpty()) {
                season = season == null ? WorldCalendar.now(level).season() : season;
                if (!change.seasons().contains(season)) {
                    continue;
                }
            }
            if (random.nextDouble() < change.chance()) {
                level.setBlock(at, change.becomes(), ALL);
                return true;
            }
        }
        return false;
    }

    /**
     * Changes the biome of every place of {@code level} inside {@code box} to {@code biome}, loading its chunks, and
     * sends the players who see them the new biomes: the grass, the water and the sky change colour at once, and the
     * climate there is the new biome's from then on.
     *
     * @return how many chunks changed
     */
    public static int setBiome(final ServerLevel level, final BoundingBox box, final Holder<Biome> biome) {
        final BoundingBox quarts = new BoundingBox(QuartPos.fromBlock(box.minX()), QuartPos.fromBlock(box.minY()),
                QuartPos.fromBlock(box.minZ()), QuartPos.fromBlock(box.maxX()), QuartPos.fromBlock(box.maxY()),
                QuartPos.fromBlock(box.maxZ()));
        final List<ChunkAccess> changed = new ArrayList<>();
        for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++) {
            for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) {
                final LevelChunk chunk = level.getChunk(cx, cz);
                final BiomeResolver resolver = (qx, qy, qz, sampler) -> quarts.isInside(qx, qy, qz) ? biome
                        : chunk.getNoiseBiome(qx, qy, qz);
                chunk.fillBiomesFromNoise(resolver, level.getChunkSource().randomState().sampler());
                chunk.setUnsaved(true);
                changed.add(chunk);
            }
        }
        level.getChunkSource().chunkMap.resendBiomesForChunks(changed);
        return changed.size();
    }

    /* Looks at one column of each loaded chunk near each player, until the round's share of the tick is spent. */
    private static void round(final ServerLevel level) {
        final LongLinkedOpenHashSet chunks = new LongLinkedOpenHashSet();
        for (final ServerPlayer player : level.players()) {
            final ChunkPos at = player.chunkPosition();
            for (int dx = -REACH; dx <= REACH; dx++) {
                for (int dz = -REACH; dz <= REACH; dz++) {
                    chunks.add(ChunkPos.asLong(at.x + dx, at.z + dz));
                }
            }
        }
        final RandomSource random = level.getRandom();
        for (final long packed : chunks) {
            final int cx = ChunkPos.getX(packed);
            final int cz = ChunkPos.getZ(packed);
            if (level.getChunkSource().getChunkNow(cx, cz) != null) {
                column(level, (cx << 4) + random.nextInt(16), (cz << 4) + random.nextInt(16), random);
            }
        }
    }

    /* Whether each block beside {@code at} holds water in: solid on the side that faces it, or already the same. */
    private static boolean basin(final ServerLevel level, final BlockPos at, final BlockState becomes) {
        for (final Direction side : Direction.Plane.HORIZONTAL) {
            final BlockPos beside = at.relative(side);
            final BlockState there = level.getBlockState(beside);
            if (!there.is(becomes.getBlock()) && !there.isFaceSturdy(level, beside, side.getOpposite())) {
                return false;
            }
        }
        return true;
    }

    /** A change being declared; it starts as the topmost block of a column, anywhere, in any weather. */
    public static final class LiveChangeBuilder {

        private final ResourceLocation id;
        private final List<Season> seasons = new ArrayList<>();
        private final List<ResourceKey<Level>> dimensions = new ArrayList<>();
        private RuleTest test;
        private BlockState becomes;
        private LiveChange.Where at = LiveChange.Where.TOP;
        private Optional<RuleTest> on = Optional.empty();
        private double chance = 1;
        private Optional<Double> minKelvin = Optional.empty();
        private Optional<Double> maxKelvin = Optional.empty();
        private Optional<Boolean> rain = Optional.empty();
        private boolean sky;
        private boolean basin;

        private LiveChangeBuilder(final ResourceLocation id) {
            this.id = Objects.requireNonNull(id, "id");
        }

        /** The block that changes. */
        public LiveChangeBuilder test(final RuleTest block) {
            this.test = block;
            return this;
        }

        public LiveChangeBuilder becomes(final BlockState into) {
            this.becomes = into;
            return this;
        }

        /** It is the block just above the column's topmost that changes. */
        public LiveChangeBuilder above() {
            this.at = LiveChange.Where.ABOVE;
            return this;
        }

        /** The block under it must be {@code below}. */
        public LiveChangeBuilder on(final RuleTest below) {
            this.on = Optional.of(below);
            return this;
        }

        /** How often it changes each time its column is looked at, 0 to 1. */
        public LiveChangeBuilder chance(final double share) {
            this.chance = share;
            return this;
        }

        /** Only where it is at least {@code kelvin} warm. */
        public LiveChangeBuilder warmerThan(final double kelvin) {
            this.minKelvin = Optional.of(kelvin);
            return this;
        }

        /** Only where it is at most {@code kelvin} warm. */
        public LiveChangeBuilder colderThan(final double kelvin) {
            this.maxKelvin = Optional.of(kelvin);
            return this;
        }

        /** Only while it rains there, or only while it does not. */
        public LiveChangeBuilder raining(final boolean wet) {
            this.rain = Optional.of(wet);
            return this;
        }

        /** Only under the open sky. */
        public LiveChangeBuilder sky() {
            this.sky = true;
            return this;
        }

        /** Only in a basin, each block beside it solid or already what it becomes. */
        public LiveChangeBuilder basin() {
            this.basin = true;
            return this;
        }

        /** Only in {@code season}; add more for several. */
        public LiveChangeBuilder in(final Season season) {
            this.seasons.add(season);
            return this;
        }

        /** Only in {@code dimension}; add more for several. */
        public LiveChangeBuilder in(final ResourceKey<Level> dimension) {
            this.dimensions.add(dimension);
            return this;
        }

        /**
         * Declares the change, and gives its id.
         *
         * @throws IllegalStateException when it names no block or nothing it becomes
         */
        public ResourceLocation declare() {
            if (test == null || becomes == null) {
                throw new IllegalStateException("the change " + id + " names its block and what it becomes");
            }
            synchronized (DECLARED) {
                DECLARED.put(id, new LiveChange(test, becomes, at, on, chance, minKelvin, maxKelvin, rain, sky, basin,
                        seasons, dimensions));
            }
            return id;
        }
    }
}
