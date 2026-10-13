/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.operation.exec;

/**
 * The timed transfer of one SubOperation: how a single source moves its share of an Operation's items over game ticks.
 */
public final class TransferState {

    private final long total;
    private int latencyRemaining;
    private long moved;

    public TransferState(final long total, final int latencyTicks) {
        if (total < 0L) {
            throw new IllegalArgumentException("total must be >= 0; got " + total);
        }
        if (latencyTicks < 0) {
            throw new IllegalArgumentException("latencyTicks must be >= 0; got " + latencyTicks);
        }
        this.total = total;
        this.latencyRemaining = latencyTicks;
    }

    /**
     * Plans one tick of the transfer. This advances the state: while read latency is pending it consumes one tick
     * of it and plans nothing, so it must be called exactly once per tick and be followed by
     * {@link #commit(long)} with what was really moved. Previewing a plan with it would eat latency.
     */
    public long planTick(final long throughput) {
        if (moved >= total) {
            return 0L;
        }
        if (latencyRemaining > 0) {
            latencyRemaining--;
            return 0L;
        }
        return Math.min(Math.max(0L, throughput), total - moved);
    }

    public void commit(final long actual) {
        moved += Math.min(Math.max(0L, actual), total - moved);
    }

    public long tick(final long throughput) {
        final long planned = planTick(throughput);
        commit(planned);
        return planned;
    }

    public boolean isComplete() {
        return moved >= total;
    }

    public boolean waitingOnLatency() {
        return latencyRemaining > 0;
    }

    public long moved() {
        return moved;
    }

    public long total() {
        return total;
    }

    public long remaining() {
        return total - moved;
    }
}
