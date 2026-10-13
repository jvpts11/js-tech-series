/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.network;

import dev.jstech.core.capability.IMonitorable;
import dev.jstech.core.operation.OperationType;

/**
 * How a block takes part in a data network, in one of three ways.
 *
 * <p>A data network is every block joined by data cable to one Mainframe, the computer that runs the network. The
 * category says what the network may do with each of those blocks:
 *
 * <ul>
 *   <li>{@link #A}: carries the network but is not a member of it, the way a cable is. It has no network identity
 *       (a {@code NetworkUuid}), so the network cannot address it.</li>
 *   <li>{@link #B}: a member the network can read but never give work to, such as a block it inspects for its
 *       state ({@link IMonitorable}).</li>
 *   <li>{@link #C}: a member that carries out Operations, the requests the network runs: the computers. Only these
 *       receive work from the Mainframe.</li>
 * </ul>
 *
 * <p>An {@link OperationType} names the categories that may run it. Every kind of Operation the series declares
 * today asks for {@link #C}.
 */
public enum NetworkCategory {
    A,
    B,
    C;

    /** Whether a block of this category has a network identity the network can address: B and C do, A does not. */
    public boolean hasUuid() {
        return this != A;
    }

    /** Whether a block of this category can be given an Operation to carry out: only C. */
    public boolean canReceiveOperations() {
        return this == C;
    }
}
