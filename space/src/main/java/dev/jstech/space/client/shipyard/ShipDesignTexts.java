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
    public static final TextKey SIZE_LINE = TextKey.of("jsspace.ship_design.size_line", "%s blk  %s t");
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
    public static final TextKey MORE = TextKey.of("jsspace.ship_design.more", "%s more  v");
    public static final TextKey KIND_COCKPITS = TextKey.of("jsspace.ship_design.kind.cockpits", "Cockpits");
    public static final TextKey KIND_HULL = TextKey.of("jsspace.ship_design.kind.hull", "Hull and holds");
    public static final TextKey KIND_MODULES = TextKey.of("jsspace.ship_design.kind.modules", "Modules");
    public static final TextKey KIND_REACTORS = TextKey.of("jsspace.ship_design.kind.reactors", "Reactors");
    public static final TextKey KIND_ENGINES = TextKey.of("jsspace.ship_design.kind.engines", "Engines");
    public static final TextKey KIND_WINGS = TextKey.of("jsspace.ship_design.kind.wings", "Wings and fins");
    public static final TextKey KIND_GEAR = TextKey.of("jsspace.ship_design.kind.gear", "Landing gear");
    public static final TextKey LENGTH = TextKey.of("jsspace.ship_design.length", "Length");
    public static final TextKey MASS = TextKey.of("jsspace.ship_design.mass", "Mass");
    public static final TextKey BLOCKS = TextKey.of("jsspace.ship_design.blocks", "Blocks");
    public static final TextKey POWER = TextKey.of("jsspace.ship_design.power", "Power");
    public static final TextKey BLK = TextKey.of("jsspace.ship_design.blk", "%s blk");
    public static final TextKey TONNES = TextKey.of("jsspace.ship_design.tonnes", "%s t");
    public static final TextKey MEGAWATTS = TextKey.of("jsspace.ship_design.megawatts", "%s MW");
    public static final TextKey KILONEWTONS = TextKey.of("jsspace.ship_design.kilonewtons", "%s kN");
    // The flight check, each line short enough for its column.
    public static final TextKey FLIGHT_CHECK = TextKey.of("jsspace.ship_design.flight_check", "Check");
    public static final TextKey COCKPIT_OK = TextKey.of("jsspace.ship_design.cockpit_ok", "Cockpit");
    public static final TextKey NO_COCKPIT = TextKey.of("jsspace.ship_design.no_cockpit", "No cockpit");
    public static final TextKey AIRLOCK_OK = TextKey.of("jsspace.ship_design.airlock_ok", "Airlock");
    public static final TextKey NO_AIRLOCK = TextKey.of("jsspace.ship_design.no_airlock", "No airlock");
    public static final TextKey REACTOR_OK = TextKey.of("jsspace.ship_design.reactor_ok", "Reactor");
    public static final TextKey NO_REACTOR = TextKey.of("jsspace.ship_design.no_reactor", "No reactor");
    public static final TextKey MAIN_OK = TextKey.of("jsspace.ship_design.main_ok", "Main engines");
    public static final TextKey NO_MAIN = TextKey.of("jsspace.ship_design.no_main", "No main engine");
    public static final TextKey AIR_OK = TextKey.of("jsspace.ship_design.air_ok", "Air engines");
    public static final TextKey NO_AIR = TextKey.of("jsspace.ship_design.no_air", "No air engines");
    public static final TextKey GEAR_OK = TextKey.of("jsspace.ship_design.gear_ok", "Landing gear");
    public static final TextKey NO_GEAR = TextKey.of("jsspace.ship_design.no_gear", "No landing gear");
    public static final TextKey SPINE_OK = TextKey.of("jsspace.ship_design.spine_ok", "Spine whole");
    public static final TextKey SPINE_GAP = TextKey.of("jsspace.ship_design.spine_gap", "Spine: a gap");
    public static final TextKey SPINE_SHORT = TextKey.of("jsspace.ship_design.spine_short", "Spine: too short");
    public static final TextKey MISPLACED = TextKey.of("jsspace.ship_design.misplaced", "Part misplaced");
    public static final TextKey MIXED = TextKey.of("jsspace.ship_design.mixed", "Mixed classes");
    public static final TextKey UNKNOWN = TextKey.of("jsspace.ship_design.unknown", "Unknown part");
    public static final TextKey ROOM_LINE = TextKey.of("jsspace.ship_design.room_line", "Room: %s of %s");
    public static final TextKey TOO_MUCH = TextKey.of("jsspace.ship_design.too_much", "Room: too full");
    public static final TextKey NO_FIT = TextKey.of("jsspace.ship_design.no_fit", "Dock too small");
    // The ship's figures, and the dock's.
    public static final TextKey THE_SHIP = TextKey.of("jsspace.ship_design.the_ship", "The ship");
    public static final TextKey THRUST = TextKey.of("jsspace.ship_design.thrust", "Thrust");
    public static final TextKey VTOL_TWR = TextKey.of("jsspace.ship_design.vtol_twr", "VTOL");
    public static final TextKey POWER_OF = TextKey.of("jsspace.ship_design.power_of", "%s/%s MW");
    public static final TextKey CREW = TextKey.of("jsspace.ship_design.crew", "Crew");
    public static final TextKey CREW_LINE = TextKey.of("jsspace.ship_design.crew_line", "%s seat  %s bed");
    public static final TextKey THE_DOCK = TextKey.of("jsspace.ship_design.the_dock", "The dock");
    public static final TextKey IN_THE_PORT = TextKey.of("jsspace.ship_design.in_the_port", "Item Port");
    public static final TextKey TO_BRING = TextKey.of("jsspace.ship_design.to_bring", "%s to bring");
    // The keys, and what the screen says.
    public static final TextKey KEY_DELETE = TextKey.of("jsspace.ship_design.key_delete", "Del");
    public static final TextKey SEND = TextKey.of("jsspace.ship_design.send", "Dock");
    public static final TextKey SAVE = TextKey.of("jsspace.ship_design.save", "Save");
    public static final TextKey DELETE = TextKey.of("jsspace.ship_design.delete", "Remove");
    public static final TextKey NOT_READY = TextKey.of("jsspace.ship_design.not_ready", "Not ready: %s");
    public static final TextKey SENT = TextKey.of("jsspace.ship_design.sent", "Sent to the dock");
    public static final TextKey SAVED = TextKey.of("jsspace.ship_design.saved", "Saved as %s");
    public static final TextKey NOT_SAVED = TextKey.of("jsspace.ship_design.not_saved",
            "Not saved: name it, or the dock's plans are full");
    public static final TextKey PAIRS = TextKey.of("jsspace.ship_design.pairs",
            "A small ship's side parts always go in pairs");
    public static final TextKey NO_FLIP = TextKey.of("jsspace.ship_design.no_flip", "A cockpit is never flipped");
    // By the mouse, as a part is held: loose, over a joint it snaps to, over one it does not fit and why.
    public static final TextKey DRAG_TO_GREEN = TextKey.of("jsspace.ship_design.drag_to_green",
            "%s: drag to a green joint");
    public static final TextKey SNAPS_HERE = TextKey.of("jsspace.ship_design.snaps_here",
            "Snaps here: let go to place");
    public static final TextKey ONLY_COCKPIT_AT_NOSE = TextKey.of("jsspace.ship_design.only_cockpit_at_nose",
            "Only a cockpit fits the nose");
    public static final TextKey COCKPIT_ONLY_AT_NOSE = TextKey.of("jsspace.ship_design.cockpit_only_at_nose",
            "A cockpit fits only the nose");
    public static final TextKey NOT_IN_SPINE = TextKey.of("jsspace.ship_design.not_in_spine",
            "%s goes on the hull, not in the spine");
    // By the mouse over a part of the ship: its maker, and what a click with the tool does to it.
    public static final TextKey MAKER_AND_CLICK = TextKey.of("jsspace.ship_design.maker_and_click", "%s - %s");
    public static final TextKey CLICK_PICK = TextKey.of("jsspace.ship_design.click_pick", "click to pick it up");
    public static final TextKey CLICK_REMOVE = TextKey.of("jsspace.ship_design.click_remove",
            "click to take it off");
    public static final TextKey CLICK_FLIP = TextKey.of("jsspace.ship_design.click_flip", "click to flip it");
    public static final TextKey CLICK_FINISH = TextKey.of("jsspace.ship_design.click_finish",
            "click to change the finish");
    public static final TextKey SPAN = TextKey.of("jsspace.ship_design.span", "Span");

    private ShipDesignTexts() {
    }
}
