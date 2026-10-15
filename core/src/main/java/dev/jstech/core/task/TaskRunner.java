/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.task;

import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import org.jetbrains.annotations.ApiStatus;

/**
 * Work done away from the main thread, and its result handed back to the main thread, where the game may be touched:
 * the work runs on a thread of its own (a virtual one, cheap enough for one per task), and when it is done what is
 * to happen with its result waits until the main thread {@link #drain drains} it, a few at a time within a budget,
 * so a burst of finished work never stretches one tick.
 *
 * <p>The server's runner is {@link Tasks}; a runner of one's own is drained wherever one's main thread is.
 */
@ApiStatus.Experimental
public final class TaskRunner {

    private final ExecutorService workers;
    private final LongSupplier nanoClock;
    private final Queue<Runnable> handBacks = new ConcurrentLinkedQueue<>();
    private final Set<Task<?>> live = ConcurrentHashMap.newKeySet();

    /** A runner on {@code workers}, timing its budget by {@code nanoClock}. */
    public TaskRunner(final ExecutorService workers, final LongSupplier nanoClock) {
        this.workers = workers;
        this.nanoClock = nanoClock;
    }

    /** A runner whose every task runs on a virtual thread of its own. */
    public static TaskRunner onVirtualThreads() {
        return new TaskRunner(Executors.newVirtualThreadPerTaskExecutor(), System::nanoTime);
    }

    /**
     * Runs {@code work} away from the main thread, and hands what it gives to {@code onDone}, or what it threw to
     * {@code onFailure}, on the main thread, at a {@link #drain}; neither, once it is cancelled.
     */
    public <T> Task<T> submit(final Callable<? extends T> work, final Consumer<? super T> onDone,
                              final Consumer<? super Throwable> onFailure) {
        final Task<T> task = new Task<>();
        live.add(task);
        task.runningAs(workers.submit(() -> {
            if (task.cancelled()) {
                live.remove(task);
                return;
            }
            try {
                final T result = work.call();
                handBack(task, () -> onDone.accept(result));
            } catch (final Throwable failure) {
                handBack(task, () -> onFailure.accept(failure));
            }
        }));
        return task;
    }

    /**
     * Hands back, on the calling thread, what finished work waits to hand back, until {@code budgetNanos} are spent;
     * the rest waits for the next drain. At least one is handed back whenever one waits, so a small budget still moves.
     *
     * @return how many were handed back
     */
    public int drain(final long budgetNanos) {
        final long start = nanoClock.getAsLong();
        int handed = 0;
        Runnable next;
        while ((next = handBacks.poll()) != null) {
            next.run();
            handed++;
            if (nanoClock.getAsLong() - start >= budgetNanos) {
                break;
            }
        }
        return handed;
    }

    /** How many finished tasks wait to be handed back. */
    public int waiting() {
        return handBacks.size();
    }

    /** How many tasks are neither handed back nor cancelled. */
    public int live() {
        return (int) live.stream().filter(task -> !task.cancelled()).count();
    }

    /** Cancels every task not over yet: what a server does as it stops, so nothing reaches the next world. */
    public void cancelAll() {
        for (final Task<?> task : live) {
            task.cancel();
        }
        live.clear();
        handBacks.clear();
    }

    private void handBack(final Task<?> task, final Runnable handBack) {
        handBacks.add(() -> {
            live.remove(task);
            if (!task.cancelled()) {
                task.finish();
                handBack.run();
            }
        });
    }
}
