/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.operation.MoveLabels;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.computers.program.cli.ICliNetwork;
import dev.jstech.computers.program.cli.ICliOperations;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.core.network.NetworkSystem;
import dev.jstech.core.operation.OperationPriority;
import dev.jstech.core.uuid.NetworkUuid;
import java.util.List;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
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

    private final IComputerTerminalHost terminal;
    private final ServerLevel level;
    /** The network's work as the machine's shell asks for it, for what this service does not do itself yet. */
    private final ICliOperations operations;
    /** The network as the machine's shell reads it. */
    private final ICliNetwork network;

    public OperationsService(final IComputerTerminalHost terminal, final ServerLevel level,
                             final ICliOperations operations, final ICliNetwork network) {
        this.terminal = terminal;
        this.level = level;
        this.operations = operations;
        this.network = network;
    }

    /** Whether the machine is on a network at all. */
    public boolean onNetwork() {
        return this.network.onNetwork();
    }

    /** Asks the network to bring an item into the machine's own storage. */
    public ICliComputer.OpResult pull(final String item, final long amount, final String requester) {
        return this.select(item, amount, requester);
    }

    /** Asks the network to take an item from the machine's own storage. */
    public ICliComputer.OpResult push(final String item, final long amount, final String requester) {
        return this.insert(item, amount, OperationPriority.DEFAULT, requester);
    }

    /**
     * Brings an item out of the network and into this machine's own storage.
     *
     * @param origin what to put on the row of the network's log, from {@link MoveLabels}
     */
    public ICliComputer.OpResult select(final String item, final long quantity, final String origin) {
        final StorageKey key = ServerCliComputer.itemKey(item);
        if (key == null) {
            return ICliComputer.OpResult.fail("unknown item: " + item);
        }
        final MainframeBlockEntity mainframe = this.mainframe();
        if (mainframe == null) {
            return ICliComputer.OpResult.fail("the network has no running Mainframe");
        }
        final var op = mainframe.submitNetworkSelect(key, demand(quantity), this.terminal.localStorage(),
                this.terminal.originLabel(origin));
        if (op == null) {
            return ICliComputer.OpResult.fail("could not start the SELECT");
        }
        op.abortWhen(this.hostGone());
        return ICliComputer.OpResult.ok("SELECT queued: " + qtyLabel(quantity) + " "
                + key.displayName().getString() + " -> local storage");
    }

    /**
     * Puts an item this machine holds into the network.
     *
     * <p>What is taken out of the machine's own storage is put back when nothing carries it away: no dispatcher to
     * take it, or an Operation that settled without moving all of it. Nothing is lost on the way.
     */
    public ICliComputer.OpResult insert(final String item, final long quantity, final OperationPriority priority,
                                        final String origin) {
        final StorageKey key = ServerCliComputer.itemKey(item);
        if (key == null) {
            return ICliComputer.OpResult.fail("unknown item: " + item);
        }
        final long held = this.terminal.localStore().count(key);
        if (held <= 0L) {
            return ICliComputer.OpResult.fail("this computer holds no " + key.displayName().getString());
        }
        final long take = Math.min(demand(quantity), held);
        final long taken = this.terminal.localStore().extract(key, take);
        if (taken <= 0L) {
            return ICliComputer.OpResult.fail("nothing to push");
        }
        final MainframeBlockEntity mainframe = this.mainframe();
        final var op = mainframe == null ? null
                : mainframe.submitNetworkInsert(key, taken, this.terminal.originLabel(origin));
        if (op == null) {
            this.terminal.localStore().insert(key, taken); // no dispatcher: put it straight back, never lose it
            return ICliComputer.OpResult.fail("the network has no running Mainframe");
        }
        op.setPriority(priority);
        op.onSettle(() -> {
            final long leftover = op.leftover();
            if (leftover > 0L) {
                this.terminal.localStore().insert(key, leftover);
            }
        });
        return ICliComputer.OpResult.ok("INSERT queued: " + taken + " " + key.displayName().getString()
                + " -> network");
    }

    /** Asks the network to make an item, at the default priority. */
    public ICliComputer.OpResult craft(final String item, final long quantity, final String origin) {
        return this.craft(item, quantity, OperationPriority.DEFAULT, origin);
    }

    /** Asks the network to make an item, at the priority given. */
    public ICliComputer.OpResult craft(final String item, final long quantity, final OperationPriority priority,
                                       final String origin) {
        final StorageKey key = ServerCliComputer.itemKey(item);
        if (key == null) {
            return ICliComputer.OpResult.fail("unknown item: " + item);
        }
        final MainframeBlockEntity mainframe = this.mainframe();
        if (mainframe == null) {
            return ICliComputer.OpResult.fail("the network has no running Mainframe");
        }
        /*
         * Route through the shared entry point so the CLI and IQL craft a machine or multi-stage recipe
         * directly (not only a bench-planned tree), exactly as the terminal and Network Interactor do.
         */
        final var op = mainframe.submitCraftRequest(key, demand(quantity), true,
                this.terminal.originLabel(origin), null);
        if (op == null) {
            return ICliComputer.OpResult.fail("no pattern crafts " + key.displayName().getString());
        }
        op.setPriority(priority);
        return ICliComputer.OpResult.ok("CRAFT queued: " + qtyLabel(quantity) + " "
                + key.displayName().getString());
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

    /** A parsed quantity as a concrete demand: ALL, or none given, means "as much as possible". */
    public static long demand(final long quantity) {
        return quantity <= 0L ? Long.MAX_VALUE : quantity;
    }

    /** How a quantity reads back to whoever asked: a real count, or {@code "all"} for ALL and none given. */
    public static String qtyLabel(final long quantity) {
        return quantity <= 0L ? "all" : Long.toString(quantity);
    }

    /** True once the machine has left the world: a pull into its storage stops there instead of feeding a ghost. */
    public BooleanSupplier hostGone() {
        return this.terminal instanceof BlockEntity be ? be::isRemoved : () -> false;
    }

    /** The Mainframe of the machine's network, or null when it is on none, or none is running. */
    @Nullable
    private MainframeBlockEntity mainframe() {
        final NetworkUuid net = this.terminal.networkUuid();
        if (net == null) {
            return null;
        }
        return NetworkSystem.get(this.level).mainframePositionOf(net)
                .map(pos -> this.level.getBlockEntity(BlockPos.of(pos)) instanceof MainframeBlockEntity mf ? mf : null)
                .orElse(null);
    }
}
