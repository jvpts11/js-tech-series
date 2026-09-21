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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    void release_givesTheBytesBackAndKeepsNothingToCatchALaterUse() {
        final Heap heap = new Heap(ROOM);
        final String copy = heap.allocate(new String("own".toCharArray()), Heap.sizeOfText("own"), 1);

        heap.release(copy);

        assertEquals(0, heap.used());
        assertFalse(heap.isFreed(copy));
        assertEquals(0, heap.bytesOf(copy));
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

    @Test
    void adopt_weighsAListItIsHandedContentsAndAll() {
        final Heap heap = new Heap(ROOM);
        final Values.ListValue list = new Values.ListValue();
        list.items().add(new String("iron".toCharArray()));
        list.items().add(new String("gold".toCharArray()));

        heap.adopt(list, 5);

        assertEquals(list.bytes(), heap.bytesOf(list));
        assertEquals(Heap.sizeOfText("iron"), heap.bytesOf(list.items().get(0)));
        assertEquals(list.bytes() + Heap.sizeOfText("iron") + Heap.sizeOfText("gold"), heap.used());
    }

    @Test
    void adopt_putsWhatAnObjectsFieldsHoldOnTheHeapToo() {
        final Heap heap = new Heap(ROOM);
        final Values.Obj object = new Values.Obj("Process");
        object.set("Name", new String("miner".toCharArray()));
        object.set("Id", 7);

        heap.adopt(object, 1);

        assertEquals(Heap.HEADER + 2L * Heap.REFERENCE, heap.bytesOf(object));
        assertEquals(Heap.sizeOfText("miner"), heap.bytesOf(object.get("Name")));
        assertEquals(Heap.HEADER + 2L * Heap.REFERENCE + Heap.sizeOfText("miner"), heap.used());
    }

    @Test
    void adopt_leavesWhatIsAlreadyHeldWhereItIs() {
        final Heap heap = new Heap(ROOM);
        final String held = heap.text("kept", 1);

        assertSame(held, heap.adopt(held, 2));
        assertEquals(Heap.sizeOfText("kept"), heap.used());
    }

    @Test
    void adopt_givesANumberAndNothingNoWeight() {
        final Heap heap = new Heap(ROOM);

        assertEquals(42L, heap.adopt(42L, 1));
        assertNull(heap.adopt(null, 1));
        assertEquals(0, heap.used());
    }

    @Test
    void alive_givesBackWhatIsHeld() {
        final Heap heap = new Heap(ROOM);
        final Values.ListValue list = heap.allocate(new Values.ListValue(), LIST, 1);

        assertSame(list, heap.alive(list, 2));
    }

    @Test
    void alive_haltsOnNothing() {
        final Heap heap = new Heap(ROOM);

        final Halt halt = assertThrows(Halt.class, () -> heap.alive(null, 9));

        assertEquals(Halt.Reason.NO_OBJECT, halt.reason());
    }

    @Test
    void alive_haltsOnWhatWasFreed() {
        final Heap heap = new Heap(ROOM);
        final Values.ListValue list = heap.allocate(new Values.ListValue(), LIST, 1);
        heap.dispose(list, 2);

        final Halt halt = assertThrows(Halt.class, () -> heap.alive(list, 3));

        assertEquals(Halt.Reason.USE_AFTER_DISPOSE, halt.reason());
    }

    private static WeakReference<Values.ListValue> freeOne(final Heap heap) {
        final Values.ListValue list = heap.allocate(new Values.ListValue(), LIST, 1);
        heap.dispose(list, 2);
        return new WeakReference<>(list);
    }
}
