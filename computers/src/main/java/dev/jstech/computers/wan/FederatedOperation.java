/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.wan;

import dev.jstech.computers.block.DataLinkNames;
import dev.jstech.computers.block.DataWires;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.config.ComputersServerConfig;
import dev.jstech.computers.engine.CraftRequest;
import dev.jstech.computers.operation.DataHandoff;
import dev.jstech.computers.operation.INetworkOperation;
import dev.jstech.computers.operation.NetworkInsertOperation;
import dev.jstech.computers.operation.payload.OperationRecord;
import dev.jstech.computers.storage.IDataSink;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.jstechseries.operation.OperationFailure;
import dev.jstech.core.jstechseries.operation.OperationPriority;
import dev.jstech.core.jstechseries.operation.OperationStatus;
import dev.jstech.core.text.GameText;
import dev.jstech.core.uuid.NetworkUuid;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * An Operation that crosses to a network this one is federated with (computers/docs/design/network.md): it runs on the
 * asking network's Mainframe, and the network it reaches runs its own half in its own storage under its own locks, each
 * side logging its own. A network that relays for it logs the crossing too.
 *
 * <ul>
 *   <li>Take: the far network takes the items from its storage onto the line, at most what the line holds, and this one
 *   takes them off at the line's speed into where they were asked to go.</li>
 *   <li>Send: the items go down the line at its speed and the far network puts them into its storage; what it has no
 *   room for comes back to whoever sent them.</li>
 *   <li>Craft: the far network makes them with its own storage, and they stay there.</li>
 * </ul>
 *
 * <p>While the far network's Mainframe is not loaded the Operation waits for it. While the way is down it waits for the
 * line, unless items were on it: those are lost, by the rule for a network that splits, when the server says so.
 * Cancelling it cancels the far half, and what was on the line goes back to the far network's storage.
 */
public final class FederatedOperation implements INetworkOperation {

    private final UUID operationId = UUID.randomUUID();
    private final ServerLevel level;
    private final NetworkUuid home;
    private final NetworkUuid target;
    private final WanRight ask;
    private final StorageKey key;
    private final long requested;
    @Nullable
    private final IDataSink to;
    @Nullable
    private final Player sender;
    private final String homeName;
    private final String targetName;
    private final List<NetworkUuid> relays;
    private final LineBuffer line = new LineBuffer();
    private OperationPriority priority = OperationPriority.DEFAULT;
    private OperationStatus status = OperationStatus.PENDING;
    private OperationFailure note = OperationFailure.NONE;
    @Nullable
    private INetworkOperation farHalf;
    private long moved;
    private long crossed;
    private int stalled;
    private boolean settled;

    /** How many ticks of its speed a line holds: how long an item takes to cross. */
    public static final int TRANSIT_TICKS = 20;
    /** What the Operation is, in the Operations tab and the log. */
    public static final String KIND = "wan";
    /* Ticks with nothing delivered, the far half done, before what is left on the line goes back. */
    private static final int STALL_LIMIT = 40;

    private FederatedOperation(final ServerLevel level, final FederationPath path, final WanRight ask,
                               final StorageKey key, final long requested, @Nullable final IDataSink to,
                               @Nullable final Player sender) {
        this.level = level;
        this.home = path.networks().getFirst();
        this.target = path.target();
        this.ask = ask;
        this.key = Objects.requireNonNull(key, "key");
        this.requested = requested;
        this.to = to;
        this.sender = sender;
        this.homeName = WanFederation.gatewayName(level, path.out().getFirst().gateway());
        this.targetName = WanFederation.farName(level, path);
        this.relays = path.relays();
    }

    /** Takes {@code amount} of {@code key} from the far end of {@code path} into {@code to}. */
    public static FederatedOperation take(final ServerLevel level, final FederationPath path, final StorageKey key,
                                          final long amount, final IDataSink to) {
        return new FederatedOperation(level, path, WanRight.TAKE, key, amount, to, null);
    }

    /** Sends {@code amount} of {@code key}, already taken from {@code sender}, to the far end of {@code path}. */
    public static FederatedOperation send(final ServerLevel level, final FederationPath path, final StorageKey key,
                                          final long amount, final Player sender) {
        return new FederatedOperation(level, path, WanRight.SEND, key, amount, null, sender);
    }

    /** Has {@code amount} of {@code key} crafted at the far end of {@code path}, where it stays. */
    public static FederatedOperation craft(final ServerLevel level, final FederationPath path, final StorageKey key,
                                           final long amount) {
        return new FederatedOperation(level, path, WanRight.CRAFT, key, amount, null, null);
    }

    /** The network it reaches. */
    public NetworkUuid target() {
        return target;
    }

    /** Where it has got to, as its record's reason says it. */
    public OperationFailure note() {
        return note;
    }

    @Override
    public UUID operationId() {
        return operationId;
    }

    @Override
    public String typeId() {
        return KIND;
    }

    @Override
    public void tick(final long throughputBudget) {
        if (settled) {
            return;
        }
        final FederationPath path = WanFederation.of(level).path(home, target, level.getGameTime()).orElse(null);
        if (path == null) {
            lineDown();
            return;
        }
        if (!path.allows(ask)) {
            give(OperationStatus.FAILED, OperationFailure.of(WanTexts.NOT_ALLOWED, targetName,
                    GameText.resolve(WanTexts.rightName(ask))));
            return;
        }
        final MainframeBlockEntity far = DataWires.mainframeOf(level, target);
        if (far == null) {
            status = OperationStatus.WAITING;
            note = OperationFailure.of(WanTexts.NOT_LOADED, targetName);
            return;
        }
        final long speed = Math.max(0L, Math.min(path.speed(), throughputBudget));
        switch (ask) {
            case TAKE -> take(far, path, speed);
            case SEND -> send(far, path, speed);
            case CRAFT -> craft(far, path);
            default -> give(OperationStatus.FAILED, OperationFailure.NONE);
        }
    }

    @Override
    public boolean isDone() {
        return settled;
    }

    @Override
    public boolean isWaiting() {
        return status == OperationStatus.WAITING;
    }

    @Override
    public void abandon() {
        letGo(OperationStatus.DISCARDED);
    }

    @Override
    public void cancel() {
        letGo(OperationStatus.DISCARDED);
    }

    @Override
    public Map<StorageKey, Long> lose() {
        if (settled) {
            return Map.of();
        }
        final Map<StorageKey, Long> lost = onTheLine();
        line.empty();
        crossed = 0L;
        cancelFarHalf();
        give(OperationStatus.FAILED, OperationFailure.of(WanTexts.LINE_FELL, String.valueOf(total(lost))));
        return lost;
    }

    @Override
    public OperationPriority priority() {
        return priority;
    }

    @Override
    public void setPriority(final OperationPriority priority) {
        this.priority = priority;
    }

    @Override
    public OperationRecord toRecord() {
        final byte type = switch (ask) {
            case SEND -> OperationRecord.TYPE_INSERT;
            case CRAFT -> OperationRecord.TYPE_CRAFT;
            default -> OperationRecord.TYPE_SELECT;
        };
        return new OperationRecord(operationId, type, key, requested, moved, (byte) status.id(), priority,
                List.of(), List.of(), 0L, 0L, note, KIND, targetName, "");
    }

    @Override
    public OperationRecord liveRecord() {
        return toRecord();
    }

    /* Take: the far half onto the line, this side off it at the line's speed. */
    private void take(final MainframeBlockEntity far, final FederationPath path, final long speed) {
        line.holdAtMost(path.speed() * TRANSIT_TICKS);
        if (farHalf == null) {
            farHalf = far.networkOperations().pull(key, requested, line, WanTexts.from(homeName));
            if (farHalf == null) {
                give(OperationStatus.FAILED, OperationFailure.of(WanTexts.FAR_REFUSED, targetName));
                return;
            }
        }
        final long delivered = to == null ? 0L : line.deliver(to, speed);
        moved += delivered;
        status = OperationStatus.PROCESSING;
        note = crossing(path, moved);
        if (moved >= requested) {
            give(OperationStatus.COMPLETED, OperationFailure.of(WanTexts.ALL_HERE, String.valueOf(moved)));
            return;
        }
        stalled = farHalf.isDone() && delivered == 0L ? stalled + 1 : 0;
        if (farHalf.isDone() && (line.count() == 0L || stalled >= STALL_LIMIT)) {
            giveBack(far, line.empty());
            give(moved > 0L ? OperationStatus.COMPLETED_PARTIAL : OperationStatus.FAILED,
                    OperationFailure.of(WanTexts.SOME_HERE, String.valueOf(moved), String.valueOf(requested)));
        }
    }

    /* Send: the items cross at the line's speed, and the far network puts them away once they are all over. */
    private void send(final MainframeBlockEntity far, final FederationPath path, final long speed) {
        if (farHalf == null) {
            crossed = Math.min(requested, crossed + speed);
            status = OperationStatus.PROCESSING;
            note = crossing(path, crossed);
            if (crossed < requested) {
                return;
            }
            farHalf = far.networkOperations().push(key, requested, WanTexts.from(homeName));
            if (farHalf == null) {
                returnToSender(requested);
                give(OperationStatus.FAILED, OperationFailure.of(WanTexts.FAR_REFUSED, targetName));
            }
            return;
        }
        if (!farHalf.isDone()) {
            return;
        }
        final long leftover = farHalf instanceof NetworkInsertOperation insert ? insert.leftover() : 0L;
        moved = requested - leftover;
        returnToSender(leftover);
        give(leftover == 0L ? OperationStatus.COMPLETED
                        : moved > 0L ? OperationStatus.COMPLETED_PARTIAL : OperationStatus.FAILED,
                OperationFailure.of(WanTexts.TOOK, targetName, String.valueOf(moved), String.valueOf(requested)));
    }

    /* Craft: the far network makes them its own way; this side follows it to the end. */
    private void craft(final MainframeBlockEntity far, final FederationPath path) {
        if (farHalf == null) {
            farHalf = far.networkOperations().craft(CraftRequest.of(key, requested, true, WanTexts.from(homeName),
                    null));
            if (farHalf == null) {
                give(OperationStatus.FAILED, OperationFailure.of(WanTexts.FAR_REFUSED, targetName));
                return;
            }
        }
        status = OperationStatus.PROCESSING;
        note = path.hops() > 1 ? through(path) : OperationFailure.of(WanTexts.CRAFTING_THERE, targetName);
        if (farHalf.isDone()) {
            final OperationRecord there = farHalf.toRecord();
            moved = there.moved();
            give(OperationStatus.of(there.status()).orElse(OperationStatus.FAILED),
                    OperationFailure.of(WanTexts.CRAFTED_THERE, targetName, String.valueOf(moved)));
        }
    }

    /*
     * The way is down: what was on the line is lost when the server says a network that splits loses it, else the
     * Operation waits for the line to come back.
     */
    private void lineDown() {
        final Map<StorageKey, Long> onIt = onTheLine();
        if (!onIt.isEmpty() && ComputersServerConfig.loseOnNetworkSplit()) {
            line.empty();
            crossed = 0L;
            cancelFarHalf();
            give(OperationStatus.FAILED, OperationFailure.of(WanTexts.LINE_FELL, String.valueOf(total(onIt))));
            return;
        }
        status = OperationStatus.WAITING;
        note = OperationFailure.of(WanTexts.LINE_DOWN, targetName);
    }

    /* Cancelled or let go of: the far half is cancelled and what was on its way goes back where it came from. */
    private void letGo(final OperationStatus ending) {
        if (settled) {
            return;
        }
        cancelFarHalf();
        final MainframeBlockEntity far = DataWires.mainframeOf(level, target);
        if (ask == WanRight.TAKE) {
            final Map<StorageKey, Long> onIt = line.empty();
            if (far != null) {
                giveBack(far, onIt);
            }
        } else if (ask == WanRight.SEND && farHalf == null) {
            returnToSender(requested);
        }
        give(ending, OperationFailure.of(WanTexts.CANCELLED_BOTH, targetName));
    }

    private void cancelFarHalf() {
        if (farHalf == null || farHalf.isDone()) {
            return;
        }
        final MainframeBlockEntity far = DataWires.mainframeOf(level, target);
        if (far == null || !far.cancelOperation(farHalf.operationId())) {
            farHalf.cancel();
        }
    }

    /* Settles it, and has every network that relayed it log the crossing too. */
    private void give(final OperationStatus ending, final OperationFailure why) {
        status = ending;
        note = why;
        settled = true;
        for (final NetworkUuid relay : relays) {
            final MainframeBlockEntity between = DataWires.mainframeOf(level, relay);
            if (between != null) {
                between.recordOperation(new OperationRecord(operationId, toRecord().type(), key, requested, moved,
                        (byte) status.id(), priority, List.of(), List.of(), 0L, 0L,
                        OperationFailure.of(WanTexts.RELAYED, homeName, targetName), KIND, targetName, ""));
            }
        }
    }

    /* Where a crossing has got to: on its line, or through the networks that relay it. */
    private OperationFailure crossing(final FederationPath path, final long here) {
        if (path.hops() > 1) {
            return through(path);
        }
        final String line = GameText.resolve(DataLinkNames.of(path.out().getFirst().line()));
        return OperationFailure.of(WanTexts.IN_TRANSIT, line, String.valueOf(here), String.valueOf(requested));
    }

    private OperationFailure through(final FederationPath path) {
        final List<String> names = new ArrayList<>();
        for (int hop = 0; hop < path.hops() - 1; hop++) {
            names.add(WanFederation.gatewayName(level, path.out().get(hop).farGateway()));
        }
        return OperationFailure.of(WanTexts.THROUGH, String.join(", ", names), String.valueOf(path.hops()));
    }

    /* What is on its way and not yet where it goes: what the line holds, or what was sent and not yet put away. */
    private Map<StorageKey, Long> onTheLine() {
        if (ask == WanRight.TAKE) {
            return line.held();
        }
        return ask == WanRight.SEND && farHalf == null ? Map.of(key, requested) : Map.of();
    }

    private void giveBack(final MainframeBlockEntity far, final Map<StorageKey, Long> items) {
        for (final Map.Entry<StorageKey, Long> item : items.entrySet()) {
            far.networkOperations().push(item.getKey(), item.getValue(), WanTexts.from(homeName));
        }
    }

    private void returnToSender(final long amount) {
        if (sender == null || amount <= 0L) {
            return;
        }
        long left = amount;
        while (left > 0L) {
            final int count = (int) Math.min(left, key.prototype().getMaxStackSize());
            DataHandoff.returnToPlayer(sender, key.stack(count));
            left -= count;
        }
    }

    private static long total(final Map<StorageKey, Long> items) {
        long sum = 0L;
        for (final long amount : items.values()) {
            sum += amount;
        }
        return sum;
    }
}
