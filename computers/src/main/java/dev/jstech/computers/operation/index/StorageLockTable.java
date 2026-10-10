/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.index;

import dev.jstech.core.uuid.NodeUuid;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks which items each Operation has reserved, per server.
 */
public final class StorageLockTable<K> {

    private final Map<UUID, Map<K, Map<NodeUuid, Long>>> byOperation = new HashMap<>();
    private final Map<K, Map<NodeUuid, Long>> aggregate = new HashMap<>();

    public void lock(final UUID operation, final K key, final Map<NodeUuid, Long> allocation) {
        for (final Map.Entry<NodeUuid, Long> entry : allocation.entrySet()) {
            final long quantity = entry.getValue();
            if (quantity <= 0L) {
                continue;
            }
            byOperation.computeIfAbsent(operation, op -> new HashMap<>())
                    .computeIfAbsent(key, k -> new HashMap<>())
                    .merge(entry.getKey(), quantity, Long::sum);
            aggregate.computeIfAbsent(key, k -> new HashMap<>())
                    .merge(entry.getKey(), quantity, Long::sum);
        }
    }

    public void unlock(final UUID operation) {
        final Map<K, Map<NodeUuid, Long>> held = byOperation.remove(operation);
        if (held == null) {
            return;
        }
        for (final Map.Entry<K, Map<NodeUuid, Long>> perItem : held.entrySet()) {
            for (final Map.Entry<NodeUuid, Long> perServer : perItem.getValue().entrySet()) {
                subtractAggregate(perItem.getKey(), perServer.getKey(), perServer.getValue());
            }
        }
    }

    public void release(final UUID operation, final K key, final NodeUuid server, final long amount) {
        if (amount <= 0L) {
            return;
        }
        final Map<K, Map<NodeUuid, Long>> held = byOperation.get(operation);
        final Map<NodeUuid, Long> perServer = held == null ? null : held.get(key);
        final Long current = perServer == null ? null : perServer.get(server);
        if (current == null) {
            return;
        }
        final long released = Math.min(amount, current);
        if (released >= current) {
            perServer.remove(server);
            if (perServer.isEmpty()) {
                held.remove(key);
            }
            if (held.isEmpty()) {
                byOperation.remove(operation);
            }
        } else {
            perServer.put(server, current - released);
        }
        subtractAggregate(key, server, released);
    }

    public long lockedOn(final K key, final NodeUuid server) {
        final Map<NodeUuid, Long> aggForItem = aggregate.get(key);
        return aggForItem == null ? 0L : aggForItem.getOrDefault(server, 0L);
    }

    public long totalLocked(final K key) {
        final Map<NodeUuid, Long> aggForItem = aggregate.get(key);
        if (aggForItem == null) {
            return 0L;
        }
        long sum = 0L;
        for (final long locked : aggForItem.values()) {
            sum += locked;
        }
        return sum;
    }

    public boolean holdsLocks(final UUID operation) {
        return byOperation.containsKey(operation);
    }

    public int lockingOperationCount() {
        return byOperation.size();
    }

    public void clear() {
        byOperation.clear();
        aggregate.clear();
    }

    /**
     * Takes a released amount off the per-server aggregate. An entry that is not there is left alone
     * instead of being created with a negative amount, which would make free stock look larger than it is.
     */
    private void subtractAggregate(final K key, final NodeUuid server, final long amount) {
        final Map<NodeUuid, Long> aggForItem = aggregate.get(key);
        final Long current = aggForItem == null ? null : aggForItem.get(server);
        if (current == null) {
            return;
        }
        if (current - amount <= 0L) {
            aggForItem.remove(server);
        } else {
            aggForItem.put(server, current - amount);
        }
        if (aggForItem.isEmpty()) {
            aggregate.remove(key);
        }
    }
}
