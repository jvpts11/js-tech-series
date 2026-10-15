/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.diagnostic;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.ApiStatus;

/**
 * What each of the series' blocks costs the server: while a measure runs, the time each block entity's tick takes is
 * added up where it stands, and at the end the worst are listed, with their owners, by {@code /jstech cost}. Nothing
 * is measured between measures, so the map costs nothing while nobody looks; a measure lasts some seconds and then
 * stops by itself.
 *
 * <p>The blocks of every mod of the series are measured as they tick, through {@code BlockEntityTickers}; a kit with
 * work of its own in one place, such as a structure, adds it with {@link #add}. The last measure stays, for the map
 * drawn in the world.
 */
public final class CostMap {

    /** The longest a measure may last, in seconds. */
    public static final int LONGEST_SECONDS = 60;

    private static final int TICKS_PER_SECOND = 20;
    private static final Map<Place, Cost> COSTS = new ConcurrentHashMap<>();
    private static final List<Consumer<Measure>> WAITING = new CopyOnWriteArrayList<>();

    private static volatile boolean measuring;
    private static volatile long endsAtTick;
    private static volatile long startedAtTick;
    private static volatile Measure last;

    private CostMap() {
    }

    /** Whether a measure is running. */
    public static boolean measuring() {
        return measuring;
    }

    /**
     * Starts a measure of {@code seconds}, at most {@value #LONGEST_SECONDS}, handing its result to {@code done} when
     * it ends; when one runs already, {@code done} waits for that one instead.
     *
     * @return how many ticks are left of the measure {@code done} waits for
     */
    public static long measure(final MinecraftServer server, final int seconds, final Consumer<Measure> done) {
        WAITING.add(done);
        final long now = server.getTickCount();
        if (!measuring) {
            COSTS.clear();
            startedAtTick = now;
            endsAtTick = now + (long) Math.clamp(seconds, 1, LONGEST_SECONDS) * TICKS_PER_SECOND;
            measuring = true;
        }
        return Math.max(0L, endsAtTick - now);
    }

    /** The last measure that ended, or none before the first. */
    public static Optional<Measure> last() {
        return Optional.ofNullable(last);
    }

    /** Adds {@code nanos} of work done at {@code pos} as {@code kind}, while a measure runs. */
    public static void add(final Level level, final BlockPos pos, final ResourceLocation kind, final long nanos) {
        if (measuring && !level.isClientSide()) {
            COSTS.computeIfAbsent(new Place(level.dimension(), pos.immutable()), place -> new Cost(kind)).add(nanos);
        }
    }

    /**
     * {@code ticker}, measured while a measure runs: what every block entity ticker of the series is wrapped in, and
     * what costs nothing but a look at a flag between measures.
     */
    public static <T extends BlockEntity> BlockEntityTicker<T> measured(final BlockEntityTicker<T> ticker) {
        return (level, pos, state, entity) -> {
            if (!measuring || level.isClientSide()) {
                ticker.tick(level, pos, state, entity);
                return;
            }
            final long start = System.nanoTime();
            try {
                ticker.tick(level, pos, state, entity);
            } finally {
                add(level, pos, kindOf(entity, state), System.nanoTime() - start);
            }
        };
    }

    /** Hands the game's bus the end of each tick, where a measure ends, once, from the Core's constructor. */
    @ApiStatus.Internal
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, event -> {
            if (measuring && event.getServer().getTickCount() >= endsAtTick) {
                finish(event.getServer().getTickCount());
            }
        });
    }

    private static void finish(final long now) {
        measuring = false;
        final long ticks = Math.max(1L, now - startedAtTick);
        final List<Entry> places = new ArrayList<>(COSTS.size());
        COSTS.forEach((place, cost) -> places.add(new Entry(place.dimension(), place.pos(), cost.kind,
                cost.total() / ticks)));
        COSTS.clear();
        places.sort(Comparator.comparingLong(Entry::nanosPerTick).reversed());
        final Measure measure = new Measure(ticks, places);
        last = measure;
        final List<Consumer<Measure>> told = List.copyOf(WAITING);
        WAITING.removeAll(told);
        told.forEach(done -> done.accept(measure));
    }

    private static ResourceLocation kindOf(final BlockEntity entity, final BlockState state) {
        final ResourceLocation type = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(entity.getType());
        return type != null ? type : BuiltInRegistries.BLOCK.getKey(state.getBlock());
    }

    /**
     * What one measure found.
     *
     * @param ticks  how many ticks it lasted
     * @param places what each place cost, the worst first
     */
    public record Measure(long ticks, List<Entry> places) {

        public Measure {
            places = List.copyOf(places);
        }

        /** What every place cost a tick together, in nanoseconds. */
        public long totalNanosPerTick() {
            long sum = 0L;
            for (final Entry entry : this.places) {
                sum += entry.nanosPerTick();
            }
            return sum;
        }

        /** What each kind of block cost a tick, all its places together, the worst first. */
        public List<Kind> kinds() {
            final Map<ResourceLocation, long[]> byKind = new HashMap<>();
            for (final Entry entry : this.places) {
                final long[] sum = byKind.computeIfAbsent(entry.kind(), kind -> new long[2]);
                sum[0] += entry.nanosPerTick();
                sum[1]++;
            }
            final List<Kind> out = new ArrayList<>();
            byKind.forEach((kind, sum) -> out.add(new Kind(kind, sum[0], (int) sum[1])));
            out.sort(Comparator.comparingLong(Kind::nanosPerTick).reversed());
            return out;
        }
    }

    /**
     * What one place cost.
     *
     * @param dimension    the dimension it is in
     * @param pos          where it is
     * @param kind         what stands there: its block entity type, or what a kit named its work
     * @param nanosPerTick what it cost a tick on average, in nanoseconds
     */
    public record Entry(ResourceKey<Level> dimension, BlockPos pos, ResourceLocation kind, long nanosPerTick) {
    }

    /**
     * What one kind of block cost, all its places together.
     *
     * @param kind         the kind
     * @param nanosPerTick what they cost a tick, in nanoseconds
     * @param places       how many places they stand in
     */
    public record Kind(ResourceLocation kind, long nanosPerTick, int places) {
    }

    /* A place in a dimension. */
    private record Place(ResourceKey<Level> dimension, BlockPos pos) {
    }

    /* What one place cost so far, added to on the thread its dimension ticks on. */
    private static final class Cost {

        private final ResourceLocation kind;
        private long nanos;

        private Cost(final ResourceLocation kind) {
            this.kind = kind;
        }

        private synchronized void add(final long more) {
            this.nanos += more;
        }

        private synchronized long total() {
            return this.nanos;
        }
    }
}
