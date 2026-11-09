/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.wan;

import dev.jstech.core.jstechseries.network.DataLink;
import java.util.Objects;
import java.util.Set;
import net.minecraft.core.BlockPos;

/**
 * A long-distance line run from a port of one WAN Gateway Computer to a port of another, as seen from the first.
 *
 * @param near     the origin of the gateway it is seen from
 * @param nearPort that gateway's port, 1 to 4
 * @param far      the origin of the gateway at the other end
 * @param farPort  that gateway's port
 * @param line     the cable the run is laid with: the line and its era
 * @param plugged  the run's cable plugged into the near port, as the grid numbers it
 * @param wires    the run's cables
 */
public record WanLink(BlockPos near, int nearPort, BlockPos far, int farPort, DataLink line, long plugged,
                      Set<Long> wires) {

    public WanLink {
        Objects.requireNonNull(near, "near");
        Objects.requireNonNull(far, "far");
        Objects.requireNonNull(line, "line");
        wires = Set.copyOf(wires);
    }
}
