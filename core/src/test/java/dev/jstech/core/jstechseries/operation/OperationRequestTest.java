/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.operation;

import dev.jstech.core.jstechseries.network.NetworkCategory;
import dev.jstech.core.jstechseries.tier.IndustrialTier;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperationRequestTest {

    private static final TextKey NAME = TextKey.of("jsc.operation.test", "Test");
    private static final OperationFailure OVERHEATED = OperationFailure.of("jsc.test.overheated", "Reactor 1");

    private record Seconds(int seconds) implements IOperationArgs {
    }

    @Test
    void start_keepsWhatTheHandlerAnswers() {
        final OperationRequest<Seconds> done = OperationRequest.of(kind(request -> OperationStatus.COMPLETED),
                new Seconds(0));

        assertEquals(OperationStatus.COMPLETED, done.start());
        assertEquals(OperationStatus.COMPLETED, done.status());
    }

    /* Work that goes on answers PROCESSING, and the machine says the rest later, from its own ticks. */
    @Test
    void report_followsTheWorkUntilItIsOver() {
        final List<IOperationRequest<Seconds>> kept = new ArrayList<>();
        final OperationRequest<Seconds> request = OperationRequest.of(kind(given -> {
            kept.add(given);
            return OperationStatus.PROCESSING;
        }), new Seconds(10));

        assertEquals(OperationStatus.PROCESSING, request.start());
        kept.getFirst().report(OperationStatus.WAITING);
        assertEquals(OperationStatus.WAITING, request.status());
        kept.getFirst().report(OperationStatus.COMPLETED_PARTIAL);
        kept.getFirst().report(OperationStatus.PROCESSING);

        assertEquals(OperationStatus.COMPLETED_PARTIAL, request.status());
    }

    /* PENDING is where a request starts: answering it, as a handler that only queues work does, moves nothing back. */
    @Test
    void report_ofPendingChangesNothing() {
        final OperationRequest<Seconds> request = OperationRequest.of(kind(given -> {
            given.report(OperationStatus.PROCESSING);
            return OperationStatus.PENDING;
        }), new Seconds(1));

        assertEquals(OperationStatus.PROCESSING, request.start());
    }

    @Test
    void fail_keepsTheReason() {
        final OperationRequest<Seconds> request = OperationRequest.of(kind(given -> {
            given.fail(OVERHEATED);
            return OperationStatus.FAILED;
        }), new Seconds(1));

        assertEquals(OperationStatus.FAILED, request.start());
        assertEquals(OVERHEATED, request.failure());
    }

    /* A handler that throws fails its request with a reason naming the kind, rather than taking the tick down. */
    @Test
    void start_failsTheRequestWhenTheHandlerThrows() {
        final OperationRequest<Seconds> request = OperationRequest.of(kind(given -> {
            throw new IllegalStateException("broken on purpose");
        }), new Seconds(1));

        assertEquals(OperationStatus.FAILED, request.start());
        assertEquals(OperationRequest.HANDLER_BROKE.key(), request.failure().key());
        assertEquals(List.of("jsc:test"), request.failure().arguments());
    }

    @Test
    void start_refusesASecondStart() {
        final OperationRequest<Seconds> request = OperationRequest.of(kind(given -> OperationStatus.PROCESSING),
                new Seconds(1));
        request.start();

        assertThrows(IllegalStateException.class, request::start);
    }

    @Test
    void cancel_discardsAndTellsTheMachineOnce() {
        final AtomicInteger stopped = new AtomicInteger();
        final List<IOperationRequest<Seconds>> kept = new ArrayList<>();
        final OperationRequest<Seconds> request = OperationRequest.of(kind(given -> {
            kept.add(given);
            given.whenCancelled(stopped::incrementAndGet);
            return OperationStatus.PROCESSING;
        }), new Seconds(30));
        request.start();

        request.cancel();
        request.cancel();
        kept.getFirst().report(OperationStatus.COMPLETED);

        assertEquals(OperationStatus.DISCARDED, request.status());
        assertTrue(kept.getFirst().cancelled());
        assertEquals(1, stopped.get());
    }

    @Test
    void whenCancelled_runsAtOnceOnARequestAlreadyCalledOff() {
        final AtomicInteger stopped = new AtomicInteger();
        final OperationRequest<Seconds> request = OperationRequest.of(kind(given -> OperationStatus.PROCESSING),
                new Seconds(1));
        request.start();
        request.cancel();

        request.whenCancelled(stopped::incrementAndGet);

        assertEquals(1, stopped.get());
    }

    /* A request that ended any other way was never called off, so whoever waited to stop never hears of it. */
    @Test
    void cancel_ofAFinishedRequestChangesNothing() {
        final AtomicInteger stopped = new AtomicInteger();
        final OperationRequest<Seconds> request = OperationRequest.of(kind(given -> {
            given.whenCancelled(stopped::incrementAndGet);
            return OperationStatus.COMPLETED;
        }), new Seconds(0));
        request.start();

        request.cancel();

        assertEquals(OperationStatus.COMPLETED, request.status());
        assertFalse(request.cancelled());
        assertEquals(0, stopped.get());
    }

    @Test
    void machine_isNoneForARequestNoMachineDoes() {
        final OperationRequest<Seconds> request = OperationRequest.of(kind(given -> OperationStatus.COMPLETED),
                new Seconds(0));

        assertTrue(request.machine().isEmpty());
        assertSame(NAME, request.type().name());
    }

    private static OperationType<Seconds> kind(final IOperationHandler<Seconds> handler) {
        return new OperationType<>("jsc:test", NAME, Seconds.class, OperationCategory.INDUSTRIAL, IndustrialTier.T1,
                EnumSet.of(NetworkCategory.C), handler);
    }
}
