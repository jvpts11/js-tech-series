/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.energy;

/**
 * A block of an energy line: limits the rate of flow between nodes of the network, and may lose some of what crosses
 * it.
 */
public interface IEnergyCable {

    /** The most energy that crosses the block in a tick; {@link Long#MAX_VALUE} for no limit. */
    long maxThroughput();

    /** The millionths of what crosses the block that it loses, from 0 to {@link EnergyLoss#WHOLE}. */
    default int loss() {
        return 0;
    }
}
