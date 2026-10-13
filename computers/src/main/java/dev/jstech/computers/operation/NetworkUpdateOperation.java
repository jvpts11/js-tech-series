/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation;

import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.operation.payload.OperationRecord;
import dev.jstech.computers.operation.payload.network.NetworkLookup;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.computers.workshop.UpdateAction;
import dev.jstech.computers.workshop.UpdateDoor;
import dev.jstech.computers.workshop.UpdateDrawer;
import dev.jstech.computers.workshop.UpdateRequest;
import dev.jstech.computers.workshop.Workshop;
import dev.jstech.core.operation.ILatencyScheduler;
import dev.jstech.core.operation.OperationFailure;
import dev.jstech.core.operation.OperationPriority;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.util.Loaded;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.core.uuid.NodeUuid;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * An UPDATE: an item the network holds goes to one of the asking computer's personal-use cards, the card works on
 * it, and what it made goes back to the network, to the server the item came from when that server has room.
 *
 * <p>Three steps, each a row of its own. SUB_SELECT takes the item out of the servers into the card's drawer, as a
 * SELECT does, locks and latency and all. SUB_UPDATE is the card's work: smelting at the card's furnace pace, in the
 * same queue as the Workshop's own smelting; enchanting and the anvil at once, whoever asked paying the card's
 * price. SUB_INSERT writes the result back, as an INSERT does, trying the item's own server first; what the network
 * then has no room for is left at the card's output. Smelting more than a stack goes round the three again, a stack
 * at a time; a step that cannot be done sends the item back as it was.
 *
 * <p>The UPDATE belongs to the computer that asked. If that computer stops or loses its card, the UPDATE is
 * discarded and what the card held is left at its output; for the same reason it does not outlive the world being
 * closed, and the drawer hands what it held to the card's output when the computer is next loaded.
 */
@TextHolder
public final class NetworkUpdateOperation implements INetworkOperation {

    private final ServerLevel level;
    private final NetworkUuid network;
    private final UpdateRequest request;
    private final NetworkIndex index;
    @Nullable
    private final ILatencyScheduler scheduler;
    private final UUID operationId = UUID.randomUUID();
    private final long demand;
    private final List<OperationRecord.MoveRow> moves = new ArrayList<>();
    /* What is on its way back to the network, each beside the INSERT writing it. */
    private final List<Returning> returning = new ArrayList<>();
    private Phase phase = Phase.WAITING;
    @Nullable
    private NetworkSelectOperation taking;
    @Nullable
    private NetworkSelectOperation takingSecond;
    @Nullable
    private NodeUuid origin;
    private long selected;
    private long roundTaken;
    private long updatedBefore;
    private long updated;
    private long inserted;
    /* Why the work stopped short, found on the way and said once the item is back. */
    private OperationFailure failure = OperationFailure.NONE;
    private boolean done;
    private boolean cancelled;
    private byte status = OperationRecord.STATUS_PARTIAL;
    private OperationFailure cause = OperationFailure.NONE;
    private OperationPriority priority = OperationPriority.DEFAULT;
    @Nullable
    private Runnable onSettle;

    /** The network had none of the item, or less than was asked for. */
    public static final TextKey NOT_HELD = TextKey.of("jsc.operation.failure.update.not_held",
            "the network was not holding the %s");
    /** A repair found nothing to mend with, or a combine nothing to add. */
    public static final TextKey NO_SECOND = TextKey.of("jsc.operation.failure.update.no_second",
            "the network was not holding the %s to work it with");
    /** Whoever asked has left, and the card's price is theirs to pay. */
    public static final TextKey NOBODY_TO_PAY = TextKey.of("jsc.operation.failure.update.nobody_to_pay",
            "whoever asked for it was not here to pay");
    /** The card would not do it: nothing to change, too expensive, or too few levels to pay. */
    public static final TextKey NOT_DONE = TextKey.of("jsc.operation.failure.update.not_done",
            "the card could not do it to the %s: nothing to change, too expensive or too few levels");

    /** The rows the three steps are shown under, the words of the network's own log. */
    private static final String SUB_SELECT = "SUB_SELECT";
    private static final String SUB_UPDATE = "SUB_UPDATE";
    private static final String SUB_INSERT = "SUB_INSERT";

    public NetworkUpdateOperation(final ServerLevel level, final NetworkUuid network, final UpdateRequest request,
                                  final NetworkIndex index, @Nullable final ILatencyScheduler scheduler) {
        this.level = level;
        this.network = network;
        this.request = Objects.requireNonNull(request, "request");
        this.index = index;
        this.scheduler = scheduler;
        final int stack = request.key().stack(1).getMaxStackSize();
        final long asked = Math.max(1L, request.quantity());
        this.demand = switch (request.action()) {
            case SMELT -> asked;
            case ENCHANT -> 1L;
            case REPAIR, COMBINE, NAME -> Math.min(asked, stack);
        };
    }

    private enum Phase {
        /** The card is busy: another UPDATE holds its drawer, or the Workshop's own smelting has its furnace. */
        WAITING,
        TAKING,
        WORKING,
        RETURNING
    }

    /** One stack on its way back, from which of the drawer's slots, and the INSERT writing it. */
    private record Returning(ItemStack stack, boolean second, NetworkInsertOperation insert) {
    }

    /** What was asked for. */
    public UpdateRequest request() {
        return request;
    }

    /** How many have been changed so far. */
    public long updated() {
        return updated;
    }

    /** Runs {@code callback} once the UPDATE has settled, at once when it already has. */
    public NetworkUpdateOperation onSettle(final Runnable callback) {
        this.onSettle = callback;
        if (done) {
            callback.run();
        }
        return this;
    }

    @Override
    public UUID operationId() {
        return operationId;
    }

    @Override
    public String typeId() {
        return ComputingOperations.UPDATE;
    }

    @Override
    public void tick(final long throughputBudget) {
        if (done) {
            return;
        }
        final PersonalComputerBlockEntity computer = computer();
        if (computer == null || !computer.isRunning() || !request.action().card().in(computer.workshopCards())) {
            discard(computer);
            return;
        }
        final UpdateDrawer drawer = computer.updateDrawer();
        if (phase != Phase.WAITING && !drawer.heldBy(operationId)) {
            // The computer was loaded again under this UPDATE; its drawer no longer knows it.
            discard(computer);
            return;
        }
        switch (phase) {
            case WAITING -> claim(computer, drawer);
            case TAKING -> take(drawer, throughputBudget);
            case WORKING -> work(drawer);
            case RETURNING -> giveBack(computer, drawer, throughputBudget);
        }
    }

    @Override
    public boolean isDone() {
        return done;
    }

    @Override
    public boolean isWaiting() {
        return !done && (phase == Phase.WAITING || phase == Phase.TAKING && taking != null && taking.isWaiting());
    }

    @Override
    public void abandon() {
        discard(computer());
    }

    @Override
    public void cancel() {
        cancelled = true;
        discard(computer());
    }

    @Override
    public OperationPriority priority() {
        return priority;
    }

    @Override
    public void setPriority(final OperationPriority priority) {
        this.priority = Objects.requireNonNull(priority, "priority");
    }

    /** How it settled, or how it stands while it runs. */
    public byte status() {
        return status;
    }

    /** Why it did not go as asked, or nothing where there is nothing to say. */
    public OperationFailure cause() {
        return cause;
    }

    @Override
    public OperationRecord toRecord() {
        return rowOf(status);
    }

    @Override
    public OperationRecord liveRecord() {
        return rowOf(done ? status : isWaiting() ? OperationRecord.STATUS_WAITING
                : OperationRecord.STATUS_PROCESSING);
    }

    /* The card is free and, for smelting, the Workshop is not smelting: the UPDATE takes the drawer. */
    private void claim(final PersonalComputerBlockEntity computer, final UpdateDrawer drawer) {
        final boolean smelt = request.action() == UpdateAction.SMELT;
        if (!drawer.free() || smelt && !computer.workshop().slot(Workshop.FURNACE_IN).isEmpty()) {
            return;
        }
        drawer.hold(operationId, request.action().card(), smelt);
        startRound(drawer);
    }

    /* SUB_SELECT: as much of the item as one round takes, and the second item a repair or a combine needs. */
    private void startRound(final UpdateDrawer drawer) {
        final Set<NodeUuid> from = request.from() == null ? null : Set.of(request.from());
        taking = new NetworkSelectOperation(level, network, request.key(), roundSize(), drawer.sink(UpdateDrawer.WORK),
                request.label(), OperationRecord.TYPE_SELECT, UUID.randomUUID(), index, scheduler, from);
        if (request.action().takesSecond() && request.with() != null) {
            takingSecond = new NetworkSelectOperation(level, network, request.with(), secondAmount(),
                    drawer.sink(UpdateDrawer.SECOND), request.label(), OperationRecord.TYPE_SELECT, UUID.randomUUID(),
                    index, scheduler, null);
        }
        phase = Phase.TAKING;
    }

    private void take(final UpdateDrawer drawer, final long budget) {
        tickChild(taking, budget);
        tickChild(takingSecond, budget);
        if (taking != null && !taking.isDone() || takingSecond != null && !takingSecond.isDone()) {
            return;
        }
        final ItemStack work = drawer.slot(UpdateDrawer.WORK);
        if (taking != null) {
            final List<Map.Entry<NodeUuid, Long>> sources = taking.perNode();
            if (origin == null && !sources.isEmpty()) {
                origin = sources.get(0).getKey();
            }
            for (final Map.Entry<NodeUuid, Long> source : sources) {
                moves.add(new OperationRecord.MoveRow(NetworkLookup.serverLabel(level, source.getKey()),
                        source.getValue(), request.label()));
            }
        }
        taking = null;
        takingSecond = null;
        roundTaken = work.getCount();
        selected += roundTaken;
        if (work.isEmpty()) {
            if (selected == 0L) {
                failure = OperationFailure.of(NOT_HELD, request.key().displayName().getString());
            }
            phase = Phase.RETURNING;
            return;
        }
        if (request.action().takesSecond() && drawer.slot(UpdateDrawer.SECOND).isEmpty()) {
            failure = OperationFailure.of(NO_SECOND, request.with() == null ? request.key().displayName().getString()
                    : request.with().displayName().getString());
            phase = Phase.RETURNING;
            return;
        }
        phase = Phase.WORKING;
    }

    /* SUB_UPDATE: the furnace smelts on the computer's own ticks; the enchanting table and the anvil work at once. */
    private void work(final UpdateDrawer drawer) {
        switch (request.action()) {
            case SMELT -> {
                updated = updatedBefore + roundTaken - drawer.slot(UpdateDrawer.WORK).getCount();
                if (drawer.slot(UpdateDrawer.WORK).isEmpty()) {
                    updatedBefore = updated;
                    noteChange(request.key().stack((int) roundTaken), drawer.slot(UpdateDrawer.MADE));
                    phase = Phase.RETURNING;
                }
            }
            case ENCHANT -> enchant(drawer);
            case REPAIR, COMBINE, NAME -> anvil(drawer);
        }
    }

    private void enchant(final UpdateDrawer drawer) {
        final ServerPlayer payer = payer();
        phase = Phase.RETURNING;
        if (payer == null) {
            failure = OperationFailure.of(NOBODY_TO_PAY);
            return;
        }
        final ItemStack before = drawer.slot(UpdateDrawer.WORK).copy();
        final ItemStack enchanted = Workshop.enchanted(payer, drawer.slot(UpdateDrawer.WORK).copy(), request.offer());
        if (enchanted.isEmpty()) {
            failure = OperationFailure.of(NOT_DONE, before.getHoverName().getString());
            return;
        }
        drawer.take(UpdateDrawer.WORK);
        drawer.put(UpdateDrawer.MADE, enchanted);
        updated += 1L;
        noteChange(before, enchanted);
    }

    private void anvil(final UpdateDrawer drawer) {
        final ServerPlayer payer = payer();
        phase = Phase.RETURNING;
        if (payer == null) {
            failure = OperationFailure.of(NOBODY_TO_PAY);
            return;
        }
        final ItemStack left = drawer.slot(UpdateDrawer.WORK).copy();
        final ItemStack right = drawer.slot(UpdateDrawer.SECOND).copy();
        // A repair or a combine with no name asked for keeps the name the item has.
        final String name = request.name().isEmpty() ? left.getHoverName().getString() : request.name();
        final Workshop.AnvilTake took = Workshop.takeAnvilFor(payer, left, right, name);
        if (took == null) {
            failure = OperationFailure.of(NOT_DONE, left.getHoverName().getString());
            return;
        }
        drawer.put(UpdateDrawer.WORK, took.left());
        drawer.put(UpdateDrawer.SECOND, took.right());
        drawer.put(UpdateDrawer.MADE, took.made());
        updated += took.made().getCount();
        noteChange(left, took.made());
    }

    /* SUB_INSERT: what the card made, and anything it did not use, back to the network; the rest to its output. */
    private void giveBack(final PersonalComputerBlockEntity computer, final UpdateDrawer drawer, final long budget) {
        if (returning.isEmpty()) {
            for (final int slot : new int[] {UpdateDrawer.MADE, UpdateDrawer.WORK, UpdateDrawer.SECOND}) {
                final ItemStack stack = drawer.take(slot);
                if (!stack.isEmpty()) {
                    returning.add(new Returning(stack, slot == UpdateDrawer.SECOND, new NetworkInsertOperation(level,
                            network, StorageKey.of(stack), stack.getCount(), request.label(), index, scheduler,
                            origin)));
                }
            }
        }
        boolean all = true;
        for (final Returning back : returning) {
            tickChild(back.insert(), budget);
            all &= back.insert().isDone();
        }
        if (!all) {
            return;
        }
        for (final Returning back : returning) {
            final NetworkInsertOperation insert = back.insert();
            inserted += insert.writtenTotal();
            for (final Map.Entry<NodeUuid, Long> written : insert.perNode()) {
                moves.add(new OperationRecord.MoveRow(request.label(), written.getValue(),
                        NetworkLookup.serverLabel(level, written.getKey())));
            }
            final long left = insert.leftover();
            if (left > 0L) {
                leave(computer, back.second(), back.stack().copyWithCount((int) left));
            }
        }
        returning.clear();
        if (!failure.isPresent() && request.action() == UpdateAction.SMELT && selected < demand && roundTaken > 0L) {
            startRound(drawer);
            return;
        }
        drawer.release(operationId);
        if (failure.isPresent()) {
            settle(updated > 0L ? OperationRecord.STATUS_PARTIAL : OperationRecord.STATUS_FAILED, failure);
        } else if (updated >= demand) {
            settle(OperationRecord.STATUS_COMPLETED, OperationFailure.NONE);
        } else {
            settle(updated > 0L ? OperationRecord.STATUS_PARTIAL : OperationRecord.STATUS_FAILED,
                    OperationFailure.of(NOT_HELD, request.key().displayName().getString()));
        }
    }

    /*
     * Stops here: what is on its way is stopped where it is, and everything the card held, the drawer's work and what
     * was on its way back alike, is left at the card's output, or written straight back to the network when the
     * computer is not there to take it.
     */
    private void discard(@Nullable final PersonalComputerBlockEntity computer) {
        if (done) {
            return;
        }
        abandonChild(taking);
        abandonChild(takingSecond);
        final boolean held = computer != null && computer.updateDrawer().heldBy(operationId);
        for (final Returning back : returning) {
            back.insert().abandon();
            final long left = back.insert().leftover();
            if (left > 0L) {
                leave(held ? computer : null, back.second(), back.stack().copyWithCount((int) left));
            }
        }
        returning.clear();
        if (held) {
            final UpdateDrawer drawer = computer.updateDrawer();
            for (final int slot : new int[] {UpdateDrawer.MADE, UpdateDrawer.WORK, UpdateDrawer.SECOND}) {
                leave(computer, slot == UpdateDrawer.SECOND, drawer.take(slot));
            }
            drawer.release(operationId);
        }
        settle(OperationRecord.STATUS_DISCARDED, OperationFailure.NONE);
    }

    /*
     * Leaves {@code stack} at the card's output on {@code computer}; with no computer to take it, back into the
     * network; what neither takes falls out where the computer stands, so nothing is ever lost.
     */
    private void leave(@Nullable final PersonalComputerBlockEntity computer, final boolean second,
                       final ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        ItemStack left = stack;
        if (computer != null) {
            left = computer.updateDrawer().park(computer.workshop(), request.action().card(), second, stack);
        } else {
            final long stored = NetworkStorage.of(level, network).insert(StorageKey.of(stack), stack.getCount());
            left = stored >= stack.getCount() ? ItemStack.EMPTY
                    : stack.copyWithCount((int) (stack.getCount() - stored));
        }
        if (!left.isEmpty()) {
            final BlockPos at = request.computer();
            Containers.dropItemStack(level, at.getX() + 0.5, at.getY() + 1.0, at.getZ() + 0.5, left);
        }
    }

    private void settle(final byte finalStatus, final OperationFailure why) {
        if (done) {
            return;
        }
        done = true;
        status = cancelled && finalStatus != OperationRecord.STATUS_COMPLETED ? OperationRecord.STATUS_DISCARDED
                : finalStatus;
        cause = status == OperationRecord.STATUS_COMPLETED || status == OperationRecord.STATUS_DISCARDED
                ? OperationFailure.NONE : why;
        if (onSettle != null) {
            onSettle.run();
        }
    }

    /* The before and after of what the card changed, as the log shows it: the item it was, how many, what it is. */
    private void noteChange(final ItemStack before, final ItemStack after) {
        if (!after.isEmpty()) {
            moves.add(new OperationRecord.MoveRow(before.getHoverName().getString(), after.getCount(),
                    after.getHoverName().getString()));
        }
    }

    /* How much one round takes: a smelt as much as the drawer and what it makes have room for; the rest all of it. */
    private long roundSize() {
        final long left = Math.max(1L, demand - selected);
        if (request.action() != UpdateAction.SMELT) {
            return left;
        }
        final ItemStack one = request.key().stack(1);
        final Workshop.Smelt smelt = Workshop.smelt(level, one);
        final int each = smelt == null ? 1 : Math.max(1, smelt.result().getCount());
        final int outputRoom = smelt == null ? one.getMaxStackSize() : smelt.result().getMaxStackSize() / each;
        return Math.max(1L, Math.min(left, Math.min(one.getMaxStackSize(), outputRoom)));
    }

    /* How much of the second item to take: one to combine or to mend with a second of the same, else the material. */
    private long secondAmount() {
        if (request.action() == UpdateAction.COMBINE || request.with() != null
                && request.with().item() == request.key().item()) {
            return 1L;
        }
        return UpdateDoor.materialNeeded(request.key().stack(1));
    }

    @Nullable
    private PersonalComputerBlockEntity computer() {
        return Loaded.blockEntity(level, request.computer()) instanceof PersonalComputerBlockEntity computer
                ? computer : null;
    }

    @Nullable
    private ServerPlayer payer() {
        return request.payer() == null || level.getServer() == null ? null
                : level.getServer().getPlayerList().getPlayer(request.payer());
    }

    private static void tickChild(@Nullable final INetworkOperation child, final long budget) {
        if (child != null && !child.isDone()) {
            child.tick(budget);
        }
    }

    private static void abandonChild(@Nullable final INetworkOperation child) {
        if (child != null && !child.isDone()) {
            child.abandon();
        }
    }

    private OperationRecord rowOf(final byte recordStatus) {
        return new OperationRecord(operationId, OperationRecord.TYPE_UPDATE, request.key(), demand, updated,
                recordStatus, priority, List.copyOf(moves.subList(Math.max(0, moves.size() - OperationRecord.MAX_MOVES),
                moves.size())), subRows()).withCause(cause);
    }

    /* SUB_SELECT, SUB_UPDATE and SUB_INSERT, each with how far it got. */
    private List<OperationRecord.SubRow> subRows() {
        final long taken = selected + (taking == null ? 0L : taking.moved());
        final boolean takingNow = phase == Phase.TAKING && !done;
        final boolean working = phase == Phase.WORKING && !done;
        final boolean returningNow = phase == Phase.RETURNING && !done;
        long writing = inserted;
        for (final Returning back : returning) {
            writing += back.insert().writtenTotal();
        }
        return List.of(
                new OperationRecord.SubRow(SUB_SELECT, demand, taken, done || taken >= demand
                        ? OperationRecord.SubRow.SUB_COMPLETED : takingNow ? (taking != null && taking.isWaiting()
                        ? OperationRecord.SubRow.SUB_READING : OperationRecord.SubRow.SUB_STREAMING)
                        : OperationRecord.SubRow.SUB_PENDING),
                new OperationRecord.SubRow(SUB_UPDATE, demand, updated, done || updated >= demand
                        ? OperationRecord.SubRow.SUB_COMPLETED : working ? OperationRecord.SubRow.SUB_STREAMING
                        : OperationRecord.SubRow.SUB_PENDING),
                new OperationRecord.SubRow(SUB_INSERT, updated, writing, done
                        ? OperationRecord.SubRow.SUB_COMPLETED : returningNow ? OperationRecord.SubRow.SUB_STREAMING
                        : OperationRecord.SubRow.SUB_PENDING));
    }
}
