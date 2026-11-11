/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.backup;

import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.program.IqlJobAgent;
import dev.jstech.computers.program.iql.IqlDefinition;
import dev.jstech.computers.program.iql.IqlSavedObject;
import dev.jstech.core.jstechseries.network.NetworkSystem;
import dev.jstech.core.util.Loaded;
import dev.jstech.core.uuid.NetworkUuid;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

/**
 * A timed backup is an Automation Engine job: the service keeps the job and the Engine keeps the time, firing
 * {@code BACKUP <job>} at its hour, so the network has one place that runs things on time and the player sees the
 * backup among its other jobs (computers/docs/design/programs.md).
 */
public final class BackupSchedule {

    private BackupSchedule() {
    }

    /** The name of the Automation Engine job that runs {@code job} on time. */
    public static String engineJob(final String job) {
        return "backup_" + job;
    }

    /** The WHEN of that Engine job: the hour of the world's clock, and the day of the week for a weekly one. */
    public static String trigger(final BackupJob.Schedule when) {
        final String clock = String.format(Locale.ROOT, "%s = '%02d:00'", IqlJobAgent.CLOCK, when.hour());
        return when.kind() == BackupJob.Schedule.Kind.WEEKLY
                ? clock + " AND " + IqlJobAgent.WEEKDAY + " = '" + IqlJobAgent.WEEKDAYS[when.weekday()] + "'"
                : clock;
    }

    /**
     * Brings the network's Automation Engine jobs in line with a backup job that changed from {@code before} to
     * {@code after}, either null for one made or deleted: the old one's Engine job goes, and a timed new one gets its
     * own. Answers whether an Automation Engine runs to fire it, so the console can say when none does.
     */
    public static boolean sync(final ServerLevel level, @Nullable final NetworkUuid network,
                               @Nullable final BackupJob before, @Nullable final BackupJob after) {
        final MainframeBlockEntity mainframe = mainframeOf(level, network);
        if (mainframe == null) {
            return false;
        }
        if (before != null && before.when().timed()) {
            mainframe.deleteJob(engineJob(before.name()));
        }
        if (after != null && after.when().timed()) {
            mainframe.iqlCatalog().put(new IqlSavedObject(IqlDefinition.ObjectType.JOB, engineJob(after.name()),
                    "BACKUP " + after.name(), IqlDefinition.TriggerKind.WHEN, trigger(after.when())));
            mainframe.restartJob(engineJob(after.name()));
        }
        mainframe.markIqlCatalogChanged();
        return mainframe.isAutomationEngineActive();
    }

    /** Whether an Automation Engine runs on {@code network} to fire its timed backups. */
    public static boolean engineRuns(final ServerLevel level, @Nullable final NetworkUuid network) {
        final MainframeBlockEntity mainframe = mainframeOf(level, network);
        return mainframe != null && mainframe.isAutomationEngineActive();
    }

    @Nullable
    private static MainframeBlockEntity mainframeOf(final ServerLevel level, @Nullable final NetworkUuid network) {
        return network == null ? null : NetworkSystem.get(level).mainframePositionOf(network)
                .map(pos -> Loaded.blockEntity(level, BlockPos.of(pos)) instanceof MainframeBlockEntity found
                        ? found : null)
                .orElse(null);
    }
}
