/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import java.util.List;

/**
 * How fast each of a Mainframe's queues runs, in items per tick.
 *
 * <p>The processor's own queue runs at the processor's capacity. Every graphics card adds a queue of its own, which
 * runs at most as fast as the processor and no faster than the card can: its cores, times their clock, times its
 * design's efficiency, cut further when the card sits in a slot older than itself. A strong card's queue runs at the
 * processor's speed; a weak one's runs slower. The queues do not share the capacity among themselves.
 */
public final class QueueSpeeds {

    private QueueSpeeds() {
    }

    /** The queues of a machine built as {@code build}: the processor's first, then one for each card in turn. */
    public static long[] of(final ComputerBuild build) {
        final long cpu = build.totalCapacity();
        final List<GpuSpec> gpus = build.gpus();
        final long[] speeds = new long[1 + gpus.size()];
        speeds[0] = cpu;
        for (int i = 0; i < gpus.size(); i++) {
            speeds[i + 1] = cardQueue(cpu, gpus.get(i), build.motherboard().pcieGeneration());
        }
        return speeds;
    }

    /** A card's queue: no faster than the processor, and no faster than the card in that slot can run it. */
    public static long cardQueue(final long cpu, final GpuSpec gpu, final PcieGeneration slot) {
        final long power = Math.round(gpu.power() * gpu.bus().bandwidthFactorIn(slot));
        return Math.max(1L, Math.min(cpu, power));
    }

    /** Whether the card holds its queue below the processor's speed, which is what the Mainframe shows in amber. */
    public static boolean heldByCard(final long[] speeds, final int queue) {
        return queue > 0 && queue < speeds.length && speeds[queue] < speeds[0];
    }
}
