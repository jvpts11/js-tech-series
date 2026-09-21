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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ThreadSchedulerTest {

    /** A world the test sets by hand, counting the questions waking asks of it. */
    private static final class World implements ThreadScheduler.IWorld {
        long now;
        boolean locked = true;
        boolean running = true;
        boolean typed;
        int asked;

        @Override
        public long now() {
            return this.now;
        }

        @Override
        public boolean locked(final Object target) {
            this.asked++;
            return this.locked;
        }

        @Override
        public boolean running(final int program, final String host) {
            this.asked++;
            return this.running;
        }

        @Override
        public boolean typed() {
            this.asked++;
            return this.typed;
        }
    }

    @Test
    void start_numbersThreadsFromTwoAfterTheMainOne() {
        final ThreadScheduler scheduler = new ThreadScheduler();

        assertEquals(1, scheduler.main().id);
        assertEquals(2, scheduler.start().id);
        assertEquals(3, scheduler.start().id);
        assertEquals(List.of(1, 2, 3), scheduler.threads().stream().map(thread -> thread.id).toList());
        assertEquals(4, scheduler.nextId());
    }

    @Test
    void restore_bringsBackTheMainThreadAndNeverLowersTheNextNumber() {
        final ThreadScheduler scheduler = new ThreadScheduler();

        assertSame(scheduler.main(), scheduler.restore(1));
        assertEquals(7, scheduler.restore(7).id);
        assertEquals(8, scheduler.nextId());
        scheduler.startFrom(3);
        assertEquals(8, scheduler.nextId(), "a number already given out is never given again");
        assertEquals(2, scheduler.threads().size());
    }

    @Test
    void remove_wakesOnlyTheThreadsJoinedToIt() {
        final ThreadScheduler scheduler = new ThreadScheduler();
        final ProgramThread target = scheduler.start();
        final ProgramThread other = scheduler.start();
        final ProgramThread joined = scheduler.start();
        final ProgramThread elsewhere = scheduler.start();
        scheduler.await(joined, new IWait.Join(target.id, 0));
        scheduler.await(elsewhere, new IWait.Join(other.id, 0));

        scheduler.remove(target);

        assertFalse(joined.waiting());
        assertTrue(elsewhere.waiting());
        assertNull(scheduler.thread(target.id));
    }

    @Test
    void thread_findsALiveThreadByItsNumberAndNothingElse() {
        final ThreadScheduler scheduler = new ThreadScheduler();
        final ProgramThread made = scheduler.start();

        assertSame(made, scheduler.thread(made.id));
        assertNull(scheduler.thread(99));
        assertNull(scheduler.thread("2"));
        assertNull(scheduler.thread(null));
    }

    @Test
    void ready_keepsTheOrderThreadsStartedInAndSkipsWhatCannotRun() {
        final ThreadScheduler scheduler = new ThreadScheduler();
        final ProgramThread second = scheduler.start();
        final ProgramThread third = scheduler.start();
        scheduler.await(second, IWait.INPUT);

        assertEquals(List.of(scheduler.main(), third), scheduler.ready(thread -> !thread.waiting()));
    }

    @Test
    void firstTurn_movesOnOneThreadARound() {
        final ThreadScheduler scheduler = new ThreadScheduler();

        assertEquals(0, scheduler.firstTurn(3));
        scheduler.turned(0);
        assertEquals(1, scheduler.firstTurn(3));
        scheduler.turned(2);
        assertEquals(0, scheduler.firstTurn(3), "past the last ready thread it comes back to the first");
    }

    @Test
    void wake_endsASleepAtItsTickAndNotBefore() {
        final ThreadScheduler scheduler = new ThreadScheduler();
        final ProgramThread sleeper = scheduler.start();
        scheduler.await(sleeper, new IWait.Sleep(10));
        final World world = new World();

        world.now = 9;
        scheduler.wake(world);
        assertTrue(sleeper.waiting());
        world.now = 10;
        scheduler.wake(world);
        assertFalse(sleeper.waiting());
    }

    @Test
    void wake_givesUpATimedJoinAtItsDeadlineAndSaysSoOnce() {
        final ThreadScheduler scheduler = new ThreadScheduler();
        final ProgramThread target = scheduler.start();
        final ProgramThread joiner = scheduler.start();
        scheduler.await(joiner, new IWait.Join(target.id, 5));
        final World world = new World();

        world.now = 4;
        scheduler.wake(world);
        assertTrue(joiner.waiting());
        world.now = 5;
        scheduler.wake(world);
        assertFalse(joiner.waiting());
        assertTrue(joiner.takeGaveUp());
        assertFalse(joiner.takeGaveUp(), "the answer is given once");
    }

    @Test
    void wake_endsAJoinWhoseThreadIsGoneWithoutGivingUp() {
        final ThreadScheduler scheduler = new ThreadScheduler();
        final ProgramThread joiner = scheduler.start();
        scheduler.await(joiner, new IWait.Join(42, 100));

        scheduler.wake(new World());

        assertFalse(joiner.waiting());
        assertFalse(joiner.takeGaveUp());
    }

    @Test
    void wake_endsAWaitOnAProgramThatStoppedAndGivesUpOneThatOutlastsItsDeadline() {
        final ThreadScheduler scheduler = new ThreadScheduler();
        final ProgramThread patient = scheduler.start();
        final ProgramThread hurried = scheduler.start();
        scheduler.await(patient, new IWait.Child(10, "lab", 0));
        scheduler.await(hurried, new IWait.Child(11, "lab", 3));
        final World world = new World();

        world.now = 3;
        scheduler.wake(world);
        assertTrue(patient.waiting());
        assertFalse(hurried.waiting());
        assertTrue(hurried.takeGaveUp());
        world.running = false;
        world.now = 4;
        scheduler.wake(world);
        assertFalse(patient.waiting());
        assertFalse(patient.takeGaveUp());
    }

    @Test
    void wake_asksNothingUntilTheTickReachesTheEarliestDeadline() {
        final ThreadScheduler scheduler = new ThreadScheduler();
        final ProgramThread sleeper = scheduler.start();
        final ProgramThread locker = scheduler.start();
        scheduler.await(sleeper, new IWait.Sleep(10));
        scheduler.await(locker, new IWait.Lock(new Object()));
        final World world = new World();
        scheduler.wake(world);
        final int firstLook = world.asked;

        world.now = 5;
        scheduler.wake(world);
        scheduler.wake(world);
        assertEquals(firstLook, world.asked, "nothing can be over before the deadline, so nothing is asked");
        world.now = 10;
        scheduler.wake(world);
        assertTrue(world.asked > firstLook);
        assertFalse(sleeper.waiting());
    }

    @Test
    void wake_asksAboutAProgramOnThisMachineOnceAndThenWaitsToBeTold() {
        final ThreadScheduler scheduler = new ThreadScheduler();
        final ProgramThread waiter = scheduler.start();
        final ProgramThread other = scheduler.start();
        scheduler.await(waiter, new IWait.Child(10, "", 0));
        scheduler.await(other, new IWait.Child(11, "", 0));
        final World world = new World();

        scheduler.wake(world);
        final int firstLook = world.asked;
        world.now = 1;
        scheduler.wake(world);
        world.now = 2;
        scheduler.wake(world);
        assertEquals(firstLook, world.asked, "the machine says when a program ends, so nothing more is asked");
        scheduler.programEnded(10);
        assertFalse(waiter.waiting());
        assertFalse(waiter.takeGaveUp());
        assertTrue(other.waiting(), "a wait on another program stays");
    }

    @Test
    void wake_givesUpAWaitOnAProgramOnThisMachineAtItsDeadline() {
        final ThreadScheduler scheduler = new ThreadScheduler();
        final ProgramThread waiter = scheduler.start();
        final World world = new World();
        scheduler.wake(world);
        scheduler.await(waiter, new IWait.Child(10, "", 3));

        world.now = 2;
        scheduler.wake(world);
        assertTrue(waiter.waiting(), "not before its deadline");
        world.now = 3;
        scheduler.wake(world);
        assertFalse(waiter.waiting(), "a deadline brings a look even though the machine tells when a program ends");
        assertTrue(waiter.takeGaveUp());
    }

    @Test
    void wake_asksAboutAProgramOnAnotherMachineAtMostOnceATick() {
        final ThreadScheduler scheduler = new ThreadScheduler();
        final ProgramThread waiter = scheduler.start();
        scheduler.await(waiter, new IWait.Child(10, "lab", 0));
        final World world = new World();

        world.now = 1;
        scheduler.wake(world);
        scheduler.wake(world);
        assertEquals(1, world.asked, "twice in one tick is asked once");
        world.now = 2;
        scheduler.wake(world);
        assertEquals(2, world.asked);
        world.running = false;
        world.now = 3;
        scheduler.wake(world);
        assertFalse(waiter.waiting());
        world.now = 4;
        scheduler.wake(world);
        assertEquals(3, world.asked, "once nobody waits on a program, the machine is not asked again");
    }

    @Test
    void wake_seesOnItsFirstLookAWaitSetWithoutTheSchedulerAsARestoreDoes() {
        final ThreadScheduler scheduler = new ThreadScheduler();
        final ProgramThread locker = scheduler.start();
        final ProgramThread reader = scheduler.start();
        locker.wait = new IWait.Lock(new Object());
        reader.wait = IWait.INPUT;
        final World world = new World();
        world.locked = false;
        world.typed = true;

        scheduler.wake(world);

        assertFalse(locker.waiting(), "a lock nobody held when the world came back");
        assertFalse(reader.waiting(), "a line typed before the save");
    }

    @Test
    void wake_leavesALockOrAReadToItsOwnEventOnceItHasLooked() {
        final ThreadScheduler scheduler = new ThreadScheduler();
        final ProgramThread locker = scheduler.start();
        final ProgramThread reader = scheduler.start();
        scheduler.await(locker, new IWait.Lock(new Object()));
        scheduler.await(reader, IWait.INPUT);
        final World world = new World();
        scheduler.wake(world);

        world.locked = false;
        world.typed = true;
        scheduler.wake(world);
        assertTrue(locker.waiting(), "a released lock tells its own queue, in the lock table");
        assertTrue(reader.waiting());
        scheduler.wakeReaders();
        assertFalse(reader.waiting());
    }

    @Test
    void wakeReaders_wakesEveryReaderAndAnyReaderSaysWhetherOneIsLeft() {
        final ThreadScheduler scheduler = new ThreadScheduler();
        final ProgramThread first = scheduler.start();
        final ProgramThread second = scheduler.start();
        final ProgramThread sleeper = scheduler.start();
        scheduler.await(first, IWait.INPUT);
        scheduler.await(second, IWait.INPUT);
        scheduler.await(sleeper, new IWait.Sleep(50));

        assertTrue(scheduler.anyReader());
        scheduler.wakeReaders();
        assertFalse(scheduler.anyReader());
        assertFalse(first.waiting());
        assertFalse(second.waiting());
        assertTrue(sleeper.waiting(), "a sleeper is not a reader");
    }
}
