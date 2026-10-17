/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.hazard;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/**
 * What one player knows of the hazards of the land: for each dimension and each mapped hazard, the rate read at each
 * chunk that was measured, in the unit the hazard's rate is shown in, and the game time it was read at. Kept with the
 * player, through death; each dimension and hazard keeps its newest readings, the oldest making way.
 */
public final class HazardMap {

    private final Map<ResourceLocation, Map<ResourceLocation, Map<Long, Reading>>> readings = new HashMap<>();

    /** How many readings a dimension keeps of each hazard. */
    public static final int KEPT = 16_384;
    /** How it is saved: every reading, flat. */
    public static final Codec<HazardMap> CODEC = Saved.CODEC.listOf().xmap(HazardMap::new, HazardMap::saved);

    public HazardMap() {
    }

    private HazardMap(final List<Saved> saved) {
        for (final Saved one : saved) {
            this.put(one.dimension(), one.kind(), one.chunk(), new Reading(one.value(), one.time()));
        }
    }

    /** The reading of {@code kind} at {@code chunk} of {@code dimension}, the chunk's place packed as the game does. */
    public synchronized Optional<Reading> reading(final ResourceLocation dimension, final ResourceLocation kind,
                                                  final long chunk) {
        return Optional.ofNullable(this.readings.getOrDefault(dimension, Map.of()).getOrDefault(kind, Map.of())
                .get(chunk));
    }

    /** Every reading of {@code dimension}, by hazard and chunk. */
    public synchronized Map<ResourceLocation, Map<Long, Reading>> of(final ResourceLocation dimension) {
        final Map<ResourceLocation, Map<Long, Reading>> out = new LinkedHashMap<>();
        this.readings.getOrDefault(dimension, Map.of()).forEach((kind, chunks) -> out.put(kind, Map.copyOf(chunks)));
        return out;
    }

    /** Keeps {@code reading} of {@code kind} at {@code chunk} of {@code dimension}; past the most kept the oldest goes. */
    public synchronized void put(final ResourceLocation dimension, final ResourceLocation kind, final long chunk,
                                 final Reading reading) {
        final Map<Long, Reading> chunks = this.readings.computeIfAbsent(dimension, id -> new HashMap<>())
                .computeIfAbsent(kind, id -> new HashMap<>());
        chunks.put(chunk, reading);
        if (chunks.size() > KEPT) {
            chunks.entrySet().stream().min(Comparator.comparingLong(entry -> entry.getValue().time()))
                    .ifPresent(oldest -> chunks.remove(oldest.getKey()));
        }
    }

    private synchronized List<Saved> saved() {
        final List<Saved> out = new ArrayList<>();
        this.readings.forEach((dimension, kinds) -> kinds.forEach((kind, chunks) -> chunks.forEach((chunk, reading) ->
                out.add(new Saved(dimension, kind, chunk, reading.value(), reading.time())))));
        return out;
    }

    /**
     * A reading.
     *
     * @param value the rate read, in the unit the hazard's rate is shown in
     * @param time  the game time it was read at
     */
    public record Reading(float value, long time) {
    }

    private record Saved(ResourceLocation dimension, ResourceLocation kind, long chunk, float value, long time) {

        private static final Codec<Saved> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("dimension").forGetter(Saved::dimension),
                ResourceLocation.CODEC.fieldOf("kind").forGetter(Saved::kind),
                Codec.LONG.fieldOf("chunk").forGetter(Saved::chunk),
                Codec.FLOAT.fieldOf("value").forGetter(Saved::value),
                Codec.LONG.fieldOf("time").forGetter(Saved::time)
        ).apply(instance, Saved::new));
    }
}
