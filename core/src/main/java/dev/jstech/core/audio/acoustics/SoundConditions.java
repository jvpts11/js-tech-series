/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio.acoustics;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * When an ambience or a layer of music is heard: in which dimensions and biomes, in what weather, at what time of
 * day, indoors or out, how deep, and near what. What a condition leaves empty holds everywhere, so a file says only
 * what matters to it.
 *
 * @param dimensions the dimensions' ids, or none for any
 * @param biomes     the biomes, each an id or a tag written {@code #namespace:path}, or none for any
 * @param weather    the weathers, or none for any
 * @param fromTime   the time of day it begins, in ticks; with {@code toTime} it may run past midnight
 * @param toTime     the time of day it ends, in ticks; equal to {@code fromTime} for the whole day
 * @param indoors    under a roof, or out under the sky, or either when empty
 * @param minDepth   the least depth below the open sky, in blocks
 * @param maxDepth   the most
 * @param near       what must be near, each kind at least so many blocks of it
 */
public record SoundConditions(Set<String> dimensions, Set<String> biomes, Set<ListeningPlace.Weather> weather,
                              int fromTime, int toTime, Optional<Boolean> indoors, int minDepth, int maxDepth,
                              List<Near> near) {

    /** Anywhere, at any time. */
    public static final SoundConditions ANYWHERE = new SoundConditions(Set.of(), Set.of(), Set.of(), 0, 0,
            Optional.empty(), 0, Integer.MAX_VALUE, List.of());

    public SoundConditions {
        dimensions = Set.copyOf(dimensions);
        biomes = Set.copyOf(biomes);
        weather = Set.copyOf(weather);
        near = List.copyOf(near);
        if (minDepth < 0 || maxDepth < minDepth) {
            throw new IllegalArgumentException("not a depth: " + minDepth + " to " + maxDepth);
        }
    }

    /** Whether it holds at {@code place}. */
    public boolean matches(final ListeningPlace place) {
        if (!this.dimensions.isEmpty() && !this.dimensions.contains(place.dimension())) {
            return false;
        }
        if (!this.biomes.isEmpty() && !this.biomes.contains(place.biome())
                && this.biomes.stream().noneMatch(place.biomeTags()::contains)) {
            return false;
        }
        if (!this.weather.isEmpty() && !this.weather.contains(place.weather())) {
            return false;
        }
        if (!inTime(place.dayTime())) {
            return false;
        }
        if (this.indoors.isPresent() && this.indoors.get() != place.indoors()) {
            return false;
        }
        if (place.depth() < this.minDepth || place.depth() > this.maxDepth) {
            return false;
        }
        return this.near.stream().allMatch(one -> place.count(one.what()) >= one.least());
    }

    /* Whether the time of day falls in its hours, which may run past midnight. */
    private boolean inTime(final int time) {
        final int from = Math.floorMod(this.fromTime, ListeningPlace.DAY);
        final int to = Math.floorMod(this.toTime, ListeningPlace.DAY);
        if (from == to) {
            return true;
        }
        return from < to ? time >= from && time < to : time >= from || time < to;
    }

    /**
     * Something that must be near.
     *
     * @param what   a block's id, or a tag of blocks written {@code #namespace:path}
     * @param radius how far round the listener it is looked for, in blocks
     * @param least  how many blocks of it at least
     */
    public record Near(String what, int radius, int least) {

        public Near {
            if (radius < 1 || least < 1) {
                throw new IllegalArgumentException("near " + what + ": radius " + radius + ", least " + least);
            }
        }
    }
}
