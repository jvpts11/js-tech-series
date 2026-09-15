/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.program.cli.ICliComputer;
import java.util.List;

/**
 * The machine that orchestrates a network, as what runs on one of its computers reads it.
 *
 * <p>What is here is what the Mainframe already keeps about the work it has been doing: how many of each kind of
 * Operation it ran in the last hour, how long they waited, how long they took, and how often they came up short.
 */
public final class MainframeStatsService {

    private final ICliComputer shell;

    MainframeStatsService(final ICliComputer shell) {
        this.shell = shell;
    }

    /** Whether the machine is on a network at all. */
    public boolean onNetwork() {
        return this.shell.onNetwork();
    }

    /** Whether the machine's network has a Mainframe running it. */
    public boolean online() {
        return this.shell.onNetwork() && this.shell.network().mainframePresent();
    }

    /** The most Operations the network has had running at once today. */
    public int peakToday() {
        return this.shell.peakOperationsToday();
    }

    /**
     * What the network did with that kind of Operation in the last hour. A kind it has not run reads as zeroes rather
     * than as nothing, so whoever reads it can add up and compare without first asking whether there is anything.
     */
    public ICliComputer.OperationStat stats(final String kind) {
        for (final ICliComputer.OperationStat stat : this.shell.operationStats()) {
            if (stat.type().equalsIgnoreCase(kind)) {
                return stat;
            }
        }
        return new ICliComputer.OperationStat(kind, 0, 0, 0, 0, 0L);
    }

    /** What the network did with every kind of Operation it ran in the last hour. */
    public List<ICliComputer.OperationStat> work() {
        return this.shell.operationStats();
    }
}
