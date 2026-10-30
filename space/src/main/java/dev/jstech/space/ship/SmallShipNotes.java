/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.ship;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * A line on each part of the small starship's catalogue, what a builder says of the part chosen: what it holds, what
 * works in it, what it is for. Each is its part's name's key with {@code .note} after it.
 */
@TextHolder
public final class SmallShipNotes {

    public static final TextKey ORBITER_COCKPIT = note("orbiter_cockpit",
            "Three displays, a side stick, the master caution: the orbiter line's flight deck.");
    public static final TextKey CAB_COCKPIT = note("cab_cockpit",
            "A working cab: a green CRT, needles and switches, a wide window.");
    public static final TextKey GREENHOUSE_COCKPIT = note("greenhouse_cockpit",
            "Glazed all round, black-faced dials, guarded switches and the navigation globe.");
    public static final TextKey AIRLOCK = note("airlock",
            "The airlock: its door, its cycler, a suit locker and the oxygen panel.");
    public static final TextKey DOCKING = note("docking",
            "A dorsal collar that docks with ships and stations, an airlock too.");
    public static final TextKey CORRIDOR = note("corridor", "A short passage joining two sections.");
    public static final TextKey BAY = note("bay", "A hold of barrels behind belly doors, its hoist rail overhead.");
    public static final TextKey RAMP = note("ramp", "A hold that opens a ramp at the tail; it ends the spine.");
    public static final TextKey SIDE_DOOR = note("side_door", "A hold loaded through a door in its side.");
    public static final TextKey REACTOR_FLUSH = note("reactor_flush",
            "A fusion reactor, its heat shed by radiators flush with the hull.");
    public static final TextKey REACTOR_FINS = note("reactor_fins",
            "A larger fusion reactor, its heat shed by radiator fins.");
    public static final TextKey TWIN_BELLS = note("twin_bells", "Two bells at the tail, the main engine.");
    public static final TextKey CLUSTER = note("cluster", "Four bells at the tail, more thrust for more mass.");
    public static final TextKey MAGNETIC_NOZZLE = note("magnetic_nozzle",
            "A magnetic nozzle on each nacelle: no bell, much power drawn.");
    public static final TextKey SIDE_INTAKES = note("side_intakes",
            "Air engines along the sides, their nozzles turned down to lift off straight up.");
    public static final TextKey NACELLE_INTAKE = note("nacelle_intake",
            "An air engine in each nacelle, its nozzles swivelling down to lift.");
    public static final TextKey BLENDED_DELTA = note("blended_delta", "A wide delta blended into the hull.");
    public static final TextKey STUB_WINGS = note("stub_wings", "Short wings that carry the nacelles.");
    public static final TextKey SWEPT_WINGS = note("swept_wings", "Swept wings with pods at their tips.");
    public static final TextKey TWIN_CANTED_FINS = note("twin_canted_fins", "Two fins canted out over the tail.");
    public static final TextKey SINGLE_FIN = note("single_fin", "One tall fin over the tail.");
    public static final TextKey BLISTERS = note("blisters", "Thruster blisters at the nose and the tail, to turn.");
    public static final TextKey QUADS = note("quads", "Four-way thruster quads at the nose and the tail.");
    public static final TextKey WHEELS = note("wheels", "Wheels, to roll on a runway or a pad.");
    public static final TextKey LEGS = note("legs", "Landing legs, for rough ground.");
    public static final TextKey WORKSHOP = note("workshop",
            "Crafting table, furnace, smithing table, barrel: they work aboard.");
    public static final TextKey LAB = note("lab", "A bench, sample racks, a microscope and an analyser.");
    public static final TextKey QUARTERS = note("quarters", "A bunk to wake in, where the cabin holds its air.");
    public static final TextKey COMPUTING = note("computing", "A desk and a server rack.");
    public static final TextKey SMALL_CARGO = note("small_cargo", "Racks and a barrel for a little cargo.");

    private SmallShipNotes() {
    }

    /** The key of the note of the part whose name's key is {@code name}. */
    public static String keyOf(final String name) {
        return name + ".note";
    }

    private static TextKey note(final String part, final String english) {
        return TextKey.of(keyOf("jsspace.ship_part." + part), english);
    }
}
