/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.diagnostic;

import dev.jstech.core.energy.CoreEnergy;
import dev.jstech.core.fluid.FluidGrids;
import dev.jstech.core.grid.CoreGrids;
import dev.jstech.core.grid.GridKind;
import dev.jstech.core.log.CoreLogs;
import dev.jstech.core.persistence.NetworkRegistry;
import dev.jstech.core.protection.AuditLog;
import dev.jstech.core.protection.PlayerLimits;
import dev.jstech.core.protection.Protection;
import dev.jstech.core.protection.ProtectionBalance;
import dev.jstech.core.region.ChunkLoadingBalance;
import dev.jstech.core.region.ChunkLoaders;
import dev.jstech.core.task.TaskRunner;
import dev.jstech.core.task.Tasks;
import dev.jstech.core.team.CoreTeams;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.ApiStatus;

/**
 * The Core's own part of {@code /jstech health}: how long the server's ticks take, the sections of work and the ones
 * slowed or stalled, what is sent over the network, the work done away from the tick, each dimension's networks and
 * grids, the chunks each owner keeps loaded, the protections asked and the audit log, and the robots of the players
 * on.
 */
@TextHolder
public final class CoreHealth {

    /* The average tick, and what a tick may take, in milliseconds. */
    public static final TextKey TICKS = TextKey.of("jscore.health.ticks",
            "A tick takes %s ms on average, of the %s ms it may");
    public static final TextKey TICKS_BEHIND = TextKey.of("jscore.health.ticks_behind",
            "The server falls behind: a tick takes %s ms on average, past the %s ms it may");
    /* How many sections, the costliest, and what it takes a tick. */
    public static final TextKey SECTIONS = TextKey.of("jscore.health.sections",
            "%s sections of work are timed; the costliest, %s, takes %s ms a tick");
    /* The section, and how many ticks it spreads its work over. */
    public static final TextKey SECTION_SLOWED = TextKey.of("jscore.health.section_slowed",
            "%s is over its budget and runs one tick in %s");
    /* The section, how many times it stalled, and the longest, in milliseconds. */
    public static final TextKey SECTION_STALLED = TextKey.of("jscore.health.section_stalled",
            "%s stalled %s times, the longest for %s ms");
    /* What is sent a second, and to how many players. */
    public static final TextKey NETWORK = TextKey.of("jscore.health.network",
            "The server sends %s to the %s players counted");
    /* How many tasks are out, and how many results wait. */
    public static final TextKey TASKS = TextKey.of("jscore.health.tasks",
            "%s tasks run away from the tick, and %s results wait to come back");
    /* How many results wait. */
    public static final TextKey TASKS_BEHIND = TextKey.of("jscore.health.tasks_behind",
            "%s results wait to come back: more come in than the tick has time to take");
    /* The dimension, its data networks, its wires and pipes, and the parts of its energy and fluid grids. */
    public static final TextKey GRIDS = TextKey.of("jscore.health.grids",
            "%s: %s data networks; %s wires and pipes laid; %s parts of the energy grid, %s of the fluid grid");
    /* Who, how many chunks, and how many they may. */
    public static final TextKey CHUNKS = TextKey.of("jscore.health.chunks", "%s keeps %s chunks loaded, of %s");
    public static final TextKey CHUNKS_FULL = TextKey.of("jscore.health.chunks_full",
            "%s keeps all the %s chunks they may loaded");
    /* The protections' names. */
    public static final TextKey PROTECTIONS = TextKey.of("jscore.health.protections", "The protections asked: %s");
    /* How many entries, and how many it keeps. */
    public static final TextKey AUDIT = TextKey.of("jscore.health.audit", "The audit log holds %s entries, of %s");
    /* Who, how many robots, and how many they may have. */
    public static final TextKey ROBOTS = TextKey.of("jscore.health.robots", "%s has %s robots, of %s");

    /** Past how many waiting results the work away from the tick is said to fall behind. */
    public static final int RESULTS_BEHIND = 256;

    private static final String SEPARATOR = ", ";

    private CoreHealth() {
    }

    /** Adds the Core's parts, once, from the Core's constructor. */
    @ApiStatus.Internal
    public static void declare() {
        Health.provide(CoreLogs.DIAGNOSTICS, CoreHealth::ticks);
        Health.provide(CoreLogs.DIAGNOSTICS, CoreHealth::sections);
        Health.provide(CoreLogs.DIAGNOSTICS, CoreHealth::network);
        Health.provide(CoreLogs.TASKS, CoreHealth::tasks);
        Health.provide(CoreLogs.CABLES, CoreHealth::grids);
        Health.provide(CoreLogs.TEAMS, CoreHealth::chunks);
        Health.provide(CoreLogs.TEAMS, CoreHealth::protection);
        Health.provide(CoreLogs.ROBOTS, CoreHealth::robots);
    }

    private static void ticks(final MinecraftServer server, final Health.Report report) {
        final long average = server.getAverageTickTimeNanos();
        final long most = server.tickRateManager().nanosecondsPerTick();
        final String averageMillis = Diagnostics.millis(average);
        final String mostMillis = Diagnostics.millis(most);
        if (average > most) {
            report.warn(TICKS_BEHIND.with(averageMillis, mostMillis));
        } else {
            report.line(TICKS.with(averageMillis, mostMillis));
        }
    }

    private static void sections(final MinecraftServer server, final Health.Report report) {
        Section costliest = null;
        for (final Section section : Sections.all()) {
            if (costliest == null || section.averageNanos() > costliest.averageNanos()) {
                costliest = section;
            }
            if (section.slowed()) {
                report.warn(SECTION_SLOWED.with(section.toString(), section.stride()));
            }
            if (section.stalls() > 0) {
                report.warn(SECTION_STALLED.with(section.toString(), section.stalls(),
                        Diagnostics.millis(section.longestStallNanos())));
            }
        }
        if (costliest != null) {
            report.line(SECTIONS.with(Sections.all().size(), costliest.toString(),
                    Diagnostics.millis(costliest.averageNanos())));
        }
    }

    private static void network(final MinecraftServer server, final Health.Report report) {
        if (DiagnosticBalance.countNetwork() && Bandwidth.countedPlayers() > 0) {
            report.line(NETWORK.with(Bandwidth.rate(Bandwidth.everyone().bytesPerSecond()),
                    Bandwidth.countedPlayers()));
        }
    }

    private static void tasks(final MinecraftServer server, final Health.Report report) {
        final TaskRunner tasks = Tasks.server();
        report.line(TASKS.with(tasks.live(), tasks.waiting()));
        if (tasks.waiting() > RESULTS_BEHIND) {
            report.warn(TASKS_BEHIND.with(tasks.waiting()));
        }
    }

    private static void grids(final MinecraftServer server, final Health.Report report) {
        for (final ServerLevel level : server.getAllLevels()) {
            long laid = 0L;
            for (final GridKind kind : GridKind.all()) {
                laid += CoreGrids.of(level, kind).size();
            }
            final int networks = NetworkRegistry.of(level).size();
            if (laid > 0L || networks > 0) {
                report.line(GRIDS.with(level.dimension().location().toString(), networks, laid,
                        CoreEnergy.of(level).partCount(), FluidGrids.of(level).partCount()));
            }
        }
    }

    private static void chunks(final MinecraftServer server, final Health.Report report) {
        final int most = ChunkLoadingBalance.limit();
        for (final Map.Entry<UUID, Integer> owner : ChunkLoaders.loadedPerOwner(server).entrySet()) {
            final String name = CoreTeams.nameOf(server, owner.getKey()).orElse(owner.getKey().toString());
            if (owner.getValue() >= most) {
                report.warn(CHUNKS_FULL.with(name, most));
            } else {
                report.line(CHUNKS.with(name, owner.getValue(), most));
            }
        }
    }

    private static void protection(final MinecraftServer server, final Health.Report report) {
        report.line(PROTECTIONS.with(String.join(SEPARATOR, Protection.names())));
        if (ProtectionBalance.audit()) {
            report.line(AUDIT.with(AuditLog.of(server).size(), ProtectionBalance.auditEntries()));
        }
    }

    private static void robots(final MinecraftServer server, final Health.Report report) {
        final int most = ProtectionBalance.robotsPerPlayer();
        for (final ServerPlayer player : server.getPlayerList().getPlayers()) {
            final int count = PlayerLimits.count(server, player.getUUID(), PlayerLimits.ROBOTS);
            if (count > 0) {
                report.line(ROBOTS.with(player.getGameProfile().getName(), count, most));
            }
        }
    }
}
