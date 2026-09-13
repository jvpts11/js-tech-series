/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.network;

import dev.jstech.core.id.IStableName;

/**
 * Canonical tiers for data network cables.
 */
public enum DataTier implements IStableName {
    T1_ETHERNET("t1_ethernet", 500L, 64, "ethernet_cable"),
    T2_HBW("t2_hbw", 5_000L, 256, "hbw_cable"),
    T3_FIBER("t3_fiber", 7_000L, 1_024, "fiber_optic_cable"),
    T4_VLDC("t4_vldc", 5_000L, 10_000, "vldc_cable"),
    T6_QUANTUM("t6_quantum", 50_000L, 64, "quantum_interconnect_cable"),
    HPC("hpc", 20_000L, 32, "hpc_cable"),
    /*
     * Dedicated short-range cable that links a Crafting Switch to its Crafting Computer. It carries machine
     * I/O coordination rather than bulk data, so its throughput is nominal; range is short (a local cluster).
     */
    CRAFTING("crafting", 1_000L, 16, "crafting_cable");

    private final String serializedName;
    private final long maxThroughput;
    private final int maxLength;
    private final String translationKey;

    DataTier(final String serializedName,
             final long maxThroughput,
             final int maxLength,
             final String translationKey) {
        this.serializedName = serializedName;
        this.maxThroughput = maxThroughput;
        this.maxLength = maxLength;
        this.translationKey = translationKey;
    }

    @Override
    public String serializedName() {
        return serializedName;
    }

    public long maxThroughput() {
        return maxThroughput;
    }

    public int maxLength() {
        return maxLength;
    }

    public String translationKey() {
        return translationKey;
    }
}
