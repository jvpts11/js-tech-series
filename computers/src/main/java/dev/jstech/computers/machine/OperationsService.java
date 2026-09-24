/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.advancement.JscEvents;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.operation.DataHandoff;
import dev.jstech.computers.operation.INetworkOperation;
import dev.jstech.computers.operation.MoveLabels;
import dev.jstech.computers.operation.NetworkStorage;
import dev.jstech.computers.operation.payload.OperationRecord;
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.computers.program.cli.ICliNetwork;
import dev.jstech.computers.program.iql.IqlVerb;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.core.network.NetworkSystem;
import dev.jstech.core.operation.OperationPriority;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.util.ShortId;
import dev.jstech.core.uuid.NetworkUuid;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
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
@TextHolder
public final class OperationsService {

    private final IComputerTerminalHost terminal;
    private final ServerLevel level;
    /** The network as the machine's shell reads it. */
    private final ICliNetwork network;

    /* The refusals a statement of the network's language can meet as well, so it answers them in the same words. */
    static final TextKey UNKNOWN_ITEM = TextKey.of("jsc.service.operations.unknown_item", "unknown item: %s");
    static final TextKey NO_MAINFRAME =
            TextKey.of("jsc.service.operations.no_mainframe", "the network has no running Mainframe");
    private static final TextKey NO_NETWORK =
            TextKey.of("jsc.service.operations.no_network", "this machine is on no network");
    static final TextKey SELECT_FAILED =
            TextKey.of("jsc.service.operations.select_failed", "could not start the SELECT");
    private static final TextKey SELECT_QUEUED =
            TextKey.of("jsc.service.operations.select_queued", "SELECT queued: %s %s -> local storage");
    private static final TextKey HOLDS_NONE =
            TextKey.of("jsc.service.operations.holds_none", "this computer holds no %s");
    private static final TextKey NOTHING_TO_PUSH =
            TextKey.of("jsc.service.operations.nothing_to_push", "nothing to push");
    private static final TextKey INSERT_QUEUED =
            TextKey.of("jsc.service.operations.insert_queued", "INSERT queued: %s %s -> network");
    private static final TextKey NO_PATTERN = TextKey.of("jsc.service.operations.no_pattern", "no pattern crafts %s");
    private static final TextKey CRAFT_QUEUED =
            TextKey.of("jsc.service.operations.craft_queued", "CRAFT queued: %s %s");
    private static final TextKey NOBODY_HERE = TextKey.of("jsc.service.operations.nobody_here",
            "nobody is at this machine; ask for it '--to local' instead");
    private static final TextKey TO_HAND_QUEUED =
            TextKey.of("jsc.service.operations.to_hand_queued", "SELECT queued: %s %s -> your inventory");
    private static final TextKey NOBODY_TO_TAKE =
            TextKey.of("jsc.service.operations.nobody_to_take", "nobody is at this machine to take anything from");
    private static final TextKey HOLDING_NOTHING =
            TextKey.of("jsc.service.operations.holding_nothing", "you are holding nothing");
    private static final TextKey NO_ROOM_FOR =
            TextKey.of("jsc.service.operations.no_room_for", "the network has no room for %s");
    private static final TextKey NOTHING_IN_HAND =
            TextKey.of("jsc.service.operations.nothing_in_hand", "there is nothing in your hand to store");
    private static final TextKey NOBODY_TO_FILL =
            TextKey.of("jsc.service.operations.nobody_to_fill", "nobody is at this machine to fill anything for");
    private static final TextKey NOTHING_TO_FILL =
            TextKey.of("jsc.service.operations.nothing_to_fill", "you are holding nothing to fill");
    private static final TextKey NO_FLUID =
            TextKey.of("jsc.service.operations.no_fluid", "the network holds no fluid called \"%s\"");
    private static final TextKey FILLING =
            TextKey.of("jsc.service.operations.filling", "SELECT queued: filling with %s");
    private static final TextKey TAKES_NONE =
            TextKey.of("jsc.service.operations.takes_none", "what you are holding takes none of it");
    private static final TextKey DOES_NOT_HOLD =
            TextKey.of("jsc.service.operations.does_not_hold", "what you are holding does not hold %s");
    private static final TextKey ALREADY_LOCKED =
            TextKey.of("jsc.service.operations.already_locked", "%s is already locked");
    private static final TextKey NOTHING_TO_LOCK = TextKey.of("jsc.service.operations.nothing_to_lock",
            "nothing to lock: the network holds no free %s");
    private static final TextKey LOCK_HELD = TextKey.of("jsc.service.operations.lock_held",
            "LOCK held %s %s (concurrent operations will wait)");
    private static final TextKey NOT_LOCKED = TextKey.of("jsc.service.operations.not_locked", "%s is not locked");
    private static final TextKey UNLOCKED = TextKey.of("jsc.service.operations.unlocked", "UNLOCK released %s %s");
    private static final TextKey MAINFRAME_ONLY =
            TextKey.of("jsc.service.operations.mainframe_only", "maintenance runs on the Mainframe only");
    private static final TextKey ANALYZED =
            TextKey.of("jsc.service.operations.analyzed", "ANALYZE complete - %s types reconciled");
    private static final TextKey REINDEXING =
            TextKey.of("jsc.service.operations.reindexing", "REINDEX started - rebuilding the catalog from disks");
    private static final TextKey VACUUMED_ONE =
            TextKey.of("jsc.service.operations.vacuumed_one", "VACUUM freed %s ghost entry");
    private static final TextKey VACUUMED_MANY =
            TextKey.of("jsc.service.operations.vacuumed_many", "VACUUM freed %s ghost entries");
    private static final TextKey UNKNOWN_MAINTENANCE =
            TextKey.of("jsc.service.operations.unknown_maintenance", "unknown maintenance action: %s");
    private static final TextKey CANCEL_USAGE =
            TextKey.of("jsc.service.operations.cancel_usage", "usage: cancel <id>   (see 'ops')");
    private static final TextKey ALREADY_SETTLED =
            TextKey.of("jsc.service.operations.already_settled", "operation %s has already settled");
    private static final TextKey CANCELLED = TextKey.of("jsc.service.operations.cancelled", "cancelled %s %s");
    private static final TextKey NOT_IN_FLIGHT =
            TextKey.of("jsc.service.operations.not_in_flight", "no operation %s in flight (see 'ops')");
    private static final TextKey NO_PRIORITY =
            TextKey.of("jsc.service.operations.no_priority", "no such priority: %s");
    private static final TextKey WHICH_OPERATION =
            TextKey.of("jsc.service.operations.which_operation", "which operation?");
    private static final TextKey NOW_AT = TextKey.of("jsc.service.operations.now_at", "%s is now %s");
    private static final TextKey NOT_RUNNING =
            TextKey.of("jsc.service.operations.not_running", "no operation %s is still running");
    private static final TextKey ALL = TextKey.of("jsc.service.operations.all", "all");

    public OperationsService(final IComputerTerminalHost terminal, final ServerLevel level,
                             final ICliNetwork network) {
        this.terminal = terminal;
        this.level = level;
        this.network = network;
    }

    /** Whether the machine is on a network at all. */
    /** Credits whoever works this machine with a program of theirs having set an Operation going. */
    public void creditProgram() {
        if (this.terminal instanceof BlockEntity machine) {
            JscEvents.awardOperator(machine, JscEvents.SIGMA_OPERATION);
        }
    }

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
        final StorageKey key = StorageKey.byName(item);
        if (key == null) {
            return ICliComputer.OpResult.fail(UNKNOWN_ITEM.with(item));
        }
        final MainframeBlockEntity mainframe = this.mainframe();
        if (mainframe == null) {
            return ICliComputer.OpResult.fail(NO_MAINFRAME);
        }
        final var op = mainframe.submitNetworkSelect(key, demand(quantity), this.terminal.localStorage(),
                this.terminal.originLabel(origin));
        if (op == null) {
            return ICliComputer.OpResult.fail(SELECT_FAILED);
        }
        op.abortWhen(this.hostGone());
        return ICliComputer.OpResult.ok(SELECT_QUEUED.with(qtyLabel(quantity), key.displayName().getString()));
    }

    /**
     * Puts an item this machine holds into the network.
     *
     * <p>What is taken out of the machine's own storage is put back when nothing carries it away: no dispatcher to
     * take it, or an Operation that settled without moving all of it. Nothing is lost on the way.
     */
    public ICliComputer.OpResult insert(final String item, final long quantity, final OperationPriority priority,
                                        final String origin) {
        final StorageKey key = StorageKey.byName(item);
        if (key == null) {
            return ICliComputer.OpResult.fail(UNKNOWN_ITEM.with(item));
        }
        final long held = this.terminal.localStore().count(key);
        if (held <= 0L) {
            return ICliComputer.OpResult.fail(HOLDS_NONE.with(key.displayName().getString()));
        }
        final long take = Math.min(demand(quantity), held);
        final long taken = this.terminal.localStore().extract(key, take);
        if (taken <= 0L) {
            return ICliComputer.OpResult.fail(NOTHING_TO_PUSH);
        }
        final MainframeBlockEntity mainframe = this.mainframe();
        final var op = mainframe == null ? null
                : mainframe.submitNetworkInsert(key, taken, this.terminal.originLabel(origin));
        if (op == null) {
            this.terminal.localStore().insert(key, taken); // no dispatcher: put it straight back, never lose it
            return ICliComputer.OpResult.fail(NO_MAINFRAME);
        }
        op.setPriority(priority);
        op.onSettle(() -> {
            final long leftover = op.leftover();
            if (leftover > 0L) {
                this.terminal.localStore().insert(key, leftover);
            }
        });
        return ICliComputer.OpResult.ok(INSERT_QUEUED.with(taken, key.displayName().getString()));
    }

    /** Asks the network to make an item, at the default priority. */
    public ICliComputer.OpResult craft(final String item, final long quantity, final String origin) {
        return this.craft(item, quantity, OperationPriority.DEFAULT, origin);
    }

    /** Asks the network to make an item, at the priority given. */
    public ICliComputer.OpResult craft(final String item, final long quantity, final OperationPriority priority,
                                       final String origin) {
        final StorageKey key = StorageKey.byName(item);
        if (key == null) {
            return ICliComputer.OpResult.fail(UNKNOWN_ITEM.with(item));
        }
        final MainframeBlockEntity mainframe = this.mainframe();
        if (mainframe == null) {
            return ICliComputer.OpResult.fail(NO_MAINFRAME);
        }
        /*
         * Route through the shared entry point so the CLI and IQL craft a machine or multi-stage recipe
         * directly (not only a bench-planned tree), exactly as the terminal and Network Interactor do.
         */
        final var op = mainframe.submitCraftRequest(key, demand(quantity), true,
                this.terminal.originLabel(origin), null);
        if (op == null) {
            return ICliComputer.OpResult.fail(NO_PATTERN.with(key.displayName().getString()));
        }
        op.setPriority(priority);
        return ICliComputer.OpResult.ok(CRAFT_QUEUED.with(qtyLabel(quantity), key.displayName().getString()));
    }

    /**
     * Brings items out of the network and into the hands of whoever is typing, through this machine.
     *
     * <p>The same road the graphical program takes: the network hands them to the computer and the computer
     * hands them to the player, so a full inventory leaves them here rather than losing them. With nobody at
     * the machine there is nowhere to put anything, and that is said plainly instead of guessed at.
     */
    public ICliComputer.OpResult takeToHand(@Nullable final ServerPlayer player, final String item,
                                            final long quantity, final String origin) {
        if (player == null) {
            return ICliComputer.OpResult.fail(NOBODY_HERE);
        }
        final StorageKey key = StorageKey.byName(item);
        final NetworkUuid net = this.terminal.networkUuid();
        if (key == null || net == null) {
            return ICliComputer.OpResult.fail(key == null ? UNKNOWN_ITEM.with(item) : NO_NETWORK.text());
        }
        final MainframeBlockEntity mainframe = this.mainframe();
        if (mainframe == null) {
            return ICliComputer.OpResult.fail(NO_MAINFRAME);
        }
        final DataHandoff.Outcome outcome = DataHandoff.toPlayer(mainframe, this.level, net, player,
                this.terminal.localStore(), key, demand(quantity), this.terminal.originLabel(origin), () -> { });
        if (outcome != DataHandoff.Outcome.DEPOSITED) {
            return ICliComputer.OpResult.fail(SELECT_FAILED);
        }
        return ICliComputer.OpResult.ok(TO_HAND_QUEUED.with(qtyLabel(quantity), key.displayName().getString()));
    }

    /**
     * Hands what the player is holding to the network.
     *
     * <p>What the network cannot take comes back to the hand it left, which is the same promise every other
     * way of handing something over makes.
     */
    public ICliComputer.OpResult storeFromHand(@Nullable final ServerPlayer player, final long quantity,
                                               final String origin) {
        if (player == null) {
            return ICliComputer.OpResult.fail(NOBODY_TO_TAKE);
        }
        final ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            return ICliComputer.OpResult.fail(HOLDING_NOTHING);
        }
        final NetworkUuid net = this.terminal.networkUuid();
        final MainframeBlockEntity mainframe = this.mainframe();
        if (net == null || mainframe == null) {
            return ICliComputer.OpResult.fail(NO_MAINFRAME);
        }
        final int count = quantity <= 0L ? held.getCount() : (int) Math.min(quantity, held.getCount());
        final String name = held.getHoverName().getString();
        final DataHandoff.Outcome outcome = DataHandoff.intoNetwork(mainframe, this.level, net, player,
                DataHandoff.inventory(player, player.getInventory().selected), count, false,
                this.terminal.originLabel(origin), () -> { });
        return switch (outcome) {
            case DEPOSITED -> ICliComputer.OpResult.ok(INSERT_QUEUED.with(count, name));
            case NO_ROOM -> ICliComputer.OpResult.fail(NO_ROOM_FOR.with(name));
            case NO_DISPATCHER -> ICliComputer.OpResult.fail(NO_MAINFRAME);
            default -> ICliComputer.OpResult.fail(NOTHING_IN_HAND);
        };
    }

    /** Fills the container the player is holding with a fluid or chemical the network has. */
    public ICliComputer.OpResult fillHeld(@Nullable final ServerPlayer player, final String item,
                                          final String origin) {
        if (player == null) {
            return ICliComputer.OpResult.fail(NOBODY_TO_FILL);
        }
        if (player.getMainHandItem().isEmpty()) {
            return ICliComputer.OpResult.fail(NOTHING_TO_FILL);
        }
        final StorageKey key = this.fluidNamed(item);
        final NetworkUuid net = this.terminal.networkUuid();
        final MainframeBlockEntity mainframe = this.mainframe();
        if (key == null) {
            return ICliComputer.OpResult.fail(NO_FLUID.with(item));
        }
        if (net == null || mainframe == null) {
            return ICliComputer.OpResult.fail(NO_MAINFRAME);
        }
        final DataHandoff.Outcome outcome = DataHandoff.fillFromNetwork(mainframe, this.level, net, player,
                DataHandoff.inventory(player, player.getInventory().selected), key,
                this.terminal.originLabel(origin), () -> { });
        return switch (outcome) {
            case FILLED -> ICliComputer.OpResult.ok(FILLING.with(key.displayName().getString()));
            case NO_ROOM -> ICliComputer.OpResult.fail(TAKES_NONE);
            case NO_DISPATCHER -> ICliComputer.OpResult.fail(NO_MAINFRAME);
            default -> ICliComputer.OpResult.fail(DOES_NOT_HOLD.with(item));
        };
    }

    /** The fluid or chemical of that name the network is holding, or null when it holds none of it. */
    @Nullable
    private StorageKey fluidNamed(final String item) {
        final NetworkUuid net = this.terminal.networkUuid();
        if (net == null || item.isBlank()) {
            return null;
        }
        for (final StorageKey key : NetworkStorage.of(this.level, net).query().keySet()) {
            if ((key.isFluid() || key.isChemical())
                    && (key.registryId().toString().equalsIgnoreCase(item)
                        || key.registryId().getPath().equalsIgnoreCase(item)
                        || key.displayName().getString().equalsIgnoreCase(item))) {
                return key;
            }
        }
        return null;
    }

    /**
     * Holds an item where it is, so that whatever else asks for it waits instead of taking it.
     *
     * @param quantity how much to hold; none given holds everything the network has free of it
     */
    public ICliComputer.OpResult lock(final String item, final long quantity) {
        final StorageKey key = StorageKey.byName(item);
        if (key == null) {
            return ICliComputer.OpResult.fail(UNKNOWN_ITEM.with(item));
        }
        final MainframeBlockEntity mainframe = this.mainframe();
        if (mainframe == null) {
            return ICliComputer.OpResult.fail(NO_MAINFRAME);
        }
        final long demand = quantity > 0L ? quantity : Long.MAX_VALUE; // 0 locks everything available
        final long held = mainframe.lockType(key, demand, null);
        final String name = key.displayName().getString();
        if (held <= 0L) {
            return ICliComputer.OpResult.fail(mainframe.networkIndex().isManuallyLocked(key)
                    ? ALREADY_LOCKED.with(name) : NOTHING_TO_LOCK.with(name));
        }
        return ICliComputer.OpResult.ok(LOCK_HELD.with(held, name));
    }

    /** Lets an item go again, so that what was waiting on it can have it. */
    public ICliComputer.OpResult unlock(final String item) {
        final StorageKey key = StorageKey.byName(item);
        if (key == null) {
            return ICliComputer.OpResult.fail(UNKNOWN_ITEM.with(item));
        }
        final MainframeBlockEntity mainframe = this.mainframe();
        if (mainframe == null) {
            return ICliComputer.OpResult.fail(NO_MAINFRAME);
        }
        final long released = mainframe.unlockType(key);
        if (released <= 0L) {
            return ICliComputer.OpResult.fail(NOT_LOCKED.with(key.displayName().getString()));
        }
        return ICliComputer.OpResult.ok(UNLOCKED.with(released, key.displayName().getString()));
    }

    /**
     * Runs one of the Mainframe's own jobs on its index: {@code analyze} reconciles the catalog against the disks,
     * {@code reindex} rebuilds it from them, and {@code vacuum} clears the entries nothing stands behind any more.
     *
     * <p>Only the Mainframe runs these, because they are what it keeps.
     */
    public ICliComputer.OpResult maintenance(final IqlVerb action) {
        if (!this.terminal.isMainframeHost()) {
            return ICliComputer.OpResult.fail(MAINFRAME_ONLY);
        }
        final NetworkUuid net = this.terminal.networkUuid();
        final MainframeBlockEntity mainframe = this.mainframe();
        if (mainframe == null || net == null) {
            return ICliComputer.OpResult.fail(NO_MAINFRAME);
        }
        final var index = mainframe.networkIndex();
        return switch (action) {
            case ANALYZE -> {
                index.analyzeIncremental(this.level, net);
                yield ICliComputer.OpResult.ok(ANALYZED.with(index.catalogSize()));
            }
            case REINDEX -> {
                // The disks are read now; the catalog is built off the tick and swapped in a tick or two later.
                mainframe.reindexAsync(null);
                yield ICliComputer.OpResult.ok(REINDEXING);
            }
            case VACUUM -> {
                final int freed = index.vacuum(this.level, net);
                yield ICliComputer.OpResult.ok((freed == 1 ? VACUUMED_ONE : VACUUMED_MANY).with(freed));
            }
            default -> ICliComputer.OpResult.fail(UNKNOWN_MAINTENANCE.with(action.toString()));
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
            return ICliComputer.OpResult.fail(NO_MAINFRAME);
        }
        final String wanted = id.trim().toLowerCase(Locale.ROOT);
        if (wanted.isEmpty()) {
            return ICliComputer.OpResult.fail(CANCEL_USAGE);
        }
        for (final INetworkOperation operation : mainframe.liveOperations()) {
            final String full = operation.operationId().toString();
            if (full.startsWith(wanted) && wanted.length() >= ShortId.of(full).length()) {
                final OperationRecord record = operation.liveRecord();
                if (!mainframe.cancelOperation(operation.operationId())) {
                    return ICliComputer.OpResult.fail(ALREADY_SETTLED.with(ShortId.of(full)));
                }
                return ICliComputer.OpResult.ok(CANCELLED.with(OperationRecord.typeName(record.type()),
                        record.name().getString()));
            }
        }
        return ICliComputer.OpResult.fail(NOT_IN_FLIGHT.with(wanted));
    }

    /** Asks the network to move an Operation in flight to another priority; one that has settled cannot be hurried. */
    public ICliComputer.OpResult reprioritise(final String id, final String priority) {
        final MainframeBlockEntity mainframe = this.mainframe();
        if (mainframe == null) {
            return ICliComputer.OpResult.fail(NO_MAINFRAME);
        }
        final OperationPriority wanted = OperationPriority.fromKeyword(priority).orElse(null);
        if (wanted == null) {
            return ICliComputer.OpResult.fail(NO_PRIORITY.with(priority));
        }
        final String prefix = id.trim().toLowerCase(Locale.ROOT);
        if (prefix.isEmpty()) {
            return ICliComputer.OpResult.fail(WHICH_OPERATION);
        }
        for (final INetworkOperation operation : mainframe.liveOperations()) {
            final String full = operation.operationId().toString();
            if (full.startsWith(prefix) && prefix.length() >= ShortId.of(full).length()) {
                operation.setPriority(wanted);
                return ICliComputer.OpResult.ok(NOW_AT.with(ShortId.of(full), wanted.serializedName()));
            }
        }
        return ICliComputer.OpResult.fail(NOT_RUNNING.with(id));
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

    /** How a quantity reads back to whoever asked: a real count, or the word for all, for ALL and none given. */
    public static Text qtyLabel(final long quantity) {
        return quantity <= 0L ? ALL.text() : Text.literal(Long.toString(quantity));
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
