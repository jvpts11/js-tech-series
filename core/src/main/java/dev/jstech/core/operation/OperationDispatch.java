/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.operation;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * The Operation dispatcher: runs CPU-bound Operation work on virtual threads while keeping every world mutation on the main (server) thread. A task does its computation on a virtual thread and uses its {@link IOperationContext} to bounce world reads/writes back to the main thread and to wait on whole game ticks, so disk latency and throughput are paced deterministically by ticks, never by a wall clock. Multiple disks (one task per server) therefore process in parallel without ever touching the world off-thread.
 */
@TextHolder
public final class OperationDispatch implements AutoCloseable, ILatencyScheduler {

    private record PendingOp(UUID id, IOperationTask task, OperationPriority priority, long sequence) {
    }

    private static final Comparator<PendingOp> ORDER =
            Comparator.comparing(PendingOp::priority).reversed()
                    .thenComparingLong(PendingOp::sequence);

    private static final int MAX_TERMINAL_HISTORY = 256;

    /** An Operation that ran to the end and then answered with nothing at all, which is a mistake in its code. */
    private static final TextKey NO_RESULT = TextKey.of("jscore.operation.failure.no_result",
            "the Operation finished without saying what it did");

    /** An Operation given up on because the machine running it stopped. */
    private static final TextKey CANCELLED = TextKey.of("jscore.operation.failure.cancelled",
            "the machine running it stopped");

    /** An Operation that threw: the type of what was thrown, then what it said. */
    private static final TextKey CRASHED = TextKey.of("jscore.operation.failure.crashed",
            "the Operation ran into a problem: %s (%s)");

    /*
     * Not final: the lane count follows the Mainframe's GPU count, which the player can change at runtime by
     * hot-swapping a GPU. It is resized in place (see setParallelQueues) rather than by rebuilding the dispatcher,
     * so a hardware change never tears down the in-flight Operations the Mainframe is tracking.
     */
    private volatile int parallelQueues;
    private final ExecutorService workers;
    private final PriorityQueue<PendingOp> pending = new PriorityQueue<>(ORDER);
    private final ConcurrentLinkedQueue<Runnable> mainThreadActions = new ConcurrentLinkedQueue<>();
    private final Map<UUID, OperationStatus> statuses = new ConcurrentHashMap<>();
    private final Map<UUID, OperationFailure> failures = new ConcurrentHashMap<>();
    /*
     * The one thread allowed to queue, promote, cancel and settle. The queue of waiting Operations, the running
     * count and the settle order are plain objects on purpose, because they are touched once a tick and a lock
     * around them would be paid for by every Operation; what keeps them safe is that only this thread reaches
     * them. That was written down nowhere before, so an Operation queued from anywhere else silently corrupted
     * the queue instead of saying so.
     */
    private final Thread owner;
    /*
     * Settle order, so the status map keeps only the most recent terminal entries: without this it would
     * grow one entry per Operation forever on a long-lived dispatcher. Touched on the main thread only.
     */
    private final ArrayDeque<UUID> terminalOrder = new ArrayDeque<>();
    private final Set<CompletableFuture<?>> inFlight = ConcurrentHashMap.newKeySet();
    private final Object tickMonitor = new Object();

    private volatile long tickCount;
    private volatile boolean closed;
    private long sequenceCounter;
    private int running;
    private long completed;
    private long failed;

    /**
     * A dispatcher owned by the thread that builds it, which is the thread the world runs on.
     *
     * <p>Every dispatcher is built while the world is being ticked, so the thread doing the building is the one
     * that will be ticking it. Taking the owner from here rather than being told it is what keeps this class
     * free of any knowledge of what a server is.
     */
    public OperationDispatch(final int parallelQueues) {
        if (parallelQueues < 1) {
            throw new IllegalArgumentException("parallelQueues must be >= 1; got " + parallelQueues);
        }
        this.parallelQueues = parallelQueues;
        this.owner = Thread.currentThread();
        this.workers = Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * Queues an Operation and gives back the name it will be known by.
     *
     * <p>Queued after the dispatcher is closed, it is given back already discarded rather than refused: whoever
     * asked has an id to look up and will find out it will never run, which is what happens to a craft asked for
     * in the same tick the Mainframe loses power.
     */
    public UUID submit(final IOperationTask task, final OperationPriority priority) {
        Objects.requireNonNull(task, "task must not be null");
        Objects.requireNonNull(priority, "priority must not be null");
        requireOwnerThread("submit");
        final UUID id = UUID.randomUUID();
        if (closed) {
            statuses.put(id, OperationStatus.DISCARDED);
            rememberTerminal(id);
            return id;
        }
        statuses.put(id, OperationStatus.PENDING);
        pending.add(new PendingOp(id, task, priority, sequenceCounter++));
        return id;
    }

    /** The current server-tick count; advanced once per {@link #tick()}. */
    public long tickCount() {
        return tickCount;
    }

    public void tick() {
        requireOwnerThread("tick");
        if (closed) {
            return;
        }
        // 1. Apply everything the virtual threads asked the main thread to do since the last tick.
        Runnable action;
        while ((action = mainThreadActions.poll()) != null) {
            action.run();
        }
        // 2. Promote pending Operations onto virtual threads, up to the parallel-queue capacity.
        while (running < parallelQueues) {
            final PendingOp op = pending.poll();
            if (op == null) {
                break;
            }
            statuses.put(op.id(), OperationStatus.PROCESSING);
            running++;
            dispatch(op);
        }
        // 3. Advance the tick clock and wake any task waiting on ticks (disk latency / pacing).
        synchronized (tickMonitor) {
            tickCount++;
            tickMonitor.notifyAll();
        }
    }

    @Override
    public void afterTicks(final int ticks, final Runnable callback) {
        Objects.requireNonNull(callback, "callback must not be null");
        /*
         * Park a virtual thread for the disk's read time, then resume the transfer on the main thread.
         * Many disks call this at once, so their reads genuinely overlap, capped only by the latency.
         */
        workers.execute(() -> {
            try {
                awaitTicks(ticks);
            } catch (final OperationCancelledException cancelled) {
                return; // dispatcher shut down, the Operation is being abandoned, drop the read
            }
            mainThreadActions.add(callback);
        });
    }

    /**
     * Blocks the calling virtual thread until {@code ticks} server ticks have elapsed, or throws
     * {@link OperationCancelledException} if the dispatcher shuts down while waiting. Shared by the
     * per-task context and {@link #afterTicks}.
     */
    private void awaitTicks(final int ticks) {
        if (ticks <= 0) {
            return;
        }
        final long target = tickCount + ticks;
        synchronized (tickMonitor) {
            while (tickCount < target) {
                if (closed) {
                    throw new OperationCancelledException();
                }
                try {
                    tickMonitor.wait();
                } catch (final InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new OperationCancelledException();
                }
            }
        }
    }

    private void dispatch(final PendingOp op) {
        final IOperationContext context = new DispatchContext();
        workers.execute(() -> {
            IOperationResult result;
            try {
                result = op.task().run(context);
                if (result == null) {
                    result = IOperationResult.failure(NO_RESULT.key());
                }
            } catch (final OperationCancelledException cancelled) {
                result = IOperationResult.failure(CANCELLED.key());
            } catch (final Throwable throwable) {
                /*
                 * The type and the message both, because a player reads the line and whoever is asked about it
                 * afterwards reads the same line: an exception with no message says nothing without its type,
                 * and a message with no type rarely says where it came from.
                 */
                result = IOperationResult.failure(CRASHED.key(),
                        throwable.getClass().getSimpleName(),
                        String.valueOf(throwable.getMessage()));
            }
            final IOperationResult finalResult = result;
            mainThreadActions.add(() -> complete(op.id(), finalResult));
        });
    }

    /*
     * Reached one tick after the work itself finished, which leaves room for the dispatcher to have been closed
     * in between. An Operation settling then was already discarded by the close, and letting it settle again
     * would count it twice and undo the discarding, so a late answer is dropped where it arrives.
     */
    private void complete(final UUID id, final IOperationResult result) {
        if (closed || statuses.get(id) != OperationStatus.PROCESSING) {
            return;
        }
        if (result instanceof IOperationResult.Failure failure) {
            statuses.put(id, OperationStatus.FAILED);
            if (failure.cause().isPresent()) {
                failures.put(id, failure.cause());
            }
            this.failed++;
        } else {
            statuses.put(id, OperationStatus.COMPLETED);
            completed++;
        }
        rememberTerminal(id);
        running--;
    }

    public OperationStatus statusOf(final UUID id) {
        return statuses.getOrDefault(id, OperationStatus.PENDING);
    }

    /**
     * Why that Operation failed, or {@link OperationFailure#NONE} where there is nothing to say.
     *
     * <p>A reason lasts as long as the state it belongs to, so an Operation old enough to have fallen out of the
     * settled history has no reason to give either.
     */
    public OperationFailure failureOf(final UUID id) {
        return failures.getOrDefault(id, OperationFailure.NONE);
    }

    public boolean cancel(final UUID id) {
        requireOwnerThread("cancel");
        if (pending.removeIf(op -> op.id().equals(id))) {
            statuses.put(id, OperationStatus.DISCARDED);
            rememberTerminal(id);
            return true;
        }
        return false;
    }

    /**
     * Records a settled Operation and evicts the oldest once more than {@value #MAX_TERMINAL_HISTORY}
     * have settled, so the status map stays bounded. A caller that polls a just-settled status (the
     * usual pattern) still sees it; only long-stale terminal entries fall back to {@code PENDING}.
     */
    private void rememberTerminal(final UUID id) {
        terminalOrder.addLast(id);
        while (terminalOrder.size() > MAX_TERMINAL_HISTORY) {
            final UUID oldest = terminalOrder.pollFirst();
            statuses.remove(oldest);
            failures.remove(oldest);
        }
    }

    /**
     * Refuses a call from anywhere but the thread that owns this dispatcher.
     *
     * <p>Loudly, because the alternative is what used to happen: a queue built for one thread being written by
     * two, which does not fail where the mistake is but somewhere else entirely, a tick or an hour later.
     */
    private void requireOwnerThread(final String what) {
        final Thread current = Thread.currentThread();
        if (current != this.owner) {
            throw new IllegalStateException(what + " is only for the thread that owns the dispatcher ("
                    + this.owner.getName() + "); it was called from " + current.getName());
        }
    }

    public int runningCount() {
        return running;
    }

    public int pendingCount() {
        return pending.size();
    }

    public int parallelQueues() {
        return parallelQueues;
    }

    /**
     * Resizes the number of parallel lanes in place, following the Mainframe's GPU count. The next {@link #tick()}
     * promotes up to the new count; already-running tasks are never interrupted. This exists so hot-swapping a GPU
     * changes throughput without rebuilding the dispatcher, which would otherwise abandon every in-flight Operation.
     */
    public void setParallelQueues(final int queues) {
        if (queues < 1) {
            throw new IllegalArgumentException("parallelQueues must be >= 1; got " + queues);
        }
        this.parallelQueues = queues;
    }

    public long completedCount() {
        return completed;
    }

    public long failedCount() {
        return failed;
    }

    /**
     * Shuts the dispatcher down, and says so about every Operation it was holding.
     *
     * <p>What was queued and what was running both end as discarded. That word is the whole point: an Operation
     * left sitting at the state it happened to be in read as still pending or still running, so a terminal showed
     * a craft as under way by a Mainframe that had been switched off, and it stayed that way until somebody broke
     * the machine. Whoever asked for it can see now that it will not be finished.
     */
    @Override
    public void close() {
        requireOwnerThread("close");
        if (closed) {
            return;
        }
        closed = true;
        for (PendingOp op = pending.poll(); op != null; op = pending.poll()) {
            discard(op.id());
        }
        /*
         * The running ones too. Their virtual threads are about to be interrupted, and the answer they would have
         * given arrives on a tick that will never come, so nothing else would ever move them off PROCESSING.
         */
        for (final Map.Entry<UUID, OperationStatus> entry : statuses.entrySet()) {
            if (!entry.getValue().isTerminal()) {
                discard(entry.getKey());
            }
        }
        running = 0;
        // Unblock every task waiting on the main thread or on a tick so no virtual thread hangs.
        for (final CompletableFuture<?> future : inFlight) {
            future.completeExceptionally(new OperationCancelledException());
        }
        synchronized (tickMonitor) {
            tickMonitor.notifyAll();
        }
        workers.shutdownNow();
    }

    private void discard(final UUID id) {
        statuses.put(id, OperationStatus.DISCARDED);
        failures.put(id, OperationFailure.of(CANCELLED.key()));
        rememberTerminal(id);
    }

    /** The per-task handle: marshals work to the main thread and waits on ticks, all virtual-thread-safe. */
    private final class DispatchContext implements IOperationContext {

        @Override
        public void onMainThread(final Runnable mainThreadAction) {
            mainThreadActions.add(mainThreadAction);
        }

        @Override
        public <T> T runOnMain(final Supplier<T> work) {
            if (closed) {
                throw new OperationCancelledException();
            }
            final CompletableFuture<T> future = new CompletableFuture<>();
            inFlight.add(future);
            /*
             * Close the race with close(): if a shutdown set closed after our first check but before
             * this add became visible, close()'s drain may have missed this future; re-check now so
             * we never join() on a future nobody will ever complete.
             */
            if (closed) {
                inFlight.remove(future);
                throw new OperationCancelledException();
            }
            mainThreadActions.add(() -> {
                inFlight.remove(future);
                try {
                    future.complete(work.get());
                } catch (final Throwable error) {
                    future.completeExceptionally(error);
                }
            });
            try {
                return future.join();
            } catch (final CompletionException joinError) {
                if (joinError.getCause() instanceof OperationCancelledException cancelled) {
                    throw cancelled;
                }
                throw new OperationCancelledException();
            } finally {
                inFlight.remove(future);
            }
        }

        @Override
        public void awaitTicks(final int ticks) {
            OperationDispatch.this.awaitTicks(ticks);
        }

        @Override
        public long currentTick() {
            return tickCount;
        }

        @Override
        public boolean isActive() {
            return !closed;
        }
    }
}
