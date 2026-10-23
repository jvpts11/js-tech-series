/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.operation;

import dev.jstech.core.config.ConfigFile;
import dev.jstech.core.config.ConfigKey;
import dev.jstech.core.config.ConfigSide;
import dev.jstech.core.config.format.ConfigFormats;

/**
 * The Operations engine's settings, {@code jstech-operations.toml} beside the world: how long an Operation waits on a
 * locked resource or a busy machine, how fast a queued one gains priority, and how long a saved one nobody resumed is
 * kept. Each value read is passed into {@link OperationBalance}. What the machines of a mod lend the engine (a
 * disk's latency, a Subframe's share) is that mod's own setting.
 */
public final class OperationSettings {

    public static final String ENGINE = "engine";
    private static final int MAX_TIMEOUT_TICKS = 72_000;
    private static final int MAX_EXPIRY_HOURS = 24 * 365;

    /** How long an Operation waits on a locked resource or a busy machine before it gives up, in ticks. */
    public static final ConfigKey<Integer> WAITING_TIMEOUT_TICKS = ConfigKey.whole(ENGINE + ".waiting_timeout_ticks",
                    OperationBalance.DEFAULT_WAITING_TIMEOUT_TICKS).range(20, MAX_TIMEOUT_TICKS)
            .comment("How long an Operation waits on a locked resource or a busy machine before it gives up, "
                    + "in ticks.")
            .named("Operation waiting time").unit("ticks");
    /** Ticks a queued Operation waits per level of priority it gains; 0 never raises it. */
    public static final ConfigKey<Integer> PRIORITY_AGING_TICKS = ConfigKey.whole(ENGINE + ".priority_aging_ticks",
                    OperationBalance.DEFAULT_PRIORITY_AGING_TICKS).range(0, MAX_TIMEOUT_TICKS)
            .comment("How many ticks a queued Operation waits for each level of priority it gains; 0 never raises "
                    + "it.")
            .named("Priority aging").unit("ticks");
    /** Hours a saved Operation nobody resumed may sit before it is discarded; 0 keeps it for ever. */
    public static final ConfigKey<Integer> ORPHANED_EXPIRY_HOURS = ConfigKey.whole(ENGINE + ".orphaned_expiry_hours",
                    OperationBalance.DEFAULT_ORPHANED_OPERATIONS_EXPIRY_HOURS).range(0, MAX_EXPIRY_HOURS)
            .comment("How many hours a saved Operation nobody resumed waits before it is discarded; 0 keeps it for "
                    + "ever.")
            .named("Forgotten Operations last").unit("hours");

    /** The file, each setting passed into the engine's balance. */
    public static final ConfigFile FILE = ConfigFile.builder("jstech-operations", ConfigSide.SERVER,
                    ConfigFormats.TOML)
            .comment("The Operations engine of the series' data networks: how it waits and how it keeps its work.",
                    "Values outside their range are pulled to the nearer end when the file is read.")
            .sectionComment(ENGINE, "How an Operation waits, gains priority and is kept while nobody resumes it.")
            .sectionNamed(ENGINE, "The Operations engine")
            .key(WAITING_TIMEOUT_TICKS, OperationBalance::setWaitingTimeoutTicks)
            .key(PRIORITY_AGING_TICKS, OperationBalance::setPriorityAgingTicks)
            .key(ORPHANED_EXPIRY_HOURS, OperationBalance::setOrphanedOperationsExpiryHours)
            .build();

    private OperationSettings() {
    }
}
