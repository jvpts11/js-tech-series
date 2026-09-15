/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import java.util.List;

/**
 * What a command asks of the network's work: moving, crafting and holding items, the Operations in flight and how the
 * network did with them, statements of the network's own language, and the Mainframe's services.
 *
 * <p>Every member answers as a computer that cannot reach the network would: an ask is refused, and there is nothing
 * in flight to list.
 */
public interface ICliOperations {

    /** Pull items from the network into this computer's local storage. */
    default ICliComputer.OpResult select(final String item, final long quantity) {
        return ICliComputer.OpResult.fail("this machine cannot reach the network");
    }

    /** Push items from this computer's local storage into the network. */
    default ICliComputer.OpResult insert(final String item, final long quantity) {
        return ICliComputer.OpResult.fail("this machine cannot reach the network");
    }

    /** Ask the network to craft the named item. */
    default ICliComputer.OpResult craft(final String item, final long quantity) {
        return ICliComputer.OpResult.fail("this machine cannot reach the network");
    }

    /**
     * The same three, saying who asked.
     *
     * <p>Every one of these ends up as a row in the network's log, and the row names where it came
     * from. The prompt says so by default; anything else that submits work says what it is, so a player
     * reading the log can tell a script's pull from one they made themselves.
     *
     * @param origin what to put on the row, from {@link dev.jstech.computers.operation.MoveLabels}
     */
    default ICliComputer.OpResult select(final String item, final long quantity, final String origin) {
        return select(item, quantity);
    }

    /** Push, saying who asked. */
    default ICliComputer.OpResult insert(final String item, final long quantity, final String origin) {
        return insert(item, quantity);
    }

    /** Craft, saying who asked. */
    default ICliComputer.OpResult craft(final String item, final long quantity, final String origin) {
        return craft(item, quantity);
    }

    /** Place a standing hold on the named item so concurrent operations WAIT on it. */
    default ICliComputer.OpResult lock(final String item, final long quantity) {
        return ICliComputer.OpResult.fail("this machine cannot reach the network");
    }

    /** Release the standing hold on the named item. */
    default ICliComputer.OpResult unlock(final String item) {
        return ICliComputer.OpResult.fail("this machine cannot reach the network");
    }

    /** The item types currently held by a manual lock, and how much each holds. */
    default List<ICliComputer.StoredItem> locks() {
        return List.of();
    }

    /** The operations currently in flight on the network. */
    default List<ICliComputer.ActiveOp> activeOps() {
        return List.of();
    }

    /** Stops the in-flight operation with this short id (as listed by {@code ops}). */
    default ICliComputer.OpResult cancelOperation(final String id) {
        return ICliComputer.OpResult.fail("this computer cannot cancel network operations");
    }

    /**
     * Moves a running operation up or down the queue.
     *
     * <p>Only one that is still going: what has already settled cannot be hurried.
     *
     * @param id       the short id, or any longer prefix of the full one
     * @param priority the name of an {@code OperationPriority}
     */
    default ICliComputer.OpResult repriorityOperation(final String id, final String priority) {
        return ICliComputer.OpResult.fail("this machine cannot reach the network");
    }

    /** The network's operation statistics for the last hour, empty off a network. */
    default List<ICliComputer.OperationStat> operationStats() {
        return List.of();
    }

    /** The most operations the network had in flight at once today. */
    default int peakOperationsToday() {
        return 0;
    }

    /**
     * Run an index-maintenance action (analyze / reindex / vacuum) on the Mainframe.
     *
     * @return a result whose message describes the outcome, or a failure when this computer is not a Mainframe
     */
    default ICliComputer.OpResult maintenance(final String action) {
        return ICliComputer.OpResult.fail("this machine cannot reach the network");
    }

    /** Runs a parsed effecting IQL statement (SELECT/INSERT/MOVE/CRAFT/DELETE/DROP/LOCK/...) against the network. */
    default ICliComputer.OpResult execute(final dev.jstech.computers.program.iql.IqlOperation operation) {
        return ICliComputer.OpResult.fail("this machine cannot reach the network");
    }

    /** Controls the network's IQL Engine service: {@code install}/{@code start}/{@code stop}/{@code status}. */
    default ICliComputer.OpResult engineControl(final String action) {
        return ICliComputer.OpResult.fail("the IQL Engine can only be controlled from a networked computer");
    }

    /** The services the network exposes (the IQL Engine, and any future service) with their current state. */
    default List<ICliComputer.ServiceStatus> services() {
        return List.of();
    }

    /** Whether the network's IQL Engine is installed, which gates the Engine's own commands in the prompt. */
    default boolean iqlEngineInstalled() {
        return false;
    }
}
