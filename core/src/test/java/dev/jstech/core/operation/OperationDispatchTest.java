/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.operation;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class OperationDispatchTest {

    private OperationDispatch dispatch;

    @AfterEach
    void tearDown() {
        if (dispatch != null) {
            dispatch.close();
        }
    }

    private void tickUntilTerminal(final UUID id) {
        for (int attempt = 0; attempt < 200; attempt++) {
            dispatch.tick();
            if (dispatch.statusOf(id).isTerminal()) {
                return;
            }
            sleep(5);
        }
        fail("operation " + id + " did not reach a terminal state");
    }

    private static void sleep(final long millis) {
        try {
            Thread.sleep(millis);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    void successfulTask_completes() {
        dispatch = new OperationDispatch(1);
        final UUID id = dispatch.submit(context -> IOperationResult.success(), OperationPriority.MEDIUM);
        tickUntilTerminal(id);
        assertEquals(OperationStatus.COMPLETED, dispatch.statusOf(id));
    }

    @Test
    void throwingTask_marksFailedWithoutCrashing() {
        dispatch = new OperationDispatch(1);
        final UUID id = dispatch.submit(context -> {
            throw new IllegalStateException("boom");
        }, OperationPriority.MEDIUM);
        tickUntilTerminal(id);
        assertEquals(OperationStatus.FAILED, dispatch.statusOf(id));
    }

    @Test
    void nullResult_marksFailed() {
        dispatch = new OperationDispatch(1);
        final UUID id = dispatch.submit(context -> null, OperationPriority.MEDIUM);
        tickUntilTerminal(id);
        assertEquals(OperationStatus.FAILED, dispatch.statusOf(id));
    }

    @Test
    void unknownId_reportsPending() {
        dispatch = new OperationDispatch(1);
        assertEquals(OperationStatus.PENDING, dispatch.statusOf(UUID.randomUUID()));
    }

    @Test
    void sideEffect_runsOnTickThreadAndTaskOnVirtualThread() {
        dispatch = new OperationDispatch(1);
        final Thread tickThread = Thread.currentThread();
        final AtomicReference<Thread> taskThread = new AtomicReference<>();
        final AtomicReference<Thread> sideEffectThread = new AtomicReference<>();

        final UUID id = dispatch.submit(context -> {
            taskThread.set(Thread.currentThread());
            context.onMainThread(() -> sideEffectThread.set(Thread.currentThread()));
            return IOperationResult.success();
        }, OperationPriority.MEDIUM);

        tickUntilTerminal(id);

        assertEquals(tickThread, sideEffectThread.get(),
                "world side effects must run on the tick (main) thread");
        assertNotEquals(tickThread, taskThread.get(),
                "task work must run off the main thread (a virtual thread)");
        assertTrue(taskThread.get().isVirtual(), "task must run on a virtual thread");
    }

    @Test
    void higherPriority_runsFirst() {
        dispatch = new OperationDispatch(1); // single queue forces a strict order
        final List<String> order = new CopyOnWriteArrayList<>();
        final UUID low = dispatch.submit(context -> {
            order.add("low");
            return IOperationResult.success();
        }, OperationPriority.LOW);
        final UUID high = dispatch.submit(context -> {
            order.add("high");
            return IOperationResult.success();
        }, OperationPriority.HIGH);

        tickUntilTerminal(low);
        tickUntilTerminal(high);

        assertEquals(List.of("high", "low"), order);
    }

    @Test
    void concurrency_neverExceedsQueueCount() {
        dispatch = new OperationDispatch(2); // two parallel queues
        final CountDownLatch release = new CountDownLatch(1);
        final AtomicInteger concurrent = new AtomicInteger();
        final AtomicInteger maxConcurrent = new AtomicInteger();

        final IOperationTask blocking = context -> {
            maxConcurrent.accumulateAndGet(concurrent.incrementAndGet(), Math::max);
            try {
                release.await();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            concurrent.decrementAndGet();
            return IOperationResult.success();
        };

        final UUID a = dispatch.submit(blocking, OperationPriority.MEDIUM);
        final UUID b = dispatch.submit(blocking, OperationPriority.MEDIUM);
        final UUID c = dispatch.submit(blocking, OperationPriority.MEDIUM);

        dispatch.tick(); // promote up to two
        for (int i = 0; i < 200 && concurrent.get() < 2; i++) {
            sleep(5);
        }

        assertEquals(2, dispatch.runningCount());
        assertEquals(1, dispatch.pendingCount());

        release.countDown();
        tickUntilTerminal(a);
        tickUntilTerminal(b);
        tickUntilTerminal(c);

        assertEquals(2, maxConcurrent.get(), "no more than two tasks may run at once");
        assertEquals(OperationStatus.COMPLETED, dispatch.statusOf(c));
    }

    @Test
    void cancelPending_discards() {
        dispatch = new OperationDispatch(1);
        final UUID id = dispatch.submit(context -> IOperationResult.success(), OperationPriority.MEDIUM);
        assertTrue(dispatch.cancel(id));
        assertEquals(OperationStatus.DISCARDED, dispatch.statusOf(id));
        assertEquals(0, dispatch.pendingCount());
    }

    @Test
    void cancelUnknown_returnsFalse() {
        dispatch = new OperationDispatch(1);
        assertFalse(dispatch.cancel(UUID.randomUUID()));
    }

    @Test
    void constructor_rejectsZeroQueues() {
        try {
            new OperationDispatch(0);
            fail("expected IllegalArgumentException");
        } catch (final IllegalArgumentException expected) {
            // expected
        }
    }

    @Test
    void completedAndFailedCounts_tallyTerminalOutcomes() {
        dispatch = new OperationDispatch(1);
        final UUID ok = dispatch.submit(context -> IOperationResult.success(), OperationPriority.MEDIUM);
        tickUntilTerminal(ok);
        final UUID bad = dispatch.submit(context -> IOperationResult.failure("nope"), OperationPriority.MEDIUM);
        tickUntilTerminal(bad);
        assertEquals(1L, dispatch.completedCount());
        assertEquals(1L, dispatch.failedCount());
    }

    @Test
    void parallelQueues_reportsConfiguredCount() {
        dispatch = new OperationDispatch(3);
        assertEquals(3, dispatch.parallelQueues());
    }

    @Test
    void runOnMain_returnsValueComputedOnTickThread() {
        dispatch = new OperationDispatch(1);
        final Thread tickThread = Thread.currentThread();
        final AtomicReference<Thread> computeThread = new AtomicReference<>();
        final AtomicReference<Thread> taskThread = new AtomicReference<>();
        final AtomicInteger value = new AtomicInteger(-1);

        final UUID id = dispatch.submit(context -> {
            taskThread.set(Thread.currentThread());
            final int result = context.runOnMain(() -> {
                computeThread.set(Thread.currentThread());
                return 7 * 6;
            });
            value.set(result);
            return IOperationResult.success();
        }, OperationPriority.MEDIUM);

        tickUntilTerminal(id);

        assertEquals(42, value.get(), "runOnMain must return the value computed on the tick thread");
        assertEquals(tickThread, computeThread.get(), "the supplier must run on the tick (main) thread");
        assertTrue(taskThread.get().isVirtual(), "the task itself must run on a virtual thread");
    }

    @Test
    void awaitTicks_completesOnlyAfterEnoughTicksElapse() {
        dispatch = new OperationDispatch(1);
        final UUID id = dispatch.submit(context -> {
            context.awaitTicks(3);
            return IOperationResult.success();
        }, OperationPriority.MEDIUM);

        // Tick 1 promotes the task; it then blocks until three ticks have elapsed from that point.
        for (int i = 0; i < 3; i++) {
            dispatch.tick();
            sleep(5);
            assertFalse(dispatch.statusOf(id).isTerminal(),
                    "task must not finish before the awaited ticks elapse (tick " + i + ")");
        }
        tickUntilTerminal(id);
        assertEquals(OperationStatus.COMPLETED, dispatch.statusOf(id));
    }

    @Test
    void close_unblocksTaskWaitingOnTicks() {
        dispatch = new OperationDispatch(1);
        final CountDownLatch started = new CountDownLatch(1);
        final AtomicReference<Boolean> cancelled = new AtomicReference<>(Boolean.FALSE);

        dispatch.submit(context -> {
            started.countDown();
            try {
                context.awaitTicks(1_000_000);
            } catch (final OperationCancelledException expected) {
                cancelled.set(Boolean.TRUE);
                throw expected;
            }
            return IOperationResult.success();
        }, OperationPriority.MEDIUM);

        dispatch.tick(); // promote and let it enter the long wait
        try {
            started.await();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        sleep(20);

        dispatch.close();
        dispatch = null; // tearDown must not double-close

        for (int i = 0; i < 200 && !cancelled.get(); i++) {
            sleep(5);
        }
        assertTrue(cancelled.get(), "closing the dispatcher must unblock a task waiting on ticks");
    }

    @Test
    void terminalStatuses_areBoundedAndEvictTheOldest() {
        dispatch = new OperationDispatch(4);
        final List<UUID> ids = new java.util.ArrayList<>();
        for (int i = 0; i < 300; i++) {
            ids.add(dispatch.submit(context -> IOperationResult.success(), OperationPriority.MEDIUM));
        }
        for (final UUID id : ids) {
            tickUntilTerminal(id);
        }
        // The most recently settled Operation keeps its terminal status...
        assertTrue(dispatch.statusOf(ids.get(299)).isTerminal(),
                "a recent terminal status is retained");
        /*
         * ...but an old one is evicted once the bounded history fills, falling back to the unknown-id
         * default, so the status map can never grow without bound on a long-lived dispatcher.
         */
        assertEquals(OperationStatus.PENDING, dispatch.statusOf(ids.get(0)),
                "an old terminal status is evicted");
    }

    @Test
    void failure_keepsTheReasonItGave() {
        dispatch = new OperationDispatch(1);
        final UUID id = dispatch.submit(
                context -> IOperationResult.failure("jscore.test.reason", "Iron Ingot"), OperationPriority.MEDIUM);
        tickUntilTerminal(id);
        assertEquals(OperationStatus.FAILED, dispatch.statusOf(id));
        assertEquals("jscore.test.reason", dispatch.failureOf(id).key());
        assertEquals(List.of("Iron Ingot"), dispatch.failureOf(id).arguments());
    }

    @Test
    void success_hasNoReasonToGive() {
        dispatch = new OperationDispatch(1);
        final UUID id = dispatch.submit(context -> IOperationResult.success(), OperationPriority.MEDIUM);
        tickUntilTerminal(id);
        assertFalse(dispatch.failureOf(id).isPresent());
    }

    /* What was thrown is the only thing anybody has to go on, so both its type and its words are kept. */
    @Test
    void thrownTask_keepsWhatWasThrownAsTheReason() {
        dispatch = new OperationDispatch(1);
        final UUID id = dispatch.submit(context -> {
            throw new IllegalStateException("the rack went away");
        }, OperationPriority.MEDIUM);
        tickUntilTerminal(id);
        final OperationFailure why = dispatch.failureOf(id);
        assertEquals(List.of("IllegalStateException", "the rack went away"), why.arguments());
    }

    @Test
    void close_discardsWhatWasStillQueued() {
        dispatch = new OperationDispatch(1);
        final UUID queued = dispatch.submit(context -> IOperationResult.success(), OperationPriority.MEDIUM);
        assertEquals(OperationStatus.PENDING, dispatch.statusOf(queued));
        dispatch.close();
        assertEquals(OperationStatus.DISCARDED, dispatch.statusOf(queued),
                "an Operation the dispatcher will never run must say so");
        dispatch = null;
    }

    /*
     * The one that was already running matters more than the queued one: it read as still running, so a view
     * showed a craft under way by a machine that had been switched off, with nothing ever to move it off that.
     */
    @Test
    void close_discardsWhatWasAlreadyRunning() {
        dispatch = new OperationDispatch(1);
        final CountDownLatch started = new CountDownLatch(1);
        final UUID running = dispatch.submit(context -> {
            started.countDown();
            context.awaitTicks(1_000_000);
            return IOperationResult.success();
        }, OperationPriority.MEDIUM);
        dispatch.tick();
        try {
            started.await();
        } catch (final InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
        assertEquals(OperationStatus.PROCESSING, dispatch.statusOf(running));
        dispatch.close();
        assertEquals(OperationStatus.DISCARDED, dispatch.statusOf(running));
        dispatch = null;
    }

    @Test
    void close_refusesAnAnswerThatArrivesAfterwards() {
        dispatch = new OperationDispatch(1);
        final CountDownLatch started = new CountDownLatch(1);
        final CountDownLatch mayFinish = new CountDownLatch(1);
        final UUID id = dispatch.submit(context -> {
            started.countDown();
            try {
                mayFinish.await();
            } catch (final InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
            return IOperationResult.success();
        }, OperationPriority.MEDIUM);
        dispatch.tick();
        try {
            started.await();
        } catch (final InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
        dispatch.close();
        mayFinish.countDown();
        sleep(50);
        assertEquals(OperationStatus.DISCARDED, dispatch.statusOf(id),
                "a late answer must not undo the discarding");
        assertEquals(0L, dispatch.completedCount(), "nor must it be counted as one that finished");
        dispatch = null;
    }

    @Test
    void submit_afterCloseGivesBackAnOperationAlreadyDiscarded() {
        dispatch = new OperationDispatch(1);
        dispatch.close();
        final UUID id = dispatch.submit(context -> IOperationResult.success(), OperationPriority.MEDIUM);
        assertEquals(OperationStatus.DISCARDED, dispatch.statusOf(id));
        assertEquals(0, dispatch.pendingCount(), "a closed dispatcher queues nothing");
        dispatch = null;
    }

    /*
     * The queue of waiting Operations is a plain one, on purpose, because it is touched once a tick. Written
     * from two threads it does not fail where the mistake is, so it has to fail at the door instead.
     */
    @Test
    void submit_refusesAnyThreadButTheOneThatOwnsIt() throws Exception {
        dispatch = new OperationDispatch(1);
        final AtomicReference<Throwable> refused = new AtomicReference<>();
        final Thread other = new Thread(() -> {
            try {
                dispatch.submit(context -> IOperationResult.success(), OperationPriority.MEDIUM);
            } catch (final Throwable thrown) {
                refused.set(thrown);
            }
        }, "not-the-owner");
        other.start();
        other.join();
        assertTrue(refused.get() instanceof IllegalStateException,
                "queueing from another thread must be refused, not quietly corrupt the queue");
        assertTrue(refused.get().getMessage().contains("not-the-owner"),
                "and the message must name the thread that did it");
    }

    @Test
    void tick_refusesAnyThreadButTheOneThatOwnsIt() throws Exception {
        dispatch = new OperationDispatch(1);
        final AtomicReference<Throwable> refused = new AtomicReference<>();
        final Thread other = new Thread(() -> {
            try {
                dispatch.tick();
            } catch (final Throwable thrown) {
                refused.set(thrown);
            }
        }, "not-the-owner");
        other.start();
        other.join();
        assertTrue(refused.get() instanceof IllegalStateException);
    }
}
