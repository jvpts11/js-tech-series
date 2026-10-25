/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.operation;

import dev.jstech.core.jstechseries.network.INetworkMember;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * A request of a kind of Operation as whoever gives it holds it: it is handed to its kind's handler once
 * ({@link #start}), and from then on read for the state the machine says it is in, or called off ({@link #cancel}).
 * The machine sees it as its {@link IOperationRequest}.
 *
 * <pre>{@code
 * final OperationRequest<CoolArgs> request = new OperationRequest<>(MyOperations.COOL, new CoolArgs(300), reactor);
 * request.start();
 * // ... ticks later
 * if (request.status().isTerminal()) {
 *     show(request.status(), request.failure());
 * }
 * }</pre>
 *
 * @param <T> the arguments it carries
 */
@ApiStatus.Experimental
@TextHolder
public final class OperationRequest<T extends IOperationArgs> implements IOperationRequest<T> {

    private final OperationType<T> type;
    private final T args;
    private final @Nullable INetworkMember machine;
    private final List<Runnable> stops = new ArrayList<>();
    private OperationStatus status = OperationStatus.PENDING;
    private OperationFailure failure = OperationFailure.NONE;
    private boolean started;

    /** Why a request failed when its kind's code broke on it, naming the kind. */
    public static final TextKey HANDLER_BROKE = TextKey.of("jscore.operation.handler_broke",
            "the code of %s broke on the request");

    /**
     * A request of {@code type} carrying {@code args}, given to {@code machine}, the member of the network that
     * carries it out; none for one that is no machine's to do.
     */
    public OperationRequest(final OperationType<T> type, final T args, final @Nullable INetworkMember machine) {
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.args = Objects.requireNonNull(args, "args must not be null");
        this.machine = machine;
    }

    /** A request of {@code type} carrying {@code args}, no machine's to do. */
    public static <T extends IOperationArgs> OperationRequest<T> of(final OperationType<T> type, final T args) {
        return new OperationRequest<>(type, args, null);
    }

    /**
     * Hands it to its kind's handler, once, and keeps the state it answers: a handler that throws fails it, saying
     * so, rather than taking the server's tick down with it.
     *
     * @return the state it is in now
     */
    public OperationStatus start() {
        if (this.started) {
            throw new IllegalStateException("a request of " + this.type.id() + " is started once");
        }
        this.started = true;
        try {
            report(this.type.handler().execute(this));
        } catch (final RuntimeException broke) {
            // The platform's own logger, as the dispatcher's: this runs where the game is not loaded too.
            final System.Logger log = System.getLogger(OperationRequest.class.getName());
            log.log(System.Logger.Level.ERROR, "The handler of the Operation " + this.type.id() + " threw", broke);
            fail(OperationFailure.of(HANDLER_BROKE, this.type.id()));
        }
        return this.status;
    }

    /**
     * Calls it off: {@link OperationStatus#DISCARDED DISCARDED}, and the machine told to stop. Once it is over, it
     * changes nothing.
     */
    public void cancel() {
        if (this.status.isTerminal()) {
            return;
        }
        this.status = OperationStatus.DISCARDED;
        final List<Runnable> told = List.copyOf(this.stops);
        this.stops.clear();
        told.forEach(Runnable::run);
    }

    /** Its kind. */
    public OperationType<T> type() {
        return this.type;
    }

    /** The state it is in now. */
    public OperationStatus status() {
        return this.status;
    }

    /** Why it failed, where it did; {@link OperationFailure#NONE} otherwise. */
    public OperationFailure failure() {
        return this.failure;
    }

    @Override
    public T args() {
        return this.args;
    }

    @Override
    public Optional<INetworkMember> machine() {
        return Optional.ofNullable(this.machine);
    }

    @Override
    public void report(final OperationStatus reported) {
        Objects.requireNonNull(reported, "status must not be null");
        if (this.status.isTerminal() || reported == OperationStatus.PENDING) {
            return;
        }
        this.status = reported;
        if (reported.isTerminal()) {
            this.stops.clear();
        }
    }

    @Override
    public void fail(final OperationFailure reason) {
        if (this.status.isTerminal()) {
            return;
        }
        this.failure = reason == null ? OperationFailure.NONE : reason;
        report(OperationStatus.FAILED);
    }

    @Override
    public boolean cancelled() {
        return this.status == OperationStatus.DISCARDED;
    }

    @Override
    public void whenCancelled(final Runnable stop) {
        Objects.requireNonNull(stop, "stop must not be null");
        if (cancelled()) {
            stop.run();
        } else if (!this.status.isTerminal()) {
            this.stops.add(stop);
        }
    }
}
