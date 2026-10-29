/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.rocket;

import static java.util.Map.entry;

import dev.jstech.core.content.BlockEntry;
import dev.jstech.space.api.RocketKit;
import dev.jstech.space.api.RocketKits;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

/**
 * The kits of the skeleton's rocket parts, where each block of a part goes, layer by layer from the bottom. The rings
 * are hollow, the instrument ring's guidance platform in its middle on its beams; an engine's top layer is its mount
 * with the chamber in the middle, the turbopump and the gimbal actuators in its ring, and the layers under it are its
 * bell's walls, a vacuum engine's extension below them; a tank's walls carry the cable raceway up its side, the fill
 * port low and the vent valve high, its domes in the middle of its lowest and highest layers, and it stands on its
 * skirt over an engine; the legs are at the corners with their actuators beside them; the lander's descent stage is
 * its engine under two layers of foil with its legs and ladder, its ascent stage the cabin; the capsules lie the crew
 * on couches over the heat shield, the hatch on the right, the windows at the head's corners and in the left wall, the
 * flight computer at the front right, the thrusters and the parachute bays round the docking port on top.
 */
public final class SkeletonRocketKits {

    private SkeletonRocketKits() {
    }

    /** Declares the kits, once, from the mod's constructor. */
    public static void declare() {
        final Map<String, ResourceLocation> instrument = keys(
                entry("W", RocketKitBlocks.INSTRUMENT_WALL), entry("A", RocketKitBlocks.AVIONICS_PANEL),
                entry("B", RocketKitBlocks.BATTERY_PANEL), entry("D", RocketKitBlocks.ACCESS_DOOR),
                entry("U", RocketKitBlocks.UMBILICAL_PLATE), entry("N", RocketKitBlocks.TELEMETRY_ANTENNA),
                entry("C", RocketKitBlocks.COOLING_LOUVRES), entry("G", RocketKitBlocks.GUIDANCE_PLATFORM));
        fixed("instrument_ring_3", instrument, List.of("CND", "AGU", "AWB"));
        fixed("instrument_ring_5", instrument, List.of("ACNDW", "A   U", "W G B", "A   A", "CWNWB"));
        final Map<String, ResourceLocation> separation = keys(entry("S", RocketKitBlocks.SEPARATION_WALL),
                entry("R", RocketKitBlocks.RETRO_ROCKET), entry("L", RocketKitBlocks.ULLAGE_MOTOR));
        fixed("separation_ring_3", separation, List.of("LSR", "S S", "RSL"));
        fixed("separation_ring_5", separation, List.of("RLSSR", "S   L", "S   S", "R   L", "LSSSR"));
        final Map<String, ResourceLocation> engine = keys(entry("M", RocketKitBlocks.ENGINE_MOUNT),
                entry("H", RocketKitBlocks.COMBUSTION_CHAMBER), entry("P", RocketKitBlocks.TURBOPUMP),
                entry("X", RocketKitBlocks.GIMBAL_ACTUATOR), entry("Z", RocketKitBlocks.NOZZLE_WALL),
                entry("E", RocketKitBlocks.NOZZLE_EXTENSION));
        fixed("sea_level_engine_3", engine, List.of(" Z ", "ZZZ", " Z "), List.of("MPM", "MHX", "MXM"));
        fixed("vacuum_engine_3", engine, List.of("EEE", "E E", "EEE"), List.of("ZZZ", "ZZZ", "ZZZ"),
                List.of("MPM", "MHX", "MXM"));
        fixed("sea_level_engine_5", engine, List.of("     ", " ZZZ ", " Z Z ", " ZZZ ", "     "),
                List.of("     ", " ZZZ ", " ZZZ ", " ZZZ ", "     "),
                List.of("MMPMM", "M   M", "M H X", "M   M", "MMXMM"));
        fixed("vacuum_engine_5", engine, List.of(" EEE ", "EEEEE", "EE EE", "EEEEE", " EEE "),
                List.of("     ", " EEE ", " E E ", " EEE ", "     "),
                List.of("     ", " ZZZ ", " ZZZ ", " ZZZ ", "     "),
                List.of("MMPMM", "M   M", "M H X", "M   M", "MMXMM"));
        final Map<String, ResourceLocation> tank = keys(entry("T", RocketKitBlocks.TANK_WALL),
                entry("O", RocketKitBlocks.TANK_DOME), entry("F", RocketKitBlocks.FILL_PORT),
                entry("V", RocketKitBlocks.VENT_VALVE), entry("Y", RocketKitBlocks.CABLE_RACEWAY),
                entry("K", RocketKitBlocks.TANK_SKIRT));
        RocketKits.declare(part("tank_3"), new RocketKit(List.of(List.of("TYF", "TOT", "TTT"),
                List.of("TYT", "T T", "TTT"), List.of("VYT", "TOT", "TTT")), tank, Optional.of(1),
                Optional.of(List.of("VYF", "TOT", "TTT")), Optional.of(List.of("KKK", "K K", "KKK"))));
        RocketKits.declare(part("tank_5"), new RocketKit(List.of(
                List.of("TTTYT", "TOOOF", "TOOOT", "TOOOT", "TTTTT"),
                List.of("TTTYT", "T   T", "T   T", "T   T", "TTTTT"),
                List.of("TVTYT", "TOOOT", "TOOOT", "TOOOT", "TTTTT")), tank, Optional.of(1),
                Optional.of(List.of("TVTYT", "TOOOF", "TOOOT", "TOOOT", "TTTTT")),
                Optional.of(List.of("KKKKK", "K   K", "K   K", "K   K", "KKKKK"))));
        final Map<String, ResourceLocation> legs = keys(entry("m", RocketKitBlocks.LEG_MOUNT),
                entry("l", RocketKitBlocks.LANDING_LEG), entry("a", RocketKitBlocks.LEG_ACTUATOR));
        fixed("landing_legs_3", legs, List.of("lal", "a a", "lal"));
        fixed("landing_legs_5", legs, List.of("lamal", "a   a", "m   m", "a   a", "lamal"));
        fixed("lunar_lander_descent", keys(entry("f", RocketKitBlocks.DESCENT_FOIL),
                        entry("e", RocketKitBlocks.DESCENT_ENGINE), entry("g", RocketKitBlocks.LANDER_LEG),
                        entry("d", RocketKitBlocks.LANDER_LADDER)),
                List.of("     ", "     ", "  e  ", "     ", "     "),
                List.of("ffgff", "f   f", "g   g", "f   f", "ffgff"),
                List.of("ffdff", "f   f", "f   f", "f   f", "fffff"));
        fixed("lunar_lander_ascent", keys(entry("w", RocketKitBlocks.CABIN_WALL),
                        entry("v", RocketKitBlocks.CABIN_WINDOW), entry("p", RocketKitBlocks.PROPELLANT_TANK),
                        entry("r", RocketKitBlocks.RENDEZVOUS_RADAR), entry("n", RocketKitBlocks.ASCENT_ENGINE),
                        entry("h", RocketKitBlocks.CAPSULE_HATCH), entry("s", RocketKitBlocks.CREW_SEAT),
                        entry("c", RocketKitBlocks.FLIGHT_COMPUTER), entry("t", RocketKitBlocks.THRUSTER_QUAD),
                        entry("o", RocketKitBlocks.DOCKING_PORT), entry("N", RocketKitBlocks.TELEMETRY_ANTENNA)),
                List.of("     ", " whw ", " psp ", " wnw ", "     "),
                List.of("     ", " vcv ", " w w ", " www ", "     "),
                List.of("     ", " trt ", " Now ", " twt ", "     "));
        final Map<String, ResourceLocation> capsule = keys(entry("q", RocketKitBlocks.HEAT_SHIELD),
                entry("k", RocketKitBlocks.CAPSULE_HULL), entry("i", RocketKitBlocks.CAPSULE_WINDOW),
                entry("h", RocketKitBlocks.CAPSULE_HATCH), entry("s", RocketKitBlocks.CREW_SEAT),
                entry("c", RocketKitBlocks.FLIGHT_COMPUTER), entry("t", RocketKitBlocks.THRUSTER_QUAD),
                entry("b", RocketKitBlocks.PARACHUTE_BAY), entry("o", RocketKitBlocks.DOCKING_PORT));
        fixed("capsule_3", capsule, List.of("qqq", "qqq", "qqq"), List.of("kkc", "ish", "iki"),
                List.of("tbt", "bob", "tbt"));
        fixed("capsule_5", capsule, List.of(" qqq ", "qqqqq", "qqqqq", "qqqqq", " qqq "),
                List.of("kkkkc", "k   k", "isssh", "k   k", "ikkki"),
                List.of("     ", " tbt ", " b b ", " tbt ", "     "),
                List.of("     ", "     ", "  o  ", "     ", "     "));
    }

    @SafeVarargs
    private static void fixed(final String part, final Map<String, ResourceLocation> key,
                              final List<String>... layers) {
        RocketKits.declare(part(part), RocketKit.of(List.of(layers), key));
    }

    @SafeVarargs
    private static Map<String, ResourceLocation> keys(final Map.Entry<String, BlockEntry<Block>>... entries) {
        final Map<String, ResourceLocation> key = new LinkedHashMap<>();
        for (final Map.Entry<String, BlockEntry<Block>> each : entries) {
            key.put(each.getKey(), each.getValue().getId());
        }
        return Map.copyOf(key);
    }

    private static ResourceLocation part(final String path) {
        return SkeletonRocketParts.id(path);
    }
}
