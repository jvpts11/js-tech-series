/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.world.field;

import com.mojang.serialization.Codec;
import dev.jstech.core.state.CoreState;
import dev.jstech.core.state.DimensionState;
import dev.jstech.core.task.Task;
import dev.jstech.core.task.Tasks;
import it.unimi.dsi.fastutil.longs.Long2FloatMap;
import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * A value over the chunks of each dimension that spreads to the neighbouring chunks and fades: fallout, pollution, a
 * cloud of spores. A mod adds to it where its source is and reads it where it matters; every few ticks it moves on a
 * step ({@link FieldMath}), worked out away from the server's thread, and a synced field is sent to the players near
 * enough to see it. Its values are kept with each dimension. Declared with {@link ChunkFields#field}.
 */
public final class ChunkField {

    private final ResourceLocation id;
    private final double spread;
    private final double decay;
    private final float least;
    private final int every;
    private final boolean synced;
    private final DimensionState<Map<String, Float>> state;
    private final Map<ResourceKey<Level>, Long2FloatOpenHashMap> live = new ConcurrentHashMap<>();
    private final Map<ResourceKey<Level>, Long2FloatOpenHashMap> addedMeanwhile = new ConcurrentHashMap<>();
    private final Map<ResourceKey<Level>, Task<Long2FloatOpenHashMap>> stepping = new ConcurrentHashMap<>();

    private static final Codec<Map<String, Float>> VALUES = Codec.unboundedMap(Codec.STRING, Codec.FLOAT);

    ChunkField(final ResourceLocation id, final double spread, final double decay, final float least,
               final int every, final boolean synced) {
        this.id = id;
        this.spread = spread;
        this.decay = decay;
        this.least = least;
        this.every = every;
        this.synced = synced;
        this.state = CoreState.builder(ResourceLocation.fromNamespaceAndPath(id.getNamespace(),
                "field_" + id.getPath()), VALUES, Map.<String, Float>of()).dimension();
    }

    public ResourceLocation id() {
        return this.id;
    }

    /** The share of each chunk's value handed to its neighbours each step. */
    public double spread() {
        return this.spread;
    }

    /** The share of everything that fades each step. */
    public double decay() {
        return this.decay;
    }

    /** Every how many ticks it moves on a step; 0 for a field moved only by {@link #stepNow}. */
    public int every() {
        return this.every;
    }

    /** Whether it is sent to the players near enough to see it. */
    public boolean synced() {
        return this.synced;
    }

    /** Its value at {@code chunk} of {@code level}; 0 where it has none. */
    public float get(final ServerLevel level, final ChunkPos chunk) {
        return values(level).get(chunk.toLong());
    }

    /** Adds {@code amount} at {@code chunk} of {@code level}; a negative amount takes away, never below nothing. */
    public void add(final ServerLevel level, final ChunkPos chunk, final float amount) {
        addTo(values(level), chunk.toLong(), amount);
        final Long2FloatOpenHashMap meanwhile = this.addedMeanwhile.get(level.dimension());
        if (meanwhile != null) {
            meanwhile.addTo(chunk.toLong(), amount);
        }
    }

    /** How many chunks of {@code level} hold some of it. */
    public int size(final ServerLevel level) {
        return values(level).size();
    }

    /** The sum of it over {@code level}. */
    public double total(final ServerLevel level) {
        double sum = 0;
        for (final float value : values(level).values()) {
            sum += value;
        }
        return sum;
    }

    /**
     * Its values round {@code centre}, {@code radius} chunks out each way, row by row along x: what a player near it is
     * sent.
     */
    public float[] near(final ServerLevel level, final ChunkPos centre, final int radius) {
        final Long2FloatOpenHashMap values = values(level);
        final int side = radius * 2 + 1;
        final float[] out = new float[side * side];
        for (int dz = -radius; dz <= radius; dz++) {
            for (int dx = -radius; dx <= radius; dx++) {
                out[(dz + radius) * side + dx + radius] = values.get(ChunkPos.asLong(centre.x + dx, centre.z + dz));
            }
        }
        return out;
    }

    /** Moves it on a step in {@code level} now, on the calling thread: what a test or a command does. */
    public void stepNow(final ServerLevel level) {
        final Task<Long2FloatOpenHashMap> under = this.stepping.remove(level.dimension());
        if (under != null) {
            under.cancel();
            this.addedMeanwhile.remove(level.dimension());
        }
        this.live.put(level.dimension(), stepped(values(level)));
        keep(level);
    }

    /* Starts a step away from the server's thread, unless one is under way; what was added meanwhile is kept. */
    void stepAway(final ServerLevel level) {
        if (this.stepping.containsKey(level.dimension()) || values(level).isEmpty()) {
            return;
        }
        final Long2FloatOpenHashMap snapshot = values(level).clone();
        this.addedMeanwhile.put(level.dimension(), new Long2FloatOpenHashMap());
        this.stepping.put(level.dimension(), Tasks.onServer(() -> stepped(snapshot), next -> {
                    this.stepping.remove(level.dimension());
                    final Long2FloatOpenHashMap meanwhile = this.addedMeanwhile.remove(level.dimension());
                    if (meanwhile != null) {
                        for (final Long2FloatMap.Entry added : meanwhile.long2FloatEntrySet()) {
                            addTo(next, added.getLongKey(), added.getFloatValue());
                        }
                    }
                    this.live.put(level.dimension(), next);
                    keep(level);
                }, failure -> {
                    this.stepping.remove(level.dimension());
                    this.addedMeanwhile.remove(level.dimension());
                }));
    }

    /* Writes the values of {@code level} to its saved data. */
    void keep(final ServerLevel level) {
        final Map<String, Float> saved = new HashMap<>();
        for (final Long2FloatMap.Entry entry : values(level).long2FloatEntrySet()) {
            saved.put(Long.toString(entry.getLongKey()), entry.getFloatValue());
        }
        this.state.set(level, Map.copyOf(saved));
    }

    /* Forgets what is kept in memory for {@code dimension}, its values saved: as it unloads, or the server stops. */
    void forget(final ResourceKey<Level> dimension) {
        final Task<Long2FloatOpenHashMap> under = this.stepping.remove(dimension);
        if (under != null) {
            under.cancel();
        }
        this.addedMeanwhile.remove(dimension);
        this.live.remove(dimension);
    }

    DimensionState<Map<String, Float>> state() {
        return this.state;
    }

    /* The values of {@code level}, read from its saved data the first time. */
    private Long2FloatOpenHashMap values(final ServerLevel level) {
        return this.live.computeIfAbsent(level.dimension(), dimension -> {
            final Long2FloatOpenHashMap read = new Long2FloatOpenHashMap();
            this.state.get(level).forEach((packed, value) -> {
                final @Nullable Long at = parse(packed);
                if (at != null) {
                    read.put(at.longValue(), value.floatValue());
                }
            });
            return read;
        });
    }

    /* The values after a step, what falls below the least worth keeping gone. */
    private Long2FloatOpenHashMap stepped(final Long2FloatOpenHashMap now) {
        final long[] chunks = now.keySet().toLongArray();
        final float[] amounts = new float[chunks.length];
        for (int i = 0; i < chunks.length; i++) {
            amounts[i] = now.get(chunks[i]);
        }
        final Long2FloatOpenHashMap next = new Long2FloatOpenHashMap(chunks.length * 2);
        FieldMath.step(chunks, amounts, this.spread, this.decay, next::addTo);
        next.long2FloatEntrySet().removeIf(entry -> entry.getFloatValue() < this.least);
        return next;
    }

    private static void addTo(final Long2FloatOpenHashMap values, final long at, final float amount) {
        final float next = values.get(at) + amount;
        if (next <= 0) {
            values.remove(at);
        } else {
            values.put(at, next);
        }
    }

    private static @Nullable Long parse(final String packed) {
        try {
            return Long.parseLong(packed);
        } catch (final NumberFormatException wrong) {
            return null;
        }
    }
}
