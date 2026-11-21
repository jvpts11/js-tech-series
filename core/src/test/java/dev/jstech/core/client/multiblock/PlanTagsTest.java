/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.multiblock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PlanTagsTest {

    /* A screen of 960 by 540 at GUI scale 2, in its scaled pixels, and a tag's size on it. */
    private static final int WIDTH = 480;
    private static final int HEIGHT = 270;
    private static final int WIDE = 80;
    private static final int HIGH = 20;
    /* The line across the top of the screen, its ground's box. */
    private static final PlanTags.Box BANNER = new PlanTags.Box(180, 3, 120, 14);

    @Test
    void lay_putsATagAtTheFirstOfItsHeightsOverItsPart() {
        // A part in the screen's upper half: its tag 150 of the drawings' pixels over it, 75 of this screen's.
        final PlanTags.Box box = only(PlanTags.lay(List.of(new PlanTags.Wanted(240, 100, WIDE, HIGH)), WIDTH, HEIGHT,
                Optional.empty()));
        assertEquals(new PlanTags.Box(200, 100 - 75 - HIGH, WIDE, HIGH), box);
    }

    @Test
    void lay_keepsATagClearOfTheLineAcrossTheTop() {
        final PlanTags.Box box = only(PlanTags.lay(List.of(new PlanTags.Wanted(240, 100, WIDE, HIGH)), WIDTH, HEIGHT,
                Optional.of(BANNER)));
        assertFalse(box.meets(BANNER, 0), "the tag stands clear of the line over the screen: " + box);
        assertTrue(box.y() + HIGH <= 100, "still lifted over its part: " + box);
    }

    @Test
    void lay_neverLaysOneTagOverAnother() {
        // Twenty parts tagged at one point: the heights the drawings try run out, and the rest find the nearest.
        final List<PlanTags.Wanted> wanted = new ArrayList<>(Collections.nCopies(20,
                new PlanTags.Wanted(240, 135, WIDE, HIGH)));
        final List<PlanTags.Box> laid = PlanTags.lay(wanted, WIDTH, HEIGHT, Optional.of(BANNER)).stream()
                .flatMap(Optional::stream).toList();
        // The drawings' heights hold five of them under the line over this screen; the nearest free heights, more.
        assertTrue(laid.size() > 5, "more tags laid than the drawings' heights hold: " + laid.size());
        for (int i = 0; i < laid.size(); i++) {
            final PlanTags.Box box = laid.get(i);
            assertTrue(box.x() >= 0 && box.y() >= 0 && box.x() + box.wide() <= WIDTH && box.y() + box.high() <= HEIGHT,
                    "on the screen: " + box);
            assertFalse(box.meets(BANNER, 0), "clear of the line over the screen: " + box);
            for (int j = i + 1; j < laid.size(); j++) {
                assertFalse(box.meets(laid.get(j), 0), "tags " + i + " and " + j + " apart: " + box + ", "
                        + laid.get(j));
            }
        }
    }

    @Test
    void lay_leavesOutATagWithNoRoomOnTheScreen() {
        // Tags nearly half the screen high: one fits just over the point, and under it the other has no room left.
        final List<Optional<PlanTags.Box>> laid = PlanTags.lay(List.of(new PlanTags.Wanted(240, 135, WIDE, 128),
                new PlanTags.Wanted(240, 135, WIDE, 128)), WIDTH, HEIGHT, Optional.empty());
        assertTrue(laid.get(0).isPresent(), "the first finds room");
        assertTrue(laid.get(1).isEmpty(), "the second, with none left, is not laid over it: " + laid.get(1));
    }

    @Test
    void lay_movesATagInFromASideOfTheScreen() {
        final PlanTags.Box box = only(PlanTags.lay(List.of(new PlanTags.Wanted(10, 100, WIDE, HIGH)), WIDTH, HEIGHT,
                Optional.empty()));
        assertTrue(box.x() > 0 && box.x() <= 10, "whole on the screen, its point still under it: " + box);
    }

    /* The one tag laid of {@code laid}. */
    private static PlanTags.Box only(final List<Optional<PlanTags.Box>> laid) {
        assertEquals(1, laid.size());
        return laid.getFirst().orElseThrow();
    }
}
