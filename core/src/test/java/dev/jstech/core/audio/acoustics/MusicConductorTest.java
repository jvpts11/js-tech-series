/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio.acoustics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MusicConductorTest {

    private static final SoundConditions AT_NIGHT = new SoundConditions(Set.of(), Set.of(), Set.of(), 13_000,
            23_000, Optional.empty(), 0, Integer.MAX_VALUE, List.of());
    private static final SoundConditions ON_MARS = new SoundConditions(Set.of("jsspace:mars"), Set.of(), Set.of(),
            0, 0, Optional.empty(), 0, Integer.MAX_VALUE, List.of());
    private static final MusicSet EXPLORING = new MusicSet("exploring", 1, SoundConditions.ANYWHERE, List.of(
            new MusicSet.Layer("music:pads", 1F, SoundConditions.ANYWHERE),
            new MusicSet.Layer("music:drums", 0.8F, AT_NIGHT)));
    private static final MusicSet MARS = new MusicSet("mars", 5, ON_MARS, List.of(
            new MusicSet.Layer("music:mars", 1F, SoundConditions.ANYWHERE)));

    private MusicConductor conductor;

    @BeforeEach
    void setUp() {
        conductor = new MusicConductor();
    }

    @Test
    void second_playsTheSetOfHighestPriorityThatHolds() {
        final List<MusicSet> sets = List.of(EXPLORING, MARS);
        assertEquals("exploring", conductor.second(sets, place("minecraft:overworld", 6_000)).set());
        assertEquals("mars", conductor.second(sets, place("jsspace:mars", 6_000)).set(),
                "a set of higher priority takes over at once");
    }

    @Test
    void second_bringsALayerInWithItsConditions() {
        final MusicConductor.Plan day = conductor.second(List.of(EXPLORING), place("minecraft:overworld", 6_000));
        assertEquals(Map.of("music:pads", 1F, "music:drums", 0F), day.volumes());
        final MusicConductor.Plan night = conductor.second(List.of(EXPLORING), place("minecraft:overworld", 15_000));
        assertEquals(0.8F, night.volumes().get("music:drums"));
    }

    @Test
    void second_keepsASetThroughAShortStepOutside() {
        conductor.second(List.of(MARS), place("jsspace:mars", 6_000));
        for (int second = 1; second < MusicConductor.LETS_GO_AFTER; second++) {
            final MusicConductor.Plan away = conductor.second(List.of(MARS), place("minecraft:overworld", 6_000));
            assertEquals("mars", away.set(), "still playing after " + second + " s outside");
            assertEquals(0F, away.volumes().get("music:mars"), "but silent meanwhile");
        }
        assertFalse(conductor.second(List.of(MARS), place("minecraft:overworld", 6_000)).plays(),
                "and let go after " + MusicConductor.LETS_GO_AFTER + " s");
        assertTrue(conductor.playing().isEmpty());
    }

    @Test
    void second_isSilentWithNoSetThatHolds() {
        assertEquals(MusicConductor.Plan.SILENT, conductor.second(List.of(MARS), place("minecraft:overworld", 0)));
    }

    private static ListeningPlace place(final String dimension, final int time) {
        return new ListeningPlace(dimension, "minecraft:plains", Set.of(), ListeningPlace.Weather.CLEAR, time, false,
                0, Map.of());
    }
}
