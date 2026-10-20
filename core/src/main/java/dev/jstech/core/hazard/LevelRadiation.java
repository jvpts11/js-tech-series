/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.hazard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

/**
 * A dimension's sources of radiation, by the chunk they are in: each place a block gives off radiation, and how much of
 * each kind. Never saved: a block that gives off radiation says so again as it loads.
 */
public final class LevelRadiation {

    private final Map<Long, Map<Long, Map<RadiationKind, Double>>> byChunk = new HashMap<>();

    public LevelRadiation() {
    }

    /** Sets how much of {@code kind} the block at {@code pos} gives off; nothing or less stops it. */
    public synchronized void set(final BlockPos pos, final RadiationKind kind, final double strength) {
        final long chunk = ChunkPos.asLong(pos);
        if (strength <= 0) {
            final Map<Long, Map<RadiationKind, Double>> places = this.byChunk.get(chunk);
            if (places == null) {
                return;
            }
            final Map<RadiationKind, Double> kinds = places.get(pos.asLong());
            if (kinds != null) {
                kinds.remove(kind);
                if (kinds.isEmpty()) {
                    places.remove(pos.asLong());
                }
            }
            if (places.isEmpty()) {
                this.byChunk.remove(chunk);
            }
            return;
        }
        this.byChunk.computeIfAbsent(chunk, key -> new HashMap<>()).computeIfAbsent(pos.asLong(),
                key -> new HashMap<>()).put(kind, strength);
    }

    /** Takes away everything the block at {@code pos} gives off, and gives it back, by kind; empty for none. */
    public synchronized Map<RadiationKind, Double> takeAt(final BlockPos pos) {
        final long chunk = ChunkPos.asLong(pos);
        final Map<Long, Map<RadiationKind, Double>> places = this.byChunk.get(chunk);
        if (places == null) {
            return Map.of();
        }
        final Map<RadiationKind, Double> kinds = places.remove(pos.asLong());
        if (places.isEmpty()) {
            this.byChunk.remove(chunk);
        }
        return kinds == null ? Map.of() : kinds;
    }

    /** How much of {@code kind} the block at {@code pos} gives off. */
    public synchronized double strength(final BlockPos pos, final RadiationKind kind) {
        final Map<Long, Map<RadiationKind, Double>> places = this.byChunk.get(ChunkPos.asLong(pos));
        if (places == null) {
            return 0;
        }
        return places.getOrDefault(pos.asLong(), Map.of()).getOrDefault(kind, 0.0);
    }

    /** Every source of {@code kind} in the chunks from {@code (fromX, fromZ)} to {@code (toX, toZ)}. */
    public synchronized List<Source> in(final int fromX, final int fromZ, final int toX, final int toZ,
                                        final RadiationKind kind) {
        final List<Source> out = new ArrayList<>();
        for (int x = fromX; x <= toX; x++) {
            for (int z = fromZ; z <= toZ; z++) {
                final Map<Long, Map<RadiationKind, Double>> places = this.byChunk.get(ChunkPos.asLong(x, z));
                if (places == null) {
                    continue;
                }
                places.forEach((pos, kinds) -> {
                    final Double strength = kinds.get(kind);
                    if (strength != null) {
                        out.add(new Source(BlockPos.of(pos), strength));
                    }
                });
            }
        }
        return out;
    }

    /** Forgets every source in the chunk {@code chunk}, as it unloads. */
    public synchronized void forget(final ChunkPos chunk) {
        this.byChunk.remove(chunk.toLong());
    }

    /**
     * A place that gives off radiation.
     *
     * @param pos      the block
     * @param strength how much, at a block from it
     */
    public record Source(BlockPos pos, double strength) {
    }
}
