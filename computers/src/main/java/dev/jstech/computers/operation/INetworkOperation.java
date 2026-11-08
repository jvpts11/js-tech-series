/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation;

import dev.jstech.computers.operation.payload.OperationRecord;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.jstechseries.operation.OperationPriority;
import dev.jstech.core.uuid.NodeUuid;

import java.util.Map;
import java.util.UUID;

/**
 * A multi-tick network Operation the Mainframe advances over time: a SELECT, INSERT or DELETE that has been decomposed into SubOperations and streams its items respecting storage latency and the Mainframe's orchestration budget.
 */
public interface INetworkOperation {

    /** The identity views and commands address this Operation by while it is in flight. */
    UUID operationId();

    /** The registered Operation type this is an instance of, such as {@code jsc:select}. */
    String typeId();

    /**
     * True when this Operation must leave no log entry, statistics or lifecycle event of its own when it
     * settles: a placeholder that turned into another Operation, which carries the record instead.
     */
    default boolean silent() {
        return false;
    }

    void tick(long throughputBudget);

    boolean isDone();

    default boolean isWaiting() {
        return false;
    }

    /**
     * Settles the Operation early because the engine can no longer run it (a power-off, a lost network):
     * whatever it held goes back, and it reports how far it got.
     */
    void abandon();

    /**
     * Settles the Operation early because somebody chose to stop it (a task manager, the command prompt):
     * the same clean-up as {@link #abandon()}, but the record reads DISCARDED so the log tells the two apart.
     */
    void cancel();

    /**
     * Settles the Operation early because the machine running it went dark suddenly: what it was carrying (in the
     * memory buffer, on its way to a disk or to an inventory) is lost rather than handed back, and the rest is let go
     * of as {@link #abandon()} lets it go. What stays where it was, in storage or in a machine in the world, stays.
     *
     * @return what was lost, by kind; nothing for an Operation that carries nothing, or has settled already
     */
    default Map<StorageKey, Long> lose() {
        abandon();
        return Map.of();
    }

    /**
     * Whether the Operation, still running, is moving items through the storage node {@code node}: when that node
     * leaves the network, the network has split under it.
     */
    default boolean reaches(final NodeUuid node) {
        return false;
    }

    /** The level the Mainframe schedules this Operation at; {@link OperationPriority#DEFAULT} unless changed. */
    OperationPriority priority();

    /** Changes the scheduling level; takes effect on the next tick, so a queued Operation can be bumped. */
    void setPriority(OperationPriority priority);

    OperationRecord toRecord();

    OperationRecord liveRecord();
}
