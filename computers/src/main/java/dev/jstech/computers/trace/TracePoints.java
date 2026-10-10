/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.trace;

import dev.jstech.computers.operation.ComputingOperations;
import dev.jstech.computers.operation.INetworkOperation;
import dev.jstech.computers.operation.OperationTypeId;
import dev.jstech.computers.operation.payload.OperationRecord;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.util.ShortId;
import dev.jstech.core.uuid.NetworkUuid;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

/**
 * The places in the network's work a trace hears from, each a line where that work already passes: a statement
 * through the network's door, an Operation taken on and settled, a lock taken, a plan chosen, a bus moving something.
 * Each costs a map read when nobody traces the network.
 */
@TextHolder
public final class TracePoints {

    /** An Operation as a trace names it: its id, its verb, how many and of what. */
    private static final TextKey OPERATION = TextKey.of("jsc.trace.operation", "#%s %s %s %s");
    /** The same once it settled, with how it went. */
    private static final TextKey SETTLED = TextKey.of("jsc.trace.settled", "#%s %s %s %s %s");
    private static final TextKey LOCK = TextKey.of("jsc.trace.lock", "#%s %s x%s on %s");
    private static final TextKey LOCK_BY_HAND = TextKey.of("jsc.trace.lock_by_hand", "%s x%s held by hand");
    private static final TextKey PLAN = TextKey.of("jsc.trace.plan", "#%s via %s");
    private static final TextKey BENCH = TextKey.of("jsc.trace.bench", "bench recipes");
    private static final TextKey MACHINE = TextKey.of("jsc.trace.machine", "a machine recipe");
    private static final TextKey STAGES = TextKey.of("jsc.trace.stages", "a recipe in stages");
    private static final TextKey BUS = TextKey.of("jsc.trace.bus", "%s \"%s\": %s x%s");
    private static final TextKey IMPORT_BUS = TextKey.of("jsc.trace.import_bus", "Import Bus");
    private static final TextKey EXPORT_BUS = TextKey.of("jsc.trace.export_bus", "Export Bus");
    private static final TextKey MOVED = TextKey.of("jsc.trace.moved", "moved: %s %s to %s");
    private static final TextKey TOOK = TextKey.of("jsc.trace.took", "took %s ticks, waited %s");
    private static final TextKey FAILED = TextKey.of("jsc.trace.failed", "why: %s");

    private TracePoints() {
    }

    /** A statement goes through the network's door. */
    public static void statementStarting(final ServerLevel level, @Nullable final NetworkUuid network,
                                         final String statement) {
        IsmsTraces.post(level, network, TraceEventClass.STATEMENT_STARTING, () -> IsmsTraces.event(level, network,
                TraceEventClass.STATEMENT_STARTING, Text.literal(statement), TraceEvent.NONE, TraceEvent.NONE,
                List.of()));
    }

    /** A statement came back, having read {@code rows} rows. */
    public static void statementCompleted(final ServerLevel level, @Nullable final NetworkUuid network,
                                          final String statement, final int rows, final Text said) {
        IsmsTraces.post(level, network, TraceEventClass.STATEMENT_COMPLETED, () -> IsmsTraces.event(level, network,
                TraceEventClass.STATEMENT_COMPLETED, Text.literal(statement), rows, 0L, List.of(said)));
    }

    /** The Mainframe took an Operation on. */
    public static void operationCreated(final ServerLevel level, @Nullable final NetworkUuid network,
                                        final INetworkOperation operation) {
        IsmsTraces.post(level, network, TraceEventClass.OPERATION_CREATED, () -> {
            final OperationRecord row = operation.liveRecord();
            return IsmsTraces.event(level, network, TraceEventClass.OPERATION_CREATED, OPERATION.with(
                    ShortId.of(operation.operationId().toString()), OperationRecord.typeName(row.type()),
                    row.requested(), GameText.of(row.name())), row.requested(), TraceEvent.NONE, List.of());
        });
    }

    /** An Operation settled, as its row in the log reads. */
    public static void operationSettled(final ServerLevel level, @Nullable final NetworkUuid network,
                                        final OperationRecord row) {
        if (!row.hasId()) {
            return;
        }
        IsmsTraces.post(level, network, TraceEventClass.OPERATION_SETTLED, () -> {
            final List<Text> detail = new ArrayList<>();
            for (final OperationRecord.MoveRow move : row.moves()) {
                detail.add(MOVED.with(move.qty(), move.from(), move.to()));
            }
            detail.add(TOOK.with(row.ranTicks(), row.waitedTicks()));
            row.failureText().ifPresent(why -> detail.add(FAILED.with(GameText.of(why))));
            return IsmsTraces.event(level, network, TraceEventClass.OPERATION_SETTLED, SETTLED.with(
                    ShortId.of(row.id().toString()), OperationRecord.typeName(row.type()), row.requested(),
                    GameText.of(row.name()), OperationRecord.statusName(row.status()).toUpperCase(Locale.ROOT)),
                    row.moved(), row.ranTicks(), detail);
        });
    }

    /** An Operation took a lock on {@code quantity} of {@code key} across {@code servers} servers. */
    public static void lockAcquired(final ServerLevel level, @Nullable final NetworkUuid network,
                                    final UUID operation, final StorageKey key, final long quantity,
                                    final int servers) {
        IsmsTraces.post(level, network, TraceEventClass.LOCK_ACQUIRED, () -> IsmsTraces.event(level, network,
                TraceEventClass.LOCK_ACQUIRED, LOCK.with(ShortId.of(operation.toString()),
                        key.registryId().getPath(), quantity, servers), quantity, TraceEvent.NONE, List.of()));
    }

    /** Somebody held {@code quantity} of {@code key} by hand. */
    public static void lockedByHand(final ServerLevel level, @Nullable final NetworkUuid network,
                                    final StorageKey key, final long quantity) {
        IsmsTraces.post(level, network, TraceEventClass.LOCK_ACQUIRED, () -> IsmsTraces.event(level, network,
                TraceEventClass.LOCK_ACQUIRED, LOCK_BY_HAND.with(key.registryId().getPath(), quantity), quantity,
                TraceEvent.NONE, List.of()));
    }

    /** The engine chose how a craft is made. */
    public static void planChosen(final ServerLevel level, @Nullable final NetworkUuid network,
                                  final INetworkOperation operation) {
        IsmsTraces.post(level, network, TraceEventClass.PLAN_CHOSEN, () -> {
            final String type = operation.typeId();
            final TextKey how = type.equals(OperationTypeId.CRAFT.registryId()) ? BENCH
                    : ComputingOperations.MULTI_STAGE.equals(type) ? STAGES : MACHINE;
            return IsmsTraces.event(level, network, TraceEventClass.PLAN_CHOSEN, PLAN.with(
                    ShortId.of(operation.operationId().toString()), how), TraceEvent.NONE, TraceEvent.NONE, List.of());
        });
    }

    /** An Import Bus set {@code quantity} of {@code key} moving into the network. */
    public static void imported(final ServerLevel level, @Nullable final NetworkUuid network, final String name,
                                final StorageKey key, final long quantity) {
        busMoved(level, network, IMPORT_BUS.text(), name, key, quantity);
    }

    /** An Export Bus set {@code quantity} of {@code key} moving out of the network. */
    public static void exported(final ServerLevel level, @Nullable final NetworkUuid network, final String name,
                                final StorageKey key, final long quantity) {
        busMoved(level, network, EXPORT_BUS.text(), name, key, quantity);
    }

    private static void busMoved(final ServerLevel level, @Nullable final NetworkUuid network, final Text bus,
                                 final String name, final StorageKey key, final long quantity) {
        IsmsTraces.post(level, network, TraceEventClass.BUS_MOVED, () -> IsmsTraces.event(level, network,
                TraceEventClass.BUS_MOVED, BUS.with(bus, name, key.registryId().getPath(), quantity), quantity,
                TraceEvent.NONE, List.of()));
    }
}
