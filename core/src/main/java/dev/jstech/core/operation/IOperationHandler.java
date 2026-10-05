/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.operation;

/**
 * The code that carries out one kind of Operation, handed each request of that kind.
 *
 * <p>Some kinds finish on the spot, and their handler does the work and answers how it went. Others take time, a
 * search across many disks or a craft of many steps, and their handler only starts the work and answers
 * {@link OperationStatus#PENDING PENDING}; the network then follows the Operation through its other states on its
 * own ticks.
 *
 * @param <T> the arguments a request of this kind carries
 */
@FunctionalInterface
public interface IOperationHandler<T extends IOperationArgs> {

    /**
     * Carries out, or starts, one request.
     *
     * @param args what this request carries
     * @return the state the request is in now: a finished one, or {@link OperationStatus#PENDING PENDING} when the
     *         work goes on after this call
     */
    OperationStatus execute(T args);
}
