/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.operation.INetworkOperation;
import dev.jstech.computers.operation.MoveLabels;
import dev.jstech.computers.operation.payload.OperationRecord;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.computers.program.cli.ICliNetwork;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.core.network.NetworkSystem;
import dev.jstech.core.operation.OperationPriority;
import dev.jstech.core.util.ShortId;
import dev.jstech.core.uuid.NetworkUuid;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
    /** The network as the machine's shell reads it. */
    private final ICliNetwork network;

    public OperationsService(final IComputerTerminalHost terminal, final ServerLevel level,
                             final ICliNetwork network) {
        this.terminal = terminal;
        this.level = level;
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

    /**
     * Holds an item where it is, so that whatever else asks for it waits instead of taking it.
     *
     * @param quantity how much to hold; none given holds everything the network has free of it
     */
    public ICliComputer.OpResult lock(final String item, final long quantity) {
        final StorageKey key = ServerCliComputer.itemKey(item);
        if (key == null) {
            return ICliComputer.OpResult.fail("unknown item: " + item);
        }
        final MainframeBlockEntity mainframe = this.mainframe();
        if (mainframe == null) {
            return ICliComputer.OpResult.fail("the network has no running Mainframe");
        }
        final long demand = quantity > 0L ? quantity : Long.MAX_VALUE; // 0 locks everything available
        final long held = mainframe.lockType(key, demand, null);
        if (held <= 0L) {
            return ICliComputer.OpResult.fail(mainframe.networkIndex().isManuallyLocked(key)
                    ? key.displayName().getString() + " is already locked"
                    : "nothing to lock: the network holds no free " + key.displayName().getString());
        }
        return ICliComputer.OpResult.ok("LOCK held " + held + " " + key.displayName().getString()
                + " (concurrent operations will wait)");
    }

    /** Lets an item go again, so that what was waiting on it can have it. */
    public ICliComputer.OpResult unlock(final String item) {
        final StorageKey key = ServerCliComputer.itemKey(item);
        if (key == null) {
            return ICliComputer.OpResult.fail("unknown item: " + item);
        }
        final MainframeBlockEntity mainframe = this.mainframe();
        if (mainframe == null) {
            return ICliComputer.OpResult.fail("the network has no running Mainframe");
        }
        final long released = mainframe.unlockType(key);
        if (released <= 0L) {
            return ICliComputer.OpResult.fail(key.displayName().getString() + " is not locked");
        }
        return ICliComputer.OpResult.ok("UNLOCK released " + released + " " + key.displayName().getString());
    }

    /**
     * Runs one of the Mainframe's own jobs on its index: {@code analyze} reconciles the catalog against the disks,
     * {@code reindex} rebuilds it from them, and {@code vacuum} clears the entries nothing stands behind any more.
     *
     * <p>Only the Mainframe runs these, because they are what it keeps.
     */
    public ICliComputer.OpResult maintenance(final String action) {
        if (!this.terminal.isMainframeHost()) {
            return ICliComputer.OpResult.fail("maintenance runs on the Mainframe only");
        }
        final NetworkUuid net = this.terminal.networkUuid();
        final MainframeBlockEntity mainframe = this.mainframe();
        if (mainframe == null || net == null) {
            return ICliComputer.OpResult.fail("the network has no running Mainframe");
        }
        final var index = mainframe.networkIndex();
        return switch (action) {
            case "analyze" -> {
                index.analyzeIncremental(this.level, net);
                yield ICliComputer.OpResult.ok("ANALYZE complete - " + index.catalogSize() + " types reconciled");
            }
            case "reindex" -> {
                // The disks are read now; the catalog is built off the tick and swapped in a tick or two later.
                mainframe.reindexAsync(null);
                yield ICliComputer.OpResult.ok("REINDEX started - rebuilding the catalog from disks");
            }
            case "vacuum" -> {
                final int freed = index.vacuum(this.level, net);
                yield ICliComputer.OpResult.ok("VACUUM freed " + freed
                        + (freed == 1 ? " ghost entry" : " ghost entries"));
            }
            default -> ICliComputer.OpResult.fail("unknown maintenance action: " + action);
        };
    }

    /** What is being held by hand, and how much of each. */
    public List<ICliComputer.StoredItem> locks() {
        final MainframeBlockEntity mainframe = this.mainframe();
        if (mainframe == null) {
            return List.of();
        }
        final List<ICliComputer.StoredItem> rows = new ArrayList<>();
        mainframe.lockedTypes().forEach((key, amount) ->
                rows.add(new ICliComputer.StoredItem(key.displayName().getString(), amount)));
        return rows;
    }

    /**
     * Asks the network to stop an Operation in flight, named by its id.
     *
     * <p>The id is the short one everything shows, or any longer prefix of the full one, so whoever reads a list can
     * name what they see.
     */
    public ICliComputer.OpResult cancel(final String id) {
        final MainframeBlockEntity mainframe = this.mainframe();
        if (mainframe == null) {
            return ICliComputer.OpResult.fail("the network has no running Mainframe");
        }
        final String wanted = id.trim().toLowerCase(Locale.ROOT);
        if (wanted.isEmpty()) {
            return ICliComputer.OpResult.fail("usage: cancel <id>   (see 'ops')");
        }
        for (final INetworkOperation operation : mainframe.liveOperations()) {
            final String full = operation.operationId().toString();
            if (full.startsWith(wanted) && wanted.length() >= ShortId.of(full).length()) {
                final OperationRecord record = operation.liveRecord();
                if (!mainframe.cancelOperation(operation.operationId())) {
                    return ICliComputer.OpResult.fail("operation " + ShortId.of(full) + " has already settled");
                }
                return ICliComputer.OpResult.ok("cancelled " + OperationRecord.typeName(record.type()) + " "
                        + record.name().getString());
            }
        }
        return ICliComputer.OpResult.fail("no operation " + wanted + " in flight (see 'ops')");
    }

    /** Asks the network to move an Operation in flight to another priority; one that has settled cannot be hurried. */
    public ICliComputer.OpResult reprioritise(final String id, final String priority) {
        final MainframeBlockEntity mainframe = this.mainframe();
        if (mainframe == null) {
            return ICliComputer.OpResult.fail("the network has no running Mainframe");
        }
        final OperationPriority wanted = OperationPriority.fromKeyword(priority).orElse(null);
        if (wanted == null) {
            return ICliComputer.OpResult.fail("no such priority: " + priority);
        }
        final String prefix = id.trim().toLowerCase(Locale.ROOT);
        if (prefix.isEmpty()) {
            return ICliComputer.OpResult.fail("which operation?");
        }
        for (final INetworkOperation operation : mainframe.liveOperations()) {
            final String full = operation.operationId().toString();
            if (full.startsWith(prefix) && prefix.length() >= ShortId.of(full).length()) {
                operation.setPriority(wanted);
                return ICliComputer.OpResult.ok(ShortId.of(full) + " is now " + wanted.serializedName());
            }
        }
        return ICliComputer.OpResult.fail("no operation " + id + " is still running");
    }

    /** The Operation in flight with that id, or null when none is, which includes one that has settled. */
    @Nullable
    public ICliComputer.ActiveOp get(final String id) {
        for (final ICliComputer.ActiveOp op : this.list()) {
            if (op.id().equalsIgnoreCase(id)) {
                return op;
            }
        }
        return null;
    }

    /** Every Operation in flight, as whoever reads a list of them sees it. */
    public List<ICliComputer.ActiveOp> list() {
        final MainframeBlockEntity mainframe = this.mainframe();
        if (mainframe == null) {
            return List.of();
        }
        final List<ICliComputer.ActiveOp> rows = new ArrayList<>();
        for (final OperationRecord record : mainframe.activeOperationRecords()) {
            rows.add(new ICliComputer.ActiveOp(ShortId.of(record.id().toString()),
                    OperationRecord.typeName(record.type()), record.name().getString(), record.moved(),
                    record.requested(), OperationRecord.statusName(record.status()), record.priority().label()));
        }
        return rows;
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
