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
import dev.jstech.core.jstechseries.network.INetworkMember;
import dev.jstech.core.jstechseries.network.NetworkMembers;
import dev.jstech.core.jstechseries.network.NetworkMembership;
import dev.jstech.core.jstechseries.network.NetworkSystem;
import dev.jstech.core.jstechseries.operation.OperationFailure;
import dev.jstech.core.jstechseries.operation.OperationPriority;
import dev.jstech.core.jstechseries.operation.OperationRequest;
import dev.jstech.core.jstechseries.operation.OperationStatus;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.uuid.NetworkUuid;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * A request of another mod's kind of Operation, which the Mainframe gives to a machine of its network and follows
 * until the machine says it is over.
 *
 * <p>It waits for a queue as any Operation does, by its priority, and on the tick a queue is its it hands the request
 * to the kind's handler and lets the queue go: the Mainframe's queues measure the Mainframe's own work, and an
 * accelerator's run of minutes is the accelerator's. From then on it stays in flight, as the machine says it is
 * going, until it is over, the machine leaves the network, or somebody calls it off, which the machine is told.
 */
@TextHolder
public final class MemberOperation implements INetworkOperation {

    private final UUID operationId = UUID.randomUUID();
    private final ServerLevel level;
    private final NetworkUuid network;
    private final OperationRequest<?> request;
    private final long machinePos;
    private final String machineName;
    private final String asked;
    private final StorageKey icon;
    private OperationPriority priority = OperationPriority.DEFAULT;
    private boolean handedOff;

    /** Why it failed when the machine went from the network, by breaking, unloading or a cut cable, before the end. */
    public static final TextKey MACHINE_GONE = TextKey.of("jsc.operation.failure.machine_gone",
            "%s left the network before it was done");

    /**
     * A request for the machine at {@code machinePos} on {@code network}.
     *
     * @param machineName the name the machine answers to on the network, for the log
     * @param asked       what was asked of it, as the network's language writes it, for the log
     */
    public MemberOperation(final ServerLevel level, final NetworkUuid network, final OperationRequest<?> request,
                           final long machinePos, final String machineName, final String asked) {
        this.level = level;
        this.network = network;
        this.request = request;
        this.machinePos = machinePos;
        this.machineName = machineName;
        this.asked = asked;
        // The machine's block is the row's icon; a block with no item of its own shows what a lost subject shows.
        final Item block = level.getBlockState(BlockPos.of(machinePos)).getBlock().asItem();
        this.icon = StorageKey.of(block == Items.AIR ? Items.BARRIER : block);
    }

    /** The request as the machine was given it, or will be. */
    public OperationRequest<?> request() {
        return this.request;
    }

    /** Where the machine it is for stands. */
    public long machinePos() {
        return this.machinePos;
    }

    /** Whether the machine was given the request, which is when it let its queue go. */
    public boolean handedOff() {
        return this.handedOff;
    }

    @Override
    public UUID operationId() {
        return this.operationId;
    }

    @Override
    public String typeId() {
        return this.request.type().id();
    }

    @Override
    public void tick(final long throughputBudget) {
        if (isDone()) {
            return;
        }
        if (!machineThere()) {
            this.request.fail(OperationFailure.of(MACHINE_GONE, this.machineName));
            return;
        }
        if (!this.handedOff) {
            this.handedOff = true;
            this.request.start();
        }
    }

    @Override
    public boolean isDone() {
        return this.request.status().isTerminal();
    }

    @Override
    public boolean isWaiting() {
        return this.request.status() == OperationStatus.WAITING;
    }

    @Override
    public void abandon() {
        this.request.cancel();
    }

    @Override
    public void cancel() {
        this.request.cancel();
    }

    @Override
    public OperationPriority priority() {
        return this.priority;
    }

    @Override
    public void setPriority(final OperationPriority priority) {
        this.priority = priority;
    }

    @Override
    public OperationRecord toRecord() {
        return new OperationRecord(this.operationId, OperationRecord.TYPE_RUN, this.icon, 0L, 0L,
                (byte) this.request.status().id(), this.priority, List.of(), List.of(), 0L, 0L,
                this.request.failure(), this.request.type().id(), this.machineName, this.asked);
    }

    @Override
    public OperationRecord liveRecord() {
        return toRecord();
    }

    /* Whether the machine it was given to is still the one standing there, on this network. */
    private boolean machineThere() {
        final boolean onNetwork = NetworkSystem.get(this.level).memberAt(this.machinePos)
                .filter(member -> member.networkUuid().equals(this.network)).isPresent();
        final INetworkMember given = this.request.machine().orElse(null);
        return onNetwork && NetworkMembers.at(this.level, BlockPos.of(this.machinePos))
                .map(NetworkMembership::member).filter(member -> member == given).isPresent();
    }
}
