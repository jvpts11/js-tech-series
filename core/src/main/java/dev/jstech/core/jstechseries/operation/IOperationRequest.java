/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.operation;

import dev.jstech.core.jstechseries.network.INetworkMember;
import java.util.Optional;
import org.jetbrains.annotations.ApiStatus;

/**
 * One request of a kind of Operation, as its handler and the machine doing it see it: what it carries, the machine
 * of the network it was given to, and the way that machine says how the work goes until it is over.
 *
 * <p>A machine whose work takes time keeps the request and reports on it from its own ticks: {@link #report} with
 * {@link OperationStatus#PROCESSING PROCESSING} once it runs, {@link OperationStatus#WAITING WAITING} while it waits
 * on something, and an outcome once it is over; {@link #fail} when it cannot go on, with the reason. Whoever asked can
 * call it off at any time ({@link OperationStatus#DISCARDED DISCARDED}): the machine hears it through
 * {@link #whenCancelled} and stops, and nothing it reports after that counts. Everything on the server's thread.
 *
 * <pre>{@code
 * public OperationStatus accelerate(final IOperationRequest<AccelerateArgs> request) {
 *     this.running = request;
 *     this.left = request.args().seconds() * 20;
 *     request.whenCancelled(() -> this.running = null);
 *     return OperationStatus.PROCESSING;
 * }
 *
 * // in the machine's tick
 * if (this.running != null && --this.left <= 0) {
 *     this.running.report(OperationStatus.COMPLETED);
 *     this.running = null;
 * }
 * }</pre>
 *
 * @param <T> the arguments it carries
 */
@ApiStatus.Experimental
public interface IOperationRequest<T extends IOperationArgs> {

    /** What it carries. */
    T args();

    /**
     * The machine of the network given it, the member that carries it out; none for a request that is no machine's
     * to do, such as a kind of J's Computers' own, which its Mainframe runs.
     */
    Optional<INetworkMember> machine();

    /**
     * Says how the work goes: {@link OperationStatus#PROCESSING PROCESSING} or {@link OperationStatus#WAITING WAITING}
     * while it goes on, and {@link OperationStatus#COMPLETED COMPLETED}, {@link OperationStatus#COMPLETED_PARTIAL
     * COMPLETED_PARTIAL} or {@link OperationStatus#RESOURCE_LOCKED RESOURCE_LOCKED} once it is over. A request starts
     * {@link OperationStatus#PENDING PENDING}, so saying that changes nothing; once it is over, or called off, nothing
     * said changes it.
     */
    void report(OperationStatus status);

    /** Says the work failed, and why: {@link OperationStatus#FAILED FAILED}, with a reason each player reads. */
    void fail(OperationFailure reason);

    /** Whether whoever asked called it off: the work should stop, and nothing it reports counts any more. */
    boolean cancelled();

    /**
     * Runs {@code stop} when it is called off, or at once when it already is: where a machine stops its work. Never
     * run for a request that ended any other way.
     */
    void whenCancelled(Runnable stop);
}
