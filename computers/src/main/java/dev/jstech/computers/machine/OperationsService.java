/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.computers.program.cli.ICliNetwork;
import dev.jstech.computers.program.cli.ICliOperations;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * Asking a machine's network to move and make things, and seeing what is in flight.
 *
 * <p>This changes the world rather than reading it, and it does so through exactly the doors the Network Interactor
 * and the prompt use, so whatever they check is checked here too. Every ask is signed with who made it, because a
 * base can have a dozen programs and players asking at once. A refusal is an answer, not an error: the network may
 * have no Mainframe running, or nothing that makes the thing.
 */
public final class OperationsService {

    /** The network's work as the machine's shell asks for it. */
    private final ICliOperations operations;
    /** The network as the machine's shell reads it. */
    private final ICliNetwork network;

    public OperationsService(final ICliOperations operations, final ICliNetwork network) {
        this.operations = operations;
        this.network = network;
    }

    /** Whether the machine is on a network at all. */
    public boolean onNetwork() {
        return this.network.onNetwork();
    }

    /** Asks the network to bring an item into the machine's own storage. */
    public ICliComputer.OpResult pull(final String item, final long amount, final String requester) {
        return this.operations.select(item, amount, requester);
    }

    /** Asks the network to take an item from the machine's own storage. */
    public ICliComputer.OpResult push(final String item, final long amount, final String requester) {
        return this.operations.insert(item, amount, requester);
    }

    /** Asks the network to craft an item. */
    public ICliComputer.OpResult craft(final String item, final long amount, final String requester) {
        return this.operations.craft(item, amount, requester);
    }

    /** Asks the network to stop an Operation in flight, named by its id. */
    public ICliComputer.OpResult cancel(final String id) {
        return this.operations.cancelOperation(id);
    }

    /** Asks the network to move an Operation in flight to another priority. */
    public ICliComputer.OpResult reprioritise(final String id, final String priority) {
        return this.operations.repriorityOperation(id, priority);
    }

    /** The Operation in flight with that id, or null when none is, which includes one that has settled. */
    @Nullable
    public ICliComputer.ActiveOp get(final String id) {
        for (final ICliComputer.ActiveOp op : this.operations.activeOps()) {
            if (op.id().equalsIgnoreCase(id)) {
                return op;
            }
        }
        return null;
    }

    /** Every Operation in flight. */
    public List<ICliComputer.ActiveOp> list() {
        return this.operations.activeOps();
    }
}
