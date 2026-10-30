/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.shipyard;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** What the Starship Design screen says. */
@TextHolder
public final class ShipDesignTexts {

    public static final TextKey UNNAMED = TextKey.of("jsspace.ship_design.unnamed", "JS-%s");
    public static final TextKey CLASS_LINE = TextKey.of("jsspace.ship_design.class_line", "%s class   %s blk   %s t");
    public static final TextKey SMALL_CLASS = TextKey.of("jsspace.ship_design.class.small", "Small");
    // The tools.
    public static final TextKey PLACE = TextKey.of("jsspace.ship_design.place", "Place");
    public static final TextKey REMOVE = TextKey.of("jsspace.ship_design.remove", "Remove");
    public static final TextKey FLIP = TextKey.of("jsspace.ship_design.flip", "Flip");
    public static final TextKey MIRROR = TextKey.of("jsspace.ship_design.mirror", "Mirror");
    public static final TextKey FINISH_TOOL = TextKey.of("jsspace.ship_design.finish_tool", "Finish");
    public static final TextKey SNAP = TextKey.of("jsspace.ship_design.snap", "Snap");
    // The parts.
    public static final TextKey PARTS = TextKey.of("jsspace.ship_design.parts", "Parts");
    public static final TextKey SEARCH = TextKey.of("jsspace.ship_design.search", "Search parts");
    public static final TextKey ALL = TextKey.of("jsspace.ship_design.all", "All");
    public static final TextKey KIND_COCKPITS = TextKey.of("jsspace.ship_design.kind.cockpits", "Cockpits");
    public static final TextKey KIND_HULL = TextKey.of("jsspace.ship_design.kind.hull", "Hull and holds");
    public static final TextKey KIND_MODULES = TextKey.of("jsspace.ship_design.kind.modules", "Modules");
    public static final TextKey KIND_REACTORS = TextKey.of("jsspace.ship_design.kind.reactors", "Reactors");
    public static final TextKey KIND_ENGINES = TextKey.of("jsspace.ship_design.kind.engines", "Engines");
    public static final TextKey KIND_WINGS = TextKey.of("jsspace.ship_design.kind.wings", "Wings and fins");
    public static final TextKey KIND_GEAR = TextKey.of("jsspace.ship_design.kind.gear", "Landing gear");
    public static final TextKey PART_LINE = TextKey.of("jsspace.ship_design.part_line", "%s class   %s");
    public static final TextKey MODULE_LINE = TextKey.of("jsspace.ship_design.module_line", "%s module   %s");
    public static final TextKey MEDIUM = TextKey.of("jsspace.ship_design.medium", "Medium");
    public static final TextKey LENGTH = TextKey.of("jsspace.ship_design.length", "Length");
    public static final TextKey MASS = TextKey.of("jsspace.ship_design.mass", "Mass");
    public static final TextKey BLOCKS = TextKey.of("jsspace.ship_design.blocks", "Blocks");
    public static final TextKey POWER = TextKey.of("jsspace.ship_design.power", "Power");
    public static final TextKey INSIDE = TextKey.of("jsspace.ship_design.inside", "Inside: %s");
    public static final TextKey PAINT = TextKey.of("jsspace.ship_design.paint", "Paint: the ship's finish");
    public static final TextKey BLK = TextKey.of("jsspace.ship_design.blk", "%s blk");
    public static final TextKey TONNES = TextKey.of("jsspace.ship_design.tonnes", "%s t");
    public static final TextKey MEGAWATTS = TextKey.of("jsspace.ship_design.megawatts", "%s MW");
    public static final TextKey KILONEWTONS = TextKey.of("jsspace.ship_design.kilonewtons", "%s kN");
    // The flight check.
    public static final TextKey FLIGHT_CHECK = TextKey.of("jsspace.ship_design.flight_check", "Flight check");
    public static final TextKey COCKPIT_OK = TextKey.of("jsspace.ship_design.cockpit_ok", "Cockpit  pressurised");
    public static final TextKey NO_COCKPIT = TextKey.of("jsspace.ship_design.no_cockpit", "No cockpit at the nose");
    public static final TextKey AIRLOCK_OK = TextKey.of("jsspace.ship_design.airlock_ok", "Airlock  suit locker");
    public static final TextKey NO_AIRLOCK = TextKey.of("jsspace.ship_design.no_airlock", "No airlock");
    public static final TextKey REACTOR_OK = TextKey.of("jsspace.ship_design.reactor_ok", "Reactor  fusion");
    public static final TextKey NO_REACTOR = TextKey.of("jsspace.ship_design.no_reactor", "No reactor");
    public static final TextKey MAIN_OK = TextKey.of("jsspace.ship_design.main_ok", "Main engines  %s");
    public static final TextKey NO_MAIN = TextKey.of("jsspace.ship_design.no_main", "No main engine");
    public static final TextKey AIR_OK = TextKey.of("jsspace.ship_design.air_ok", "Air engines  VTOL");
    public static final TextKey NO_AIR = TextKey.of("jsspace.ship_design.no_air", "No air engines: no VTOL");
    public static final TextKey GEAR_OK = TextKey.of("jsspace.ship_design.gear_ok", "Landing gear  %s");
    public static final TextKey NO_GEAR = TextKey.of("jsspace.ship_design.no_gear", "No landing gear");
    public static final TextKey SPINE_OK = TextKey.of("jsspace.ship_design.spine_ok", "Spine whole");
    public static final TextKey SPINE_GAP = TextKey.of("jsspace.ship_design.spine_gap", "Spine broken: a gap");
    public static final TextKey SPINE_SHORT = TextKey.of("jsspace.ship_design.spine_short",
            "Spine broken: one section");
    public static final TextKey MISPLACED = TextKey.of("jsspace.ship_design.misplaced", "A part out of its place");
    public static final TextKey MIXED = TextKey.of("jsspace.ship_design.mixed", "Parts of other classes");
    public static final TextKey UNKNOWN = TextKey.of("jsspace.ship_design.unknown", "A part no longer known");
    public static final TextKey ROOM_LINE = TextKey.of("jsspace.ship_design.room_line", "Room: %s small of %s");
    public static final TextKey TOO_MUCH = TextKey.of("jsspace.ship_design.too_much", "Room: more than the hull holds");
    public static final TextKey FITS = TextKey.of("jsspace.ship_design.fits", "Fits the dock");
    public static final TextKey NO_FIT = TextKey.of("jsspace.ship_design.no_fit", "Does not fit the dock");
    // The ship's figures.
    public static final TextKey THE_SHIP = TextKey.of("jsspace.ship_design.the_ship", "The ship");
    public static final TextKey THRUST = TextKey.of("jsspace.ship_design.thrust", "Thrust");
    public static final TextKey VTOL_TWR = TextKey.of("jsspace.ship_design.vtol_twr", "VTOL TWR");
    public static final TextKey POWER_OF = TextKey.of("jsspace.ship_design.power_of", "%s of %s MW");
    public static final TextKey CREW = TextKey.of("jsspace.ship_design.crew", "Crew");
    public static final TextKey CREW_LINE = TextKey.of("jsspace.ship_design.crew_line", "%s seat  %s bed");
    public static final TextKey ROOM = TextKey.of("jsspace.ship_design.room", "Room in the hull");
    public static final TextKey FREE = TextKey.of("jsspace.ship_design.free", "Free");
    public static final TextKey ROOM_NOTE = TextKey.of("jsspace.ship_design.room_note", "1 medium or 2 to 3 small");
    public static final TextKey FINISH = TextKey.of("jsspace.ship_design.finish", "Finish");
    public static final TextKey THE_DOCK = TextKey.of("jsspace.ship_design.the_dock", "The dock");
    public static final TextKey IN_THE_PORT = TextKey.of("jsspace.ship_design.in_the_port", "In the item port");
    public static final TextKey TO_BRING = TextKey.of("jsspace.ship_design.to_bring", "%s still to bring");
    // The keys and what the screen is doing.
    public static final TextKey KEY_DELETE = TextKey.of("jsspace.ship_design.key_delete", "Del");
    public static final TextKey SEND = TextKey.of("jsspace.ship_design.send", "Send to the dock");
    public static final TextKey SAVE = TextKey.of("jsspace.ship_design.save", "Save");
    public static final TextKey DELETE = TextKey.of("jsspace.ship_design.delete", "Remove");
    public static final TextKey DRAG = TextKey.of("jsspace.ship_design.drag", "%s: drag to a joint");
    public static final TextKey READY = TextKey.of("jsspace.ship_design.ready", "Ready: E sends it to the dock");
    public static final TextKey NOT_READY = TextKey.of("jsspace.ship_design.not_ready", "Not ready: %s");
    public static final TextKey SENT = TextKey.of("jsspace.ship_design.sent", "Sent to the dock");
    public static final TextKey SAVED = TextKey.of("jsspace.ship_design.saved", "Saved as %s");
    public static final TextKey NOT_SAVED = TextKey.of("jsspace.ship_design.not_saved",
            "Not saved: name it, or the dock's plans are full");
    public static final TextKey PAIRS = TextKey.of("jsspace.ship_design.pairs",
            "A small ship's side parts always go in pairs");
    public static final TextKey NO_FLIP = TextKey.of("jsspace.ship_design.no_flip", "A cockpit is never flipped");
    public static final TextKey SNAPS_HERE = TextKey.of("jsspace.ship_design.snaps_here", "Snaps here");
    public static final TextKey SPAN = TextKey.of("jsspace.ship_design.span", "Span");

    private ShipDesignTexts() {
    }
}
