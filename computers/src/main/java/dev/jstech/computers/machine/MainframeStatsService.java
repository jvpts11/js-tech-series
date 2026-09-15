/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.operation.payload.OperationRecord;
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.core.network.NetworkSystem;
import dev.jstech.core.uuid.NetworkUuid;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

/**
 * The machine that orchestrates a network, as what runs on one of its computers reads it.
 *
 * <p>What is here is what the Mainframe already keeps about the work it has been doing: how many of each kind of
 * Operation it ran in the last hour, how long they waited, how long they took, and how often they came up short.
 * Reading it costs nothing beyond the reading: the Mainframe counted it all as the work went by.
 */
public final class MainframeStatsService {

    private final IComputerTerminalHost terminal;
    private final ServerLevel level;

    public MainframeStatsService(final IComputerTerminalHost terminal, final ServerLevel level) {
        this.terminal = terminal;
        this.level = level;
    }

    /** Whether the machine is on a network at all. */
    public boolean onNetwork() {
        return this.terminal.networkUuid() != null;
    }

    /** Whether the machine's network has a Mainframe running it. */
    public boolean online() {
        return this.mainframe() != null;
    }

    /** The most Operations the network has had running at once today. */
    public int peakToday() {
        final MainframeBlockEntity mainframe = this.mainframe();
        return mainframe == null ? 0 : mainframe.statistics().peakConcurrentLastDay(this.level.getGameTime());
    }

    /**
     * What the network did with that kind of Operation in the last hour. A kind it has not run reads as zeroes rather
     * than as nothing, so whoever reads it can add up and compare without first asking whether there is anything.
     */
    public ICliComputer.OperationStat stats(final String kind) {
        for (final ICliComputer.OperationStat stat : this.work()) {
            if (stat.type().equalsIgnoreCase(kind)) {
                return stat;
            }
        }
        return new ICliComputer.OperationStat(kind, 0, 0, 0, 0, 0L);
    }

    /** What the network did with every kind of Operation it ran in the last hour. */
    public List<ICliComputer.OperationStat> work() {
        final MainframeBlockEntity mainframe = this.mainframe();
        if (mainframe == null) {
            return List.of();
        }
        final List<ICliComputer.OperationStat> rows = new ArrayList<>();
        for (final var summary : mainframe.statistics().summaries(this.level.getGameTime())) {
            rows.add(new ICliComputer.OperationStat(OperationRecord.typeName((byte) summary.type()),
                    summary.count(), summary.averageWait(), summary.averageRun(), summary.shortfallPercent(),
                    summary.moved()));
        }
        return rows;
    }

    /** The Mainframe of the machine's network, or null when it is on none, or none is running. */
    @Nullable
    private MainframeBlockEntity mainframe() {
        final NetworkUuid net = this.terminal.networkUuid();
        if (net == null) {
            return null;
        }
        return NetworkSystem.get(this.level).mainframePositionOf(net)
                .map(pos -> this.level.getBlockEntity(BlockPos.of(pos)) instanceof MainframeBlockEntity mf ? mf : null)
                .orElse(null);
    }
}
