/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.developer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class AlignmentGuidesTest {

    @Test
    void of_runsALineDownWhereLeftEdgesMeet() {
        final List<AlignmentGuides.Guide> guides = AlignmentGuides.of(new int[] {10, 40, 20, 10},
                List.of(new int[] {10, 0, 30, 18}));
        assertTrue(guides.contains(new AlignmentGuides.Guide(true, 10, 0, 50)),
                "a line down the shared left, over both boxes: " + guides);
    }

    @Test
    void of_runsALineAcrossWhereMiddlesMeet() {
        final List<AlignmentGuides.Guide> guides = AlignmentGuides.of(new int[] {0, 10, 10, 20},
                List.of(new int[] {40, 15, 10, 10}));
        assertTrue(guides.contains(new AlignmentGuides.Guide(false, 20, 0, 50)),
                "a line across the shared middle: " + guides);
    }

    @Test
    void of_findsNothingForBoxesThatLineUpNowhere() {
        assertTrue(AlignmentGuides.of(new int[] {3, 7, 11, 13}, List.of(new int[] {50, 60, 9, 9})).isEmpty());
    }

    @Test
    void pull_bringsTheNearestEdgeWithinReachOntoAnother() {
        assertArrayEquals(new int[] {2, -1}, AlignmentGuides.pull(new int[] {8, 41, 20, 10},
                List.of(new int[] {10, 0, 30, 40}), 3), "its left two short of the other's, its top one past");
    }

    @Test
    void pull_leavesWhatIsOutOfReach() {
        assertArrayEquals(new int[] {0, 0}, AlignmentGuides.pull(new int[] {0, 0, 10, 10},
                List.of(new int[] {30, 30, 10, 10}), 3));
    }
}
