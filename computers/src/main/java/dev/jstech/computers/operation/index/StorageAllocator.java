/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.index;

import dev.jstech.core.uuid.NodeUuid;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Decides where an item amount is served from across the network, fastest tier first; and where an amount is written,
 * the storage of the highest priority first and then the fastest.
 */
public final class StorageAllocator {

    private static final Comparator<ItemLocation> FASTEST = Comparator.comparingInt(ItemLocation::latencyTicks)
            .thenComparing(Comparator.comparingLong(ItemLocation::quantity).reversed());

    private StorageAllocator() {
    }

    /** Where {@code demand} is read from: the fastest first, and of those the fullest. */
    public static Allocation allocate(final List<ItemLocation> sources, final long demand) {
        return allocate(sources, demand, FASTEST);
    }

    /** Where {@code demand} is written: the storage the network fills first, then the fastest and the roomiest. */
    public static Allocation allocateByPriority(final List<ItemLocation> rooms, final long demand) {
        return allocate(rooms, demand, Comparator.comparingInt(ItemLocation::priority).reversed().thenComparing(
                FASTEST));
    }

    private static Allocation allocate(final List<ItemLocation> sources, final long demand,
                                       final Comparator<ItemLocation> order) {
        if (demand <= 0L || sources.isEmpty()) {
            return new Allocation(Map.of(), 0L);
        }
        final List<ItemLocation> ordered = sources.stream()
                .filter(location -> location.quantity() > 0L)
                .sorted(order)
                .toList();

        final Map<NodeUuid, Long> plan = new LinkedHashMap<>();
        long remaining = demand;
        for (final ItemLocation location : ordered) {
            if (remaining <= 0L) {
                break;
            }
            final long take = Math.min(remaining, location.quantity());
            if (take > 0L) {
                plan.merge(location.server(), take, Long::sum);
                remaining -= take;
            }
        }
        return new Allocation(plan, demand - remaining);
    }
}
