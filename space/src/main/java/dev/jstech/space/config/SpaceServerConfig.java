/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.config;

import dev.jstech.core.config.ConfigFile;
import dev.jstech.core.config.ConfigFiles;
import dev.jstech.core.config.ConfigKey;
import dev.jstech.core.config.ConfigSide;
import dev.jstech.core.config.format.ConfigFormats;
import java.util.List;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;

/**
 * The settings of J's Space for a world, {@code jsspace-server.toml} beside it. None of them turns a part of the mod
 * off: each is a consequence, on unless a server turns it off and saying what happens instead, or a rule of balance.
 * Off, the failures of a rocket leave only a rocket that does not leave the ground or does not arrive. The settings of
 * what J's Core's kits do (sealed rooms, radiation, the knowledge gate) live in J's Core's files.
 */
public final class SpaceServerConfig {

    private static final String LAUNCH = "launch";
    private static final String ROCKETS = "rockets";
    private static final String FAILURES = "failures";
    private static final String TRAVEL = "travel";
    private static final String WORLDS = "worlds";
    private static final String LIFE = "life_support";
    private static final String ORBIT = "orbit";
    private static final String SERIES = "series";
    private static final String HANGAR = "hangar";

    /** Whether a pad with no flame trench and no deluge is damaged at launch. */
    public static final ConfigKey<Boolean> LAUNCH_DAMAGE = ConfigKey.flag(LAUNCH + ".launch_damage", true)
            .comment("A pad with no flame trench and no deluge is damaged at launch, and so is what stands around it.",
                    "Off, the pad takes no harm, whatever it is built with.")
            .named("Launch damage");
    /** Whether the launch panel holds the countdown in a thunderstorm. */
    public static final ConfigKey<Boolean> NO_LAUNCH_IN_THUNDERSTORMS = ConfigKey.flag(LAUNCH
                    + ".no_launch_in_thunderstorms", true)
            .comment("The launch panel holds the countdown while a thunderstorm is over the pad.",
                    "Off, a rocket launches in any weather.")
            .named("No launch in a thunderstorm");
    /** Whether a stage that falls where nothing waits for it leaves a crater. */
    public static final ConfigKey<Boolean> FALLING_STAGES_DO_DAMAGE = ConfigKey.flag(ROCKETS
                    + ".falling_stages_do_damage", true)
            .comment("A stage that falls with no fall zone and no platform to land on leaves a crater where it comes "
                    + "down.", "Off, it only lies where it fell, as wreckage.")
            .named("Falling stages do damage");
    /** Whether a rocket heavier than its thrust burns its propellant on the pad. */
    public static final ConfigKey<Boolean> TOO_HEAVY_TO_LIFT = ConfigKey.flag(FAILURES + ".too_heavy_to_lift", true)
            .comment("A rocket whose thrust is less than its weight lights its engines and stays on the pad, burning "
                    + "its propellant.", "Off, its engines do not light, and nothing is spent.")
            .named("Too heavy to lift");
    /** Whether a capsule with no fitting heat shield burns up coming down through air. */
    public static final ConfigKey<Boolean> BURNING_UP = ConfigKey.flag(FAILURES + ".burning_up", true)
            .comment("A capsule that comes down through air with no heat shield, or with the wrong one for that air, "
                    + "burns up.", "Off, such a capsule cannot begin to come down, and stays where it is.")
            .named("Burning up on re-entry");
    /** Whether a lander with no legs or no propellant to slow down crashes. */
    public static final ConfigKey<Boolean> CRASH_LANDINGS = ConfigKey.flag(FAILURES + ".crash_landings", true)
            .comment("A lander with no legs, or with no propellant left to slow down, crashes.",
                    "Off, such a lander cannot begin its landing, and stays where it is.")
            .named("Crash landings");
    /** Whether a reused stage past its limit, unrepaired, fails. */
    public static final ConfigKey<Boolean> WORN_STAGES_FAIL = ConfigKey.flag(FAILURES + ".worn_stages_fail", true)
            .comment("Every flight wears the parts of a reused stage: inspected and repaired, it keeps flying; past "
                    + "its limit unrepaired, an engine shuts down or a tank cracks.", "Off, reused stages never wear.")
            .named("Worn stages fail");
    /** Whether a damaged oxygen tank used anyway explodes. */
    public static final ConfigKey<Boolean> DAMAGED_TANKS_EXPLODE = ConfigKey.flag(FAILURES + ".damaged_tanks_explode",
                    true)
            .comment("A damaged oxygen tank used anyway explodes, destroying the rocket and harming what is around "
                    + "it.", "Off, a rocket with a damaged tank does not leave the ground.")
            .named("Damaged tanks explode");
    /** Whether solar storms, micrometeoroids and failures come on the way between worlds. */
    public static final ConfigKey<Boolean> CRUISE_EVENTS = ConfigKey.flag(TRAVEL + ".cruise_events", true)
            .comment("Solar storms, micrometeoroids and failures come on the way between worlds.",
                    "Off, the cruise is quiet.")
            .named("Cruise events");
    /** Whether the cruise of rockets and starships is skipped: a rule of balance, off. */
    public static final ConfigKey<Boolean> FAST_TRAVEL = ConfigKey.flag(TRAVEL + ".fast_travel", false)
            .comment("Whether a rocket or a starship skips its cruise and arrives at once.",
                    "Off, the cruise is flown, with its time and its events.")
            .named("Fast travel");
    /** Whether a crew lands only on a world whose essentials its team knows. */
    public static final ConfigKey<Boolean> LANDING_NEEDS_THE_ESSENTIALS = ConfigKey.flag(TRAVEL
                    + ".landing_needs_the_essentials", true)
            .comment("A crew cannot land on a world until its team knows the world's atmosphere, temperature, "
                    + "radiation, gravity and relief.", "Off, a crew may land on any world it reaches, known or not.")
            .named("Landing needs the essentials");
    /** Whether dust settles on solar panels on Mars and global dust storms come. */
    public static final ConfigKey<Boolean> DUST_ON_MARS = ConfigKey.flag(WORLDS + ".dust_on_mars", true)
            .comment("Dust settles on solar panels on Mars, and now and then a dust storm covers the whole planet.",
                    "Off, Mars has no dust on its panels and no dust storms.")
            .named("Dust on Mars");
    /** Whether the first team to find a hidden world names it. */
    public static final ConfigKey<Boolean> NAMING_HIDDEN_WORLDS = ConfigKey.flag(WORLDS + ".naming_hidden_worlds",
                    true)
            .comment("The first team to find a hidden world names it.", "Off, every world keeps its default name.")
            .named("Naming hidden worlds");
    /** Whether torches, lanterns, candles and campfires go out where the air holds too little oxygen to burn. */
    public static final ConfigKey<Boolean> FLAMES_NEED_AIR = ConfigKey.flag(LIFE + ".flames_need_air", true)
            .comment("Torches, lanterns, candles and campfires go out where the air holds too little oxygen to burn: "
                    + "in a vacuum, in the thin air of Mars, in a room whose air runs short.",
                    "Off, they burn anywhere, as on the Earth.")
            .named("Flames need air");
    /** Whether a destroyed satellite leaves debris in orbit. */
    public static final ConfigKey<Boolean> DEBRIS = ConfigKey.flag(ORBIT + ".debris", true)
            .comment("A destroyed satellite leaves debris in orbit, which strikes what passes through it and "
                    + "multiplies.", "Off, a destroyed satellite leaves nothing behind.")
            .named("Debris");
    /** Whether the Laser Satellite may strike players and what they built: off for a server without PvP. */
    public static final ConfigKey<Boolean> LASER_SATELLITE_WEAPON = ConfigKey.flag(ORBIT + ".laser_satellite_weapon",
                    true)
            .comment("The Laser Satellite may strike players and what they built.",
                    "Off, for a server without PvP, it harms no player and nothing a player built.")
            .named("Laser Satellite as a weapon");
    /** Whether J's Industrial's chains become the only path for what both mods make: a rule of balance, off. */
    public static final ConfigKey<Boolean> INDUSTRIAL_CHAIN_ONLY = ConfigKey.flag(SERIES + ".industrial_chain_only",
                    false)
            .comment("Whether, with J's Industrial installed, its chains become the only path for what both mods "
                    + "make.", "Off, what J's Space makes it makes its own way too.")
            .named("Industrial chain only");

    /** The narrowest a Rocket Hangar's bay may be, across and deep, in blocks. */
    public static final ConfigKey<Integer> HANGAR_NARROWEST = ConfigKey.whole(HANGAR + ".narrowest", 7)
            .range(5, 33)
            .comment("The narrowest a Rocket Hangar's bay may be, across and deep, inside its walls, in blocks.")
            .named("Narrowest bay");
    /** The widest a Rocket Hangar's bay may be, across and deep, in blocks. */
    public static final ConfigKey<Integer> HANGAR_WIDEST = ConfigKey.whole(HANGAR + ".widest", 48)
            .range(9, 96)
            .comment("The widest a Rocket Hangar's bay may be, across and deep, inside its walls, in blocks.",
                    "A wider one measures more of its walls each time it looks at itself.")
            .named("Widest bay");
    /** The lowest a Rocket Hangar's bay may be, in blocks. */
    public static final ConfigKey<Integer> HANGAR_LOWEST = ConfigKey.whole(HANGAR + ".lowest", 12)
            .range(8, 64)
            .comment("The lowest a Rocket Hangar's bay may be, from its floor to its roof, in blocks.")
            .named("Lowest bay");
    /** The tallest a Rocket Hangar's bay may be, in blocks. */
    public static final ConfigKey<Integer> HANGAR_TALLEST = ConfigKey.whole(HANGAR + ".tallest", 128)
            .range(16, 256)
            .comment("The tallest a Rocket Hangar's bay may be, from its floor to its roof, in blocks.")
            .named("Tallest bay");

    public static final ConfigFile FILE = ConfigFile.builder("jsspace-server", ConfigSide.SERVER, ConfigFormats.TOML)
            .sectionNamed(LAUNCH, "Launch")
            .sectionComment(LAUNCH, "What a launch does to its pad, and the weather it waits for.")
            .key(LAUNCH_DAMAGE).key(NO_LAUNCH_IN_THUNDERSTORMS)
            .sectionNamed(ROCKETS, "Rockets")
            .sectionComment(ROCKETS, "What a rocket's spent stages do where they fall.")
            .key(FALLING_STAGES_DO_DAMAGE)
            .sectionNamed(HANGAR, "Rocket Hangar")
            .sectionComment(HANGAR, "How small and how large a Rocket Hangar's bay may be.")
            .key(HANGAR_NARROWEST).key(HANGAR_WIDEST).key(HANGAR_LOWEST).key(HANGAR_TALLEST)
            .sectionNamed(FAILURES, "Rocket failures")
            .sectionComment(FAILURES, "What a badly built or badly kept rocket does: always a consequence of a "
                    + "choice, never bad luck.")
            .key(TOO_HEAVY_TO_LIFT).key(BURNING_UP).key(CRASH_LANDINGS).key(WORN_STAGES_FAIL)
            .key(DAMAGED_TANKS_EXPLODE)
            .sectionNamed(TRAVEL, "Travel")
            .sectionComment(TRAVEL, "The way between worlds, and what a crew must know before it lands.")
            .key(CRUISE_EVENTS).key(FAST_TRAVEL).key(LANDING_NEEDS_THE_ESSENTIALS)
            .sectionNamed(WORLDS, "Worlds")
            .sectionComment(WORLDS, "What the worlds do, and who names the hidden ones.")
            .key(DUST_ON_MARS).key(NAMING_HIDDEN_WORLDS)
            .sectionNamed(LIFE, "Life support")
            .sectionComment(LIFE, "What the air of other worlds does to what burns in it.")
            .key(FLAMES_NEED_AIR)
            .sectionNamed(ORBIT, "Orbit")
            .sectionComment(ORBIT, "What is left in orbit, and what a satellite may strike.")
            .key(DEBRIS).key(LASER_SATELLITE_WEAPON)
            .sectionNamed(SERIES, "The series")
            .sectionComment(SERIES, "How J's Space shares its work with the other mods of the series.")
            .key(INDUSTRIAL_CHAIN_ONLY)
            .build();

    /** Its sizes, each held to its range: how small and how large a Rocket Hangar's bay may be. */
    public static final List<ConfigKey<Integer>> SIZES = List.of(HANGAR_NARROWEST, HANGAR_WIDEST, HANGAR_LOWEST,
            HANGAR_TALLEST);
    /** Its consequences, each on unless a server turns it off. */
    public static final List<ConfigKey<Boolean>> CONSEQUENCES = List.of(LAUNCH_DAMAGE, NO_LAUNCH_IN_THUNDERSTORMS,
            FALLING_STAGES_DO_DAMAGE, TOO_HEAVY_TO_LIFT, BURNING_UP, CRASH_LANDINGS, WORN_STAGES_FAIL,
            DAMAGED_TANKS_EXPLODE, CRUISE_EVENTS, LANDING_NEEDS_THE_ESSENTIALS, DUST_ON_MARS, NAMING_HIDDEN_WORLDS,
            FLAMES_NEED_AIR, DEBRIS, LASER_SATELLITE_WEAPON);

    private SpaceServerConfig() {
    }

    /** Puts the file beside the world, where the server reads it. */
    public static void register(final IEventBus modEventBus, final ModContainer modContainer) {
        ConfigFiles.register(FILE, modEventBus, modContainer);
    }

    /** Whether torches, lanterns, candles and campfires go out where the air holds too little oxygen to burn. */
    public static boolean flamesNeedAir() {
        return FILE.get(FLAMES_NEED_AIR);
    }

    /** Whether a stage falling with no fall zone and no platform leaves a crater, or only lies there as wreckage. */
    public static boolean fallingStagesDoDamage() {
        return FILE.get(FALLING_STAGES_DO_DAMAGE);
    }

    /** Whether a rocket or a starship skips its cruise and arrives at once. */
    public static boolean fastTravel() {
        return FILE.get(FAST_TRAVEL);
    }

    /** Whether solar storms, micrometeoroids and failures come on the way between worlds. */
    public static boolean cruiseEvents() {
        return FILE.get(CRUISE_EVENTS);
    }

    /** Whether a destroyed satellite leaves debris in orbit. */
    public static boolean debris() {
        return FILE.get(DEBRIS);
    }
}
