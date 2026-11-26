/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.crafting;

import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.operation.ComputingOperations;
import dev.jstech.computers.operation.IPersistentOperation;
import dev.jstech.computers.operation.payload.OperationRecord;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.jstechseries.operation.OperationPriority;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

/**
 * A craft restored after a restart while some of its machine steps were running: they go on, and when the last of them
 * settles the craft plans the rest of the order from what the network then holds. Until then it stands in the list of
 * Operations as the order, waiting to resume, which can be paused or cancelled and is saved again as it is; it had
 * vanished from the list meanwhile, and a second save before it planned again lost it.
 */
public final class ResumingCraft implements IPersistentOperation {

    private final UUID operationId;
    private final MainframeBlockEntity mainframe;
    private final StorageKey result;
    private final long requested;
    private final long delivered;
    private final String label;
    @Nullable
    private final CraftingPattern embedded;
    private final List<NetworkProcessingOperation> steps;
    private OperationPriority priority;
    private boolean paused;
    private boolean done;
    /* Whether it became the craft that plans the rest: then the craft carries the record, and this leaves none. */
    private boolean replanned;
    private byte status = OperationRecord.STATUS_DISCARDED;

    /**
     * The order {@code operationId}, of {@code requested} of {@code result} of which {@code delivered} came back with
     * its pool as it was restored, waiting for {@code steps}, the machine steps it had running.
     */
    ResumingCraft(final UUID operationId, final MainframeBlockEntity mainframe, final StorageKey result,
                  final long requested, final long delivered, final String label,
                  @Nullable final CraftingPattern embedded, final OperationPriority priority,
                  final List<NetworkProcessingOperation> steps) {
        this.operationId = Objects.requireNonNull(operationId, "operationId");
        this.mainframe = mainframe;
        this.result = result;
        this.requested = requested;
        this.delivered = Math.max(0L, delivered);
        this.label = label;
        this.embedded = embedded;
        this.priority = Objects.requireNonNull(priority, "priority");
        this.steps = List.copyOf(steps);
    }

    @Override
    public UUID operationId() {
        return operationId;
    }

    @Override
    public String typeId() {
        return ComputingOperations.CRAFT;
    }

    /** It plans the rest once every step it waits for has settled, unless it is paused. */
    @Override
    public void tick(final long throughputBudget) {
        if (done || paused || steps.stream().anyMatch(step -> !step.isDone())) {
            return;
        }
        done = true;
        replanned = true;
        status = OperationRecord.STATUS_COMPLETED;
        // A tick later, once the storage index has seen what the steps made.
        final long remaining = requested - delivered;
        mainframe.runNextTick(() -> NetworkCraftOperation.replan(mainframe, result, remaining, label, embedded,
                priority));
    }

    @Override
    public boolean isDone() {
        return done;
    }

    /** It waits on its machine steps, which are gated by their computers, and holds no Mainframe queue. */
    @Override
    public boolean isWaiting() {
        return !done;
    }

    @Override
    public boolean silent() {
        return replanned;
    }

    @Override
    public boolean pause(final boolean pause) {
        if (done) {
            return false;
        }
        this.paused = pause;
        for (final NetworkProcessingOperation step : steps) {
            step.pause(pause);
        }
        return true;
    }

    @Override
    public boolean paused() {
        return paused && !done;
    }

    @Override
    public void abandon() {
        done = true;
    }

    /** Stops the order: the steps it waits for stop with it, and it plans nothing more. */
    @Override
    public void cancel() {
        if (done) {
            return;
        }
        for (final NetworkProcessingOperation step : steps) {
            step.cancel();
        }
        status = OperationRecord.STATUS_DISCARDED;
        done = true;
    }

    @Override
    public OperationPriority priority() {
        return priority;
    }

    @Override
    public void setPriority(final OperationPriority priority) {
        this.priority = Objects.requireNonNull(priority, "priority");
        for (final NetworkProcessingOperation step : steps) {
            step.setPriority(priority);
        }
    }

    @Override
    public String askedBy() {
        return label;
    }

    @Override
    public OperationRecord toRecord() {
        return record(status);
    }

    @Override
    public OperationRecord liveRecord() {
        return record(done ? status : OperationRecord.STATUS_WAITING).withHold(done ? OperationRecord.HOLD_NONE
                : paused ? OperationRecord.HOLD_PAUSED : OperationRecord.HOLD_RESUMING);
    }

    /** Saved as the craft it was, what is left of its order and the steps it still waits for, to resume again. */
    @Override
    public CompoundTag saveState(final HolderLookup.Provider registries) {
        final List<UUID> waitingFor = new ArrayList<>();
        for (final NetworkProcessingOperation step : steps) {
            if (!step.isDone()) {
                waitingFor.add(step.operationId());
            }
        }
        return NetworkCraftOperation.savedWaiting(operationId, result, requested - delivered, label, embedded,
                priority, paused, waitingFor, registries);
    }

    /* The order as the list shows it: what came back so far of what was asked, and each step it waits for. */
    private OperationRecord record(final byte recordStatus) {
        final List<OperationRecord.SubRow> subs = new ArrayList<>();
        for (final NetworkProcessingOperation step : steps) {
            final byte state = step.isDone() ? OperationRecord.SubRow.SUB_COMPLETED
                    : paused ? OperationRecord.SubRow.SUB_PAUSED : OperationRecord.SubRow.SUB_STREAMING;
            subs.add(new OperationRecord.SubRow(step.pattern().displayName(), step.requested(), step.produced(),
                    state));
        }
        return new OperationRecord(operationId, OperationRecord.TYPE_CRAFT, result, requested, delivered,
                recordStatus, priority, List.of(), subs);
    }
}
