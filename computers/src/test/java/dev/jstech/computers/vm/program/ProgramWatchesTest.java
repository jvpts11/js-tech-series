/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ProgramWatchesTest {

    private static final Values.DelegateValue HANDLER = new Values.DelegateValue("Handler", List.of());

    /** Sets a watch the way a process does, with a token of its own. */
    private static Values.Obj add(final ProgramWatches watches, final String item, final Process.Watching kind,
                                  final long threshold) {
        final Values.Obj token = new Values.Obj("Subscription");
        token.set("Id", watches.nextId());
        token.set("Item", item);
        watches.add(item, kind, threshold, HANDLER, token);
        return token;
    }

    /** Hands in the totals and says what went off, each as "item before -> now". */
    private static List<String> deliver(final ProgramWatches watches, final Map<String, Long> totals,
                                        final Set<Values.Obj> freed) {
        final List<String> told = new ArrayList<>();
        watches.deliver(totals, freed::contains,
                (watch, before, now, first) -> told.add(watch.item() + " " + before + " -> " + now));
        return told;
    }

    private static List<String> read(final ProgramWatches watches, final String item, final long total) {
        return deliver(watches, Map.of(item, total), Set.of());
    }

    @Test
    void watching_namesEachThingOnceInTheOrderItWasFirstWatched() {
        final ProgramWatches watches = new ProgramWatches();
        add(watches, "iron", Process.Watching.CHANGE, 0);
        add(watches, "iron", Process.Watching.BELOW, 10);
        add(watches, "copper", Process.Watching.CHANGE, 0);

        assertEquals(List.of("iron", "copper"), watches.watching());
    }

    @Test
    void add_numbersTheWatchesInTheOrderTheyAreSet() {
        final ProgramWatches watches = new ProgramWatches();
        assertEquals(1, watches.nextId());

        add(watches, "iron", Process.Watching.CHANGE, 0);
        add(watches, "copper", Process.Watching.CHANGE, 0);

        assertEquals(List.of(1, 2), watches.all().stream().map(ProgramWatches.Watch::id).toList());
        assertEquals(3, watches.nextId());
    }

    @Test
    void deliver_dropsAWatchThatWasLetGoWithoutItGoingOff() {
        final ProgramWatches watches = new ProgramWatches();
        final Values.Obj first = add(watches, "iron", Process.Watching.CHANGE, 0);
        add(watches, "copper", Process.Watching.CHANGE, 0);
        final Values.Obj last = add(watches, "iron", Process.Watching.CHANGE, 0);
        deliver(watches, Map.of("iron", 100L, "copper", 5L), Set.of());

        final List<String> told = deliver(watches, Map.of("iron", 80L, "copper", 5L), Set.of(first));

        assertEquals(List.of("iron 100 -> 80"), told);
        assertEquals(2, watches.all().size());
        assertEquals(List.of("copper", "iron"), watches.watching(), "named where the watches kept first name them");
        deliver(watches, Map.of("copper", 5L), Set.of(first, last));
        assertEquals(List.of("copper"), watches.watching());
    }

    @Test
    void watching_stillNamesAWatchLetGoUntilTheNextDelivery() {
        final ProgramWatches watches = new ProgramWatches();
        final Values.Obj token = add(watches, "iron", Process.Watching.CHANGE, 0);

        assertEquals(List.of("iron"), watches.watching());
        deliver(watches, Map.of("iron", 1L), Set.of(token));
        assertTrue(watches.watching().isEmpty());
    }

    @Test
    void deliver_changeGoesOffOnlyAfterAFirstReading() {
        final ProgramWatches watches = new ProgramWatches();
        add(watches, "iron", Process.Watching.CHANGE, 0);

        assertEquals(List.of(), read(watches, "iron", 100));
        assertEquals(List.of(), read(watches, "iron", 100));
        assertEquals(List.of("iron 100 -> 80"), read(watches, "iron", 80));
    }

    @Test
    void deliver_belowGoesOffOnTheCrossingAndRearmsOnceBackAbove() {
        final ProgramWatches watches = new ProgramWatches();
        add(watches, "iron", Process.Watching.BELOW, 50);

        assertEquals(List.of(), read(watches, "iron", 100));
        assertEquals(List.of("iron 100 -> 40"), read(watches, "iron", 40));
        assertEquals(List.of(), read(watches, "iron", 30));
        assertEquals(List.of(), read(watches, "iron", 90));
        assertEquals(List.of("iron 90 -> 10"), read(watches, "iron", 10));
    }

    @Test
    void deliver_aboveGoesOffOnTheWayUp() {
        final ProgramWatches watches = new ProgramWatches();
        add(watches, "iron", Process.Watching.ABOVE, 500);

        assertEquals(List.of(), read(watches, "iron", 100));
        assertEquals(List.of("iron 100 -> 900"), read(watches, "iron", 900));
        assertEquals(List.of(), read(watches, "iron", 1000));
    }

    @Test
    void deliver_leavesAWatchUnreadWhenItsThingIsNotInTheTotals() {
        final ProgramWatches watches = new ProgramWatches();
        add(watches, "iron", Process.Watching.CHANGE, 0);

        assertEquals(List.of(), read(watches, "copper", 5));
        assertFalse(watches.all().getFirst().seen());
        assertEquals(List.of(), read(watches, "iron", 100), "its first reading is still only a reading");
    }

    @Test
    void deliver_goesOffInTheOrderTheWatchesWereSet() {
        final ProgramWatches watches = new ProgramWatches();
        add(watches, "copper", Process.Watching.CHANGE, 0);
        add(watches, "iron", Process.Watching.CHANGE, 0);
        final Map<String, Long> totals = new LinkedHashMap<>();
        totals.put("iron", 1L);
        totals.put("copper", 1L);
        deliver(watches, totals, Set.of());
        totals.put("iron", 2L);
        totals.put("copper", 2L);

        assertEquals(List.of("copper 1 -> 2", "iron 1 -> 2"), deliver(watches, totals, Set.of()));
    }

    @Test
    void restore_bringsBackTheNumberTheReadingAndWhetherItWasArmed() {
        final ProgramWatches watches = new ProgramWatches();
        final Values.Obj token = new Values.Obj("Subscription");

        watches.restore(new Snapshot.WatchShot(7, "iron", "below", 50, null, null, 40, false, true), HANDLER, token);

        final ProgramWatches.Watch watch = watches.all().getFirst();
        assertEquals(7, watch.id());
        assertEquals(Process.Watching.BELOW, watch.kind());
        assertEquals(40L, watch.last());
        assertFalse(watch.armed());
        assertTrue(watch.seen());
        assertEquals(8, watches.nextId());
        assertEquals(List.of("iron"), watches.watching());
        // It knew it was already below, so the next low reading is not a crossing.
        assertEquals(List.of(), read(watches, "iron", 30));
        assertEquals(List.of(), read(watches, "iron", 80));
        assertEquals(List.of("iron 80 -> 10"), read(watches, "iron", 10));
        watches.restore(new Snapshot.WatchShot(3, "copper", "change", 0, null, null, 0, true, false), HANDLER, token);
        assertEquals(8, watches.nextId(), "a lower number never takes the count back");
    }
}
