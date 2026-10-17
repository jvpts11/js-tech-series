/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio.acoustics;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SoundConditionsTest {

    private static final ListeningPlace FOREST_NIGHT = new ListeningPlace("minecraft:overworld",
            "minecraft:forest", Set.of("#minecraft:is_forest", "#minecraft:is_overworld"),
            ListeningPlace.Weather.CLEAR, 18_000, false, 0, Map.of("#minecraft:leaves", 300));

    @Test
    void anywhere_holdsEverywhere() {
        assertTrue(SoundConditions.ANYWHERE.matches(FOREST_NIGHT));
    }

    @Test
    void matches_byDimensionAndByBiomeOrItsTag() {
        assertTrue(when(Set.of("minecraft:overworld"), Set.of(), 0, 0).matches(FOREST_NIGHT));
        assertFalse(when(Set.of("minecraft:the_nether"), Set.of(), 0, 0).matches(FOREST_NIGHT));
        assertTrue(when(Set.of(), Set.of("#minecraft:is_forest"), 0, 0).matches(FOREST_NIGHT));
        assertTrue(when(Set.of(), Set.of("minecraft:forest"), 0, 0).matches(FOREST_NIGHT));
        assertFalse(when(Set.of(), Set.of("minecraft:desert", "#minecraft:is_ocean"), 0, 0).matches(FOREST_NIGHT));
    }

    @Test
    void matches_hoursThatRunPastMidnight() {
        assertTrue(when(Set.of(), Set.of(), 13_000, 23_000).matches(FOREST_NIGHT));
        assertFalse(when(Set.of(), Set.of(), 0, 12_000).matches(FOREST_NIGHT));
        final ListeningPlace dawn = new ListeningPlace("minecraft:overworld", "minecraft:forest", Set.of(),
                ListeningPlace.Weather.CLEAR, 500, false, 0, Map.of());
        assertTrue(when(Set.of(), Set.of(), 22_000, 1_000).matches(dawn), "from late night to just past sunrise");
    }

    @Test
    void matches_weatherRoofDepthAndWhatIsNear() {
        final SoundConditions rainyCave = new SoundConditions(Set.of(), Set.of(),
                Set.of(ListeningPlace.Weather.RAIN), 0, 0, Optional.of(true), 10, 64, List.of());
        assertFalse(rainyCave.matches(FOREST_NIGHT));
        final SoundConditions leafy = new SoundConditions(Set.of(), Set.of(), Set.of(), 0, 0, Optional.of(false), 0,
                Integer.MAX_VALUE, List.of(new SoundConditions.Near("#minecraft:leaves", 12, 200)));
        assertTrue(leafy.matches(FOREST_NIGHT));
        final SoundConditions thick = new SoundConditions(Set.of(), Set.of(), Set.of(), 0, 0, Optional.empty(), 0,
                Integer.MAX_VALUE, List.of(new SoundConditions.Near("#minecraft:leaves", 12, 400)));
        assertFalse(thick.matches(FOREST_NIGHT));
    }

    @Test
    void constructor_refusesADepthTheWrongWayRound() {
        assertThrows(IllegalArgumentException.class, () -> new SoundConditions(Set.of(), Set.of(), Set.of(), 0, 0,
                Optional.empty(), 10, 5, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new SoundConditions.Near("#minecraft:leaves", 0, 1));
    }

    private static SoundConditions when(final Set<String> dimensions, final Set<String> biomes, final int from,
                                        final int to) {
        return new SoundConditions(dimensions, biomes, Set.of(), from, to, Optional.empty(), 0, Integer.MAX_VALUE,
                List.of());
    }
}
