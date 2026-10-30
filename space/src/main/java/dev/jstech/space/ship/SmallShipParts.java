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
import dev.jstech.space.JsSpace;
import dev.jstech.space.api.ShipClass;
import dev.jstech.space.api.ShipPart;
import dev.jstech.space.api.ShipPartKind;
import dev.jstech.space.api.ShipParts;
import dev.jstech.space.api.ShipPlace;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/**
 * The small starship's catalogue of 25 parts in ten kinds, the base the player builds a ship of: every fuselage section
 * ends in the class's standard joint, so any cockpit fits any module and any module any engine. Three cockpits; the
 * airlock, docking and corridor hull modules (the docking module's dorsal collar is an airlock too); three holds of the
 * medium size, one of them opening its ramp at the tail; two fusion reactors; a twin-bell and a four-bell tail engine
 * and a magnetic nozzle in a nacelle; side intakes and a nacelle intake, both turning their nozzles down for a vertical
 * take-off; three wings, the stub wings carrying the nacelles; two fins; two thrusters; wheels and legs. The lengths
 * along the spine are those of the parts on the block grid.
 */
@TextHolder
public final class SmallShipParts {

    public static final TextKey ORBITER_COCKPIT = TextKey.of("jsspace.ship_part.orbiter_cockpit",
            "Orbiter Cockpit");
    public static final TextKey CAB_COCKPIT = TextKey.of("jsspace.ship_part.cab_cockpit", "Cab Cockpit");
    public static final TextKey GREENHOUSE_COCKPIT = TextKey.of("jsspace.ship_part.greenhouse_cockpit",
            "Greenhouse Cockpit");
    public static final TextKey AIRLOCK = TextKey.of("jsspace.ship_part.airlock", "Airlock");
    public static final TextKey DOCKING = TextKey.of("jsspace.ship_part.docking", "Docking Module");
    public static final TextKey CORRIDOR = TextKey.of("jsspace.ship_part.corridor", "Corridor");
    public static final TextKey BAY = TextKey.of("jsspace.ship_part.bay", "Bay Doors Hold");
    public static final TextKey RAMP = TextKey.of("jsspace.ship_part.ramp", "Ramp Hold");
    public static final TextKey SIDE_DOOR = TextKey.of("jsspace.ship_part.side_door", "Side Door Hold");
    public static final TextKey REACTOR_FLUSH = TextKey.of("jsspace.ship_part.reactor_flush",
            "Reactor, Flush Radiators");
    public static final TextKey REACTOR_FINS = TextKey.of("jsspace.ship_part.reactor_fins",
            "Reactor, Radiator Fins");
    public static final TextKey TWIN_BELLS = TextKey.of("jsspace.ship_part.twin_bells", "Twin Bells");
    public static final TextKey CLUSTER = TextKey.of("jsspace.ship_part.cluster", "Cluster");
    public static final TextKey MAGNETIC_NOZZLE = TextKey.of("jsspace.ship_part.magnetic_nozzle",
            "Magnetic Nozzle");
    public static final TextKey SIDE_INTAKES = TextKey.of("jsspace.ship_part.side_intakes", "Side Intakes");
    public static final TextKey NACELLE_INTAKE = TextKey.of("jsspace.ship_part.nacelle_intake", "Nacelle Intake");
    public static final TextKey BLENDED_DELTA = TextKey.of("jsspace.ship_part.blended_delta", "Blended Delta");
    public static final TextKey STUB_WINGS = TextKey.of("jsspace.ship_part.stub_wings", "Stub Wings");
    public static final TextKey SWEPT_WINGS = TextKey.of("jsspace.ship_part.swept_wings", "Swept Wings");
    public static final TextKey TWIN_CANTED_FINS = TextKey.of("jsspace.ship_part.twin_canted_fins",
            "Twin Canted Fins");
    public static final TextKey SINGLE_FIN = TextKey.of("jsspace.ship_part.single_fin", "Single Fin");
    public static final TextKey BLISTERS = TextKey.of("jsspace.ship_part.blisters", "Thruster Blisters");
    public static final TextKey QUADS = TextKey.of("jsspace.ship_part.quads", "Thruster Quads");
    public static final TextKey WHEELS = TextKey.of("jsspace.ship_part.wheels", "Wheels");
    public static final TextKey LEGS = TextKey.of("jsspace.ship_part.legs", "Legs");
    /* The room a medium module takes in the hull: all of a small ship's. */
    private static final int MEDIUM = 3;

    private SmallShipParts() {
    }

    /**
     * Declares the parts, once, from the mod's constructor: each of the maker of its family in the catalogue, the
     * orbiter line Meridian Orbital's, the working line Ironside Yards', the enamel line Kosmozavod's.
     */
    public static void declare() {
        final ResourceLocation meridian = SmallShipMakers.MERIDIAN_ORBITAL;
        final ResourceLocation ironside = SmallShipMakers.IRONSIDE_YARDS;
        final ResourceLocation kosmozavod = SmallShipMakers.KOSMOZAVOD_ID;
        spine("small_orbiter_cockpit", ORBITER_COCKPIT, ShipPartKind.COCKPIT, ShipPlace.NOSE, 5, 0, false, meridian);
        spine("small_cab_cockpit", CAB_COCKPIT, ShipPartKind.COCKPIT, ShipPlace.NOSE, 5, 0, false, ironside);
        spine("small_greenhouse_cockpit", GREENHOUSE_COCKPIT, ShipPartKind.COCKPIT, ShipPlace.NOSE, 5, 0, false,
                kosmozavod);
        spine("small_airlock", AIRLOCK, ShipPartKind.HULL, ShipPlace.SPINE, 3, 0, true, meridian);
        spine("small_docking", DOCKING, ShipPartKind.HULL, ShipPlace.SPINE, 3, 0, true, kosmozavod);
        spine("small_corridor", CORRIDOR, ShipPartKind.HULL, ShipPlace.SPINE, 2, 0, false, ironside);
        spine("small_bay", BAY, ShipPartKind.HOLD, ShipPlace.SPINE, 5, MEDIUM, false, meridian);
        spine("small_ramp", RAMP, ShipPartKind.HOLD, ShipPlace.TAIL, 5, MEDIUM, false, ironside);
        spine("small_side_door", SIDE_DOOR, ShipPartKind.HOLD, ShipPlace.SPINE, 5, MEDIUM, false, kosmozavod);
        spine("small_reactor_flush", REACTOR_FLUSH, ShipPartKind.REACTOR, ShipPlace.SPINE, 2, 0, false, meridian);
        spine("small_reactor_fins", REACTOR_FINS, ShipPartKind.REACTOR, ShipPlace.SPINE, 2, 0, false, ironside);
        hung("small_twin_bells", TWIN_BELLS, ShipPartKind.MAIN_ENGINE, ShipPlace.SPINE_END, false, meridian);
        hung("small_cluster", CLUSTER, ShipPartKind.MAIN_ENGINE, ShipPlace.SPINE_END, false, kosmozavod);
        hung("small_magnetic_nozzle", MAGNETIC_NOZZLE, ShipPartKind.MAIN_ENGINE, ShipPlace.ON_WINGS, false,
                ironside);
        hung("small_side_intakes", SIDE_INTAKES, ShipPartKind.AIR_ENGINE, ShipPlace.SIDES, false, meridian);
        hung("small_nacelle_intake", NACELLE_INTAKE, ShipPartKind.AIR_ENGINE, ShipPlace.ON_WINGS, false, ironside);
        hung("small_blended_delta", BLENDED_DELTA, ShipPartKind.WINGS, ShipPlace.WING_ROOT, false, meridian);
        hung("small_stub_wings", STUB_WINGS, ShipPartKind.WINGS, ShipPlace.WING_ROOT, true, ironside);
        hung("small_swept_wings", SWEPT_WINGS, ShipPartKind.WINGS, ShipPlace.WING_ROOT, false, kosmozavod);
        hung("small_twin_canted_fins", TWIN_CANTED_FINS, ShipPartKind.FINS, ShipPlace.SPINE_END, false, meridian);
        hung("small_single_fin", SINGLE_FIN, ShipPartKind.FINS, ShipPlace.SPINE_END, false, kosmozavod);
        hung("small_blisters", BLISTERS, ShipPartKind.THRUSTERS, ShipPlace.AROUND, false, meridian);
        hung("small_quads", QUADS, ShipPartKind.THRUSTERS, ShipPlace.AROUND, false, ironside);
        hung("small_wheels", WHEELS, ShipPartKind.GEAR, ShipPlace.UNDER, false, meridian);
        hung("small_legs", LEGS, ShipPartKind.GEAR, ShipPlace.UNDER, false, ironside);
    }

    /** The id of the catalogue's part {@code path}. */
    public static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, path);
    }

    private static void spine(final String path, final TextKey name, final ShipPartKind kind, final ShipPlace place,
                              final int length, final int room, final boolean airlock, final ResourceLocation maker) {
        ShipParts.declare(id(path), new ShipPart(name.key(), kind, ShipClass.SMALL, place, length, room, airlock,
                false, Optional.of(maker)));
    }

    private static void hung(final String path, final TextKey name, final ShipPartKind kind, final ShipPlace place,
                             final boolean carries, final ResourceLocation maker) {
        ShipParts.declare(id(path), new ShipPart(name.key(), kind, ShipClass.SMALL, place, 0, 0, false, carries,
                Optional.of(maker)));
    }
}
