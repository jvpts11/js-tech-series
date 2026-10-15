/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.energy;

import dev.jstech.core.energy.internal.EnergyNetwork;

import java.util.Map;
import java.util.OptionalLong;

/**
 * Immutable snapshot of the result of one {@link EnergyNetwork#tickDistribute()} call. What the cables lost is what was
 * sent and did not arrive: {@code totalDelivered + totalLost} is all the suppliers gave.
 *
 * @param perSupplierSent what each node that gave sent
 * @param watchedFlows    for each watched cable, what crossed it toward each of its neighbours, so an instrument
 *                        clamped on it knows which way the energy runs; empty for a cable nobody watches
 */
public record EnergyDistributionResult(
        long totalSupply,
        long totalDemand,
        long totalDelivered,
        Map<Long, Long> perConsumerDelivered,
        Map<Long, Long> perCableUsage,
        long unsatisfiedDemand,
        long totalLost,
        Map<Long, Long> perSupplierSent,
        Map<Long, Map<Long, Long>> watchedFlows
) {
    public EnergyDistributionResult {
        if (totalSupply < 0 || totalDemand < 0 || totalDelivered < 0
                || unsatisfiedDemand < 0 || totalLost < 0) {
            throw new IllegalArgumentException(
                    "Aggregate values cannot be negative");
        }
        perConsumerDelivered = Map.copyOf(perConsumerDelivered);
        perCableUsage = Map.copyOf(perCableUsage);
        perSupplierSent = Map.copyOf(perSupplierSent);
        watchedFlows = Map.copyOf(watchedFlows);
    }

    public static EnergyDistributionResult empty() {
        return new EnergyDistributionResult(
                0L, 0L, 0L, Map.of(), Map.of(), 0L, 0L, Map.of(), Map.of());
    }

    /** The neighbour of {@code cable} most of what crossed it went toward, when it is watched and anything crossed. */
    public OptionalLong towardOf(final long cable) {
        final Map<Long, Long> toward = this.watchedFlows.get(cable);
        if (toward == null || toward.isEmpty()) {
            return OptionalLong.empty();
        }
        long best = 0L;
        long most = -1L;
        for (final Map.Entry<Long, Long> each : toward.entrySet()) {
            if (each.getValue() > most) {
                most = each.getValue();
                best = each.getKey();
            }
        }
        return OptionalLong.of(best);
    }
}
