/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.operation;

import org.jetbrains.annotations.ApiStatus;

/**
 * The code that carries out one kind of Operation, handed each request of that kind.
 *
 * <p>Some kinds finish on the spot, and their handler does the work and answers how it went. Others take time, a
 * search across many disks or an accelerator run of minutes, and their handler only starts the work and answers
 * {@link OperationStatus#PROCESSING PROCESSING}; the machine doing it then says how it goes through the request, on
 * its own ticks, until it is over.
 *
 * @param <T> the arguments a request of this kind carries
 */
@ApiStatus.Experimental
@FunctionalInterface
public interface IOperationHandler<T extends IOperationArgs> {

    /**
     * Carries out, or starts, one request.
     *
     * @param request what this request carries, the machine given it, and the way to say how it goes
     * @return the state the request is in now: a finished one, or an active one when the work goes on after this
     *         call
     */
    OperationStatus execute(IOperationRequest<T> request);
}
