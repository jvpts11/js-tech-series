/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.core.audio.AmbientClusters.Result;
import dev.jstech.core.audio.AmbientClusters.Rule;
import dev.jstech.core.audio.AmbientClusters.Source;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AmbientClustersTest {

    private static final Map<String, Rule> DATACENTER = Map.of("jsc:datacenter", new Rule(4, 6.0));

    @Test
    void group_makesOneBedOfARoomFullOfMachines() {
        final List<Source> racks = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            racks.add(new Source("rack" + i, "jsc:datacenter", i % 5, 64, i / 5));
        }
        final Result result = AmbientClusters.group(racks, DATACENTER);
        assertEquals(1, result.beds().size());
        assertEquals(10, result.beds().getFirst().members().size());
        assertEquals(10, result.absorbed().size());
        assertEquals(2.0, result.beds().getFirst().x(), 0.001);
    }

    @Test
    void group_leavesAFewMachinesToBeHeardOnTheirOwn() {
        final List<Source> racks = List.of(new Source("a", "jsc:datacenter", 0, 64, 0),
                new Source("b", "jsc:datacenter", 1, 64, 0), new Source("c", "jsc:datacenter", 2, 64, 0));
        final Result result = AmbientClusters.group(racks, DATACENTER);
        assertTrue(result.beds().isEmpty());
        assertTrue(result.absorbed().isEmpty());
    }

    @Test
    void group_keepsRoomsFarApartAsBedsOfTheirOwn() {
        final List<Source> racks = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            racks.add(new Source("west" + i, "jsc:datacenter", i, 64, 0));
            racks.add(new Source("east" + i, "jsc:datacenter", 100 + i, 64, 0));
        }
        assertEquals(2, AmbientClusters.group(racks, DATACENTER).beds().size());
    }

    @Test
    void group_neverTakesInASourceOfNoFieldOrOfAFieldWithNoRule() {
        final List<Source> sources = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            sources.add(new Source("loose" + i, null, i, 64, 0));
            sources.add(new Source("other" + i, "jsc:unknown", i, 64, 0));
        }
        final Result result = AmbientClusters.group(sources, DATACENTER);
        assertTrue(result.beds().isEmpty());
        assertFalse(result.absorbed().contains("loose0"));
    }

    @Test
    void bedVolume_growsWithTheRoomUpToFull() {
        assertEquals(0.4, AmbientClusters.bedVolume(4, 4), 0.001);
        assertEquals(0.6, AmbientClusters.bedVolume(8, 4), 0.001);
        assertEquals(1.0, AmbientClusters.bedVolume(200, 4), 0.001);
    }
}
