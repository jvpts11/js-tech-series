/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.computers.program.cli.ICliNetwork;
import dev.jstech.computers.program.cli.ICliOperations;
import java.util.List;

/**
 * The machine that orchestrates a network, as what runs on one of its computers reads it.
 *
 * <p>What is here is what the Mainframe already keeps about the work it has been doing: how many of each kind of
 * Operation it ran in the last hour, how long they waited, how long they took, and how often they came up short.
 */
public final class MainframeStatsService {

    /** The network as the machine's shell reads it. */
    private final ICliNetwork network;
    /** The network's work as the machine's shell reads it. */
    private final ICliOperations operations;

    MainframeStatsService(final ICliNetwork network, final ICliOperations operations) {
        this.network = network;
        this.operations = operations;
    }

    /** Whether the machine is on a network at all. */
    public boolean onNetwork() {
        return this.network.onNetwork();
    }

    /** Whether the machine's network has a Mainframe running it. */
    public boolean online() {
        return this.network.onNetwork() && this.network.network().mainframePresent();
    }

    /** The most Operations the network has had running at once today. */
    public int peakToday() {
        return this.operations.peakOperationsToday();
    }

    /**
     * What the network did with that kind of Operation in the last hour. A kind it has not run reads as zeroes rather
     * than as nothing, so whoever reads it can add up and compare without first asking whether there is anything.
     */
    public ICliComputer.OperationStat stats(final String kind) {
        for (final ICliComputer.OperationStat stat : this.operations.operationStats()) {
            if (stat.type().equalsIgnoreCase(kind)) {
                return stat;
            }
        }
        return new ICliComputer.OperationStat(kind, 0, 0, 0, 0, 0L);
    }

    /** What the network did with every kind of Operation it ran in the last hour. */
    public List<ICliComputer.OperationStat> work() {
        return this.operations.operationStats();
    }
}
