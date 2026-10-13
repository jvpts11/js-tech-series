/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.network;

import dev.jstech.core.operation.OperationBalance;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.core.uuid.NodeUuid;

import java.util.Objects;
import java.util.Optional;

/**
 * Snapshot of a Subframe BlockEntity, as used by the {@link NetworkSystem}.
 *
 * <p>{@code software} names the orchestration software the Subframe runs, by the id of its package. A Subframe
 * works for a Mainframe that runs the same software, and a mod decides what happens to one that runs other
 * software; one that names none ({@link #ANY_SOFTWARE}) works with whatever its Mainframe runs.
 */
public record SubframeNode(
        NodeUuid nodeUuid,
        NetworkUuid networkUuid,
        long ownCapacity,
        Optional<NodeUuid> orchestratingMainframeUuid,
        int parallelQueues,
        String software
) implements IComputerNode {

    /** The canonical share a Subframe lends; the balance config starts from it. */
    public static final double CONTRIBUTION_FACTOR =
            OperationBalance.DEFAULT_SUBFRAME_EFFICIENCY_FACTOR;
    /** What a Subframe that names no software of its own runs: whatever its Mainframe runs. */
    public static final String ANY_SOFTWARE = "";

    /** A Subframe that names no software of its own. */
    public SubframeNode(final NodeUuid nodeUuid, final NetworkUuid networkUuid, final long ownCapacity,
                        final Optional<NodeUuid> orchestratingMainframeUuid, final int parallelQueues) {
        this(nodeUuid, networkUuid, ownCapacity, orchestratingMainframeUuid, parallelQueues, ANY_SOFTWARE);
    }

    /** A Subframe with no GPUs of its own: it lends capacity, not queues. */
    public SubframeNode(final NodeUuid nodeUuid, final NetworkUuid networkUuid, final long ownCapacity,
                        final Optional<NodeUuid> orchestratingMainframeUuid) {
        this(nodeUuid, networkUuid, ownCapacity, orchestratingMainframeUuid, 0);
    }

    public SubframeNode {
        Objects.requireNonNull(nodeUuid, "nodeUuid must not be null");
        Objects.requireNonNull(networkUuid, "networkUuid must not be null");
        Objects.requireNonNull(orchestratingMainframeUuid,
                "orchestratingMainframeUuid must not be null (use Optional.empty for idle)");
        Objects.requireNonNull(software, "software must not be null (use ANY_SOFTWARE for none)");

        if (ownCapacity < 0) {
            throw new IllegalArgumentException(
                    "ownCapacity must be >= 0; got " + ownCapacity);
        }
        if (parallelQueues < 0) {
            throw new IllegalArgumentException(
                    "parallelQueues must be >= 0; got " + parallelQueues);
        }
    }

    /**
     * The dispatch queues this Subframe adds to its orchestrating Mainframe: one per GPU it carries, and
     * none while it is idle. A Subframe brings no base queue of its own (the Mainframe's CPU is the one
     * orchestrating), so a GPU-less Subframe only lends capacity.
     */
    public int contributedQueues() {
        return orchestratingMainframeUuid.isEmpty() ? 0 : parallelQueues;
    }

    @Override
    public long contributedCapacity() {
        if (orchestratingMainframeUuid.isEmpty()) {
            return 0; // Idle subframe contributes nothing.
        }
        // The share is a balance value: the server config may tune it away from the canonical default.
        return Math.round(ownCapacity * OperationBalance.subframeEfficiencyFactor());
    }

    @Override
    public NetworkCategory category() {
        return NetworkCategory.C;
    }

    /** Whether the Subframe works with a Mainframe that runs {@code mainframeSoftware}. */
    public boolean runsWith(final String mainframeSoftware) {
        return software.equals(ANY_SOFTWARE) || software.equals(mainframeSoftware);
    }
}
