/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.operation;

import java.util.Objects;

/**
 * Outcome of running an {@link IOperationTask} on a virtual thread.
 */
public sealed interface IOperationResult {

    /**
     * The task finished its work successfully.
     */
    record Success() implements IOperationResult {
    }

    /**
     * The task failed, and {@code cause} is why, in a form that can be shown to whoever asked for it.
     */
    record Failure(OperationFailure cause) implements IOperationResult {

        public Failure {
            Objects.requireNonNull(cause, "cause must not be null");
        }
    }

    static IOperationResult success() {
        return new Success();
    }

    /** A failure under that translation key, with whatever fills the holes in the line it names. */
    static IOperationResult failure(final String key, final String... arguments) {
        return new Failure(OperationFailure.of(key, arguments));
    }

    static IOperationResult failure(final OperationFailure cause) {
        return new Failure(cause);
    }
}
