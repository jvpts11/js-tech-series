/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.task;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jetbrains.annotations.ApiStatus;

/**
 * A piece of work running away from the tick, as {@link TaskRunner#submit} handed it back: it can be cancelled, and
 * says whether it is over.
 *
 * @param <T> what the work gives
 */
@ApiStatus.Experimental
public final class Task<T> {

    private final AtomicBoolean cancelled = new AtomicBoolean();
    private volatile boolean over;
    private volatile Future<?> running;

    Task() {
    }

    /**
     * Stops the work: a thread still at it is interrupted, and what it would have handed back to the main thread is
     * not handed back, even if it had already finished.
     */
    public void cancel() {
        if (cancelled.compareAndSet(false, true)) {
            over = true;
            final Future<?> future = running;
            if (future != null) {
                future.cancel(true);
            }
        }
    }

    /** Whether it was cancelled. */
    public boolean cancelled() {
        return cancelled.get();
    }

    /** Whether it is over: its result or its failure was handed back on the main thread, or it was cancelled. */
    public boolean over() {
        return over;
    }

    void runningAs(final Future<?> future) {
        running = future;
        if (cancelled()) {
            future.cancel(true);
        }
    }

    void finish() {
        over = true;
    }
}
