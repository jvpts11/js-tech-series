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
import dev.jstech.core.diagnostic.DiagnosticBalance;
import dev.jstech.core.hazard.HazardBalance;
import dev.jstech.core.heat.HeatBalance;
import dev.jstech.core.mechanical.MechanicalBalance;
import dev.jstech.core.language.ExecutionBalance;
import dev.jstech.core.material.NamingBalance;
import dev.jstech.core.operation.OperationBalance;
import dev.jstech.core.protection.ProtectionBalance;
import dev.jstech.core.region.ChunkLoadingBalance;
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
    /** What the machines of the series may do to the world: how many chunks an owner keeps loaded. */
    public static final String WORLD = "world";
    /** Who may hurt whom, the audit log, and how many of each thing one player may have. */
    public static final String PROTECTION = "protection";
    /** The watchdog, and what the server counts of what it sends. */
    public static final String DIAGNOSTICS = "diagnostics";
    /** Naming the elements discovered beyond the known table. */
    public static final String ELEMENTS = "elements";
    /** The world's hazards: radiation, pollution and the rest. */
    public static final String HAZARDS = "hazards";
    /** What heat does to the world: pipes that freeze, cold liquids that boil off, overheating, fires and burns. */
    public static final String HEAT = "heat";
    /** What mechanical power does to the world: parts that break under overload. */
    public static final String MECHANICAL = "mechanical";

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
    private static final int MAX_CHUNKS_PER_OWNER = 4096;
    private static final int MIN_AUDIT_ENTRIES = 100;
    private static final int MAX_AUDIT_ENTRIES = 1_000_000;
    private static final int MAX_ROBOTS_PER_PLAYER = 1024;
    private static final int MIN_STALL_MILLIS = 50;
    private static final int MAX_STALL_MILLIS = 60_000;
    private static final int SHORTEST_LONGEST_NAME = 4;
    private static final int LONGEST_LONGEST_NAME = 40;

    /** The seek latency of a hard disk drive, in ticks. */
    public static final ConfigKey<Integer> HDD_LATENCY_TICKS = ConfigKey.whole(
            BALANCE + ".hdd_latency_ticks", OperationBalance.DEFAULT_HDD_LATENCY_TICKS).range(0, MAX_LATENCY_TICKS)
            .comment("How long a hard disk drive takes to find what it is asked for, in ticks.")
            .named("Hard disk latency").unit("ticks");

    /** The seek latency of a solid-state drive, in ticks. */
    public static final ConfigKey<Integer> SSD_LATENCY_TICKS = ConfigKey.whole(
            BALANCE + ".ssd_latency_ticks", OperationBalance.DEFAULT_SSD_LATENCY_TICKS).range(0, MAX_LATENCY_TICKS)
            .comment("How long a solid-state drive takes to find what it is asked for, in ticks.")
            .named("SSD latency").unit("ticks");

    /** The seek latency of an NVMe drive, in ticks. */
    public static final ConfigKey<Integer> NVME_LATENCY_TICKS = ConfigKey.whole(
            BALANCE + ".nvme_latency_ticks", OperationBalance.DEFAULT_NVME_LATENCY_TICKS)
            .range(0, MAX_LATENCY_TICKS)
            .comment("How long an NVMe drive takes to find what it is asked for, in ticks.")
            .named("NVMe latency").unit("ticks");

    /** How long an Operation waits on a LOCKed resource or a busy executor before it gives up, in ticks. */
    public static final ConfigKey<Integer> OPERATION_WAITING_TIMEOUT_TICKS = ConfigKey.whole(
            BALANCE + ".operation_waiting_timeout_ticks", OperationBalance.DEFAULT_WAITING_TIMEOUT_TICKS)
            .range(20, MAX_TIMEOUT_TICKS)
            .comment("How long an Operation waits on a locked resource or a busy machine before it gives up, "
                    + "in ticks.")
            .named("Operation waiting time").unit("ticks");

    /** Ticks a queued Operation waits per level of priority it gains; 0 disables aging. */
    public static final ConfigKey<Integer> OPERATION_PRIORITY_AGING_TICKS = ConfigKey.whole(
            BALANCE + ".operation_priority_aging_ticks", OperationBalance.DEFAULT_PRIORITY_AGING_TICKS)
            .range(0, MAX_TIMEOUT_TICKS)
            .comment("How many ticks a queued Operation waits for each level of priority it gains; 0 never raises "
                    + "it.")
            .named("Priority aging").unit("ticks");

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
            .named("Forgotten Operations last").unit("hours");

    /** Real time one machine may spend running its programs in a tick, in microseconds. */
    public static final ConfigKey<Integer> PROGRAM_MACHINE_MICROS = ConfigKey.whole(
            BALANCE + ".program_machine_micros", ExecutionBalance.DEFAULT_MACHINE_MICROS)
            .range(MIN_MACHINE_MICROS, MAX_MACHINE_MICROS)
            .comment("The real time one machine may spend running its programs in a tick, in microseconds.")
            .named("Program time per machine").unit("µs");

    /** Real time every machine together may spend running programs in a tick, in microseconds. */
    public static final ConfigKey<Integer> PROGRAM_SERVER_MICROS = ConfigKey.whole(
            BALANCE + ".program_server_micros", ExecutionBalance.DEFAULT_SERVER_MICROS)
            .range(MIN_SERVER_MICROS, MAX_SERVER_MICROS)
            .comment("The real time every machine together may spend running programs in a tick, in microseconds.")
            .named("Program time per server").unit("µs");

    /** Kilobytes a second the server sends each player of the recordings they are about to hear. */
    public static final ConfigKey<Integer> MEDIA_DOWNLOAD_KILOBYTES_PER_SECOND = ConfigKey.whole(
            MEDIA + ".download_kilobytes_per_second", MediaBalance.DEFAULT_DOWNLOAD_KILOBYTES_PER_SECOND)
            .range(MIN_MEDIA_RATE, MAX_MEDIA_RATE)
            .comment("How many kilobytes a second the server sends each player of the recordings they are about "
                    + "to hear.")
            .named("Download speed").unit("KB/s");

    /** Kilobytes a second a player sends the server of a recording they bring. */
    public static final ConfigKey<Integer> MEDIA_UPLOAD_KILOBYTES_PER_SECOND = ConfigKey.whole(
            MEDIA + ".upload_kilobytes_per_second", MediaBalance.DEFAULT_UPLOAD_KILOBYTES_PER_SECOND)
            .range(MIN_MEDIA_RATE, MAX_MEDIA_RATE)
            .comment("How many kilobytes a second a player sends the server of a recording they bring.")
            .named("Upload speed").unit("KB/s");

    /** The biggest recording a player may bring, in megabytes; 0 takes none. */
    public static final ConfigKey<Integer> MEDIA_MAX_FILE_MEGABYTES = ConfigKey.whole(
            MEDIA + ".max_file_megabytes", MediaBalance.DEFAULT_MAX_FILE_MEGABYTES)
            .range(0, MAX_MEDIA_FILE_MEGABYTES)
            .comment("The biggest recording a player may bring, in megabytes; 0 takes none.")
            .named("Largest recording").unit("MB");

    /** How much of the store the recordings one player brought may take, in megabytes; 0 sets no limit. */
    public static final ConfigKey<Integer> MEDIA_PLAYER_QUOTA_MEGABYTES = ConfigKey.whole(
            MEDIA + ".player_quota_megabytes", MediaBalance.DEFAULT_PLAYER_QUOTA_MEGABYTES)
            .range(0, MAX_MEDIA_QUOTA_MEGABYTES)
            .comment("How much of the server's store the recordings one player brought may take, in megabytes; "
                    + "0 sets no limit.")
            .named("Recordings per player").unit("MB");

    /** How many days a season of the world's year lasts. */
    public static final ConfigKey<Integer> CALENDAR_DAYS_PER_SEASON = ConfigKey.whole(
            CALENDAR + ".days_per_season", WorldCalendar.DEFAULT_DAYS_PER_SEASON)
            .range(1, WorldCalendar.MOST_DAYS_PER_SEASON)
            .comment("How many days a season of the world's year lasts; a year is four seasons.")
            .named("Days per season").unit("days");

    /** How many chunks one player or team may keep loaded through the series' machines, across the world. */
    public static final ConfigKey<Integer> WORLD_CHUNKS_PER_OWNER = ConfigKey.whole(
            WORLD + ".chunks_per_owner", ChunkLoadingBalance.DEFAULT_LIMIT)
            .range(0, MAX_CHUNKS_PER_OWNER)
            .comment("How many chunks one player or team may keep loaded through machines, across every dimension; "
                    + "0 lets nobody keep any.")
            .named("Loaded chunks per owner").unit("chunks");

    /** Whether players on one team may hurt each other. */
    public static final ConfigKey<Boolean> PROTECTION_TEAM_DAMAGE = ConfigKey.flag(PROTECTION + ".team_damage", false)
            .comment("Whether players on one team can hurt each other. While the teams are the game's scoreboard "
                    + "teams, a team's own friendly fire decides when this is off.")
            .named("Teammates can hurt each other");

    /** Whether who used, changed or broke what others own is written down. */
    public static final ConfigKey<Boolean> PROTECTION_AUDIT = ConfigKey.flag(PROTECTION + ".audit", false)
            .comment("Whether who used, changed or broke what another player owns is written down, for "
                    + "/jstech audit to read.")
            .named("Keep the audit log");

    /** How many entries the audit log keeps. */
    public static final ConfigKey<Integer> PROTECTION_AUDIT_ENTRIES = ConfigKey.whole(PROTECTION + ".audit_entries",
            ProtectionBalance.DEFAULT_AUDIT_ENTRIES).range(MIN_AUDIT_ENTRIES, MAX_AUDIT_ENTRIES)
            .comment("How many entries the audit log keeps; the oldest make way for new ones.")
            .named("Audit log entries").unit("entries").activeWhen(PROTECTION_AUDIT);

    /** How many robots one player may have at once. */
    public static final ConfigKey<Integer> PROTECTION_ROBOTS_PER_PLAYER = ConfigKey.whole(
            PROTECTION + ".robots_per_player", ProtectionBalance.DEFAULT_ROBOTS_PER_PLAYER)
            .range(0, MAX_ROBOTS_PER_PLAYER)
            .comment("How many robots one player may have at once; 0 lets nobody have any.")
            .named("Robots per player").unit("robots");

    /** How long a section of the series' work may run before the watchdog writes it down, in milliseconds. */
    public static final ConfigKey<Integer> DIAGNOSTICS_STALL_MILLIS = ConfigKey.whole(
            DIAGNOSTICS + ".stall_millis", DiagnosticBalance.DEFAULT_STALL_MILLIS)
            .range(MIN_STALL_MILLIS, MAX_STALL_MILLIS)
            .comment("How long one piece of a mod's work may run before the watchdog writes down what it was doing, "
                    + "in milliseconds.")
            .named("Stall after").unit("ms");

    /** Whether what the server sends each player is counted. */
    public static final ConfigKey<Boolean> DIAGNOSTICS_COUNT_NETWORK = ConfigKey.flag(
            DIAGNOSTICS + ".count_network", true)
            .comment("Whether what the server sends each player is counted, for /jstech bandwidth.")
            .named("Count network use");

    /** Whether operators are told in chat when a part of the series stalls or slows itself. */
    public static final ConfigKey<Boolean> DIAGNOSTICS_WARN_OPERATORS = ConfigKey.flag(
            DIAGNOSTICS + ".warn_operators", true)
            .comment("Whether operators are told in chat when a piece of a mod's work stalls or slows itself.")
            .named("Tell operators of stalls");

    /** Whether the discoverer of an element beyond the known table gives it its name and its symbol. */
    public static final ConfigKey<Boolean> ELEMENTS_DISCOVERERS_NAME = ConfigKey.flag(
            ELEMENTS + ".discoverers_name", true)
            .comment("Whether the player who first discovers an element beyond the 118 known gives it its name and "
                    + "its symbol, for everyone; when off, the series' own names are used.")
            .named("Discoverers name elements");

    /** Whether an element's name must end in "-ium". */
    public static final ConfigKey<Boolean> ELEMENTS_REQUIRE_IUM = ConfigKey.flag(ELEMENTS + ".require_ium", false)
            .comment("Whether a name given to an element must end in -ium, as the elements' names do.")
            .named("Names end in -ium").activeWhen(ELEMENTS_DISCOVERERS_NAME);

    /** How many letters a name given to an element may have. */
    public static final ConfigKey<Integer> ELEMENTS_LONGEST_NAME = ConfigKey.whole(
            ELEMENTS + ".longest_name", NamingBalance.DEFAULT_LONGEST)
            .range(SHORTEST_LONGEST_NAME, LONGEST_LONGEST_NAME)
            .comment("How many letters a name given to an element may have.")
            .named("Longest name").unit("letters").activeWhen(ELEMENTS_DISCOVERERS_NAME);

    /** Whether minerals with uranium or thorium in them are naturally radioactive. */
    public static final ConfigKey<Boolean> HAZARDS_NATURAL_RADIOACTIVITY = ConfigKey.flag(
            HAZARDS + ".natural_radioactivity", true)
            .comment("Whether every mineral with uranium or thorium in it is naturally radioactive, at a low rate.")
            .named("Radioactive minerals");

    /** Whether a pipe of a liquid freezes where the place is colder than the liquid's melting point. */
    public static final ConfigKey<Boolean> HEAT_PIPES_FREEZE = ConfigKey.flag(HEAT + ".pipes_freeze", true)
            .comment("Whether a pipe of a liquid freezes and carries none of it where the place is colder than the "
                    + "liquid's melting point, unless the pipe is insulated.")
            .named("Pipes freeze");

    /** Whether the cold liquids kept in tanks boil off as the heat of the place leaks in. */
    public static final ConfigKey<Boolean> HEAT_BOIL_OFF = ConfigKey.flag(HEAT + ".boil_off", true)
            .comment("Whether a liquid kept colder than the place it is in, as liquid nitrogen is, boils off as the "
                    + "place's heat leaks into its tank, the faster the worse the tank is insulated.")
            .named("Cold liquids boil off");

    /** Whether a machine past its heat limit loses health. */
    public static final ConfigKey<Boolean> HEAT_OVERHEATING_DAMAGES = ConfigKey.flag(
            HEAT + ".overheating_damages", true)
            .comment("Whether a machine hotter than it stands loses health while it is; it stops to cool either way.")
            .named("Overheating damages machines");

    /** Whether a block past 300 °C sets alight what burns against it. */
    public static final ConfigKey<Boolean> HEAT_IGNITES = ConfigKey.flag(HEAT + ".ignites", true)
            .comment("Whether a block hotter than 300 °C sets alight what burns against it, as fire spreads.")
            .named("Hot blocks start fires");

    /** Whether a block past 150 °C burns whoever stands against it. */
    public static final ConfigKey<Boolean> HEAT_HOT_SURFACES_BURN = ConfigKey.flag(HEAT + ".hot_surfaces_burn", true)
            .comment("Whether a block hotter than 150 °C burns whoever stands against it, unless they resist fire.")
            .named("Hot surfaces burn");

    /** Whether a part with a rating breaks when the torque through it passes the rating. */
    public static final ConfigKey<Boolean> MECHANICAL_PARTS_BREAK = ConfigKey.flag(MECHANICAL + ".parts_break", true)
            .comment("Whether a shaft, a gear or another part with a rating breaks when the torque through it passes "
                    + "the rating. A shear pin shears either way.")
            .named("Parts break under overload");

    /** The file, each setting passed into the balance it sets. */
    public static final ConfigFile FILE = ConfigFile.builder("jstech-balance", ConfigSide.SERVER, ConfigFormats.TOML)
            .comment("Balance of the Operations engine and of the programs machines run.",
                    "Values outside their range are pulled to the nearer end when the file is read.")
            .sectionComment(BALANCE,
                    "How the Operations engine waits and shares its work, and how long programs run.")
            .sectionComment(MEDIA, "The recordings players hear and bring: how much of the connection they take, "
                    + "and how big one may be.")
            .sectionComment(CALENDAR, "The world's calendar: how long its seasons last.")
            .sectionComment(WORLD, "What machines may do to the world: how many chunks an owner keeps loaded.")
            .sectionComment(PROTECTION, "Who may hurt whom, what is written down of what players do to others' "
                    + "things, and how many of each thing one player may have.")
            .sectionComment(DIAGNOSTICS, "The watchdog, and what the server counts of what it sends each player.")
            .sectionComment(ELEMENTS, "Naming the elements discovered beyond the 118 known.")
            .sectionComment(HAZARDS, "The world's hazards: which of them there are.")
            .sectionComment(HEAT, "What heat does to the world: pipes that freeze, cold liquids that boil off, "
                    + "machines that overheat, fires and burns.")
            .sectionComment(MECHANICAL, "What mechanical power does to the world: parts that break under overload.")
            .sectionNamed(BALANCE, "Engine and programs")
            .sectionNamed(MEDIA, "Recordings")
            .sectionNamed(CALENDAR, "Calendar")
            .sectionNamed(WORLD, "World")
            .sectionNamed(PROTECTION, "Protection")
            .sectionNamed(DIAGNOSTICS, "Diagnostics")
            .sectionNamed(ELEMENTS, "Elements")
            .sectionNamed(HAZARDS, "Hazards")
            .sectionNamed(HEAT, "Heat")
            .sectionNamed(MECHANICAL, "Mechanical power")
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
            .key(WORLD_CHUNKS_PER_OWNER, ChunkLoadingBalance::setLimit)
            .key(PROTECTION_TEAM_DAMAGE, ProtectionBalance::setTeamDamage)
            .key(PROTECTION_AUDIT, ProtectionBalance::setAudit)
            .key(PROTECTION_AUDIT_ENTRIES, ProtectionBalance::setAuditEntries)
            .key(PROTECTION_ROBOTS_PER_PLAYER, ProtectionBalance::setRobotsPerPlayer)
            .key(DIAGNOSTICS_STALL_MILLIS, DiagnosticBalance::setStallMillis)
            .key(DIAGNOSTICS_COUNT_NETWORK, DiagnosticBalance::setCountNetwork)
            .key(DIAGNOSTICS_WARN_OPERATORS, DiagnosticBalance::setWarnOperators)
            .key(ELEMENTS_DISCOVERERS_NAME, NamingBalance::setDiscoverersName)
            .key(ELEMENTS_REQUIRE_IUM, NamingBalance::setRequireIum)
            .key(ELEMENTS_LONGEST_NAME, NamingBalance::setLongest)
            .key(HAZARDS_NATURAL_RADIOACTIVITY, HazardBalance::setNaturalRadioactivity)
            .key(HEAT_PIPES_FREEZE, HeatBalance::setPipesFreeze)
            .key(HEAT_BOIL_OFF, HeatBalance::setBoilOff)
            .key(HEAT_OVERHEATING_DAMAGES, HeatBalance::setOverheatingDamages)
            .key(HEAT_IGNITES, HeatBalance::setIgnites)
            .key(HEAT_HOT_SURFACES_BURN, HeatBalance::setHotSurfacesBurn)
            .key(MECHANICAL_PARTS_BREAK, MechanicalBalance::setPartsBreak)
            .build();

    private CoreConfigKeys() {
    }
}
