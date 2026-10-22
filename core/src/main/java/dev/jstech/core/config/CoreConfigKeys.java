/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.config;

import com.mojang.serialization.Codec;
import dev.jstech.core.audio.media.MediaBalance;
import dev.jstech.core.config.format.ConfigFormats;
import dev.jstech.core.diagnostic.DiagnosticBalance;
import dev.jstech.core.explosion.BlastBalance;
import dev.jstech.core.explosion.BlastConsequence;
import dev.jstech.core.fire.FireBalance;
import dev.jstech.core.hazard.HazardBalance;
import dev.jstech.core.heat.HeatBalance;
import dev.jstech.core.mechanical.MechanicalBalance;
import dev.jstech.core.moving.StructureBalance;
import dev.jstech.core.language.ExecutionBalance;
import dev.jstech.core.material.NamingBalance;
import dev.jstech.core.multiblock.BuildBalance;
import dev.jstech.core.operation.OperationBalance;
import dev.jstech.core.protection.ProtectionBalance;
import dev.jstech.core.region.ChunkLoadingBalance;
import dev.jstech.core.room.RoomBalance;
import dev.jstech.core.time.WorldCalendar;
import dev.jstech.core.tool.ToolBalance;
import dev.jstech.core.world.event.WorldEventBalance;
import dev.jstech.core.worldgen.vein.VeinBalance;
import java.util.List;
import java.util.Map;

/**
 * The series' balance, {@code jstech-balance.toml} beside the world: how the Operations engine waits and shares its
 * work, how long the programs of the machines may run each tick, how much of the connection the recordings players
 * hear and bring may take, how long the world's seasons last, how its ores lie in veins, and how often its events
 * happen. A value outside its range
 * is pulled to the nearer end rather than refused, so a slip in the file costs one setting its value instead of
 * breaking the world; each value read is passed straight into the balance it sets.
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
    /** What blasts do to the world, and how much of a tick they take. */
    public static final String EXPLOSIONS = "explosions";
    /** What fire does: the remains it leaves, the wind and the smoke. */
    public static final String FIRE = "fire";
    /** The world's hazards: radiation, pollution and the rest. */
    public static final String HAZARDS = "hazards";
    /** What heat does to the world: pipes that freeze, cold liquids that boil off, overheating, fires and burns. */
    public static final String HEAT = "heat";
    /** What mechanical power does to the world: parts that break under overload. */
    public static final String MECHANICAL = "mechanical";
    /** The moving structures: how big one may be. */
    public static final String MOVING = "moving_structures";
    /** The section of building multiblocks for players. */
    public static final String MULTIBLOCK = "multiblock";
    /** Tools that break or place many blocks at once, and copies of regions. */
    public static final String TOOLS = "tools";
    /** What sealed rooms do: decompression, fire and oxygen, breathing mobs, plants, bursting blocks, their size. */
    public static final String ROOMS = "rooms";
    /** How the world lays its ores: in veins, or scattered as the game lays them. */
    public static final String VEINS = "veins";
    /** The events the world goes through: whether they happen, and how often each. */
    public static final String WORLD_EVENTS = "world_events";

    private static final int MAX_LATENCY_TICKS = 200;
    private static final int MAX_BLOCKS_PER_SECOND = 40;
    private static final int MIN_BUILD_REACH = 8;
    private static final int MAX_BUILD_REACH = 128;
    private static final int MAX_TIMEOUT_TICKS = 72_000;
    private static final int MAX_EXPIRY_HOURS = 24 * 365;
    private static final int MIN_LARGEST_ROOM = 64;
    private static final int MAX_LARGEST_ROOM = 262_144;
    private static final int MIN_MACHINE_MICROS = 50;
    private static final int MAX_MACHINE_MICROS = 1_000_000;
    private static final int MIN_SERVER_MICROS = 100;
    private static final int MAX_SERVER_MICROS = 10_000_000;
    private static final int MIN_MEDIA_RATE = 16;
    private static final int MAX_MEDIA_RATE = 65_536;
    private static final int MAX_MEDIA_FILE_MEGABYTES = 1024;
    private static final int MAX_MEDIA_QUOTA_MEGABYTES = 1_048_576;
    private static final int MAX_CHUNKS_PER_OWNER = 4096;
    private static final int MAX_SERVER_CHUNKS = 65_536;
    private static final double MAX_VEIN_FACTOR = 100;
    private static final double MAX_EVENT_FACTOR = 100;
    private static final int MIN_AUDIT_ENTRIES = 100;
    private static final int MAX_AUDIT_ENTRIES = 1_000_000;
    private static final int MAX_ROBOTS_PER_PLAYER = 1024;
    private static final int MAX_STRUCTURES_PER_PLAYER = 1024;
    private static final int MAX_EDITS_PER_PLAYER = 256;
    private static final int MIN_STALL_MILLIS = 50;
    private static final int MAX_STALL_MILLIS = 60_000;
    private static final int SHORTEST_LONGEST_NAME = 4;
    private static final int LONGEST_LONGEST_NAME = 40;
    private static final int FEWEST_LOOKED_AT = 1_024;
    private static final int MOST_LOOKED_AT = 1_048_576;
    private static final int FEWEST_BROKEN = 16;
    private static final int MOST_BROKEN = 65_536;
    private static final int MOST_STRUCTURE_BLOCKS = 4_194_304;
    private static final double MOST_PHYSICS_MILLIS = 45.0;
    private static final int MOST_TOOL_BLOCKS = 4096;
    private static final double MOST_HARDNESS_MARGIN = 100.0;
    private static final int MOST_COPIED_BLOCKS = 65_536;

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

    /** How many chunks every owner together may keep loaded through the series' machines: the server's budget. */
    public static final ConfigKey<Integer> WORLD_CHUNKS_SERVER_BUDGET = ConfigKey.whole(
            WORLD + ".chunks_server_budget", ChunkLoadingBalance.DEFAULT_SERVER_BUDGET)
            .range(0, MAX_SERVER_CHUNKS)
            .comment("How many chunks every player and team together may keep loaded through machines, across every "
                    + "dimension: the server's budget, besides each owner's own number; 0 lets nobody keep any.")
            .named("Loaded chunks on the server").unit("chunks");

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

    /** How many moving structures one player may have made and not yet set down. */
    public static final ConfigKey<Integer> PROTECTION_STRUCTURES_PER_PLAYER = ConfigKey.whole(
            PROTECTION + ".structures_per_player", ProtectionBalance.DEFAULT_STRUCTURES_PER_PLAYER)
            .range(0, MAX_STRUCTURES_PER_PLAYER)
            .comment("How many moving structures one player may have made and not yet set down; the pieces a broken "
                    + "one parts into are not counted. 0 lets nobody make one.")
            .named("Moving structures per player").unit("structures");

    /** How many large edits one player may have under way at once. */
    public static final ConfigKey<Integer> PROTECTION_EDITS_PER_PLAYER = ConfigKey.whole(
            PROTECTION + ".edits_per_player", ProtectionBalance.DEFAULT_EDITS_PER_PLAYER)
            .range(0, MAX_EDITS_PER_PLAYER)
            .comment("How many large edits of the world (a crater, a levelled hill) one player may have under way "
                    + "at once; 0 lets nobody start one.")
            .named("Large edits per player").unit("edits");

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

    /** Whether there is radiation at all. */
    public static final ConfigKey<Boolean> HAZARDS_RADIATION = ConfigKey.flag(HAZARDS + ".radiation", true)
            .comment("Whether there is radiation at all: off, no dose builds up, nothing is contaminated, no source "
                    + "gives any off and the instruments read nothing.")
            .named("Radiation");

    /** Whether minerals with uranium or thorium in them are naturally radioactive. */
    public static final ConfigKey<Boolean> HAZARDS_NATURAL_RADIOACTIVITY = ConfigKey.flag(
            HAZARDS + ".natural_radioactivity", true)
            .comment("Whether every mineral with uranium or thorium in it is naturally radioactive, at a low rate.")
            .named("Radioactive minerals").activeWhen(HAZARDS_RADIATION);

    /** Whether creatures suffer hazards as players do. */
    public static final ConfigKey<Boolean> HAZARDS_MOBS_SUFFER = ConfigKey.flag(HAZARDS + ".mobs_suffer", true)
            .comment("Whether creatures suffer hazards as players do: animals sicken and die in a contaminated place.")
            .named("Creatures suffer hazards");

    /** Whether dusts breathed harm the lungs. */
    public static final ConfigKey<Boolean> HAZARDS_LUNGS = ConfigKey.flag(HAZARDS + ".lungs", true)
            .comment("Whether coal dust, silica and asbestos breathed without a mask build up a lasting harm to the "
                    + "lungs.")
            .named("Dust harms the lungs");

    /** Whether what a player carries harms them. */
    public static final ConfigKey<Boolean> HAZARDS_ITEM_HAZARDS = ConfigKey.flag(HAZARDS + ".item_hazards", true)
            .comment("Whether what is carried harms its carrier: a hot ingot burns, sodium catches fire in the rain, a "
                    + "radioactive rod doses.")
            .named("Hazards of what is carried");

    /** Whether laws of physics that depart from ours build up dissonance. */
    public static final ConfigKey<Boolean> HAZARDS_DISSONANCE = ConfigKey.flag(HAZARDS + ".dissonance", true)
            .comment("Whether laws of physics that depart from ours (time at another pace, a pull that is not down, "
                    + "space that wraps) build up dissonance in a body, from warped sight to coming apart.")
            .named("Dissonance");

    /** Whether there is pollution. */
    public static final ConfigKey<Boolean> HAZARDS_POLLUTION = ConfigKey.flag(HAZARDS + ".pollution", true)
            .comment("Whether there is pollution: soot, acid rain, heavy metals and carbon dioxide given off and doing "
                    + "what they do; radioactive fallout goes with radiation.")
            .named("Pollution");

    /** Whether contamination withers the land. */
    public static final ConfigKey<Boolean> HAZARDS_WITHERING = ConfigKey.flag(HAZARDS + ".withering", true)
            .comment("Whether the world shows contamination: grass dies, plants wither and crops stop growing where "
                    + "radiation or pollution is high, and come back slowly where it falls.")
            .named("Contamination withers the land");

    /** What each mod's explosions do to the world, by the mod's id or the explosive's. */
    public static final ConfigKey<Map<String, BlastConsequence>> EXPLOSIONS_CONSEQUENCES = ConfigKey.of(
            EXPLOSIONS + ".consequences", Codec.unboundedMap(Codec.STRING, BlastConsequence.CODEC), Map.of())
            .comment("What each mod's explosions do to the world, by the mod's id or by one explosive's, as in "
                    + "\"jsindustrial\" = \"spare_blocks\": \"full\" breaks blocks, hurts and throws; \"spare_blocks\" "
                    + "hurts and throws but breaks nothing; \"harmless\" only flashes and sounds. An explosive's own "
                    + "setting comes before its mod's; one not named does it all.")
            .named("What explosions do");

    /** How many blocks the blasts of a level look at in a tick. */
    public static final ConfigKey<Integer> EXPLOSIONS_LOOKED_AT = ConfigKey.whole(EXPLOSIONS + ".blocks_looked_at",
            BlastBalance.DEFAULT_LOOKED_AT).range(FEWEST_LOOKED_AT, MOST_LOOKED_AT)
            .comment("How many blocks the blasts of a level look at in a tick, as their shock waves go out; a great "
                    + "blast past it goes on over the next ticks.")
            .named("Blocks a blast looks at in a tick").unit("blocks");

    /** How many blocks the blasts of a level break in a tick. */
    public static final ConfigKey<Integer> EXPLOSIONS_BROKEN = ConfigKey.whole(EXPLOSIONS + ".blocks_broken",
            BlastBalance.DEFAULT_BROKEN).range(FEWEST_BROKEN, MOST_BROKEN)
            .comment("How many blocks the blasts of a level break in a tick; the rest wait for the next ticks, so a "
                    + "great blast never stalls the server.")
            .named("Blocks a blast breaks in a tick").unit("blocks");

    /** Whether the game's own explosions show the Core's fireball, smoke and scorch marks. */
    public static final ConfigKey<Boolean> EXPLOSIONS_GAME_SHOWN = ConfigKey.flag(EXPLOSIONS + ".show_game_explosions",
            true)
            .comment("Whether the game's own explosions (TNT, creepers, beds in the Nether) show the fireball, the "
                    + "smoke, the sparks and the scorch marks the mods' blasts do. They break what they always did.")
            .named("The game's explosions look the part");

    /** Whether what burned stays burned. */
    public static final ConfigKey<Boolean> FIRE_REMAINS = ConfigKey.flag(FIRE + ".remains", true)
            .comment("Whether what burned stays burned: a log burns to charred wood and wool to ash, as data packs "
                    + "say, instead of vanishing.")
            .named("What burned stays burned");

    /** Whether the wind spreads fire. */
    public static final ConfigKey<Boolean> FIRE_WIND = ConfigKey.flag(FIRE + ".wind", true)
            .comment("Whether the wind spreads a fire faster downwind and slower upwind, and a strong wind makes it "
                    + "leap a few blocks ahead.")
            .named("The wind spreads fire");

    /** Whether fires give off smoke. */
    public static final ConfigKey<Boolean> FIRE_SMOKE = ConfigKey.flag(FIRE + ".smoke", true)
            .comment("Whether fires give off smoke that rises and drifts with the wind.")
            .named("Fires give off smoke");

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

    /** Whether the air rushing out of a room pulls things towards the hole. */
    public static final ConfigKey<Boolean> ROOMS_DECOMPRESSION_PULL = ConfigKey.flag(
            ROOMS + ".decompression_pull", true)
            .comment("Whether the air rushing out of a sealed room through a hole pulls items, mobs and players "
                    + "towards it.")
            .named("Decompression pulls");

    /** Whether fire in air mostly of oxygen spreads at once. */
    public static final ConfigKey<Boolean> ROOMS_FIRE_IN_PURE_OXYGEN = ConfigKey.flag(
            ROOMS + ".fire_in_pure_oxygen", true)
            .comment("Whether any flame in a room whose air is mostly oxygen sets alight at once everything that "
                    + "burns round it, an inferno.")
            .named("Infernos in pure oxygen");

    /** Whether fire needs oxygen. */
    public static final ConfigKey<Boolean> ROOMS_FIRE_NEEDS_OXYGEN = ConfigKey.flag(
            ROOMS + ".fire_needs_oxygen", true)
            .comment("Whether fire goes out, and will not light, where the air has too little oxygen, in a room or "
                    + "out in a dimension without it.")
            .named("Fire needs oxygen");

    /** Whether mobs that breathe suffocate in air they cannot breathe. */
    public static final ConfigKey<Boolean> ROOMS_MOBS_BREATHE = ConfigKey.flag(ROOMS + ".mobs_breathe", true)
            .comment("Whether mobs that breathe, by their kind, suffocate in air they cannot breathe; the undead and "
                    + "golems do not breathe.")
            .named("Mobs breathe");

    /** Whether plants need air to grow. */
    public static final ConfigKey<Boolean> ROOMS_PLANTS_NEED_AIR = ConfigKey.flag(ROOMS + ".plants_need_air", true)
            .comment("Whether crops and saplings grow only where there is air.")
            .named("Plants need air");

    /** Whether a block facing more pressure than it stands bursts. */
    public static final ConfigKey<Boolean> ROOMS_BLOCKS_BURST = ConfigKey.flag(ROOMS + ".blocks_burst", true)
            .comment("Whether a block of a room's walls facing a greater difference in pressure than it stands "
                    + "bursts, as a weak window does under water.")
            .named("Blocks burst");

    /** The largest room looked for, in blocks. */
    public static final ConfigKey<Integer> ROOMS_LARGEST = ConfigKey.whole(ROOMS + ".largest_room",
            RoomBalance.LARGEST_DEFAULT).range(MIN_LARGEST_ROOM, MAX_LARGEST_ROOM)
            .comment("The largest sealed room, in blocks: a closed space larger than this is taken as open to the "
                    + "place round it.")
            .named("Largest room").unit("blocks");

    /** Whether ores lie in veins, the game's scattered iron, copper and gold taken away. */
    public static final ConfigKey<Boolean> VEINS_ENABLED = ConfigKey.flag(VEINS + ".enabled", true)
            .comment("Whether ores lie in veins about three chunks wide, dense at the middle and thinning outwards, "
                    + "instead of scattered; the game's scattered iron, copper and gold are taken away while they do. "
                    + "Only chunks made from then on change.")
            .named("Ores lie in veins");

    /** How many times as often each kind of vein is found, by its id. */
    public static final ConfigKey<Map<String, Double>> VEINS_RARITY = ConfigKey.of(VEINS + ".rarity",
            Codec.unboundedMap(Codec.STRING, Codec.doubleRange(0, MAX_VEIN_FACTOR)), Map.of())
            .activeWhen(VEINS_ENABLED)
            .comment("How many times as often each kind of vein is found, by its id, as in "
                    + "\"jscore:iron\" = 0.5; 0 lays none of it. A kind not named keeps its own.")
            .named("How often veins are found");

    /** How many times as large each kind of vein is, by its id. */
    public static final ConfigKey<Map<String, Double>> VEINS_SPREAD = ConfigKey.of(VEINS + ".spread",
            Codec.unboundedMap(Codec.STRING, Codec.doubleRange(0, MAX_VEIN_FACTOR)), Map.of())
            .activeWhen(VEINS_ENABLED)
            .comment("How many times as large each kind of vein is, by its id; a vein is never wider than its "
                    + "zone of three chunks.")
            .named("How large veins are");

    /** How many times as dense each kind of vein is, by its id. */
    public static final ConfigKey<Map<String, Double>> VEINS_DENSITY = ConfigKey.of(VEINS + ".density",
            Codec.unboundedMap(Codec.STRING, Codec.doubleRange(0, MAX_VEIN_FACTOR)), Map.of())
            .activeWhen(VEINS_ENABLED)
            .comment("How many times as dense each kind of vein is, by its id; its middle is at most wholly ore.")
            .named("How dense veins are");

    /** The materials whose ore is never laid, by their ids. */
    public static final ConfigKey<List<String>> VEINS_WITHOUT_ORE = ConfigKey.of(VEINS + ".without_ore",
            Codec.STRING.listOf(), List.of())
            .comment("The materials whose ore is never laid, by their ids, as in \"jscore:tin\"; veins of them "
                    + "keep their other ores.")
            .named("Ores never laid");

    /** Whether other mods' scattered ores are moved into veins of their own. */
    public static final ConfigKey<Boolean> VEINS_MOVE_OTHERS = ConfigKey.flag(VEINS + ".try_moving_other_ores",
            false)
            .activeWhen(VEINS_ENABLED)
            .comment("Try moving other mods' scattered ores into veins, finding them by the common ores tag. It "
                    + "works with mods that lay their ores as data, and may miss a mod that lays them its own way.")
            .named("Try moving other mods' ores");

    /** Whether the world's events happen by themselves. */
    public static final ConfigKey<Boolean> WORLD_EVENTS_ENABLED = ConfigKey.flag(WORLD_EVENTS + ".enabled", true)
            .comment("Whether the events the mods declare (storms, showers, quakes) happen by themselves, by their "
                    + "schedules and their chances. An operator's command starts any of them still.")
            .named("World events happen");

    /** How many times as often each world event happens by chance, by its id. */
    public static final ConfigKey<Map<String, Double>> WORLD_EVENTS_FREQUENCY = ConfigKey.of(
            WORLD_EVENTS + ".frequency", Codec.unboundedMap(Codec.STRING, Codec.doubleRange(0, MAX_EVENT_FACTOR)),
            Map.of())
            .activeWhen(WORLD_EVENTS_ENABLED)
            .comment("How many times as often each event that happens by chance does, by its id, as in "
                    + "\"mymod:solar_storm\" = 0.5. An event not named keeps its own chance.")
            .named("How often world events happen");

    /** The world events that never happen by themselves, by their ids. */
    public static final ConfigKey<List<String>> WORLD_EVENTS_DISABLED = ConfigKey.of(WORLD_EVENTS + ".disabled",
            Codec.STRING.listOf(), List.of())
            .activeWhen(WORLD_EVENTS_ENABLED)
            .comment("The events that never happen by themselves, by their ids, as in \"mymod:earthquake\".")
            .named("World events that never happen");

    /** The world events that leave the world as it is, by their ids. */
    public static final ConfigKey<List<String>> WORLD_EVENTS_HARMLESS = ConfigKey.of(WORLD_EVENTS + ".harmless",
            Codec.STRING.listOf(), List.of())
            .comment("The events that leave the world as it is, by their ids: no craters, no radiation, no cracks. "
                    + "They still come, warn and play their effects.")
            .named("World events that change nothing");

    /** How many blocks a second a tool that builds a multiblock for a player places. */
    public static final ConfigKey<Integer> MULTIBLOCK_BLOCKS_PER_SECOND = ConfigKey.whole(
            MULTIBLOCK + ".blocks_per_second", BuildBalance.DEFAULT_BLOCKS_PER_SECOND).range(1, MAX_BLOCKS_PER_SECOND)
            .comment("How many blocks a second a tool that builds a multiblock for a player places, one at a time.")
            .named("Blocks built a second").unit("blocks/s");

    /** How many blocks one moving structure may be made of. */
    public static final ConfigKey<Integer> MOVING_MOST_BLOCKS = ConfigKey.whole(MOVING + ".most_blocks",
            StructureBalance.DEFAULT_MOST_BLOCKS).range(1, MOST_STRUCTURE_BLOCKS)
            .comment("How many blocks one moving structure may be made of; a structure of more is refused, saying "
                    + "why.")
            .named("Most blocks in a moving structure").unit("blocks");

    /** How much of a tick the physics of a dimension's moving structures may take. */
    public static final ConfigKey<Double> MOVING_PHYSICS_MILLIS = ConfigKey.number(MOVING + ".physics_millis",
            StructureBalance.DEFAULT_PHYSICS_MILLIS).range(0.5, MOST_PHYSICS_MILLIS)
            .comment("How much of a tick, in milliseconds, the physics of a dimension's moving structures may take; "
                    + "past it a structure waits a tick, and goes the next whatever the budget.")
            .named("Most physics a tick").unit("ms");

    /** Whether a moving structure that loses its middle becomes two. */
    public static final ConfigKey<Boolean> MOVING_SPLIT = ConfigKey.flag(MOVING + ".split", true)
            .comment("Whether a moving structure whose blocks are no longer all joined, its middle broken out, "
                    + "becomes as many structures as it now is, each going its own way; off, it holds together.")
            .named("Broken structures part");

    /** Whether gravity pulls moving structures moving by their physics. */
    public static final ConfigKey<Boolean> MOVING_GRAVITY = ConfigKey.flag(MOVING + ".gravity", true)
            .comment("Whether gravity pulls moving structures moving by their physics; off, they float where they "
                    + "are let go.")
            .named("Gravity on structures");

    /** Whether the faces of moving structures drag in the air and in liquids. */
    public static final ConfigKey<Boolean> MOVING_DRAG = ConfigKey.flag(MOVING + ".drag", true)
            .comment("Whether the faces of moving structures drag as they meet the air and liquids; off, nothing "
                    + "slows them but what they touch.")
            .named("Drag on structures");

    /** Whether the wings of moving structures lift. */
    public static final ConfigKey<Boolean> MOVING_LIFT = ConfigKey.flag(MOVING + ".lift", true)
            .comment("Whether the wings, sails and rudders of moving structures lift and turn them by the air going "
                    + "past.")
            .named("Lift of wings");

    /** Whether the engines of moving structures push. */
    public static final ConfigKey<Boolean> MOVING_THRUST = ConfigKey.flag(MOVING + ".thrust", true)
            .comment("Whether the engines and propellers of moving structures push.")
            .named("Thrust of engines");

    /** Whether liquids, the air a hull holds and balloons hold moving structures up. */
    public static final ConfigKey<Boolean> MOVING_BUOYANCY = ConfigKey.flag(MOVING + ".buoyancy", true)
            .comment("Whether liquids, the air a hull holds and the balloons aboard hold moving structures up; off, "
                    + "a ship sinks and a balloon lies on the ground.")
            .named("Buoyancy and balloons");

    /** Whether the levitators of moving structures hold them up. */
    public static final ConfigKey<Boolean> MOVING_LEVITATION = ConfigKey.flag(MOVING + ".levitation", true)
            .comment("Whether the levitators aboard moving structures hold them up.")
            .named("Levitation");

    /** Whether the magnets of moving structures pull. */
    public static final ConfigKey<Boolean> MOVING_MAGNETISM = ConfigKey.flag(MOVING + ".magnetism", true)
            .comment("Whether the magnets aboard moving structures pull them towards the magnetic blocks of the "
                    + "world.")
            .named("Magnetism");

    /** Whether the wind blows on moving structures. */
    public static final ConfigKey<Boolean> MOVING_WIND = ConfigKey.flag(MOVING + ".wind", true)
            .comment("Whether the wind blows on moving structures, pushing sails and balloons and drifting light "
                    + "craft; off, the air they meet is still.")
            .named("Wind on structures");

    /** Whether the blows of moving structures break the blocks they are too hard for. */
    public static final ConfigKey<Boolean> MOVING_IMPACT_DAMAGE = ConfigKey.flag(MOVING + ".impact_damage", true)
            .comment("Whether a blow of a moving structure harder than a block holds, by its blast strength, breaks "
                    + "it, of the world or of the structure; off, structures meet and bounce, and nothing breaks.")
            .named("Impacts break blocks");

    /** Whether the physics tool is made by its recipe. */
    public static final ConfigKey<Boolean> MOVING_PHYSICS_TOOL_RECIPE = ConfigKey.flag(MOVING
                    + ".physics_tool_recipe", true)
            .comment("Whether the physics tool is made at a crafting table; off, only a command or the creative "
                    + "menu gives it.")
            .named("Physics tool recipe");

    /** How far from the controller the player may be while a tool builds a multiblock. */
    public static final ConfigKey<Integer> MULTIBLOCK_BUILD_REACH = ConfigKey.whole(
            MULTIBLOCK + ".build_reach", BuildBalance.DEFAULT_REACH).range(MIN_BUILD_REACH, MAX_BUILD_REACH)
            .comment("How far from a multiblock's controller, in blocks, the player may walk while a tool builds it; "
                    + "past that it stops.")
            .named("Building reach").unit("blocks");

    /** The most blocks a tool breaks in one blow. */
    public static final ConfigKey<Integer> TOOLS_MOST_BROKEN = ConfigKey.whole(TOOLS + ".most_broken",
            ToolBalance.DEFAULT_MOST_BROKEN).range(1, MOST_TOOL_BLOCKS)
            .comment("The most blocks a tool that mines an area or a vein breaks in one blow, the one broken among "
                    + "them; a tool's own area or vein never takes more.")
            .named("Blocks broken at once").unit("blocks");

    /** How much harder than the block broken a block in the area may be. */
    public static final ConfigKey<Double> TOOLS_HARDNESS_MARGIN = ConfigKey.number(TOOLS + ".hardness_margin",
            ToolBalance.DEFAULT_HARDNESS_MARGIN).range(0.0, MOST_HARDNESS_MARGIN)
            .comment("How much harder than the block broken, as the game measures hardness (stone 1.5, an ore 3, "
                    + "obsidian 50), a block in a tool's area or vein may be and still be taken.")
            .named("Hardness margin");

    /** The most blocks a building tool places in one use. */
    public static final ConfigKey<Integer> TOOLS_MOST_PLACED = ConfigKey.whole(TOOLS + ".most_placed",
            ToolBalance.DEFAULT_MOST_PLACED).range(1, MOST_TOOL_BLOCKS)
            .comment("The most blocks a building tool places in one use along a line or over a plane; a paste places "
                    + "the whole copy, which the most copied keeps small.")
            .named("Blocks placed at once").unit("blocks");

    /** The most blocks a copy of a region holds. */
    public static final ConfigKey<Integer> TOOLS_MOST_COPIED = ConfigKey.whole(TOOLS + ".most_copied",
            ToolBalance.DEFAULT_MOST_COPIED).range(1, MOST_COPIED_BLOCKS)
            .comment("The most blocks a building tool's copy of a region holds; a larger region is not copied.")
            .named("Blocks copied").unit("blocks");

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
            .sectionComment(EXPLOSIONS, "What blasts do to the world, mod by mod, and how much of a tick they take.")
            .sectionComment(FIRE, "What fire does: the remains it leaves, the wind and the smoke.")
            .sectionComment(HAZARDS, "The world's hazards: which of them there are.")
            .sectionComment(HEAT, "What heat does to the world: pipes that freeze, cold liquids that boil off, "
                    + "machines that overheat, fires and burns.")
            .sectionComment(MECHANICAL, "What mechanical power does to the world: parts that break under overload.")
            .sectionComment(MOVING, "The moving structures: how big one may be, how much of a tick their "
                    + "physics may take, whether one broken in two parts, which forces of their physics act, whether "
                    + "their blows break blocks, and whether the physics tool has a recipe.")
            .sectionComment(MULTIBLOCK, "Building multiblocks for players: how fast, and how far from them.")
            .sectionComment(TOOLS, "Tools that break or place many blocks at once: how many, how much harder than "
                    + "the block broken, and how big a copy of a region may be.")
            .sectionNamed(BALANCE, "Engine and programs")
            .sectionNamed(MEDIA, "Recordings")
            .sectionNamed(CALENDAR, "Calendar")
            .sectionNamed(WORLD, "World")
            .sectionNamed(PROTECTION, "Protection")
            .sectionNamed(DIAGNOSTICS, "Diagnostics")
            .sectionNamed(ELEMENTS, "Elements")
            .sectionNamed(EXPLOSIONS, "Explosions")
            .sectionNamed(FIRE, "Fire")
            .sectionNamed(HAZARDS, "Hazards")
            .sectionNamed(HEAT, "Heat")
            .sectionNamed(MECHANICAL, "Mechanical power")
            .sectionNamed(MOVING, "Moving structures")
            .sectionNamed(MULTIBLOCK, "Multiblocks")
            .sectionNamed(TOOLS, "Tools")
            .sectionNamed(ROOMS, "Sealed rooms")
            .sectionNamed(VEINS, "Veins")
            .sectionNamed(WORLD_EVENTS, "World events")
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
            .key(WORLD_CHUNKS_SERVER_BUDGET, ChunkLoadingBalance::setServerBudget)
            .key(PROTECTION_TEAM_DAMAGE, ProtectionBalance::setTeamDamage)
            .key(PROTECTION_AUDIT, ProtectionBalance::setAudit)
            .key(PROTECTION_AUDIT_ENTRIES, ProtectionBalance::setAuditEntries)
            .key(PROTECTION_ROBOTS_PER_PLAYER, ProtectionBalance::setRobotsPerPlayer)
            .key(PROTECTION_STRUCTURES_PER_PLAYER, ProtectionBalance::setStructuresPerPlayer)
            .key(PROTECTION_EDITS_PER_PLAYER, ProtectionBalance::setEditsPerPlayer)
            .key(DIAGNOSTICS_STALL_MILLIS, DiagnosticBalance::setStallMillis)
            .key(DIAGNOSTICS_COUNT_NETWORK, DiagnosticBalance::setCountNetwork)
            .key(DIAGNOSTICS_WARN_OPERATORS, DiagnosticBalance::setWarnOperators)
            .key(ELEMENTS_DISCOVERERS_NAME, NamingBalance::setDiscoverersName)
            .key(ELEMENTS_REQUIRE_IUM, NamingBalance::setRequireIum)
            .key(ELEMENTS_LONGEST_NAME, NamingBalance::setLongest)
            .key(HAZARDS_RADIATION, HazardBalance::setRadiation)
            .key(HAZARDS_NATURAL_RADIOACTIVITY, HazardBalance::setNaturalRadioactivity)
            .key(HAZARDS_MOBS_SUFFER, HazardBalance::setMobsSuffer)
            .key(HAZARDS_LUNGS, HazardBalance::setLungs)
            .key(HAZARDS_ITEM_HAZARDS, HazardBalance::setItemHazards)
            .key(HAZARDS_DISSONANCE, HazardBalance::setDissonance)
            .key(HAZARDS_POLLUTION, HazardBalance::setPollution)
            .key(HAZARDS_WITHERING, HazardBalance::setWithering)
            .key(EXPLOSIONS_CONSEQUENCES, BlastBalance::setConsequences)
            .key(EXPLOSIONS_LOOKED_AT, BlastBalance::setLookedAt)
            .key(EXPLOSIONS_BROKEN, BlastBalance::setBroken)
            .key(EXPLOSIONS_GAME_SHOWN, BlastBalance::setGameExplosionsShown)
            .key(FIRE_REMAINS, FireBalance::setRemains)
            .key(FIRE_WIND, FireBalance::setWind)
            .key(FIRE_SMOKE, FireBalance::setSmoke)
            .key(HEAT_PIPES_FREEZE, HeatBalance::setPipesFreeze)
            .key(HEAT_BOIL_OFF, HeatBalance::setBoilOff)
            .key(HEAT_OVERHEATING_DAMAGES, HeatBalance::setOverheatingDamages)
            .key(HEAT_IGNITES, HeatBalance::setIgnites)
            .key(HEAT_HOT_SURFACES_BURN, HeatBalance::setHotSurfacesBurn)
            .key(MECHANICAL_PARTS_BREAK, MechanicalBalance::setPartsBreak)
            .key(MOVING_MOST_BLOCKS, StructureBalance::mostBlocks)
            .key(MOVING_PHYSICS_MILLIS, StructureBalance::physicsMillis)
            .key(MOVING_SPLIT, StructureBalance::split)
            .key(MOVING_GRAVITY, StructureBalance::gravity)
            .key(MOVING_DRAG, StructureBalance::drag)
            .key(MOVING_LIFT, StructureBalance::lift)
            .key(MOVING_THRUST, StructureBalance::thrust)
            .key(MOVING_BUOYANCY, StructureBalance::buoyancy)
            .key(MOVING_LEVITATION, StructureBalance::levitation)
            .key(MOVING_MAGNETISM, StructureBalance::magnetism)
            .key(MOVING_WIND, StructureBalance::wind)
            .key(MOVING_IMPACT_DAMAGE, StructureBalance::impacts)
            .key(MOVING_PHYSICS_TOOL_RECIPE, StructureBalance::physicsToolRecipe)
            .key(MULTIBLOCK_BLOCKS_PER_SECOND, BuildBalance::setBlocksPerSecond)
            .key(MULTIBLOCK_BUILD_REACH, BuildBalance::setReach)
            .key(TOOLS_MOST_BROKEN, ToolBalance::setMostBroken)
            .key(TOOLS_HARDNESS_MARGIN, ToolBalance::setHardnessMargin)
            .key(TOOLS_MOST_PLACED, ToolBalance::setMostPlaced)
            .key(TOOLS_MOST_COPIED, ToolBalance::setMostCopied)
            .key(ROOMS_DECOMPRESSION_PULL, RoomBalance::setDecompressionPull)
            .key(ROOMS_FIRE_IN_PURE_OXYGEN, RoomBalance::setFireInPureOxygen)
            .key(ROOMS_FIRE_NEEDS_OXYGEN, RoomBalance::setFireNeedsOxygen)
            .key(ROOMS_MOBS_BREATHE, RoomBalance::setMobsBreathe)
            .key(ROOMS_PLANTS_NEED_AIR, RoomBalance::setPlantsNeedAir)
            .key(ROOMS_BLOCKS_BURST, RoomBalance::setBlocksBurst)
            .key(ROOMS_LARGEST, RoomBalance::setLargestRoom)
            .key(VEINS_ENABLED, VeinBalance::setEnabled)
            .key(VEINS_RARITY, VeinBalance::setRarity)
            .key(VEINS_SPREAD, VeinBalance::setSpread)
            .key(VEINS_DENSITY, VeinBalance::setDensity)
            .key(VEINS_WITHOUT_ORE, VeinBalance::setWithoutOre)
            .key(VEINS_MOVE_OTHERS, VeinBalance::setMoveOthers)
            .key(WORLD_EVENTS_ENABLED, WorldEventBalance::setEnabled)
            .key(WORLD_EVENTS_FREQUENCY, WorldEventBalance::setFrequency)
            .key(WORLD_EVENTS_DISABLED, WorldEventBalance::setDisabled)
            .key(WORLD_EVENTS_HARMLESS, WorldEventBalance::setHarmless)
            .build();

    private CoreConfigKeys() {
    }
}
