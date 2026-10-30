/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.exploration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ReliefCoverageTest {

    private static final double CLOSE = 1e-9;
    /* A block far from any place stood on. */
    private static final double FAR = 1e7;

    @Test
    void photographed_seesTheGroundUnderItsPassOnly() {
        final ReliefCoverage seen = ReliefCoverage.NONE.photographed(0);
        assertTrue(seen.known(0, 90, ReliefCoverage.TRACK, FAR, FAR), "the pass swings north at 90 degrees east");
        assertTrue(seen.known(0, 0, 0, FAR, FAR), "and crosses the equator at its node");
        assertFalse(seen.known(0, 90, -ReliefCoverage.TRACK, FAR, FAR), "it is far south of there");
        assertFalse(seen.known(0, 0, 60, FAR, FAR), "and never so far north");
    }

    @Test
    void photographed_samePassTwiceChangesNothing() {
        final ReliefCoverage seen = ReliefCoverage.NONE.photographed(22.5);
        assertSame(seen, seen.photographed(22.5 + 360));
        assertEquals(2, seen.photographed(45).strips().size());
    }

    @Test
    void visitedAt_knowsTheGroundRoundWhereOneStood() {
        final ReliefCoverage seen = ReliefCoverage.NONE.visitedAt(1_000, 1_000);
        assertTrue(seen.known(0, 0, 60, 1_000 + ReliefCoverage.VISIT_CELL, 1_000), "the cell beside");
        assertFalse(seen.known(0, 0, 60, 1_000 + 3 * ReliefCoverage.VISIT_CELL, 1_000), "not two cells off");
        assertSame(seen, seen.visitedAt(1_010, 1_010), "standing in the same cell sees nothing new");
    }

    @Test
    void known_reliefKnownWholeKnowsEveryPlace() {
        assertTrue(ReliefCoverage.NONE.known(1, 170, -80, FAR, FAR));
        assertFalse(ReliefCoverage.NONE.known(0, 170, -80, FAR, FAR));
    }

    @Test
    void share_growsWithEachPassAndIsWholeWithTheRelief() {
        assertEquals(0, ReliefCoverage.NONE.share(0), CLOSE);
        final double one = ReliefCoverage.NONE.photographed(0).share(0);
        assertTrue(one > 0.05 && one < 0.15, "one pass sees a strip of the world: " + one);
        ReliefCoverage all = ReliefCoverage.NONE;
        for (int pass = 0; pass < ReliefCoverage.PASSES; pass++) {
            all = all.photographed(pass * ReliefCoverage.PASS_SHIFT);
        }
        final double band = all.share(0);
        assertTrue(band > one && band < 1, "a day of passes sees the band under the orbit, not the poles: " + band);
        ReliefCoverage half = ReliefCoverage.NONE;
        for (int pass = 0; pass < ReliefCoverage.PASSES / 2; pass++) {
            half = half.photographed(pass * ReliefCoverage.PASS_SHIFT);
        }
        assertEquals(half.share(0), ReliefCoverage.NONE.share(0.5), CLOSE, "half the relief, as half a day's passes");
        assertEquals(1, ReliefCoverage.NONE.share(1), CLOSE);
    }

    @Test
    void tracks_holdTheMappedPassesAndThePhotographed() {
        final ReliefCoverage seen = ReliefCoverage.NONE.photographed(10);
        assertEquals(ReliefCoverage.PASSES / 2 + 1, seen.tracks(0.5, 0).length);
        assertTrue(ReliefCoverage.under(seen.tracks(0, 10), 0));
        assertFalse(ReliefCoverage.under(seen.tracks(0, 10), ReliefCoverage.SWATH + 1));
    }
}
