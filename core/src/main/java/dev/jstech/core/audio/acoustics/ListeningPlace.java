/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio.acoustics;

import dev.jstech.core.id.IStableName;
import java.util.Map;
import java.util.Set;

/**
 * Where the listener is, as the ambience and the music choose by it: the dimension and the biome, the weather and the
 * time of day, whether under a roof, how deep below the open sky, and what is near.
 *
 * @param dimension the dimension's id
 * @param biome     the biome's id
 * @param biomeTags the tags the biome is in, each written {@code #namespace:path}
 * @param weather   the weather
 * @param dayTime   the time of day, in ticks from 0 (sunrise) to 23999
 * @param indoors   whether a roof and walls stand round the listener
 * @param depth     how many blocks below the open sky the listener is: 0 out under it
 * @param near      how many blocks of each kind asked about are near, by what they were asked as (an id or a tag)
 */
public record ListeningPlace(String dimension, String biome, Set<String> biomeTags, Weather weather, int dayTime,
                             boolean indoors, int depth, Map<String, Integer> near) {

    /** A day, in ticks. */
    public static final int DAY = 24_000;

    public ListeningPlace {
        biomeTags = Set.copyOf(biomeTags);
        near = Map.copyOf(near);
        dayTime = Math.floorMod(dayTime, DAY);
        depth = Math.max(0, depth);
    }

    /** How many blocks of {@code what} are near, as it was asked about; none when it was not. */
    public int count(final String what) {
        return this.near.getOrDefault(what, 0);
    }

    /** The weather where the listener is, by the name a file writes it with. */
    public enum Weather implements IStableName {
        CLEAR("clear"),
        RAIN("rain"),
        THUNDER("thunder");

        private final String name;

        Weather(final String name) {
            this.name = name;
        }

        @Override
        public String serializedName() {
            return this.name;
        }
    }
}
