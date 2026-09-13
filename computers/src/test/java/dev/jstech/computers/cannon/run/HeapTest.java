/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.cannon.run;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.ref.WeakReference;
import java.util.List;
import org.junit.jupiter.api.Test;

class HeapTest {

    private static final long ROOM = 64L * 1024;
    private static final int LIST = 24;

    @Test
    void dispose_givesTheBytesBackAndStopsHoldingTheThing() {
        final Heap heap = new Heap(ROOM);
        final Values.ListValue list = heap.allocate(new Values.ListValue(), LIST, 3);

        heap.dispose(list, 4);

        assertEquals(0, heap.used());
        assertTrue(heap.isFreed(list));
        assertEquals(0, heap.bytesOf(list));
        assertEquals(List.of(), heap.live());
    }

    @Test
    void dispose_ofWhatWasAlreadyFreedChangesNothing() {
        final Heap heap = new Heap(ROOM);
        final Values.ListValue kept = heap.allocate(new Values.ListValue(), LIST, 1);
        final Values.ListValue list = heap.allocate(new Values.ListValue(), LIST, 2);

        heap.dispose(list, 3);
        heap.dispose(list, 4);

        assertEquals(LIST, heap.used());
        assertTrue(heap.isFreed(list));
        assertFalse(heap.isFreed(kept));
    }

    @Test
    void isFreed_goesByWhichThingItIsNotByWhatItReads() {
        final Heap heap = new Heap(ROOM);
        final String one = heap.allocate(new String("same".toCharArray()), Heap.sizeOfText("same"), 1);
        final String two = heap.allocate(new String("same".toCharArray()), Heap.sizeOfText("same"), 1);

        heap.dispose(one, 2);

        assertTrue(heap.isFreed(one));
        assertFalse(heap.isFreed(two));
        assertEquals(Heap.sizeOfText("same"), heap.used());
    }

    @Test
    void live_listsWhatIsStillHeldInTheOrderItWasAllocated() {
        final Heap heap = new Heap(ROOM);
        final Values.ListValue first = heap.allocate(new Values.ListValue(), LIST, 1);
        final Values.ListValue second = heap.allocate(new Values.ListValue(), LIST, 2);
        final Values.ListValue third = heap.allocate(new Values.ListValue(), LIST, 3);

        heap.dispose(second, 4);

        assertEquals(List.of(first, third), heap.live());
    }

    @Test
    void restore_bringsAFreedThingBackFreedAndCountsOnlyWhatIsHeld() {
        final Heap heap = new Heap(ROOM);
        final Values.ListValue held = new Values.ListValue();
        final Values.ListValue freed = new Values.ListValue();

        heap.restore(held, LIST, 1, false);
        heap.restore(freed, LIST, 2, true);

        assertEquals(LIST, heap.used());
        assertTrue(heap.isFreed(freed));
        assertEquals(List.of(held), heap.live());
    }

    @Test
    void dispose_keepsNothingOfAProgramThatAllocatesAndFreesForever() {
        final Heap heap = new Heap(ROOM);
        for (int i = 0; i < 100_000; i++) {
            heap.dispose(heap.allocate(new Values.ListValue(), LIST, i), i);
        }
        assertEquals(0, heap.used());
        assertEquals(List.of(), heap.live());
    }

    @Test
    void dispose_letsGoOfAFreedThingOnceNothingElseReachesIt() throws InterruptedException {
        final Heap heap = new Heap(ROOM);
        final WeakReference<Values.ListValue> watched = freeOne(heap);
        for (int attempt = 0; attempt < 50 && watched.get() != null; attempt++) {
            System.gc();
            Thread.sleep(10);
        }
        assertNull(watched.get(), "the heap held on to a freed thing that nothing else reaches");
    }

    private static WeakReference<Values.ListValue> freeOne(final Heap heap) {
        final Values.ListValue list = heap.allocate(new Values.ListValue(), LIST, 1);
        heap.dispose(list, 2);
        return new WeakReference<>(list);
    }
}
