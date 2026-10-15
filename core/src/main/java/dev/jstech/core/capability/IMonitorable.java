/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.capability;

import dev.jstech.core.network.NetworkCategory;
import dev.jstech.core.signal.Reading;
import java.util.List;

/**
 * Contract for a {@link NetworkCategory#B Category B} node that can be INSPECTED (read-only) by the network: what it
 * reports is its readings, each with its span, the same a sensor speaks in redstone, so the network reads a monitored
 * node and a sensor alike.
 */
public interface IMonitorable extends INetworkNodeCapability {

    /** What it reads now, each with its span; none for a node with nothing to report. */
    List<Reading> readings();
}
