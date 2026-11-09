/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.datacenter;

import java.util.List;

/**
 * A datacenter section's last reading by its router, beside what its {@link SectionDiagnostics} keep.
 *
 * @param baseTenths    its base latency, what its servers' drives take to answer with nothing in the way, in tenths
 *                      of a tick; 0 with no servers to tell
 * @param maxThroughput the most items a second it can move
 * @param alerts        its alerts standing now
 */
public record SectionReading(int baseTenths, int maxThroughput, List<SectionAlert> alerts) {

    public SectionReading {
        alerts = List.copyOf(alerts);
    }
}
