/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.region;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpatialIndexTest {

    @Test
    void within_findsOnlyWhatMeetsTheArea() {
        final SpatialIndex<String, Integer> index = new SpatialIndex<>();
        index.put("a", Box.at(10, 64, 10), 1);
        index.put("b", Box.at(2000, 64, 2000), 2);
        final List<SpatialIndex.Entry<String, Integer>> found = index.within(Box.around(12, 64, 12, 5));
        assertEquals(1, found.size());
        assertEquals("a", found.getFirst().key());
    }

    @Test
    void put_replacesWhatTheKeyHeld() {
        final SpatialIndex<String, Integer> index = new SpatialIndex<>();
        index.put("a", Box.at(0, 0, 0), 1);
        index.put("a", Box.at(5000, 0, 5000), 2);
        assertEquals(1, index.size());
        assertTrue(index.within(Box.around(0, 0, 0, 2)).isEmpty(), "the old place forgets the key");
        assertEquals(2, index.get("a").value());
    }

    @Test
    void remove_emptiesTheRegionsTheEntryWasFiledIn() {
        final SpatialIndex<String, Integer> index = new SpatialIndex<>();
        // From the region west of the origin's to the one east of it, three a side.
        index.put("wide", new Box(-512, 0, -512, 1023, 10, 1023), 1);
        assertEquals(9, index.filledRegions(), "a box over nine regions is filed in each");
        index.remove("wide");
        assertEquals(0, index.filledRegions());
        assertNull(index.get("wide"));
    }

    @Test
    void within_findsAnEntryThatCrossesARegionsEdge() {
        final SpatialIndex<String, Integer> index = new SpatialIndex<>();
        index.put("edge", new Box(500, 0, 0, 520, 0, 0), 1);
        assertEquals(1, index.within(Box.at(519, 0, 0)).size(), "found from the region on the far side");
        assertEquals(1, index.within(Box.at(501, 0, 0)).size());
    }

    @Test
    void nearest_picksTheClosestWithinTheRadius() {
        final SpatialIndex<String, Integer> index = new SpatialIndex<>();
        index.put("far", Box.at(30, 0, 0), 1);
        index.put("near", Box.at(5, 0, 0), 2);
        assertEquals("near", index.nearest(0, 0, 0, 40).key());
        assertNull(index.nearest(0, 0, 0, 4), "nothing within four blocks");
    }

    @Test
    void within_seesAnEntryTooWideToFile() {
        final SpatialIndex<String, Integer> index = new SpatialIndex<>();
        index.put("world", new Box(-10_000_000, 0, -10_000_000, 10_000_000, 0, 10_000_000), 1);
        assertEquals(0, index.filledRegions(), "kept apart rather than in millions of regions");
        assertEquals(1, index.within(Box.at(123_456, 0, -98_765)).size());
    }

    @Test
    void inRegion_listsWhatIsFiledThere() {
        final SpatialIndex<String, Integer> index = new SpatialIndex<>();
        index.put("a", Box.at(100, 0, 100), 1);
        index.put("b", Box.at(600, 0, 100), 2);
        assertEquals("a", index.inRegion(0, 0).getFirst().key());
        assertEquals("b", index.inRegion(1, 0).getFirst().key());
    }
}
