/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.grid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.OptionalLong;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GridTest {

    private static final GridMember ACCESS = GridMember.cable("test:access", 0, 16, 0);
    private static final GridMember BACKBONE = GridMember.cable("test:backbone", 0, 64, 0);
    private static final int RED = 14;
    private static final int BLUE = 11;

    private Grid grid;

    @BeforeEach
    void setUp() {
        grid = new Grid();
    }

    @Test
    void place_joinsCablesOfOneLineAndGeneration() {
        grid.place(1, ACCESS, List.of());
        grid.place(2, ACCESS, List.of(1L));

        assertTrue(grid.connected(1, 2));
        assertEquals(1, grid.componentCount());
    }

    @Test
    void place_keepsLinesAndGenerationsApart() {
        grid.place(1, ACCESS, List.of());
        grid.place(2, BACKBONE, List.of(1L));
        grid.place(3, GridMember.cable("test:access", 1, 16, 0), List.of(1L));

        assertFalse(grid.connected(1, 2), "two lines");
        assertFalse(grid.connected(1, 3), "two generations of a line");
        assertEquals(3, grid.componentCount());
    }

    @Test
    void place_joinsColoursByTheirOwnRule() {
        grid.place(1, ACCESS.coloured(RED), List.of());
        grid.place(2, ACCESS.coloured(BLUE), List.of(1L));
        grid.place(3, ACCESS, List.of(1L, 2L));

        assertFalse(grid.memberOf(1).joins(grid.memberOf(2)), "red and blue never join");
        assertTrue(grid.memberOf(3).joins(grid.memberOf(1)) && grid.memberOf(3).joins(grid.memberOf(2)),
                "a cable in no colour joins every colour");
        assertTrue(grid.connected(1, 2), "through the cable in no colour, red and blue are one part");
        assertTrue(ACCESS.coloured(RED).joins(ACCESS.coloured(RED)), "red joins red");
    }

    @Test
    void place_letsADeviceJoinEveryLine() {
        grid.place(1, ACCESS, List.of());
        grid.place(2, BACKBONE, List.of());
        grid.place(3, GridMember.DEVICE, List.of(1L, 2L));

        assertTrue(grid.connected(1, 2));
    }

    @Test
    void place_givesBackTheRootsItJoined() {
        grid.place(1, ACCESS, List.of());
        grid.place(2, ACCESS, List.of());
        final int first = grid.rootOf(1);
        final int second = grid.rootOf(2);

        final Set<Integer> joined = grid.place(3, ACCESS, List.of(1L, 2L));

        assertEquals(Set.of(first, second), joined);
    }

    @Test
    void place_refusesAPositionTwice() {
        grid.place(1, ACCESS, List.of());

        assertThrows(IllegalStateException.class, () -> grid.place(1, ACCESS, List.of()));
    }

    @Test
    void remove_splitsAPartInTwo() {
        line(1, 3, ACCESS);

        final Grid.Removal removal = grid.remove(2);

        assertEquals(2, removal.fragments());
        assertEquals(Set.of(1L, 2L, 3L), removal.affected());
        assertFalse(grid.connected(1, 3));
        assertFalse(grid.contains(2));
    }

    @Test
    void remove_ofAnEndLeavesOnePart() {
        line(1, 3, ACCESS);

        assertEquals(1, grid.remove(3).fragments());
        assertTrue(grid.connected(1, 2));
    }

    @Test
    void bridge_joinsRunsADeviceTouchesAndForgetsThemWhenItGoes() {
        grid.place(1, ACCESS, List.of());
        grid.place(2, BACKBONE, List.of());

        assertTrue(grid.bridge(100, List.of(1L, 2L)));
        assertTrue(grid.connected(1, 2));
        assertFalse(grid.bridge(100, List.of(1L, 2L)), "the same report again merges nothing");
        assertEquals(Set.of(1L, 2L), grid.bridgedBy(100));
        grid.forgetBridge(100);
        assertEquals(Set.of(), grid.bridgedBy(100));
    }

    @Test
    void slowestBetween_takesTheWayWhoseSlowestCableIsFastest() {
        // A slow way 1-2-3 over access, and a fast way 1-4-5-3 over backbone, joined at each end by devices.
        grid.place(1, GridMember.DEVICE, List.of());
        grid.place(2, ACCESS, List.of(1L));
        grid.place(3, GridMember.DEVICE, List.of(2L));
        grid.place(4, BACKBONE, List.of(1L));
        grid.place(5, BACKBONE, List.of(4L, 3L));

        final OptionalLong slowest = grid.slowestBetween(List.of(1L), List.of(3L));

        assertTrue(slowest.isPresent());
        assertEquals(BACKBONE, grid.memberOf(slowest.getAsLong()));
    }

    @Test
    void slowestBetween_findsNothingBetweenPartsApart() {
        grid.place(1, ACCESS, List.of());
        grid.place(2, ACCESS, List.of());

        assertTrue(grid.slowestBetween(List.of(1L), List.of(2L)).isEmpty());
    }

    @Test
    void slowestBetween_isWorkedOutAgainOnlyWhenTheGridChanges() {
        line(1, 2, BACKBONE);
        final long before = grid.version();
        final OptionalLong first = grid.slowestBetween(List.of(1L), List.of(2L));

        assertEquals(first, grid.slowestBetween(List.of(1L), List.of(2L)));
        assertEquals(before, grid.version(), "asking changes nothing");
        // Both cables become access ones.
        grid.remove(2);
        grid.remove(1);
        line(1, 2, ACCESS);
        assertEquals(ACCESS, grid.memberOf(grid.slowestBetween(List.of(1L), List.of(2L)).getAsLong()),
                "a changed grid answers anew");
    }

    @Test
    void runLength_countsOneCableUnbrokenAndDevicesEndRuns() {
        line(1, 4, ACCESS);
        grid.place(5, GridMember.DEVICE, List.of(4L));
        line(6, 7, ACCESS);
        grid.place(8, ACCESS, List.of(5L, 7L));

        assertEquals(4, grid.runLength(1));
        assertEquals(3, grid.runLength(7), "6, 7 and 8 past the device");
        assertEquals(0, grid.runLength(5), "a device is on no run");
    }

    @Test
    void runTooLong_isARunLongerThanItsCableReaches() {
        final GridMember shortReach = GridMember.cable("test:short", 0, 8, 3);
        line(1, 3, shortReach);

        assertFalse(grid.runTooLong(1), "three cables reach three");
        grid.place(4, shortReach, List.of(3L));
        assertTrue(grid.runTooLong(1), "four cables do not");
        assertFalse(GridMember.cable("test:any", 0, 8, 0).range() > 0, "a range of 0 is no limit");
    }

    @Test
    void member_refusesANegativeGenerationOrRange() {
        assertThrows(IllegalArgumentException.class, () -> GridMember.cable("test:bad", -1, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> GridMember.cable("test:bad", 0, 1, -1));
    }

    /* Cables of {@code member} at {@code first} to {@code last}, each joined to the one before. */
    private void line(final long first, final long last, final GridMember member) {
        for (long pos = first; pos <= last; pos++) {
            grid.place(pos, member, pos == first ? List.of() : List.of(pos - 1));
        }
    }
}
