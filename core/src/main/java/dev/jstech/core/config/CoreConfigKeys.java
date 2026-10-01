/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.config;

import dev.jstech.core.audio.media.MediaBalance;
import dev.jstech.core.config.format.ConfigFormats;
import dev.jstech.core.language.ExecutionBalance;
import dev.jstech.core.operation.OperationBalance;
import dev.jstech.core.time.WorldCalendar;

/**
 * The series' balance, {@code jstech-balance.toml} beside the world: how the Operations engine waits and shares its
 * work, how long the programs of the machines may run each tick, how much of the connection the recordings players
 * hear and bring may take, and how long the world's seasons last. A value outside its range is pulled to the nearer
 * end rather than refused, so a
 * slip in the file costs one setting its value instead of breaking the world; each value read is passed straight into
 * the balance it sets.
 */
public final class CoreConfigKeys {

    public static final String BALANCE = "balance";
    /** The recordings players hear and bring: how much of the connection they take, and how big one may be. */
    public static final String MEDIA = "media";
    /** The world's calendar: how long its seasons last. */
    public static final String CALENDAR = "calendar";

    private static final int MAX_LATENCY_TICKS = 200;
    private static final int MAX_TIMEOUT_TICKS = 72_000;
    private static final int MAX_EXPIRY_HOURS = 24 * 365;
    private static final int MIN_MACHINE_MICROS = 50;
    private static final int MAX_MACHINE_MICROS = 1_000_000;
    private static final int MIN_SERVER_MICROS = 100;
    private static final int MAX_SERVER_MICROS = 10_000_000;
    private static final int MIN_MEDIA_RATE = 16;
    private static final int MAX_MEDIA_RATE = 65_536;
    private static final int MAX_MEDIA_FILE_MEGABYTES = 1024;
    private static final int MAX_MEDIA_QUOTA_MEGABYTES = 1_048_576;

    /** The seek latency of a hard disk drive, in ticks. */
    public static final ConfigKey<Integer> HDD_LATENCY_TICKS = ConfigKey.whole(
            BALANCE + ".hdd_latency_ticks", OperationBalance.DEFAULT_HDD_LATENCY_TICKS).range(0, MAX_LATENCY_TICKS)
            .comment("How long a hard disk drive takes to find what it is asked for, in ticks.")
            .named("Hard disk latency");

    /** The seek latency of a solid-state drive, in ticks. */
    public static final ConfigKey<Integer> SSD_LATENCY_TICKS = ConfigKey.whole(
            BALANCE + ".ssd_latency_ticks", OperationBalance.DEFAULT_SSD_LATENCY_TICKS).range(0, MAX_LATENCY_TICKS)
            .comment("How long a solid-state drive takes to find what it is asked for, in ticks.")
            .named("SSD latency");

    /** The seek latency of an NVMe drive, in ticks. */
    public static final ConfigKey<Integer> NVME_LATENCY_TICKS = ConfigKey.whole(
            BALANCE + ".nvme_latency_ticks", OperationBalance.DEFAULT_NVME_LATENCY_TICKS)
            .range(0, MAX_LATENCY_TICKS)
            .comment("How long an NVMe drive takes to find what it is asked for, in ticks.")
            .named("NVMe latency");

    /** How long an Operation waits on a LOCKed resource or a busy executor before it gives up, in ticks. */
    public static final ConfigKey<Integer> OPERATION_WAITING_TIMEOUT_TICKS = ConfigKey.whole(
            BALANCE + ".operation_waiting_timeout_ticks", OperationBalance.DEFAULT_WAITING_TIMEOUT_TICKS)
            .range(20, MAX_TIMEOUT_TICKS)
            .comment("How long an Operation waits on a locked resource or a busy machine before it gives up, "
                    + "in ticks.")
            .named("Operation waiting time");

    /** Ticks a queued Operation waits per level of priority it gains; 0 disables aging. */
    public static final ConfigKey<Integer> OPERATION_PRIORITY_AGING_TICKS = ConfigKey.whole(
            BALANCE + ".operation_priority_aging_ticks", OperationBalance.DEFAULT_PRIORITY_AGING_TICKS)
            .range(0, MAX_TIMEOUT_TICKS)
            .comment("How many ticks a queued Operation waits for each level of priority it gains; 0 never raises "
                    + "it.")
            .named("Priority aging");

    /** The share of a Subframe's own capacity it lends to the Mainframe orchestrating it. */
    public static final ConfigKey<Double> SUBFRAME_EFFICIENCY_FACTOR = ConfigKey.number(
            BALANCE + ".subframe_efficiency_factor", OperationBalance.DEFAULT_SUBFRAME_EFFICIENCY_FACTOR)
            .range(0.0, 1.0)
            .comment("The share of its own capacity a Subframe lends the Mainframe it works for.")
            .named("Subframe share");

    /** Hours a persisted, never-resumed Operation may sit before it is discarded; 0 never expires. */
    public static final ConfigKey<Integer> ORPHANED_OPERATIONS_EXPIRY_HOURS = ConfigKey.whole(
            BALANCE + ".orphaned_operations_expiry_hours", OperationBalance.DEFAULT_ORPHANED_OPERATIONS_EXPIRY_HOURS)
            .range(0, MAX_EXPIRY_HOURS)
            .comment("How many hours a saved Operation nobody resumed waits before it is discarded; 0 keeps it for "
                    + "ever.")
            .named("Forgotten Operations last");

    /** Real time one machine may spend running its programs in a tick, in microseconds. */
    public static final ConfigKey<Integer> PROGRAM_MACHINE_MICROS = ConfigKey.whole(
            BALANCE + ".program_machine_micros", ExecutionBalance.DEFAULT_MACHINE_MICROS)
            .range(MIN_MACHINE_MICROS, MAX_MACHINE_MICROS)
            .comment("The real time one machine may spend running its programs in a tick, in microseconds.")
            .named("Program time per machine");

    /** Real time every machine together may spend running programs in a tick, in microseconds. */
    public static final ConfigKey<Integer> PROGRAM_SERVER_MICROS = ConfigKey.whole(
            BALANCE + ".program_server_micros", ExecutionBalance.DEFAULT_SERVER_MICROS)
            .range(MIN_SERVER_MICROS, MAX_SERVER_MICROS)
            .comment("The real time every machine together may spend running programs in a tick, in microseconds.")
            .named("Program time per server");

    /** Kilobytes a second the server sends each player of the recordings they are about to hear. */
    public static final ConfigKey<Integer> MEDIA_DOWNLOAD_KILOBYTES_PER_SECOND = ConfigKey.whole(
            MEDIA + ".download_kilobytes_per_second", MediaBalance.DEFAULT_DOWNLOAD_KILOBYTES_PER_SECOND)
            .range(MIN_MEDIA_RATE, MAX_MEDIA_RATE)
            .comment("How many kilobytes a second the server sends each player of the recordings they are about "
                    + "to hear.")
            .named("Download speed");

    /** Kilobytes a second a player sends the server of a recording they bring. */
    public static final ConfigKey<Integer> MEDIA_UPLOAD_KILOBYTES_PER_SECOND = ConfigKey.whole(
            MEDIA + ".upload_kilobytes_per_second", MediaBalance.DEFAULT_UPLOAD_KILOBYTES_PER_SECOND)
            .range(MIN_MEDIA_RATE, MAX_MEDIA_RATE)
            .comment("How many kilobytes a second a player sends the server of a recording they bring.")
            .named("Upload speed");

    /** The biggest recording a player may bring, in megabytes; 0 takes none. */
    public static final ConfigKey<Integer> MEDIA_MAX_FILE_MEGABYTES = ConfigKey.whole(
            MEDIA + ".max_file_megabytes", MediaBalance.DEFAULT_MAX_FILE_MEGABYTES)
            .range(0, MAX_MEDIA_FILE_MEGABYTES)
            .comment("The biggest recording a player may bring, in megabytes; 0 takes none.")
            .named("Largest recording");

    /** How much of the store the recordings one player brought may take, in megabytes; 0 sets no limit. */
    public static final ConfigKey<Integer> MEDIA_PLAYER_QUOTA_MEGABYTES = ConfigKey.whole(
            MEDIA + ".player_quota_megabytes", MediaBalance.DEFAULT_PLAYER_QUOTA_MEGABYTES)
            .range(0, MAX_MEDIA_QUOTA_MEGABYTES)
            .comment("How much of the server's store the recordings one player brought may take, in megabytes; "
                    + "0 sets no limit.")
            .named("Recordings per player");

    /** How many days a season of the world's year lasts. */
    public static final ConfigKey<Integer> CALENDAR_DAYS_PER_SEASON = ConfigKey.whole(
            CALENDAR + ".days_per_season", WorldCalendar.DEFAULT_DAYS_PER_SEASON)
            .range(1, WorldCalendar.MOST_DAYS_PER_SEASON)
            .comment("How many days a season of the world's year lasts; a year is four seasons.")
            .named("Days per season");

    /** The file, each setting passed into the balance it sets. */
    public static final ConfigFile FILE = ConfigFile.builder("jstech-balance", ConfigSide.SERVER, ConfigFormats.TOML)
            .comment("Balance of the Operations engine and of the programs machines run.",
                    "Values outside their range are pulled to the nearer end when the file is read.")
            .sectionComment(BALANCE,
                    "How the Operations engine waits and shares its work, and how long programs run.")
            .sectionComment(MEDIA, "The recordings players hear and bring: how much of the connection they take, "
                    + "and how big one may be.")
            .sectionComment(CALENDAR, "The world's calendar: how long its seasons last.")
            .sectionNamed(BALANCE, "Operations and programs")
            .sectionNamed(MEDIA, "Recordings")
            .sectionNamed(CALENDAR, "Calendar")
            .key(HDD_LATENCY_TICKS, OperationBalance::setHddLatencyTicks)
            .key(SSD_LATENCY_TICKS, OperationBalance::setSsdLatencyTicks)
            .key(NVME_LATENCY_TICKS, OperationBalance::setNvmeLatencyTicks)
            .key(OPERATION_WAITING_TIMEOUT_TICKS, OperationBalance::setWaitingTimeoutTicks)
            .key(OPERATION_PRIORITY_AGING_TICKS, OperationBalance::setPriorityAgingTicks)
            .key(SUBFRAME_EFFICIENCY_FACTOR, OperationBalance::setSubframeEfficiencyFactor)
            .key(ORPHANED_OPERATIONS_EXPIRY_HOURS, OperationBalance::setOrphanedOperationsExpiryHours)
            .key(PROGRAM_MACHINE_MICROS, ExecutionBalance::setMachineMicros)
            .key(PROGRAM_SERVER_MICROS, ExecutionBalance::setServerMicros)
            .key(MEDIA_DOWNLOAD_KILOBYTES_PER_SECOND, MediaBalance::setDownloadKilobytesPerSecond)
            .key(MEDIA_UPLOAD_KILOBYTES_PER_SECOND, MediaBalance::setUploadKilobytesPerSecond)
            .key(MEDIA_MAX_FILE_MEGABYTES, MediaBalance::setMaxFileMegabytes)
            .key(MEDIA_PLAYER_QUOTA_MEGABYTES, MediaBalance::setPlayerQuotaMegabytes)
            .key(CALENDAR_DAYS_PER_SEASON, WorldCalendar::setDaysPerSeason)
            .build();

    private CoreConfigKeys() {
    }
}
