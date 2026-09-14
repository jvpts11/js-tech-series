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
import java.util.List;
import org.junit.jupiter.api.Test;

class MonitorTableTest {

    @Test
    void enter_takesAFreeLockAndTakesItAgainForTheThreadThatHoldsIt() {
        final MonitorTable locks = new MonitorTable();
        final ProgramThread owner = new ProgramThread(1);
        final Object gate = new Object();

        assertTrue(locks.enter(owner, gate));
        assertTrue(locks.enter(owner, gate));
        assertTrue(locks.exit(owner, gate));
        assertTrue(locks.held(gate), "taken twice and let go of once, it is still held");
        assertTrue(locks.exit(owner, gate));
        assertFalse(locks.held(gate));
    }

    @Test
    void enter_refusesALockAnotherThreadHoldsAndExitWakesTheThreadWaiting() {
        final MonitorTable locks = new MonitorTable();
        final ProgramThread owner = new ProgramThread(1);
        final ProgramThread waiter = new ProgramThread(2);
        final Object gate = new Object();
        locks.enter(owner, gate);

        assertFalse(locks.enter(waiter, gate));
        waiter.wait = new IWait.Lock(gate);
        locks.exit(owner, gate);

        assertFalse(waiter.waiting());
        assertTrue(locks.enter(waiter, gate), "asked again, it takes the lock");
    }

    @Test
    void exit_refusesALockTheThreadDoesNotHold() {
        final MonitorTable locks = new MonitorTable();
        final ProgramThread owner = new ProgramThread(1);
        final ProgramThread other = new ProgramThread(2);
        final Object gate = new Object();

        assertFalse(locks.exit(owner, gate), "nobody holds it");
        locks.enter(owner, gate);
        assertFalse(locks.exit(other, gate), "someone else holds it");
        assertTrue(locks.held(gate));
    }

    @Test
    void exit_wakesOnlyTheThreadsQueuedForThatLock() {
        final MonitorTable locks = new MonitorTable();
        final ProgramThread owner = new ProgramThread(1);
        final ProgramThread atGate = new ProgramThread(2);
        final ProgramThread atDoor = new ProgramThread(3);
        final Object gate = new Object();
        final Object door = new Object();
        locks.enter(owner, gate);
        locks.enter(owner, door);
        locks.enter(atGate, gate);
        locks.enter(atDoor, door);
        atGate.wait = new IWait.Lock(gate);
        atDoor.wait = new IWait.Lock(door);

        locks.exit(owner, gate);

        assertFalse(atGate.waiting());
        assertTrue(atDoor.waiting());
    }

    @Test
    void exit_leavesAQueuedThreadThatNoLongerWaitsForTheLock() {
        final MonitorTable locks = new MonitorTable();
        final ProgramThread owner = new ProgramThread(1);
        final ProgramThread waiter = new ProgramThread(2);
        final Object gate = new Object();
        locks.enter(owner, gate);
        locks.enter(waiter, gate);
        waiter.wait = new IWait.Sleep(50);

        locks.exit(owner, gate);

        assertTrue(waiter.waiting(), "it went to sleep instead, and a released lock does not end a sleep");
    }

    @Test
    void releaseAll_letsGoOfOnlyThatThreadsLocksAndWakesTheirQueues() {
        final MonitorTable locks = new MonitorTable();
        final ProgramThread ending = new ProgramThread(1);
        final ProgramThread staying = new ProgramThread(2);
        final ProgramThread waiter = new ProgramThread(3);
        final Object gate = new Object();
        final Object door = new Object();
        final Object hatch = new Object();
        locks.enter(ending, gate);
        locks.enter(ending, gate);
        locks.enter(ending, door);
        locks.enter(staying, hatch);
        locks.enter(waiter, gate);
        waiter.wait = new IWait.Lock(gate);

        locks.releaseAll(ending.id);

        assertFalse(locks.held(gate), "however many times over it was taken");
        assertFalse(locks.held(door));
        assertTrue(locks.held(hatch));
        assertFalse(waiter.waiting());
    }

    @Test
    void clear_forgetsEveryLock() {
        final MonitorTable locks = new MonitorTable();
        final Object gate = new Object();
        locks.enter(new ProgramThread(1), gate);

        locks.clear();

        assertFalse(locks.held(gate));
    }

    @Test
    void forEach_tellsOfEveryHeldLockWithItsOwnerAndCount() {
        final MonitorTable locks = new MonitorTable();
        final ProgramThread first = new ProgramThread(1);
        final ProgramThread second = new ProgramThread(2);
        final Object gate = new Object();
        final Object door = new Object();
        locks.enter(first, gate);
        locks.enter(first, gate);
        locks.enter(second, door);
        final List<String> told = new ArrayList<>();

        locks.forEach((target, owner, count) ->
                told.add((target == gate ? "gate" : "door") + " " + owner + "x" + count));

        assertEquals(2, told.size());
        assertTrue(told.containsAll(List.of("gate 1x2", "door 2x1")), () -> String.valueOf(told));
    }

    @Test
    void requeue_letsAThreadThatCameBackWaitingBeWokenByTheRelease() {
        final MonitorTable locks = new MonitorTable();
        final ProgramThread owner = new ProgramThread(1);
        final ProgramThread waiter = new ProgramThread(2);
        final ProgramThread elsewhere = new ProgramThread(3);
        final Object gate = new Object();
        locks.restore(gate, owner.id, 1);
        waiter.wait = new IWait.Lock(gate);
        elsewhere.wait = new IWait.Lock(new Object());

        locks.requeue(List.of(owner, waiter, elsewhere));

        assertTrue(locks.exit(owner, gate));
        assertFalse(waiter.waiting());
        assertTrue(elsewhere.waiting(), "a lock nobody holds is left to the scheduler's first look");
    }
}
