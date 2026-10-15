/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TaskRunnerTest {

    private static final long WAIT_MILLIS = 5_000;

    private ExecutorService workers;
    private AtomicLong clock;
    private TaskRunner runner;

    @BeforeEach
    void setUp() {
        workers = Executors.newVirtualThreadPerTaskExecutor();
        clock = new AtomicLong();
        runner = new TaskRunner(workers, clock::get);
    }

    @AfterEach
    void tearDown() {
        workers.shutdownNow();
    }

    @Test
    void drain_handsTheResultBackOnTheThreadThatDrains() {
        final List<Thread> handedOn = new ArrayList<>();
        final List<Integer> results = new ArrayList<>();
        final Task<Integer> task = runner.submit(() -> 6 * 7, result -> {
            handedOn.add(Thread.currentThread());
            results.add(result);
        }, failure -> { });

        waitUntilWaiting(1);
        assertFalse(task.over(), "nothing is handed back before a drain");
        runner.drain(Long.MAX_VALUE);

        assertEquals(List.of(42), results);
        assertSame(Thread.currentThread(), handedOn.getFirst());
        assertTrue(task.over());
    }

    @Test
    void drain_stopsOnceTheBudgetIsSpent() {
        for (int i = 0; i < 3; i++) {
            runner.submit(() -> 1, result -> clock.addAndGet(1_000), failure -> { });
        }
        waitUntilWaiting(3);

        assertEquals(2, runner.drain(1_500), "two hand-backs of a microsecond each spend a budget of 1.5");
        assertEquals(1, runner.waiting());
        assertEquals(1, runner.drain(1_500));
    }

    @Test
    void drain_handsOneBackEvenWithNoBudget() {
        runner.submit(() -> 1, result -> { }, failure -> { });
        waitUntilWaiting(1);

        assertEquals(1, runner.drain(0));
    }

    @Test
    void cancel_keepsWhatTheWorkGaveFromBeingHandedBack() {
        final List<Integer> results = new ArrayList<>();
        final Task<Integer> task = runner.submit(() -> 1, results::add, failure -> { });
        waitUntilWaiting(1);

        task.cancel();
        runner.drain(Long.MAX_VALUE);

        assertEquals(List.of(), results);
        assertTrue(task.cancelled() && task.over());
        assertEquals(0, runner.live());
    }

    @Test
    void cancel_interruptsWorkStillRunning() throws InterruptedException {
        final CountDownLatch started = new CountDownLatch(1);
        final CountDownLatch interrupted = new CountDownLatch(1);
        final Task<Object> task = runner.submit(() -> {
            started.countDown();
            try {
                Thread.sleep(WAIT_MILLIS * 10);
            } catch (final InterruptedException expected) {
                interrupted.countDown();
            }
            return null;
        }, result -> { }, failure -> { });
        assertTrue(started.await(WAIT_MILLIS, TimeUnit.MILLISECONDS));

        task.cancel();

        assertTrue(interrupted.await(WAIT_MILLIS, TimeUnit.MILLISECONDS), "the thread at the work is interrupted");
    }

    @Test
    void submit_handsWhatTheWorkThrewToTheFailure() {
        final List<Throwable> failures = new ArrayList<>();
        runner.submit(() -> {
            throw new IllegalStateException("broken");
        }, result -> { }, failures::add);
        waitUntilWaiting(1);

        runner.drain(Long.MAX_VALUE);

        assertInstanceOf(IllegalStateException.class, failures.getFirst());
    }

    @Test
    void cancelAll_leavesNothingToHandBack() {
        final List<Integer> results = new ArrayList<>();
        runner.submit(() -> 1, results::add, failure -> { });
        runner.submit(() -> 2, results::add, failure -> { });
        waitUntilWaiting(2);

        runner.cancelAll();
        runner.drain(Long.MAX_VALUE);

        assertEquals(List.of(), results);
        assertEquals(0, runner.waiting());
    }

    private void waitUntilWaiting(final int count) {
        final long until = System.currentTimeMillis() + WAIT_MILLIS;
        while (runner.waiting() < count && System.currentTimeMillis() < until) {
            Thread.onSpinWait();
        }
        assertEquals(count, runner.waiting(), "the work finished on its threads in time");
    }
}
